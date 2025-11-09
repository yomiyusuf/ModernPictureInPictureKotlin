package com.example.android.pictureinpicture.util

/**
 * Test time provider for deterministic testing.
 * Allows manual control over time progression in tests.
 */
class TestTimeProvider : TimeProvider {
    var currentTime = 0L
        private set

    override fun uptimeMillis(): Long = currentTime

    fun advanceTime(millis: Long) {
        currentTime += millis
    }

    fun setTime(millis: Long) {
        currentTime = millis
    }

    fun reset() {
        currentTime = 0L
    }
}