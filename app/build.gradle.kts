import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Signing credentials stay out of the repo; see keystore.properties or CI env vars.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun resolveKeystoreProperty(propName: String, envVar: String): String? {
    return System.getenv(envVar)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propName)?.takeIf { it.isNotBlank() }
}

android {
    namespace = "com.balookrd.ibeacon"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.balookrd.ibeacon"
        minSdk = 26
        targetSdk = 35
        versionCode = System.getenv("BUILD_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("BUILD_VERSION_NAME") ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val storePath = resolveKeystoreProperty("storeFile", "KEYSTORE_FILE")
            if (storePath != null) {
                val resolvedFile = file(storePath).let { if (it.isAbsolute) it else rootProject.file(storePath) }
                if (resolvedFile.exists()) {
                    storeFile = resolvedFile
                    storePassword = resolveKeystoreProperty("storePassword", "KEYSTORE_PASSWORD")
                    keyAlias = resolveKeystoreProperty("keyAlias", "KEY_ALIAS")
                    keyPassword = resolveKeystoreProperty("keyPassword", "KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Falls back to an unsigned build when keystore credentials are not provided.
            signingConfig = signingConfigs.getByName("release").takeIf {
                it.storeFile != null && it.storeFile!!.exists()
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
