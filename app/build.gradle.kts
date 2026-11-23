import java.util.Properties
import java.net.URI

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

abstract class ValidateStagingIsolation : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Input
    abstract val endpoint: org.gradle.api.provider.Property<String>
    @get:org.gradle.api.tasks.Internal
    abstract val stagingConfig: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.Internal
    abstract val defaultConfig: org.gradle.api.file.RegularFileProperty

    @org.gradle.api.tasks.TaskAction
    fun validate() {
        val uri = runCatching { URI(endpoint.get()) }.getOrNull()
        val host = uri?.host?.lowercase()?.trimEnd('.')
        require(uri != null && uri.scheme == "https" && !host.isNullOrBlank() &&
            host !in setOf("api.gridfall.site", "gridfall.site", "www.gridfall.site", "192.168.222.172") &&
            uri.userInfo == null && uri.query == null && uri.fragment == null && uri.port in listOf(-1, 443)) {
            "Staging requires a non-production HTTPS GRIDFALL_STAGING_API_BASE_URL (or -PgridfallStagingApiBaseUrl)"
        }
        val config = stagingConfig.get().asFile
        require(config.isFile) { "Staging requires its own uncommitted src/staging/google-services.json" }
        val staging = groovy.json.JsonSlurper().parse(config) as Map<*, *>
        val project = (staging["project_info"] as? Map<*, *>)?.get("project_id")
        require(project is String && project.isNotBlank()) { "Missing staging Firebase project" }
        val production = defaultConfig.get().asFile
        if (production.isFile) {
            val existing = groovy.json.JsonSlurper().parse(production) as Map<*, *>
            require(project != (existing["project_info"] as? Map<*, *>)?.get("project_id")) { "Staging Firebase project must differ from the default project" }
        }
    }
}
val stagingEndpoint = providers.gradleProperty("gridfallStagingApiBaseUrl")
    .orElse(providers.environmentVariable("GRIDFALL_STAGING_API_BASE_URL")).getOrElse("")
val validateStagingIsolation = tasks.register<ValidateStagingIsolation>("validateStagingIsolation") {
    endpoint.set(stagingEndpoint)
    stagingConfig.set(layout.projectDirectory.file("src/staging/google-services.json"))
    defaultConfig.set(layout.projectDirectory.file("google-services.json"))
}
tasks.matching { it.name in setOf("preStagingBuild", "generateStagingBuildConfig", "processStagingGoogleServices") }
    .configureEach { dependsOn(validateStagingIsolation) }

// Explicit override for controlled QA. Default debug LAN and release/staging rules stay intact.
val debugEndpoint = providers.gradleProperty("gridfallDebugApiBaseUrl")
    .orElse(providers.environmentVariable("GRIDFALL_DEBUG_API_BASE_URL"))
    .getOrElse("http://192.168.222.172:8080")
val debugUri = runCatching { URI(debugEndpoint) }.getOrNull()
require(debugUri != null && debugUri.scheme in listOf("http", "https") && !debugUri.host.isNullOrBlank() &&
    debugUri.userInfo == null && debugUri.query == null && debugUri.fragment == null) { "Invalid debug API base URL" }

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
        versionCode = 24
        versionName = "1.8.1"

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
                "\"${debugEndpoint.replace("\\", "\\\\").replace("\"", "\\\"")}\""
            )
        }
        // Isolated staging: explicit endpoint and separate Firebase configuration are mandatory.
        create("staging") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField(
                "String",
                "GRIDFALL_API_BASE_URL",
                "\"${stagingEndpoint.ifBlank { "https://staging.invalid" }.replace("\\", "\\\\").replace("\"", "\\\"")}\""
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
