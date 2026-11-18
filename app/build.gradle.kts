import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.makemission.ora"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.makemission.ora"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // One personal key for debug and release, so the certificate hash never changes. The keystore and its
    // passwords live outside the repo: ora.keystore, ora.keystore.password, ora.key.alias, ora.key.password in
    // local.properties. When they are missing or the file is absent, builds fall back to the default debug key.
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val oraKeystore = localProps.getProperty("ora.keystore")?.let { file(it) }?.takeIf { it.isFile }
    val oraSigning = if (oraKeystore != null && localProps.getProperty("ora.keystore.password") != null) {
        signingConfigs.create("ora") {
            storeFile = oraKeystore
            storePassword = localProps.getProperty("ora.keystore.password")
            keyAlias = localProps.getProperty("ora.key.alias", "ora")
            keyPassword = localProps.getProperty("ora.key.password") ?: localProps.getProperty("ora.keystore.password")
        }
    } else null

    buildTypes {
        debug {
            if (oraSigning != null) signingConfig = oraSigning
        }
        release {
            if (oraSigning != null) signingConfig = oraSigning
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // android.util.Log (used by BrowserLog) is a no-op in host tests.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.mozilla.geckoview)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    // The Android JSON classes are stubs in host tests; this is the real implementation.
    testImplementation("org.json:json:20231013")
    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.mockwebserver)
    testImplementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}