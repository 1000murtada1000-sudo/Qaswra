package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.PrepareGetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.SonixCrashGuard
import com.example.diagnostics.AppDiagnosticLog
import com.example.i18n.t
import com.example.model.AppLanguage
import com.example.studio.ui.lit
import com.example.ui.components.LanguageBadgeButton
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.SonixExitConfirmDialog
import com.example.ui.theme.SonixTheme
import com.example.ui.theme.StudioColors
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private fun resolveGoogleWebClientId(context: Context): String? {
    return runCatching { context.getString(R.string.default_web_client_id) }
        .getOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun isTechnicalAuthRejection(detail: String?): Boolean {
    if (detail.isNullOrBlank()) return false
    val lower = detail.lowercase()
    return lower.contains("reauth") ||
        lower.contains("[16]") ||
        lower.contains("16:") ||
        lower.contains("blocked") ||
        lower.contains("developer") ||
        lower.contains("28444") ||
        lower.contains("signature") ||
        lower.contains("mismatch") ||
        lower.contains("cannot find") ||
        lower.contains("network") ||
        lower.contains("internal error")
}

private fun getAppSigningSha256(context: Context): String? = runCatching {
    val pm = context.packageManager
    val pkgName = context.packageName
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo
        if (signingInfo?.hasMultipleSigners() == true) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo?.signingCertificateHistory
        }
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
    }
    val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching null
    val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
    digest.joinToString(":") { "%02X".format(it) }
}.getOrNull()

