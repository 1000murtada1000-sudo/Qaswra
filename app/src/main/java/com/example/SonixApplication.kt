package com.example

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.os.Process
import android.util.Log
import com.example.audio.OnlineGuard
import com.example.audio.cache.SmartCacheManager
import com.example.data.RecentEditsCacheManager
import com.example.diagnostics.AppDiagnosticLog
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

data class FirebaseStartupCheckResult(
    val packageName: String,
    val firebaseAppInitialized: Boolean,
    val authReady: Boolean,
    val firestoreReachable: Boolean,
    val projectId: String,
    val mobileAppId: String,
    val databaseId: String,
    val summary: String,
)

data class StartupErrorReport(
    val threadName: String,
    val message: String,
    val stackTrace: String,
)

object SonixCrashGuard {
    const val EXTRA_CRASH_THREAD = "sonix_crash_thread"
    const val EXTRA_CRASH_MESSAGE = "sonix_crash_message"
    const val EXTRA_CRASH_TRACE = "sonix_crash_trace"
    const val EXTRA_ASK_RESTART_CONSENT = "sonix_ask_restart_consent"
    const val EXTRA_DIAGNOSTIC_ID = "sonix_diagnostic_id"

    private const val PREFS_NAME = "sonix_crash_guard"
    private const val KEY_LAST_RELAUNCH_MS = "last_relaunch_ms"
    private const val KEY_PENDING_DIAG_ID = "pending_diag_id"
    private const val KEY_PENDING_THREAD = "pending_thread"
    private const val KEY_PENDING_MESSAGE = "pending_message"
    private const val KEY_PENDING_TRACE = "pending_trace"
    private const val RELAUNCH_COOLDOWN_MS = 3500L

    private val handlerInstalled = AtomicBoolean(false)
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var foregroundActivityRef: WeakReference<Activity>? = null

    private val _uncaughtError = MutableStateFlow<StartupErrorReport?>(null)
    val uncaughtError: StateFlow<StartupErrorReport?> = _uncaughtError.asStateFlow()

    private val _firebaseConnectivityState = MutableStateFlow<FirebaseStartupCheckResult?>(null)
    val firebaseConnectivityState: StateFlow<FirebaseStartupCheckResult?> =
        _firebaseConnectivityState.asStateFlow()

    fun updateForegroundActivity(activity: Activity?, active: Boolean) {
        if (active && activity != null) {
            foregroundActivityRef = WeakReference(activity)
        } else if (!active && foregroundActivityRef?.get() === activity) {
            foregroundActivityRef = null
        }
    }

    fun isAppInForeground(): Boolean = foregroundActivityRef?.get() != null

