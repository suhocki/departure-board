package com.sukhotski.timetable.testing

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

// The simulator sees the disk of the Mac. The Gradle test task passes the folder as
// SIMCTL_CHILD_FIXTURES_DIR, and the simulator process gets it as FIXTURES_DIR.
@OptIn(ExperimentalForeignApi::class)
actual fun fixture(name: String): String {
    val dir = checkNotNull(NSProcessInfo.processInfo.environment["FIXTURES_DIR"] as? String) {
        "FIXTURES_DIR is not set"
    }
    return checkNotNull(
        NSString.stringWithContentsOfFile("$dir/$name", NSUTF8StringEncoding, null),
    ) {
        "No fixture $name"
    }
}
