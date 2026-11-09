import java.net.URI

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val apiBaseUrl = providers.gradleProperty("ANOTADO_API_BASE_URL").orElse("")
val configuredApiUrl = apiBaseUrl.get().trim()
require(configuredApiUrl.isEmpty() || runCatching {
    val uri = URI(configuredApiUrl)
    uri.scheme in listOf("http", "https") && uri.host != null &&
        uri.userInfo == null && uri.query == null && uri.fragment == null &&
        configuredApiUrl.endsWith("/")
}.getOrDefault(false)) { "ANOTADO_API_BASE_URL debe ser una URL http(s) válida terminada en /." }
val apiUrlLiteral = "\"" + configuredApiUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.yharim.anotado"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yharim.anotado"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", apiUrlLiteral)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