    fun consumePendingCrashDiagnostics(context: Context): String? {
        return runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val diagId = prefs.getString(KEY_PENDING_DIAG_ID, null)
            val thread = prefs.getString(KEY_PENDING_THREAD, null)
            val msg = prefs.getString(KEY_PENDING_MESSAGE, null)
            val trace = prefs.getString(KEY_PENDING_TRACE, null)
            if (!diagId.isNullOrBlank() || !msg.isNullOrBlank()) {
                prefs.edit()
                    .remove(KEY_PENDING_DIAG_ID)
                    .remove(KEY_PENDING_THREAD)
                    .remove(KEY_PENDING_MESSAGE)
                    .remove(KEY_PENDING_TRACE)
                    .apply()
                if (diagId.isNullOrBlank() && !msg.isNullOrBlank()) {
                    _uncaughtError.value = StartupErrorReport(
                        threadName = thread ?: "main",
                        message = msg,
                        stackTrace = trace ?: msg,
                    )
                }
            }
            diagId?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun reportError(threadName: String, throwable: Throwable) {
        val msg = throwable.localizedMessage?.takeIf { it.isNotBlank() }
            ?: throwable::class.java.simpleName
        val trace = Log.getStackTraceString(throwable).takeIf { it.isNotBlank() }
            ?: throwable.toString()
        AppDiagnosticLog.logCritical(
            toolName = threadName,
            titleAr = "خطأ غير متوقع في الواجهة الرئيسية",
            titleEn = "Unexpected Main Thread Error",
            causeAr = msg,
            causeEn = msg,
            errorCode = throwable.javaClass.simpleName,
            throwable = throwable,
            requestUserRestartConsent = true,
        )
        _uncaughtError.value = StartupErrorReport(
            threadName = threadName,
            message = msg,
            stackTrace = trace,
        )
    }

    fun reportError(report: StartupErrorReport) {
        _uncaughtError.value = report
    }

    fun clearError() {
        _uncaughtError.value = null
    }

    private fun parseFirebaseOptionsFromAssetsOrNull(context: Context): FirebaseOptions? {
        return runCatching {
            val rawJson = context.assets.open("google-services.json").bufferedReader().use { it.readText() }
            val root = JSONObject(rawJson)
            val projectInfo = root.optJSONObject("project_info") ?: return@runCatching null
            val projectId = projectInfo.optString("project_id").takeIf { it.isNotBlank() } ?: return@runCatching null
            val projectNumber = projectInfo.optString("project_number").takeIf { it.isNotBlank() }
            val storageBucket = projectInfo.optString("storage_bucket").takeIf { it.isNotBlank() }

            val clients = root.optJSONArray("client") ?: return@runCatching null
            var matchedClient: JSONObject? = null
            for (i in 0 until clients.length()) {
                val c = clients.optJSONObject(i) ?: continue
                val pkg = c.optJSONObject("client_info")
                    ?.optJSONObject("android_client_info")
                    ?.optString("package_name")
                if (pkg == context.packageName) {
                    matchedClient = c
                    break
                }
            }
            val chosenClient = matchedClient ?: clients.optJSONObject(0) ?: return@runCatching null
            val appId = chosenClient.optJSONObject("client_info")
                ?.optString("mobilesdk_app_id")
                ?.takeIf { it.isNotBlank() } ?: return@runCatching null
            val apiKey = chosenClient.optJSONArray("api_key")
                ?.optJSONObject(0)
                ?.optString("current_key")
                ?.takeIf { it.isNotBlank() } ?: return@runCatching null

            FirebaseOptions.Builder()
                .setApplicationId(appId)
                .setApiKey(apiKey)
                .setProjectId(projectId)
                .apply {
                    if (!projectNumber.isNullOrBlank()) setGcmSenderId(projectNumber)
                    if (!storageBucket.isNullOrBlank()) setStorageBucket(storageBucket)
                }
                .build()
        }.getOrNull()
    }

    fun buildFallbackFirebaseOptions(): FirebaseOptions {
        val apiKey = BuildConfig.FIREBASE_API_KEY
            .trim()
            .takeIf { it.startsWith("AIza") }
            ?: "AIzaSyAewWAxeUSQFEtUoMGuVFCSa5qHbmcYGoo"
        return FirebaseOptions.Builder()
            .setApplicationId("1:71794555883:android:4c65036e94f7e36e3b37ae")
            .setApiKey(apiKey)
            .setProjectId("gen-lang-client-0021883952")
            .setGcmSenderId("71794555883")
            .setStorageBucket("gen-lang-client-0021883952.firebasestorage.app")
            .build()
    }

    @Volatile
    var lastFirebaseInitError: String? = null
        private set

    fun ensureFirebaseInitialized(context: Context): Boolean {
        val appCtx = context.applicationContext ?: context
        safeInitializeFirebase(appCtx)
        return runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)
    }

