package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.audio.OnlineGuard
import com.example.diagnostics.AppDiagnosticLog
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.model.AudioPickerTarget
import com.example.model.AuthSession
import com.example.model.EffectType
import com.example.model.StudioRoute
import com.example.studio.ui.applyDisplayBoost
import com.example.studio.ui.lit
import com.example.ui.components.PersistentTransportBar
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SonixLuxuryLaunchOverlay
import com.example.BuildConfig
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AiToolDetailScreen
import com.example.ui.screens.AiVoiceStudioScreen
import com.example.ui.screens.AudioEditorScreen
import com.example.ui.screens.AudioMergeScreen
import com.example.ui.screens.AudioSplitScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BatchQueueScreen
import com.example.ui.screens.CompressAudioScreen
import com.example.ui.screens.DeveloperPanelScreen
import com.example.ui.screens.EchoDetailScreen
import com.example.ui.screens.EchoReverbScreen
import com.example.ui.screens.EffectDetailScreen
import com.example.ui.screens.EffectsChainScreen
import com.example.ui.screens.ExtractVideoAudioScreen
import com.example.ui.screens.FormatConverterScreen
import com.example.ui.screens.GenerateSilenceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InAppAudioPickerScreen
import com.example.ui.screens.NotificationDetailScreen
import com.example.ui.screens.NotificationsListScreen
import com.example.ui.screens.PitchAndSpeedScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.ProjectsExportScreen
import com.example.ui.screens.RecordingStudioScreen
import com.example.ui.screens.ReelsAudioTemplatesScreen
import com.example.ui.screens.ReverseAudioScreen
import com.example.ui.screens.RingtoneMakerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartAssistantScreen
import com.example.ui.screens.SmartMusicMixerScreen
import com.example.ui.screens.SpeechToTextScreen
import com.example.ui.screens.SurroundStereoScreen
import com.example.ui.screens.VoicePurifierScreen
import com.example.ui.screens.VoiceChangerScreen
import com.example.ui.theme.SonixTheme
import com.example.ui.theme.StudioColors
import com.example.viewmodel.StudioSideEffect
import com.example.viewmodel.StudioViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private class CreateTypedDocumentContract : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent {
        val (mimeType, suggestedName) = input
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) intent?.data else null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDisplayBoost()

        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                SonixCrashGuard.handleUncaughtException(applicationContext, thread, throwable)
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "Failed to register uncaught exception handler", t)
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:71794555883:android:4c65036e94f7e36e3b37ae")
                    .setApiKey("AIzaSyAewWAxeUSQFEtUoMGuVFCSa5qHbmcYGoo")
                    .setProjectId("gen-lang-client-0021883952")
                    .setStorageBucket("gen-lang-client-0021883952.firebasestorage.app")
                    .setGcmSenderId("71794555883")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Manual FirebaseApp initialization skipped safely: ${e.message}")
        }

        initializeFirebaseSafely()
        runFirebaseStartupDiagnostic()

        try {
            AppDiagnosticLog.init(this)
            val askRestartConsent = intent?.getBooleanExtra(SonixCrashGuard.EXTRA_ASK_RESTART_CONSENT, false) == true
            val diagId = intent?.getStringExtra(SonixCrashGuard.EXTRA_DIAGNOSTIC_ID)
            val crashMsg = intent?.getStringExtra(SonixCrashGuard.EXTRA_CRASH_MESSAGE)
            val pendingDiagId = SonixCrashGuard.consumePendingCrashDiagnostics(this)
            if (askRestartConsent) {
                val resolvedDiagId = diagId?.takeIf { it.isNotBlank() } ?: pendingDiagId
                if (!resolvedDiagId.isNullOrBlank()) {
                    AppDiagnosticLog.showPromptForEntryId(resolvedDiagId)
                }
            } else if (!crashMsg.isNullOrBlank()) {
                SonixCrashGuard.reportError(
                    StartupErrorReport(
                        threadName = intent.getStringExtra(SonixCrashGuard.EXTRA_CRASH_THREAD) ?: "main",
                        message = crashMsg,
                        stackTrace = intent.getStringExtra(SonixCrashGuard.EXTRA_CRASH_TRACE) ?: crashMsg,
                    ),
                )
            } else if (!pendingDiagId.isNullOrBlank()) {
                AppDiagnosticLog.showPromptForEntryId(pendingDiagId)
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Failed reading crash intent extras", t)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                splashScreen.setOnExitAnimationListener { splashView ->
                    runCatching {
                        splashView.animate()
                            .alpha(0f)
                            .setDuration(240L)
                            .withEndAction { splashView.remove() }
                            .start()
                    }.onFailure {
                        runCatching { splashView.remove() }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Splash exit animation skipped safely: ${t.message}")
        }

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            Log.w("MainActivity", "enableEdgeToEdge failed safely: ${t.message}")
        }

        val studioViewModel: StudioViewModel? = try {
            ViewModelProvider(this)[StudioViewModel::class.java]
        } catch (t: Throwable) {
            Log.e("MainActivity", "StudioViewModel startup initialization failed", t)
            SonixCrashGuard.reportError(Thread.currentThread().name, t)
            null
        }

        try {
            setContent {
                SonixTheme {
                    val activeCrash by SonixCrashGuard.uncaughtError.collectAsStateWithLifecycle()
                    val errorReport = activeCrash
                    if (errorReport != null || studioViewModel == null) {
                        StartupErrorScreen(
                            report = errorReport ?: StartupErrorReport(
                                threadName = "main",
                                message = "Unable to initialize Sonix Studio components.",
                                stackTrace = "StudioViewModel initialization returned null.",
                            ),
                            onRetry = {
                                SonixCrashGuard.clearError()
                                val restartIntent = Intent(this, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                startActivity(restartIntent)
                                finish()
                            },
                        )
                    } else {
                        SonixRootApp(viewModel = studioViewModel)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "setContent failed during startup", t)
            SonixCrashGuard.reportError(Thread.currentThread().name, t)
            runCatching {
                setContent {
                    SonixTheme {
                        StartupErrorScreen(
                            report = StartupErrorReport(
                                threadName = Thread.currentThread().name,
                                message = t.localizedMessage ?: t::class.java.simpleName,
                                stackTrace = Log.getStackTraceString(t),
                            ),
                            onRetry = {
                                SonixCrashGuard.clearError()
                                recreate()
                            },
                        )
                    }
                }
            }
        }

        try {
            window.decorView.post {
                runCatching {
                    window.setBackgroundDrawable(ColorDrawable(0xFF070B14.toInt()))
                }
            }
        } catch (t: Throwable) {
            Log.w("MainActivity", "Background drawable post skipped safely: ${t.message}")
        }
    }

    private fun initializeFirebaseSafely() {
        SonixCrashGuard.safeInitializeFirebase(this)
    }

    private fun runFirebaseStartupDiagnostic() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val app = FirebaseApp.getApps(this@MainActivity).firstOrNull()
                    ?: FirebaseApp.initializeApp(this@MainActivity)
                if (app == null) {
                    Log.w("SonixFirebaseDiag", "MainActivity startup check: FirebaseApp not initialized.")
                    return@launch
                }
                val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
                val authUid = runCatching { auth?.currentUser?.uid }.getOrNull()
                val dbId = runCatching { getString(R.string.firestore_database_id) }
                    .getOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val db = if (dbId != null) {
                    runCatching { FirebaseFirestore.getInstance(dbId) }.getOrNull()
                        ?: FirebaseFirestore.getInstance()
                } else {
                    FirebaseFirestore.getInstance()
                }
                val isOnline = OnlineGuard.isOnline(this@MainActivity)
                Log.i(
                    "SonixFirebaseDiag",
                    "MainActivity Firebase startup check | app=${app.name} | project=${app.options.projectId} | authReady=${auth != null} | uid=${authUid ?: "signed_out"} | db=${dbId ?: "(default)"} | online=$isOnline",
                )

                if (!isOnline) {
                    val cachedSnapshot = runCatching {
                        withTimeoutOrNull(2_000L) {
                            db.collection("app_diagnostics")
                                .document("startup_probe")
                                .get(Source.CACHE)
                                .await()
                        }
                    }.getOrNull()
                    Log.i(
                        "SonixFirebaseDiag",
                        "MainActivity Firestore offline/local cache ready | cachedExists=${cachedSnapshot?.exists() == true}",
                    )
                    return@launch
                }

                runCatching { withTimeoutOrNull(2_000L) { db.enableNetwork().await() } }

                val snapshot = withTimeoutOrNull(6_000L) {
                    db.collection("app_diagnostics")
                        .document("startup_probe")
                        .get(Source.SERVER)
                        .await()
                }
                if (snapshot != null) {
                    val probeValue = snapshot.get("status") ?: snapshot.data
                    Log.i(
                        "SonixFirebaseDiag",
                        "MainActivity Firestore test fetch succeeded | exists=${snapshot.exists()} | value=$probeValue",
                    )
                } else {
                    Log.w(
                        "SonixFirebaseDiag",
                        "MainActivity Firestore test fetch timed out after 6000ms; offline cache active.",
                    )
                }
            } catch (e: FirebaseFirestoreException) {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Log.i(
                            "SonixFirebaseDiag",
                            "MainActivity Firestore reachable (server responded with PERMISSION_DENIED per security rules).",
                        )
                    }
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                        Log.w(
                            "SonixFirebaseDiag",
                            "MainActivity Firestore currently offline/unreachable (${e.code.name}); local persistence cache active.",
                        )
                    }
                    else -> {
                        Log.w(
                            "SonixFirebaseDiag",
                            "MainActivity Firestore diagnostic notice [${e.code}]: ${e.message}",
                        )
                    }
                }
            } catch (t: Throwable) {
                Log.w("SonixFirebaseDiag", "MainActivity Firebase diagnostic skipped: ${t.message}")
            }
        }
    }
}

