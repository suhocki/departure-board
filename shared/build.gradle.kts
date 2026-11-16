import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

kotlin {
    android {
        namespace = "com.sukhotski.timetable.shared"
        compileSdk = 36
        minSdk = 26

        // Common tests run on the JVM as Android host tests.
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        allWarningsAsErrors = true
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
            // Pins OkHttp to the version the app and its tests use.
            implementation(libs.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    // The simulator reads the saved API responses from the disk of this Mac.
    environment(
        "SIMCTL_CHILD_FIXTURES_DIR",
        layout.projectDirectory.dir("src/commonTest/resources/fixtures").asFile.path,
    )
}

ktlint {
    version.set(libs.versions.ktlint)
}
