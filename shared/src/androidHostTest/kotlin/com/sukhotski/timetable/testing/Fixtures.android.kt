package com.sukhotski.timetable.testing

import java.io.File

// Gradle runs host tests in the module folder.
actual fun fixture(name: String): String = File("src/commonTest/resources/fixtures/$name").also {
    check(it.exists()) { "No fixture $name" }
}.readText()
