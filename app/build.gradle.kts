plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.roborazzi)
}

// The API key is not stored in git. Set timetableApiKey in ~/.gradle/gradle.properties,
// pass -PtimetableApiKey=..., or set TIMETABLE_API_KEY. Without a key the app shows an error.
val timetableApiKey: String =
    providers
        .gradleProperty("timetableApiKey")
        .orElse(providers.environmentVariable("TIMETABLE_API_KEY"))
        .getOrElse("")

if (timetableApiKey.isEmpty()) {
    logger.warn(
        "w: timetableApiKey is not set. The app builds, but every request fails with HTTP 401 " +
            "and shows \"Something went wrong\". See README.md, section Run.",
    )
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
        testInstrumentationRunner = "com.sukhotski.timetable.HiltTestRunner"
        buildConfigField(
            "String",
            "TIMETABLE_API_KEY",
            "\"$timetableApiKey\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
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
        // Versions are pinned on purpose. Updates are a separate change. OldTargetApi depends on
        // the newest SDK installed on the machine, so it fails on CI runners that have SDK 37.
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
        )
    }
    sourceSets {
        // All tests read the saved API responses from the shared module.
        getByName("androidTest").resources.srcDir("../shared/src/commonTest/resources")
        getByName("test").resources.srcDir("../shared/src/commonTest/resources")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose brings 3.5, which crashes on Android 16 (InputManager.getInstance).
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