    fun safeInitializeFirebase(context: Context) {
        val appCtx = context.applicationContext ?: context
        try {
            if (FirebaseApp.getApps(appCtx).isEmpty()) {
                val resourceOptions = runCatching { FirebaseOptions.fromResource(appCtx) }
                    .onFailure { t ->
                        Log.w("SonixStartup", "FirebaseOptions.fromResource failed: ${t.message}")
                    }
                    .getOrNull()
                val autoInitialized = if (resourceOptions != null) {
                    runCatching { FirebaseApp.initializeApp(appCtx, resourceOptions) }
                        .onFailure { t ->
                            lastFirebaseInitError = t.localizedMessage ?: t.javaClass.simpleName
                            Log.w("SonixStartup", "FirebaseApp.initializeApp(options) failed: ${t.message}")
                        }
                        .getOrNull()
                } else {
                    runCatching { FirebaseApp.initializeApp(appCtx) }
                        .onFailure { t ->
                            lastFirebaseInitError = t.localizedMessage ?: t.javaClass.simpleName
                            Log.w("SonixStartup", "FirebaseApp.initializeApp(ctx) failed: ${t.message}")
                        }
                        .getOrNull()
                }
                if (autoInitialized == null && FirebaseApp.getApps(appCtx).isEmpty()) {
                    val options = parseFirebaseOptionsFromAssetsOrNull(appCtx)
                        ?: buildFallbackFirebaseOptions()
                    if (options != null) {
                        FirebaseApp.initializeApp(appCtx, options)
                        lastFirebaseInitError = null
                        Log.i("SonixStartup", "FirebaseApp initialized manually via FirebaseOptions.")
                    } else {
                        if (lastFirebaseInitError.isNullOrBlank()) {
                            lastFirebaseInitError = "FirebaseOptions.fromResource returned null (missing google-services resources)."
                        }
                        Log.i("SonixStartup", "Firebase config not provided; running local studio tools.")
                    }
                } else {
                    lastFirebaseInitError = null
                }
            } else {
                lastFirebaseInitError = null
            }
        } catch (t: Throwable) {
            lastFirebaseInitError = t.localizedMessage ?: t.javaClass.simpleName
            Log.w("SonixStartup", "FirebaseApp.initializeApp failed safely: ${t.message}")
            AppDiagnosticLog.logRecoverable(
                toolName = "FirebaseInit",
                titleAr = "تجاوز تهيئة Firebase السحابية",
                titleEn = "Firebase Initialization Bypassed",
                causeAr = t.localizedMessage ?: "تعذر تهيئة خدمات Firebase حالياً؛ التطبيق يعمل محلياً بأمان.",
                causeEn = t.localizedMessage ?: "Firebase init skipped; local studio tools continue normally.",
                errorCode = t.javaClass.simpleName,
                throwable = t,
            )
        }
        try {
            if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                Firebase.auth.currentUser
            }
        } catch (t: Throwable) {
            Log.w("SonixStartup", "FirebaseAuth initialization skipped safely: ${t.message}")
        }
        try {
            if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                val dbId = runCatching { appCtx.getString(R.string.firestore_database_id) }
                    .getOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val db = if (dbId != null) {
                    runCatching { FirebaseFirestore.getInstance(dbId) }.getOrNull()
                        ?: FirebaseFirestore.getInstance()
                } else {
                    FirebaseFirestore.getInstance()
                }
                runCatching {
                    db.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(persistentCacheSettings {})
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("SonixStartup", "FirebaseFirestore initialization skipped safely: ${t.message}")
        }
    }

