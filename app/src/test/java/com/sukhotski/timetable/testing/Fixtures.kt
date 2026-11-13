package com.sukhotski.timetable.testing

/** Reads a saved API response from src/test/resources/fixtures. */
fun fixture(name: String): String {
    val url =
        checkNotNull(Thread.currentThread().contextClassLoader?.getResource("fixtures/$name")) {
            "No fixture $name"
        }
    return url.readText()
}
