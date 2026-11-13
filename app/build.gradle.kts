plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// The API key is not stored in git. Set timetableApiKey in ~/.gradle/gradle.properties,
// pass -PtimetableApiKey=..., or set TIMETABLE_API_KEY. Without a key the app shows an error.
val timetableApiKey: String =
    providers
        .gradleProperty("timetableApiKey")
        .orElse(providers.environmentVariable("TIMETABLE_API_KEY"))
        .getOrElse("")

android {
    namespace = "com.sukhotski.timetable"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sukhotski.timetable"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "TIMETABLE_API_KEY",
            "\"$timetableApiKey\"",
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        // Versions are pinned on purpose. Updates are a separate change.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
    testOptions {
        unitTests {
            all {
                // A zone far from Europe, so code that reads the device zone fails on any laptop.
                it.systemProperty("user.timezone", "Asia/Kolkata")
                it.systemProperty("user.language", "en")
                it.systemProperty("user.country", "US")
            }
        }
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