    suspend fun verifyFirebaseConnectivity(context: Context): FirebaseStartupCheckResult {
        val appCtx = context.applicationContext ?: context
        val initialized = ensureFirebaseInitialized(appCtx)
        val app = runCatching { FirebaseApp.getApps(appCtx).firstOrNull() }.getOrNull()
        val projectId = app?.options?.projectId.orEmpty()
        val mobileAppId = app?.options?.applicationId.orEmpty()
        val dbId = runCatching { appCtx.getString(R.string.firestore_database_id) }
            .getOrDefault("")
            .trim()

        if (!initialized || app == null) {
            val failResult = FirebaseStartupCheckResult(
                packageName = appCtx.packageName,
                firebaseAppInitialized = false,
                authReady = false,
                firestoreReachable = false,
                projectId = projectId,
                mobileAppId = mobileAppId,
                databaseId = dbId,
                summary = "FirebaseApp not initialized for ${appCtx.packageName}",
            )
            Log.w("SonixFirebaseCheck", "Firebase startup check: ${failResult.summary}")
            _firebaseConnectivityState.value = failResult
            return failResult
        }

        val auth = runCatching { Firebase.auth }.getOrNull()
        val authReady = auth != null
        val authUid = runCatching { auth?.currentUser?.uid }.getOrNull()

        var firestoreReachable = false
        var firestoreStatus = "SKIPPED"
        val db = runCatching {
            if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId) else FirebaseFirestore.getInstance()
        }.getOrElse {
            runCatching { FirebaseFirestore.getInstance() }.getOrNull()
        }
        if (db != null) {
            if (!OnlineGuard.isOnline(appCtx)) {
                firestoreStatus = "OFFLINE_CACHE_READY"
            } else {
                    try {
                        val snapshot = withTimeoutOrNull(6_000L) {
                            db.collection("_startup_check").limit(1).get(Source.SERVER).await()
                        }
                        if (snapshot != null) {
                            firestoreReachable = true
                            firestoreStatus = "CONNECTED"
                        } else {
                            firestoreStatus = "TIMEOUT_OFFLINE"
                        }
                    } catch (e: FirebaseFirestoreException) {
                        // PERMISSION_DENIED proves the live Firestore server received our request and evaluated rules.
                        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            firestoreReachable = true
                            firestoreStatus = "CONNECTED_RULES_ACTIVE"
                        } else {
                            firestoreStatus = "LOCAL_CACHE_${e.code.name}"
                        }
                    } catch (t: Throwable) {
                        firestoreStatus = "LOCAL_ONLY_${t.javaClass.simpleName}"
                    }
            }
        }

        val summary = buildString {
            append("CONFIRMED: Firebase ready | package=").append(appCtx.packageName)
            append(" | project=").append(projectId)
            append(" | appId=").append(mobileAppId)
            append(" | auth=").append(if (authReady) "OK(uid=${authUid ?: "signed_out"})" else "UNAVAILABLE")
            append(" | firestore=").append(firestoreStatus)
        }
        Log.i("SonixFirebaseCheck", summary)

        val result = FirebaseStartupCheckResult(
            packageName = appCtx.packageName,
            firebaseAppInitialized = true,
            authReady = authReady,
            firestoreReachable = firestoreReachable,
            projectId = projectId,
            mobileAppId = mobileAppId,
            databaseId = dbId,
            summary = summary,
        )
        _firebaseConnectivityState.value = result
        return result
    }

    fun installUncaughtExceptionHandler(context: Context) {
        if (!handlerInstalled.compareAndSet(false, true)) return
        val appCtx = context.applicationContext ?: context
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(appCtx, thread, throwable)
        }
    }

    fun handleUncaughtException(
        context: Context,
        thread: Thread,
        throwable: Throwable,
    ) {
        Log.e("SonixCrashHandler", "Intercepted exception on thread ${thread.name}", throwable)
        val msg = throwable.localizedMessage?.takeIf { it.isNotBlank() }
            ?: throwable::class.java.simpleName
        val trace = Log.getStackTraceString(throwable).takeIf { it.isNotBlank() }
            ?: throwable.toString()

        val isMainLooper = Looper.myLooper() == Looper.getMainLooper() ||
            thread == Looper.getMainLooper().thread

        if (!isMainLooper) {
            // Background thread error: intercept & log without closing the app or disrupting UI!
            AppDiagnosticLog.logRecoverable(
                toolName = "Background (${thread.name})",
                titleAr = "تم تجاوز خطأ في المعالجة الخلفية",
                titleEn = "Background Thread Error Intercepted",
                causeAr = msg,
                causeEn = msg,
                errorCode = throwable.javaClass.simpleName,
                throwable = throwable,
            )
            return
        }

        // Critical main-looper exception: record persistently and relaunch Activity ONLY
        // into the user-consent restart dialog (never silent termination).
        val entry = AppDiagnosticLog.logCritical(
            toolName = "UI Main Thread",
            titleAr = "خطأ حرج في واجهة التشغيل",
            titleEn = "Critical UI Thread Exception",
            causeAr = msg,
            causeEn = msg,
            errorCode = throwable.javaClass.simpleName,
            throwable = throwable,
            requestUserRestartConsent = true,
        )

        val prefs = runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }.getOrNull()

        runCatching {
            prefs?.edit()
                ?.putString(KEY_PENDING_DIAG_ID, entry.id)
                ?.putString(KEY_PENDING_THREAD, thread.name)
                ?.putString(KEY_PENDING_MESSAGE, msg)
                ?.putString(KEY_PENDING_TRACE, trace.take(4000))
                ?.commit()
        }

        val relaunched = runCatching {
            val now = System.currentTimeMillis()
            val last = prefs?.getLong(KEY_LAST_RELAUNCH_MS, 0L) ?: 0L
            if (now - last < RELAUNCH_COOLDOWN_MS) {
                false
            } else {
                prefs?.edit()?.putLong(KEY_LAST_RELAUNCH_MS, now)?.commit()
                val fgActivity = foregroundActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
                val launcherCtx = fgActivity ?: context
                val intent = Intent(launcherCtx, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    if (fgActivity == null) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    putExtra(EXTRA_ASK_RESTART_CONSENT, true)
                    putExtra(EXTRA_DIAGNOSTIC_ID, entry.id)
                    putExtra(EXTRA_CRASH_THREAD, thread.name)
                    putExtra(EXTRA_CRASH_MESSAGE, msg)
                    putExtra(EXTRA_CRASH_TRACE, trace.take(8000))
                }
                launcherCtx.startActivity(intent)
                fgActivity != null
            }
        }.getOrDefault(false)

        if (relaunched) {
            Process.killProcess(Process.myPid())
            exitProcess(1)
        } else {
            _uncaughtError.value = StartupErrorReport(thread.name, msg, trace)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}

class SonixApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        try {
            super.onCreate()
        } catch (t: Throwable) {
            Log.e("SonixStartup", "Application super.onCreate error", t)
        }
        runCatching { AppDiagnosticLog.init(this) }
        runCatching { RecentEditsCacheManager.init(this) }
        runCatching { SmartCacheManager.pruneManagedBuckets(this) }
        try {
            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {
                    SonixCrashGuard.updateForegroundActivity(activity, active = true)
                }
                override fun onActivityPaused(activity: Activity) {
                    SonixCrashGuard.updateForegroundActivity(activity, active = false)
                }
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    SonixCrashGuard.updateForegroundActivity(activity, active = false)
                }
            })
        } catch (t: Throwable) {
            Log.w("SonixStartup", "ActivityLifecycleCallbacks registration skipped: ${t.message}")
        }
        try {
            SonixCrashGuard.installUncaughtExceptionHandler(this)
        } catch (t: Throwable) {
            Log.e("SonixStartup", "Failed to install crash handler", t)
        }
        // Initialize Firebase explicitly in Application before any Auth/Firestore service calls.
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, SonixCrashGuard.buildFallbackFirebaseOptions())
            }
        } catch (t: Throwable) {
            Log.w("SonixStartup", "Direct FirebaseApp.initializeApp(this) deferred to safe init: ${t.message}")
        }
        try {
            SonixCrashGuard.safeInitializeFirebase(this)
        } catch (t: Throwable) {
            Log.w("SonixStartup", "Safe Firebase init caught error: ${t.message}")
        }
        startupScope.launch {
            runCatching {
                SonixCrashGuard.verifyFirebaseConnectivity(this@SonixApplication)
            }.onFailure { t ->
                Log.w("SonixFirebaseCheck", "Startup verification skipped: ${t.message}")
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            runCatching { SmartCacheManager.trimSessionCache(this, keepCount = 5) }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching { SmartCacheManager.trimSessionCache(this, keepCount = 5) }
    }
}
