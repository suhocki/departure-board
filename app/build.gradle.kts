plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
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

    testImplementation(libs.junit)
}