@Composable
private fun SonixRootApp(
    viewModel: StudioViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lang = uiState.language
    val layoutDirection = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val signedInUid = (uiState.authSession as? AuthSession.SignedIn)?.uid
    val firstLoginPermissions = remember { requiredFirstLoginPermissions() }
    val allFirstLoginPermissionsGranted = remember(signedInUid) {
        firstLoginPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    var firstLoginPermissionsComplete by rememberSaveable(signedInUid) {
        val saved = signedInUid?.let { uid ->
            context.getSharedPreferences("sonix_permissions", Context.MODE_PRIVATE)
                .getBoolean("first_login_complete_$uid", false)
        } ?: false
        mutableStateOf(signedInUid == null || saved || allFirstLoginPermissionsGranted)
    }
    LaunchedEffect(signedInUid, firstLoginPermissionsComplete) {
        if (signedInUid != null && firstLoginPermissionsComplete) {
            context.getSharedPreferences("sonix_permissions", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("first_login_complete_$signedInUid", true)
                .apply()
        }
    }

    // Hoist Home LazyListState at parent composable level outside NavHost/conditionals
    // so scroll position stays intact across any tool navigation and return.
    val homeListState = rememberLazyListState()
    var sleepTimerDialogOpen by remember { mutableStateOf(false) }
    var showLuxuryLaunch by rememberSaveable { mutableStateOf(true) }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateTypedDocumentContract(),
    ) { uri ->
        viewModel.completeSaveExportedDocument(uri)
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        viewModel.onDefaultSaveFolderPicked(treeUri)
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is StudioSideEffect.PromptSaveDocument -> {
                    saveDocumentLauncher.launch(effect.result.mimeType to effect.suggestedName)
                }
                is StudioSideEffect.PromptPickSaveFolderOnce -> {
                    openDocumentTreeLauncher.launch(null)
                }
                is StudioSideEffect.LaunchShareIntent -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = effect.mimeType
                        putExtra(Intent.EXTRA_STREAM, effect.uri)
                        putExtra(Intent.EXTRA_SUBJECT, effect.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val targetPkg = effect.targetPackage
                    if (!targetPkg.isNullOrBlank()) {
                        shareIntent.setPackage(targetPkg)
                        val launched = runCatching {
                            context.startActivity(shareIntent)
                            true
                        }.getOrDefault(false)
                        if (!launched) {
                            shareIntent.setPackage(null)
                            context.startActivity(Intent.createChooser(shareIntent, effect.title))
                        }
                    } else {
                        context.startActivity(Intent.createChooser(shareIntent, effect.title))
                    }
                }
                is StudioSideEffect.LaunchMultiShareIntent -> {
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = effect.mimeType
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(effect.uris))
                        putExtra(Intent.EXTRA_SUBJECT, effect.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, effect.title))
                }
            }
        }
    }

    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                StudioColors.BgTop,
                StudioColors.BgMid,
                StudioColors.BgTop,
            ),
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush),
        ) {
            if (uiState.authSession is AuthSession.SignedOut) {
                AuthScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onLoginSuccess = {
                        viewModel.clearBanner()
                    },
                )
            } else if (signedInUid != null && !firstLoginPermissionsComplete) {
                FirstLoginPermissionGateScreen(
                    language = lang,
                    onComplete = { firstLoginPermissionsComplete = true },
                )
            } else {
                val uidKey = signedInUid ?: "guest"
                key(uidKey) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDest = navBackStackEntry?.destination
                    val isHomeTab = currentDest?.hasRoute<StudioRoute.Home>() == true
                    val isAudioPickerScreen = currentDest?.hasRoute<StudioRoute.AudioPicker>() == true
                    val isLibraryTab = isAudioPickerScreen &&
                        runCatching { navBackStackEntry?.toRoute<StudioRoute.AudioPicker>()?.target }.getOrNull() == AudioPickerTarget.LIBRARY_BROWSE
                    val isProjectsTab = currentDest?.hasRoute<StudioRoute.ProjectsExport>() == true
                    val showBottomNav = isHomeTab || isLibraryTab || isProjectsTab
                    val isChildSubRoute = (currentDest?.hasRoute<StudioRoute.AudioPicker>() == true && !isLibraryTab) ||
                        currentDest?.hasRoute<StudioRoute.EchoDetail>() == true ||
                        currentDest?.hasRoute<StudioRoute.EffectDetail>() == true ||
                        currentDest?.hasRoute<StudioRoute.AiToolDetail>() == true ||
                        currentDest?.hasRoute<StudioRoute.NotificationDetail>() == true

                    val returnToHome: () -> Unit = {
                        if (!navController.popBackStack(StudioRoute.Home, inclusive = false)) {
                            navController.navigate(StudioRoute.Home) {
                                launchSingleTop = true
                            }
                        }
                    }
                    val popBackOrHome: () -> Unit = {
                        if (!navController.popBackStack()) {
                            navController.navigate(StudioRoute.Home) {
                                launchSingleTop = true
                            }
                        }
                    }

                    BackHandler(enabled = currentDest != null && !isHomeTab && !isChildSubRoute) {
                        returnToHome()
                    }

                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            PersistentTransportBar(
                                uiState = uiState,
                                playbackFlow = viewModel.playbackTelemetry,
                                onPlayPause = {
                                    try {
                                        viewModel.togglePlayPause()
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Play/Pause action failed safely", e)
                                    }
                                },
                                onStop = { viewModel.stopPlayback() },
                                onSeekFraction = { viewModel.seekToFraction(it) },
                                onToggleAutoLoop = { viewModel.setAutoLoopOnPlayback(!uiState.autoLoopOnPlayback) },
                                onToggleBypass = { viewModel.toggleBypass() },
                                onHoldBypassChange = { viewModel.setHoldBypass(it) },
                                onOpenSleepTimer = { sleepTimerDialogOpen = true },
                                includeNavBarsPadding = !showBottomNav,
                            )

                            if (showBottomNav) {
                                NavigationBar(
                                    containerColor = StudioColors.CardSurface,
                                    contentColor = StudioColors.TextPrimary,
                                    modifier = Modifier.navigationBarsPadding(),
                                ) {
                                    val itemColors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = StudioColors.BgTop,
                                        selectedTextColor = StudioColors.Cyan,
                                        indicatorColor = StudioColors.Cyan,
                                        unselectedIconColor = StudioColors.TextSecondary,
                                        unselectedTextColor = StudioColors.TextSecondary,
                                    )

                                    val toolsInteraction = remember { MutableInteractionSource() }
                                    NavigationBarItem(
                                        selected = isHomeTab,
                                        onClick = {
                                            navController.navigate(StudioRoute.Home) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        interactionSource = toolsInteraction,
                                        icon = { Icon(Icons.Rounded.Tune, contentDescription = t(lang, "nav_tools")) },
                                        label = {
                                            Text(
                                                text = t(lang, "nav_tools"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        colors = itemColors,
                                        modifier = Modifier
                                            .lit(toolsInteraction, MaterialTheme.colorScheme.primary, selected = isHomeTab, cornerRadius = 16.dp)
                                            .testTag("bottom_nav_tools"),
                                    )

                                    val libraryInteraction = remember { MutableInteractionSource() }
                                    NavigationBarItem(
                                        selected = isLibraryTab,
                                        onClick = {
                                            navController.navigate(StudioRoute.AudioPicker(AudioPickerTarget.LIBRARY_BROWSE)) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        interactionSource = libraryInteraction,
                                        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = t(lang, "nav_library")) },
                                        label = {
                                            Text(
                                                text = t(lang, "nav_library"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        colors = itemColors,
                                        modifier = Modifier
                                            .lit(libraryInteraction, MaterialTheme.colorScheme.primary, selected = isLibraryTab, cornerRadius = 16.dp)
                                            .testTag("bottom_nav_library"),
                                    )

                                    val projectsInteraction = remember { MutableInteractionSource() }
                                    NavigationBarItem(
                                        selected = isProjectsTab,
                                        onClick = {
                                            navController.navigate(StudioRoute.ProjectsExport) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        interactionSource = projectsInteraction,
                                        icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = t(lang, "nav_projects")) },
                                        label = {
                                            Text(
                                                text = t(lang, "nav_projects"),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        colors = itemColors,
                                        modifier = Modifier
                                            .lit(projectsInteraction, MaterialTheme.colorScheme.primary, selected = isProjectsTab, cornerRadius = 16.dp)
                                            .testTag("bottom_nav_projects"),
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .consumeWindowInsets(innerPadding),
                    ) {

                        if (!isAudioPickerScreen) {
                            uiState.importProgress?.let { progress ->
                            val byteTransferActive = !uiState.importBytesLabel.isNullOrBlank()
                            val byteFraction = uiState.importBytesProgress
                            val pct = (progress.coerceIn(0f, 1f) * 100f).toInt()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = StudioColors.CardSurface,
                                    border = BorderStroke(1.dp, StudioColors.Violet.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 640.dp)
                                        .testTag("global_import_progress_card"),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val stageLabel = if (progress >= 0.50f && !uiState.importLabel.isNullOrBlank()) {
                                                uiState.importLabel
                                            } else {
                                                t(lang, "importing_file_progress")
                                            }
                                            Text(
                                                text = if (byteTransferActive && byteFraction == null) {
                                                    stageLabel.orEmpty()
                                                } else {
                                                    "$stageLabel $pct%"
                                                },
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = StudioColors.Cyan,
                                                modifier = Modifier.weight(1f),
                                            )
                                            val detailText = uiState.importBytesLabel.orEmpty()
                                            if (detailText.isNotBlank()) {
                                                Text(
                                                    text = detailText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = StudioColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(max = 200.dp),
                                                )
                                            }
                                        }
                                        if (byteTransferActive && byteFraction == null) {
                                            LinearProgressIndicator(
                                                color = StudioColors.Cyan,
                                                trackColor = StudioColors.BgTop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .testTag("global_import_progress_bar"),
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                progress = { progress.coerceIn(0f, 1f) },
                                                color = StudioColors.Cyan,
                                                trackColor = StudioColors.BgTop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .testTag("global_import_progress_bar"),
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = StudioRoute.Home,
                            modifier = Modifier.weight(1f),
                        ) {
                            composable<StudioRoute.Home> {
                                HomeScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    listState = homeListState,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenNotifications = {
                                        navController.navigate(StudioRoute.Notifications) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onRestoreRecentEdit = { entry ->
                                        viewModel.restoreRecentEditSession(entry) { route ->
                                            navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                )
                            }
                            composable<StudioRoute.AudioPicker> { entry ->
                                val dest = entry.toRoute<StudioRoute.AudioPicker>()
                                val isBrowseTab = dest.target == AudioPickerTarget.LIBRARY_BROWSE
                                InAppAudioPickerScreen(
                                    target = dest.target,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    showBackButton = !isBrowseTab,
                                    onPickedComplete = {
                                        if (!isBrowseTab) {
                                            navController.popBackStack()
                                        }
                                    },
                                    onRouteToExtractVideo = {
                                        navController.navigate(StudioRoute.ExtractVideoAudio) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onBack = {
                                        if (isBrowseTab) returnToHome() else popBackOrHome()
                                    },
                                )
                            }
                            composable<StudioRoute.Record> {
                                RecordingStudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenAudioPicker = {
                                        navController.navigate(StudioRoute.AudioPicker(AudioPickerTarget.IMPORT_MAIN))
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.VoicePurifier> {
                                VoicePurifierScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.EchoSpace> {
                                EchoReverbScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenDetail = { grp -> navController.navigate(StudioRoute.EchoDetail(grp)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.EchoDetail> { entry ->
                                val dest = entry.toRoute<StudioRoute.EchoDetail>()
                                EchoDetailScreen(
                                    group = dest.group,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = popBackOrHome,
                                )
                            }
                            composable<StudioRoute.EffectsChain> {
                                EffectsChainScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenEffect = { eff -> navController.navigate(StudioRoute.EffectDetail(eff)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.EffectDetail> { entry ->
                                val dest = entry.toRoute<StudioRoute.EffectDetail>()
                                EffectDetailScreen(
                                    effect = dest.effect,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenEchoDetail = { grp -> navController.navigate(StudioRoute.EchoDetail(grp)) },
                                    onBack = popBackOrHome,
                                )
                            }
                            composable<StudioRoute.Editor> {
                                AudioEditorScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target ->
                                        navController.navigate(StudioRoute.AudioPicker(target))
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.Presets> {
                                PresetsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenEchoSpace = { navController.navigate(StudioRoute.EchoSpace) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.ProjectsExport> {
                                ProjectsExportScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.AiStudio> {
                                AiStudioScreen(
                                    uiState = uiState,
                                    onOpenTool = { tool -> navController.navigate(StudioRoute.AiToolDetail(tool)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.AiToolDetail> { entry ->
                                val dest = entry.toRoute<StudioRoute.AiToolDetail>()
                                AiToolDetailScreen(
                                    tool = dest.tool,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = popBackOrHome,
                                )
                            }
                            composable<StudioRoute.AiVoiceStudio> {
                                AiVoiceStudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.VoiceChanger> {
                                VoiceChangerScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.Settings> {
                                SettingsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onChangeDefaultSaveFolder = {
                                        openDocumentTreeLauncher.launch(null)
                                    },
                                    onOpenDeveloperPanel = {
                                        if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                                            navController.navigate(StudioRoute.DeveloperPanel) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.AudioMerge> {
                                AudioMergeScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.AudioSplit> {
                                AudioSplitScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.PitchAndSpeed> {
                                PitchAndSpeedScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.ReverseAudio> {
                                ReverseAudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.SurroundStereo> {
                                SurroundStereoScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.GenerateSilence> {
                                GenerateSilenceScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.FormatConverter> {
                                FormatConverterScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.ExtractVideoAudio> {
                                ExtractVideoAudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.CompressAudio> {
                                CompressAudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.RingtoneMaker> {
                                RingtoneMakerScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.SpeechToText> {
                                SpeechToTextScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.SmartMusicMixer> {
                                SmartMusicMixerScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenPicker = { target -> navController.navigate(StudioRoute.AudioPicker(target)) },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.SmartAssistant> {
                                SmartAssistantScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onNavigateToTool = { toolId ->
                                        val dest: StudioRoute = when (toolId) {
                                            "noise_reduction" -> StudioRoute.VoicePurifier
                                            "voice_changer" -> StudioRoute.VoiceChanger
                                            "reels_audio_templates" -> StudioRoute.ReelsAudioTemplates
                                            "extract_video" -> StudioRoute.ExtractVideoAudio
                                            "surround_stereo" -> StudioRoute.SurroundStereo
                                            "equalizer" -> StudioRoute.EffectDetail(EffectType.EQUALIZER)
                                            "pitch_speed" -> StudioRoute.PitchAndSpeed
                                            "smart_mixer" -> StudioRoute.SmartMusicMixer
                                            "audio_editor" -> StudioRoute.Editor
                                            "presets_echo" -> StudioRoute.Presets
                                            else -> StudioRoute.Home
                                        }
                                        navController.navigate(dest) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.ReelsAudioTemplates> {
                                ReelsAudioTemplatesScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenAudioPicker = { target ->
                                        navController.navigate(StudioRoute.AudioPicker(target))
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.Notifications> {
                                NotificationsListScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenDetail = { entryId ->
                                        navController.navigate(StudioRoute.NotificationDetail(entryId))
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.NotificationDetail> { entry ->
                                val dest = entry.toRoute<StudioRoute.NotificationDetail>()
                                NotificationDetailScreen(
                                    entryId = dest.entryId,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onBack = popBackOrHome,
                                )
                            }
                            composable<StudioRoute.BatchQueue> {
                                BatchQueueScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onOpenStudioEditor = {
                                        navController.navigate(StudioRoute.Editor) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onBack = returnToHome,
                                )
                            }
                            composable<StudioRoute.DeveloperPanel> {
                                if (BuildConfig.DEVELOPER_PANEL_ENABLED) {
                                    DeveloperPanelScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onBack = returnToHome,
                                    )
                                } else {
                                    LaunchedEffect(Unit) { returnToHome() }
                                }
                            }
                        }
                    }
                }

                    SleepTimerDialog(
                        open = sleepTimerDialogOpen,
                        language = lang,
                        selectedOption = uiState.sleepTimerOption,
                        remainingSec = uiState.sleepTimerRemainingSec,
                        onSelect = { viewModel.setSleepTimer(it) },
                        onDismiss = { sleepTimerDialogOpen = false },
                    )
                }
            }

            // Floating top-center toast notification overlay above all controls, scrolling, and keyboard
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .zIndex(20f),
                contentAlignment = Alignment.TopCenter,
            ) {
                AnimatedVisibility(
                    visible = !uiState.statusBanner.isNullOrBlank(),
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                ) {
                    val banner = uiState.statusBanner.orEmpty()
                    val repeatBadge = if (uiState.statusBannerRepeatCount > 1) " ×${uiState.statusBannerRepeatCount}" else ""
                    val isErr = banner.contains("تعذر") || banner.contains("خطأ") ||
                        banner.contains("فشل") || banner.contains("failed", ignoreCase = true) ||
                        banner.contains("error", ignoreCase = true)
                    val isWait = banner.contains("جاري") || banner.contains("انتظار") ||
                        banner.contains("تحميل") || banner.contains("Generating", ignoreCase = true) ||
                        banner.contains("Loading", ignoreCase = true)
                    val isSuccess = banner.contains("نجاح") || banner.contains("تم ") ||
                        banner.contains("جاهز") || banner.contains("success", ignoreCase = true)

                    val accentColor = when {
                        isErr -> StudioColors.RoseBright
                        isWait -> StudioColors.Amber
                        isSuccess -> StudioColors.Emerald
                        else -> StudioColors.Cyan
                    }
                    val statusIcon = when {
                        isErr -> Icons.Rounded.ErrorOutline
                        isWait -> Icons.Rounded.HourglassTop
                        isSuccess -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.Info
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = StudioColors.ElevatedSurface.copy(alpha = 0.97f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .widthIn(min = 220.dp, max = 520.dp)
                            .testTag("floating_top_status_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = banner + repeatBadge,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StudioColors.TextPrimary,
                                textAlign = TextAlign.Start,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            val dismissInteraction = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { viewModel.clearBanner() },
                                interactionSource = dismissInteraction,
                                modifier = Modifier
                                    .size(48.dp)
                                    .lit(dismissInteraction, accentColor, cornerRadius = 24.dp)
                                    .testTag("dismiss_banner_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = StudioColors.TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isExporting || uiState.exportComplete) {
                ExportProgressDialog(
                    language = lang,
                    progress = uiState.exportProgress ?: 0f,
                    fileName = uiState.exportFileName,
                    saveLocation = uiState.exportSaveLocation,
                    complete = uiState.exportComplete,
                    onDismiss = { viewModel.dismissExportStatus() },
                    onCancel = { viewModel.cancelExport() },
                )
            }

            val criticalPrompt by AppDiagnosticLog.criticalRestartPrompt.collectAsStateWithLifecycle()
            criticalPrompt?.let { promptEntry ->
                AlertDialog(
                    onDismissRequest = { AppDiagnosticLog.dismissCriticalPrompt() },
                    containerColor = StudioColors.CardSurface,
                    titleContentColor = StudioColors.TextPrimary,
                    textContentColor = StudioColors.TextSecondary,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = StudioColors.RoseBright,
                            )
                            Text(
                                text = t(lang, "crash_dialog_title"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "${t(lang, "crash_dialog_body")}\n\n${AppDiagnosticLog.userFacingCause(promptEntry, lang)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                AppDiagnosticLog.dismissCriticalPrompt()
                                val restartIntent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                context.startActivity(restartIntent)
                                (context as? Activity)?.finish()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.RoseBright,
                                contentColor = StudioColors.BgTop,
                            ),
                            modifier = Modifier.testTag("crash_restart_now_button"),
                        ) {
                            Text(t(lang, "crash_dialog_restart_now"))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { AppDiagnosticLog.dismissCriticalPrompt() },
                            modifier = Modifier.testTag("crash_stay_retry_button"),
                        ) {
                            Text(
                                text = t(lang, "crash_dialog_stay_retry"),
                                color = StudioColors.Cyan,
                            )
                        }
                    },
                )
            }

            SonixLuxuryLaunchOverlay(
                visible = showLuxuryLaunch,
                language = lang,
                onFinished = { showLuxuryLaunch = false },
            )
        }
    }
}

@Composable
private fun ExportProgressDialog(
    language: AppLanguage,
    progress: Float,
    fileName: String?,
    saveLocation: String?,
    complete: Boolean,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    val isEnglish = language == AppLanguage.EN
    val boundedProgress = progress.coerceIn(0f, 1f)
    AlertDialog(
        onDismissRequest = { if (complete) onDismiss() },
        containerColor = StudioColors.CardSurface,
        title = {
            Text(
                text = if (complete) {
                    if (isEnglish) "Export complete" else "اكتمل حفظ الملف"
                } else {
                    if (isEnglish) "Saving audio file..." else "جارٍ حفظ وتصدير الملف الصوتي..."
                },
                color = if (complete) StudioColors.EmeraldBright else StudioColors.Cyan,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = boundedProgress,
                    color = if (complete) StudioColors.EmeraldBright else StudioColors.Cyan,
                    trackColor = StudioColors.ElevatedSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(boundedProgress * 100f).toInt()}%",
                    color = StudioColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                fileName?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = if (isEnglish) "File: $it" else "الملف: $it",
                        color = StudioColors.TextPrimary,
                    )
                }
                saveLocation?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = if (isEnglish) "Saved to: $it" else "مكان الحفظ: $it",
                        color = StudioColors.TextSecondary,
                    )
                }
            }
        },
        confirmButton = {
            if (complete) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioColors.EmeraldBright,
                        contentColor = StudioColors.BgTop,
                    ),
                ) {
                    Text(if (isEnglish) "Done" else "تم")
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, StudioColors.RoseBright.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        if (isEnglish) "Cancel" else "إلغاء",
                        color = StudioColors.RoseBright,
                    )
                }
            }
        },
    )
}

private fun requiredFirstLoginPermissions(): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_AUDIO)
        add(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

@Composable
private fun FirstLoginPermissionGateScreen(
    language: AppLanguage,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val requiredPermissions = remember { requiredFirstLoginPermissions() }
    var requestDenied by remember { mutableStateOf(false) }
    val isEnglish = language == AppLanguage.EN

    fun allPermissionsGranted(): Boolean = requiredPermissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (allPermissionsGranted()) {
            onComplete()
        } else {
            requestDenied = true
        }
    }

    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.BgTop)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Cyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (isEnglish) "Required permissions" else "الأذونات المطلوبة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                    )
                    Text(
                        text = if (isEnglish) {
                            "To continue, allow the permissions used by recording, audio import, and processing notifications."
                        } else {
                            "للمتابعة، يرجى السماح بالأذونات التي يحتاجها التسجيل واستيراد الملفات الصوتية وتنبيهات اكتمال المعالجة."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextSecondary,
                    )
                    PermissionReasonRow(
                        title = if (isEnglish) "Microphone" else "الميكروفون",
                        description = if (isEnglish) {
                            "For live audio recording when you choose to record."
                        } else {
                            "للتسجيل الصوتي المباشر عند اختيارك التسجيل."
                        },
                    )
                    PermissionReasonRow(
                        title = if (isEnglish) "Audio files" else "الملفات الصوتية",
                        description = if (isEnglish) {
                            "To browse and import audio files you select."
                        } else {
                            "لتصفح واستيراد الملفات الصوتية التي تختارها."
                        },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionReasonRow(
                            title = if (isEnglish) "Notifications" else "الإشعارات",
                            description = if (isEnglish) {
                                "To notify you when audio processing finishes."
                            } else {
                                "لإعلامك عند اكتمال معالجة الصوت."
                            },
                        )
                    }

                    if (requestDenied) {
                        Text(
                            text = if (isEnglish) {
                                "Some permissions are still unavailable. Grant them in system settings, then return and verify."
                            } else {
                                "ما زالت بعض الأذونات غير مفعّلة. فعّلها من إعدادات النظام ثم عُد للتحقق."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioColors.Amber,
                        )
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (isEnglish) "Open permission settings" else "فتح إعدادات الأذونات")
                        }
                        TextButton(
                            onClick = {
                                if (allPermissionsGranted()) {
                                    onComplete()
                                } else {
                                    requestDenied = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (isEnglish) "Verify permissions" else "التحقق من الأذونات")
                        }
                    }

                    Button(
                        onClick = {
                            val missing = requiredPermissions.filter { permission ->
                                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isEmpty()) {
                                onComplete()
                            } else {
                                permissionLauncher.launch(missing.toTypedArray())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.Cyan,
                            contentColor = StudioColors.BgTop,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp),
                    ) {
                        Text(
                            text = if (isEnglish) "Continue and grant permissions" else "المتابعة ومنح الأذونات",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionReasonRow(
    title: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = StudioColors.Cyan)
        Text(description, style = MaterialTheme.typography.bodySmall, color = StudioColors.TextSecondary)
    }
}

@Composable
private fun StartupErrorScreen(
    report: StartupErrorReport,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val bgBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                StudioColors.BgTop,
                StudioColors.BgMid,
                StudioColors.BgTop,
            ),
        )
    }
    val logoBorderBrush = remember {
        Brush.linearGradient(listOf(StudioColors.Cyan, StudioColors.Violet))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFFFFFFE),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = logoBorderBrush,
                ),
                shadowElevation = 10.dp,
                modifier = Modifier.size(72.dp),
            ) {
                Image(
                    bitmap = ImageBitmap.imageResource(R.drawable.sonix_primary_logo),
                    contentDescription = "Sonix ✦",
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = StudioColors.RoseBright,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "تعذر إكمال العملية",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = StudioColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "تم إيقاف العملية لحماية التطبيق. يمكنك إعادة المحاولة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "يمكنك إعادة تشغيل الاستوديو أو نسخ ملخص التنبيه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StudioColors.BgTop,
                        border = BorderStroke(1.dp, StudioColors.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = AppDiagnosticLog.userFacingTechnicalMessage(report.message, AppLanguage.AR),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StudioColors.RoseBright,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val copyInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(
                                        ClipData.newPlainText(
                                            "Sonix Support Summary",
                                            AppDiagnosticLog.userFacingTechnicalMessage(report.message, AppLanguage.AR),
                                        ),
                                    )
                                    copied = true
                                }
                            },
                            interactionSource = copyInteraction,
                            border = BorderStroke(1.dp, StudioColors.BorderBright),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(copyInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("error_copy_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = StudioColors.Cyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (copied) "تم النسخ" else "نسخ الملخص",
                                color = StudioColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        val retryInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = onRetry,
                            interactionSource = retryInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Cyan,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .lit(retryInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("error_retry_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Retry Launch",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

