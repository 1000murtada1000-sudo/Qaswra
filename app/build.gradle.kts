import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.voicepro.sonixk"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val developerPanelEnabled = (project.findProperty("DEVELOPER_PANEL_ENABLED") as? String)
            ?.trim()
            ?.toBooleanStrictOrNull() ?: true
        buildConfigField("boolean", "DEVELOPER_PANEL_ENABLED", developerPanelEnabled.toString())
        if (!file("google-services.json").exists()) {
            resValue("string", "default_web_client_id", "")
        }
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            } else {
                storeFile = file("${rootDir}/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

val sherpaLibsDir = file("libs")
val sherpaAarFile = file("libs/sherpa-onnx-1.12.35.aar")
if (!sherpaAarFile.exists() || sherpaAarFile.length() < 100_000L) {
    sherpaLibsDir.mkdirs()
    val tmpFile = file("libs/sherpa-onnx-1.12.35.aar.part")
    val urls = listOf(
        "https://github.com",
        "https://huggingface.co"
    )
    var downloaded = false
    for (startUrl in urls) {
        try {
            var currentUrl = startUrl
            var conn: HttpURLConnection? = null
            for (hop in 0 until 8) {
                val u = URI(currentUrl).toURL()
                conn = (u.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    setRequestProperty("User-Agent", "SonixGradle/1.0")
                }
                val code = conn.responseCode
                if (code in 300..308 && code != 304) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) throw IllegalStateException("Redirect missing Location")
                    currentUrl = URI(currentUrl).resolve(loc).toString()
                    continue
                }
                if (code !in 200..299) {
                    conn.disconnect()
                    throw IllegalStateException("HTTP $code")
                }
                break
            }
            conn!!.inputStream.use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            }
            conn.disconnect()
            if (tmpFile.length() >= 100_000L) {
                if (sherpaAarFile.exists()) sherpaAarFile.delete()
                tmpFile.renameTo(sherpaAarFile)
                downloaded = true
                break
            } else {
                tmpFile.delete()
            }
        } catch (e: Exception) {
            tmpFile.delete()
            logger.warn("Could not fetch sherpa-onnx AAR from $startUrl: ${e.message}")
        }
    }
    if (!downloaded) {
        logger.warn("Continuing without bundled sherpa-onnx AAR; runtime fallback will be used.")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // Sherpa-ONNX (Kokoro / Nabra-82M / Moonshine AR + Silero VAD) AAR + Maintained FFmpegKit
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.9")
    implementation(libs.commons.compress)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.ui.test.junit4)
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}
س