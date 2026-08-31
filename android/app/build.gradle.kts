plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.miti99.loto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miti99.loto"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") {
            // Voice clips and their manifest are mounted straight from the web
            // app's static dir so both apps ship the exact same files — no copy
            // step, no drift. Mounts web/static/audio/** as assets/audio/**.
            assets.srcDir("../../web/static")
        }
    }

    androidResources {
        // The app ships Vietnamese-only copy; trim library translations.
        // `resourceConfigurations` (defaultConfig) is deprecated for locales
        // in AGP 8.13 in favor of this (L11).
        localeFilters += listOf("vi")
        // Keep only the audio/ subtree from the mounted web/static dir.
        // Pattern extends AAPT's default ignore list (hidden files, VCS dirs).
        ignoreAssetsPattern =
            "!icons:!manifest.webmanifest:!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }

    signingConfigs {
        // Reads from env vars so the keystore never lands in the repo.
        // Same contract as every previous Lô tô release (CI secrets unchanged).
        create("release") {
            val keystorePath = System.getenv("LOTO_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storeType = "pkcs12"
                storePassword = System.getenv("LOTO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LOTO_KEY_ALIAS")
                keyPassword = System.getenv("LOTO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only sign when env vars are present; debug builds stay debug-signed.
            if (System.getenv("LOTO_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // L4 added android.util.Log calls in the persistence-failure
            // fallback paths; the stock android.jar stub throws on any
            // unmocked call, which would crash those JVM unit tests without
            // pulling in Robolectric. Default-value stubbing is enough here
            // since no test asserts on the logged message itself.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // Real org.json for JVM unit tests (android.jar only ships stubs).
    testImplementation(libs.org.json)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
