package com.example.android.pictureinpicture.util

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction for system time to enable deterministic testing.
 */
interface TimeProvider {
    fun uptimeMillis(): Long
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun uptimeMillis(): Long = SystemClock.uptimeMillis()
}