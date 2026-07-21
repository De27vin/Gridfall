import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}

fun releaseSigningProperty(name: String): String? {
    return releaseKeystoreProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }
}

val hasReleaseSigningProperties = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword"
).all { releaseSigningProperty(it) != null }

android {
    namespace = "com.example.gridfall"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.gridfall"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningProperties) {
                storeFile = rootProject.file(requireNotNull(releaseSigningProperty("storeFile")))
                storePassword = requireNotNull(releaseSigningProperty("storePassword"))
                keyAlias = requireNotNull(releaseSigningProperty("keyAlias"))
                keyPassword = requireNotNull(releaseSigningProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "GRIDFALL_API_BASE_URL",
                "\"http://192.168.222.172:8080\""
            )
        }
        // Public HTTPS test build. It remains debuggable so Settings displays the active API URL.
        create("staging") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            buildConfigField(
                "String",
                "GRIDFALL_API_BASE_URL",
                "\"https://api.gridfall.site\""
            )
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField(
                "String",
                "GRIDFALL_API_BASE_URL",
                "\"https://api.gridfall.site\""
            )
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
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