@Composable
fun AuthScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onLoginSuccess: () -> Unit = {},
) {
    val lang = uiState.language
    val isEn = lang == AppLanguage.EN
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val googleWebClientId = remember(context) { resolveGoogleWebClientId(context) }
    val signInWithGoogleRequest = remember(googleWebClientId) {
        googleWebClientId?.let { clientId ->
            GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                .build()
        }
    }
    var preparedHandle by remember {
        mutableStateOf<PrepareGetCredentialResponse.PendingGetCredentialHandle?>(null)
    }

    var isSignUp by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var langDialogOpen by remember { mutableStateOf(false) }
    var exitConfirmDialogOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        exitConfirmDialogOpen = true
    }

    fun finishFirebaseGoogleUser(user: FirebaseUser?) {
        isGoogleLoading = false
        if (user != null) {
            val resolvedName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@').orEmpty()
            val resolvedEmail = user.email.orEmpty()
            viewModel.onGoogleLoginSuccess(
                uid = user.uid,
                displayName = resolvedName,
                email = resolvedEmail,
            )
            onLoginSuccess()
        } else {
            errorMsg = if (isEn) {
                "Sign-in completed but no Firebase user session was returned."
            } else {
                "تم التحقق من الحساب ولكن تعذر استرجاع بيانات الجلسة."
            }
        }
    }

    fun formatGoogleAuthFailure(e: Exception): String {
        val causeChain = generateSequence<Throwable>(e) { it.cause }
        val isNetworkError = causeChain.any {
            it is FirebaseNetworkException ||
                (it is FirebaseAuthException && it.errorCode == "ERROR_NETWORK_REQUEST_FAILED")
        }
        return t(
            lang,
            if (isNetworkError) "network_error_friendly" else "google_sign_in_error_friendly",
        )
    }

    fun submitEmailAuth() {
        if (isSubmitting || isGoogleLoading) return
        val cleanEmail = email.trim()
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(cleanEmail)) {
            errorMsg = if (isEn) {
                "Please enter a valid email address."
            } else {
                "يرجى إدخال بريد إلكتروني صحيح."
            }
            return
        }
        if (password.length < 6) {
            errorMsg = if (isEn) {
                "Please enter a password of at least 6 characters."
            } else {
                "يرجى إدخال كلمة مرور صالحة (6 أحرف على الأقل)."
            }
            return
        }
        isSubmitting = true
        errorMsg = null
        scope.launch {
            try {
                val err = if (isSignUp) {
                    viewModel.createAccountWithEmail(displayName, cleanEmail, password)
                } else {
                    viewModel.signInWithEmail(cleanEmail, password)
                }
                if (err == null) {
                    onLoginSuccess()
                } else {
                    errorMsg = err
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    // Restore active Firebase session & pre-warm device Google accounts in parallel on startup
    LaunchedEffect(isPreview, credentialManager, signInWithGoogleRequest) {
        if (isPreview) return@LaunchedEffect
        val initialized = SonixCrashGuard.ensureFirebaseInitialized(context)
        if (initialized) {
            val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
            val existingUser = runCatching { auth?.currentUser }.getOrNull()
            if (existingUser != null) {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(4_000L) {
                        existingUser.getIdToken(false).await()
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.w("Auth", "Silent Firebase token refresh failed; retaining local session.", e)
                }
                finishFirebaseGoogleUser(existingUser)
                return@LaunchedEffect
            }
        }
        // Pre-fetch device Google accounts via CredentialManager.prepareGetCredential (API 34+)
        // so tapping "Sign in with Google" pops up accounts immediately without cold IPC delay.
        if (signInWithGoogleRequest != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            credentialManager != null
        ) {
            val handle = withContext(Dispatchers.IO) {
                runCatching {
                    kotlinx.coroutines.withTimeoutOrNull(4_500L) {
                        credentialManager.prepareGetCredential(signInWithGoogleRequest)
                            .pendingGetCredentialHandle
                    }
                }.getOrNull()
            }
            if (handle != null) {
                preparedHandle = handle
                Log.i("Auth", "Device Google accounts pre-warmed and ready for instant display.")
            }
        }
    }

    AuthScreenContent(
        lang = lang,
        isSignUp = isSignUp,
        displayName = displayName,
        email = email,
        password = password,
        passwordVisible = passwordVisible,
        errorMsg = errorMsg,
        isSubmitting = isSubmitting,
        isGoogleLoading = isGoogleLoading,
        onOpenLanguageDialog = { langDialogOpen = true },
        onSelectMode = { signUp ->
            isSignUp = signUp
            errorMsg = null
        },
        onDisplayNameChange = {
            displayName = it
            errorMsg = null
        },
        onEmailChange = {
            email = it
            errorMsg = null
        },
        onPasswordChange = {
            password = it
            errorMsg = null
        },
        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
        onGoogleSignIn = {
            if (isSubmitting || isGoogleLoading) return@AuthScreenContent
            isGoogleLoading = true
            errorMsg = null

            val request = signInWithGoogleRequest
            if (request == null) {
                isGoogleLoading = false
                errorMsg = if (isEn) {
                    "Google Sign-In is not configured. Verify this app's Firebase configuration and Web OAuth client."
                } else {
                    "تسجيل الدخول عبر Google غير مهيأ. تحقق من إعداد Firebase ومعرّف عميل OAuth للويب."
                }
                return@AuthScreenContent
            }

            val firebaseReady = SonixCrashGuard.ensureFirebaseInitialized(context)
            if (!firebaseReady) {
                isGoogleLoading = false
                val initCause = SonixCrashGuard.lastFirebaseInitError
                AppDiagnosticLog.logRecoverable(
                    toolName = "GoogleSignIn",
                    titleAr = "خدمات Firebase غير مهيأة لتسجيل Google",
                    titleEn = "Firebase Not Initialized for Google Sign-In",
                    causeAr = initCause ?: "FirebaseApp.getApps(context) is empty",
                    causeEn = initCause ?: "FirebaseApp.getApps(context) is empty",
                    errorCode = "FIREBASE_NOT_INITIALIZED",
                )
                errorMsg = if (isEn) {
                    "Firebase is not configured yet for Google Sign-In. You can use Email Sign-In or Guest Mode."
                } else {
                    "خدمات Firebase السحابية غير مهيأة حالياً لتسجيل Google. يمكنك المتابعة بالبريد أو كضيف."
                }
                return@AuthScreenContent
            }

            val activity = context.findActivity()
            if (activity == null) {
                isGoogleLoading = false
                errorMsg = if (isEn) {
                    "Unable to find Activity to launch Google Sign-In."
                } else {
                    "تعذر العثور على نافذة التطبيق لفتح تسجيل الدخول بحساب Google."
                }
                return@AuthScreenContent
            }

            val cm = credentialManager ?: runCatching { CredentialManager.create(activity) }.getOrNull()
            if (cm == null) {
                isGoogleLoading = false
                errorMsg = if (isEn) {
                    "Credential Manager is unavailable on this device."
                } else {
                    "مدير بيانات الاعتماد غير متاح على هذا الجهاز حالياً."
                }
                return@AuthScreenContent
            }

            val auth = runCatching { FirebaseAuth.getInstance() }.getOrElse { e ->
                Log.e("Auth", "FirebaseAuth unavailable", e)
                isGoogleLoading = false
                errorMsg = t(lang, "google_sign_in_error_friendly")
                return@AuthScreenContent
            }

            val activityContextWrapper = MutableContextWrapper(activity)
            val cachedHandle = preparedHandle
            preparedHandle = null

            scope.launch {
                try {
                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && cachedHandle != null) {
                        try {
                            kotlinx.coroutines.withTimeoutOrNull(18_000L) {
                                cm.getCredential(activityContextWrapper, cachedHandle)
                            }
                        } catch (e: GetCredentialCancellationException) {
                            if (!isTechnicalAuthRejection(e.errorMessage?.toString() ?: e.message)) throw e
                            Log.w("Auth", "Prepared Google credential was rejected; retrying with a fresh chooser.")
                            kotlinx.coroutines.withTimeoutOrNull(18_000L) {
                                cm.getCredential(activityContextWrapper, request)
                            }
                        } catch (e: Exception) {
                            Log.w("Auth", "Prepared handle expired/failed; opening direct chooser: ${e.message}")
                            kotlinx.coroutines.withTimeoutOrNull(18_000L) {
                                cm.getCredential(activityContextWrapper, request)
                            }
                        }
                    } else {
                        // Direct GetSignInWithGoogleOption without waiting for One-Tap timeout so device accounts show immediately
                        kotlinx.coroutines.withTimeoutOrNull(18_000L) {
                            cm.getCredential(activityContextWrapper, request)
                        }
                    }

                    if (result == null) {
                        val sha256 = getAppSigningSha256(activity)
                        val causeText = buildString {
                            append("Google Credential Manager timed out without a response from Google Play Services.")
                            if (!sha256.isNullOrBlank()) {
                                append(" | App SHA-256: ")
                                append(sha256)
                            }
                        }
                        AppDiagnosticLog.logRecoverable(
                            toolName = "GoogleSignIn",
                            titleAr = "انتهت مهلة استجابة تسجيل الدخول بجوجل",
                            titleEn = "Google Sign-In Timed Out",
                            causeAr = causeText,
                            causeEn = causeText,
                            errorCode = "CREDENTIAL_MANAGER_TIMEOUT",
                        )
                        errorMsg = if (isEn) {
                            "Google Sign-In did not receive a response from Google Play Services. Check your connection, ensure Google Play Services is updated, or try Email / Guest login."
                        } else {
                            "لم تستجب خدمات Google Play لطلب تسجيل الدخول خلال المهلة المحددة. تأكد من الاتصال بالإنترنت وتحديث خدمات Google Play، أو استخدم البريد الإلكتروني أو الدخول كضيف."
                        }
                        return@launch
                    }

                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        val authResult = kotlinx.coroutines.withTimeoutOrNull(20_000L) {
                            auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                                .await()
                        }
                        if (authResult == null) {
                            AppDiagnosticLog.logRecoverable(
                                toolName = "GoogleSignIn",
                                titleAr = "انتهت مهلة المصادقة مع Firebase",
                                titleEn = "Firebase Auth Timed Out",
                                causeAr = "FirebaseAuth.signInWithCredential timed out after 20s",
                                causeEn = "FirebaseAuth.signInWithCredential timed out after 20s",
                                errorCode = "FIREBASE_AUTH_TIMEOUT",
                            )
                            errorMsg = if (isEn) {
                                "Firebase authentication timed out. Please check your internet connection and try again."
                            } else {
                                "انتهت مهلة التحقق مع خادم Firebase. تأكد من جودة الإنترنت وحاول مرة أخرى."
                            }
                            return@launch
                        }
                        finishFirebaseGoogleUser(
                            authResult.user ?: auth.currentUser,
                        )
                    } else {
                        val credType = credential.type
                        AppDiagnosticLog.logRecoverable(
                            toolName = "GoogleSignIn",
                            titleAr = "نوع بيانات اعتماد غير متوقع من جوجل",
                            titleEn = "Unexpected Google Credential Type",
                            causeAr = "Unexpected credential type: $credType",
                            causeEn = "Unexpected credential type: $credType",
                            errorCode = "UnexpectedCredentialType",
                        )
                        errorMsg = t(lang, "google_sign_in_error_friendly")
                    }
                } catch (e: GetCredentialCancellationException) {
                    val cancelDetail = (e.errorMessage?.toString() ?: e.localizedMessage ?: e.message)
                        ?.takeIf { it.isNotBlank() }
                    val technicalRejection = isTechnicalAuthRejection(cancelDetail)
                    val sha256 = if (technicalRejection) getAppSigningSha256(activity) else null
                    val causeText = buildString {
                        append(cancelDetail ?: e.javaClass.simpleName)
                        if (!sha256.isNullOrBlank()) {
                            append(" | App SHA-256: ")
                            append(sha256)
                        }
                    }
                    Log.w("Auth", "Google Sign-In stopped: $causeText", e)
                    AppDiagnosticLog.logRecoverable(
                        toolName = "GoogleSignIn",
                        titleAr = if (technicalRejection) {
                            "رفض التحقق من حساب جوجل في Google Play"
                        } else {
                            "إلغاء نافذة تسجيل الدخول بجوجل"
                        },
                        titleEn = if (technicalRejection) {
                            "Google Play Account Reauth Failed"
                        } else {
                            "Google Sign-In Cancelled"
                        },
                        causeAr = causeText,
                        causeEn = causeText,
                        errorCode = e.type,
                        throwable = e,
                    )
                    errorMsg = if (technicalRejection) {
                        if (isEn) {
                            "Google could not verify this sign-in. Update Google Play services and try again, or choose another sign-in method."
                        } else {
                            "تعذر التحقق من تسجيل الدخول عبر Google. حدّث خدمات Google Play وحاول مجدداً، أو اختر طريقة دخول أخرى."
                        }
                    } else if (isEn) {
                        "Sign-in was cancelled. Select a Google account to continue."
                    } else {
                        "تم إلغاء تسجيل الدخول. يرجى اختيار حساب Google للمتابعة."
                    }
                } catch (e: NoCredentialException) {
                    val detail = e.errorMessage?.toString() ?: e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                    Log.w("Auth", "No Google credentials found on device: $detail", e)
                    AppDiagnosticLog.logRecoverable(
                        toolName = "GoogleSignIn",
                        titleAr = "لا توجد بيانات اعتماد متاحة من جوجل",
                        titleEn = "No Google Credentials Found",
                        causeAr = detail,
                        causeEn = detail,
                        errorCode = e.type,
                        throwable = e,
                    )
                    errorMsg = if (isEn) {
                        "No Google account is available on this device. Add one in device Settings or use Email / Guest mode."
                    } else {
                        "لا يوجد حساب Google متاح على هذا الجهاز. أضف حساباً من إعدادات الجهاز أو استخدم البريد الإلكتروني أو المتابعة كضيف."
                    }
                } catch (e: GetCredentialException) {
                    val detail = e.errorMessage?.toString() ?: e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                    Log.e("Auth", "Google Credential Manager error: $detail", e)
                    AppDiagnosticLog.logRecoverable(
                        toolName = "GoogleSignIn",
                        titleAr = "تعذر إتمام تسجيل الدخول بحساب جوجل",
                        titleEn = "Google Sign-In Failed",
                        causeAr = detail,
                        causeEn = detail,
                        errorCode = e.type,
                        throwable = e,
                    )
                    errorMsg = formatGoogleAuthFailure(e)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val detail = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                    Log.e("Auth", "Google Sign-In failed: $detail", e)
                    AppDiagnosticLog.logRecoverable(
                        toolName = "GoogleSignIn",
                        titleAr = "تعذر إتمام تسجيل الدخول بحساب جوجل",
                        titleEn = "Google Sign-In Failed",
                        causeAr = detail,
                        causeEn = detail,
                        errorCode = (e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName,
                        throwable = e,
                    )
                    errorMsg = formatGoogleAuthFailure(e)
                } finally {
                    isGoogleLoading = false
                }
            }
        },
        onSubmitEmail = { submitEmailAuth() },
        onContinueAsGuest = { viewModel.continueAsGuest() },
    )

    LanguageSelectorDialog(
        open = langDialogOpen,
        currentLanguage = lang,
        onSelect = { viewModel.setLanguage(it) },
        onDismiss = { langDialogOpen = false },
    )

    SonixExitConfirmDialog(
        open = exitConfirmDialogOpen,
        language = lang,
        hasUnsavedChanges = uiState.hasUnsavedChanges,
        onStay = { exitConfirmDialogOpen = false },
        onConfirmExit = {
            exitConfirmDialogOpen = false
            context.findActivity()?.finish()
        },
    )
}

@Composable
private fun AuthScreenContent(
    lang: AppLanguage,
    isSignUp: Boolean,
    displayName: String,
    email: String,
    password: String,
    passwordVisible: Boolean,
    errorMsg: String?,
    isSubmitting: Boolean,
    isGoogleLoading: Boolean,
    onOpenLanguageDialog: () -> Unit,
    onSelectMode: (Boolean) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSubmitEmail: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isEn = lang == AppLanguage.EN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguageBadgeButton(
                currentLanguage = lang,
                onClick = onOpenLanguageDialog,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val minViewportHeight = (maxHeight - 16.dp).coerceAtLeast(0.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(min = minViewportHeight),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val logoBorderBrush = remember {
                        Brush.linearGradient(
                            colors = listOf(StudioColors.Cyan, StudioColors.Violet),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFFFFFFE),
                        border = BorderStroke(
                            width = 1.8.dp,
                            brush = logoBorderBrush,
                        ),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("auth_brand_logo"),
                    ) {
                        Image(
                            bitmap = ImageBitmap.imageResource(R.drawable.sonix_primary_logo),
                            contentDescription = t(lang, "app_name"),
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = t(lang, "app_name"),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = StudioColors.TextPrimary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = t(lang, "studio_tagline"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioColors.CardSurface),
                        border = BorderStroke(1.dp, StudioColors.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                        // Sign In / Create Account segmented toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val signInColor = if (!isSignUp) StudioColors.Cyan else StudioColors.ElevatedSurface
                            val signUpColor = if (isSignUp) StudioColors.Cyan else StudioColors.ElevatedSurface
                            val signInInteraction = remember { MutableInteractionSource() }
                            val signUpInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = { onSelectMode(false) },
                                interactionSource = signInInteraction,
                                shape = RoundedCornerShape(14.dp),
                                color = signInColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(signInInteraction, MaterialTheme.colorScheme.primary, selected = !isSignUp, cornerRadius = 14.dp)
                                    .testTag("auth_mode_signin"),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = t(lang, "auth_sign_in"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (!isSignUp) StudioColors.BgTop else StudioColors.TextSecondary,
                                    )
                                }
                            }
                            Surface(
                                onClick = { onSelectMode(true) },
                                interactionSource = signUpInteraction,
                                shape = RoundedCornerShape(14.dp),
                                color = signUpColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .lit(signUpInteraction, MaterialTheme.colorScheme.primary, selected = isSignUp, cornerRadius = 14.dp)
                                    .testTag("auth_mode_signup"),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = t(lang, "auth_create_account"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSignUp) StudioColors.BgTop else StudioColors.TextSecondary,
                                    )
                                }
                            }
                        }

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioColors.Cyan,
                            unfocusedBorderColor = StudioColors.Border,
                            focusedTextColor = StudioColors.TextPrimary,
                            unfocusedTextColor = StudioColors.TextPrimary,
                            focusedLabelColor = StudioColors.Cyan,
                            unfocusedLabelColor = StudioColors.TextSecondary,
                        )

                        if (isSignUp) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = onDisplayNameChange,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                ),
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = StudioColors.Cyan)
                                },
                                label = {
                                    Text(if (isEn) "Display Name" else "الاسم في الاستوديو")
                                },
                                colors = fieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input"),
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, contentDescription = null, tint = StudioColors.Cyan)
                            },
                            label = {
                                Text(if (isEn) "Email" else "البريد الإلكتروني")
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSubmitEmail()
                                },
                            ),
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = StudioColors.Cyan)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = onTogglePasswordVisibility,
                                    modifier = Modifier.testTag("auth_password_visibility"),
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) {
                                            if (isEn) "Hide password" else "إخفاء كلمة المرور"
                                        } else {
                                            if (isEn) "Show password" else "إظهار كلمة المرور"
                                        },
                                        tint = StudioColors.TextSecondary,
                                    )
                                }
                            },
                            label = {
                                Text(if (isEn) "Password (6+ chars)" else "كلمة المرور (6 أحرف فأكثر)")
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                        )

                        val submitInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSubmitEmail()
                            },
                            enabled = !isSubmitting && !isGoogleLoading,
                            interactionSource = submitInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.Cyan,
                                contentColor = StudioColors.BgTop,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .lit(submitInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("auth_submit_button"),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = StudioColors.BgTop,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isSignUp) t(lang, "auth_create_account") else t(lang, "auth_email_signin"),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = StudioColors.Border,
                            )
                            Text(
                                text = if (isEn) "OR" else "أو",
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioColors.TextSecondary,
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = StudioColors.Border,
                            )
                        }

                        val googleInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onGoogleSignIn()
                            },
                            enabled = !isSubmitting && !isGoogleLoading,
                            interactionSource = googleInteraction,
                            border = BorderStroke(1.dp, StudioColors.BorderBright),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioColors.ElevatedSurface,
                                contentColor = StudioColors.TextPrimary,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .lit(googleInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("auth_google_button"),
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    color = StudioColors.Cyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = StudioColors.Cyan,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isGoogleLoading) {
                                    when (lang) {
                                        AppLanguage.EN -> "Fetching accounts..."
                                        AppLanguage.FA -> "در حال دریافت حساب‌ها..."
                                        AppLanguage.TR -> "Hesaplar yükleniyor..."
                                        AppLanguage.AR -> "جاري جلب الحسابات..."
                                    }
                                } else {
                                    t(lang, "auth_google_signin")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }

                        if (!errorMsg.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = StudioColors.Rose.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, StudioColors.Rose.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_banner"),
                            ) {
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = StudioColors.RoseBright,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        val guestInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            enabled = !isSubmitting && !isGoogleLoading,
                            interactionSource = guestInteraction,
                            border = BorderStroke(1.dp, StudioColors.Emerald.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioColors.Emerald),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .lit(guestInteraction, MaterialTheme.colorScheme.primary, cornerRadius = 14.dp)
                                .testTag("auth_guest_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t(lang, "auth_continue_guest"),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B14)
@Composable
private fun AuthScreenPreview() {
    SonixTheme {
        AuthScreenContent(
            lang = AppLanguage.AR,
            isSignUp = false,
            displayName = "",
            email = "",
            password = "",
            passwordVisible = false,
            errorMsg = null,
            isSubmitting = false,
            isGoogleLoading = false,
            onOpenLanguageDialog = {},
            onSelectMode = {},
            onDisplayNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onGoogleSignIn = {},
            onSubmitEmail = {},
            onContinueAsGuest = {},
        )
    }
}
