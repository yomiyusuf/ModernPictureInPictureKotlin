package com.example.android.pictureinpicture.util

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import com.example.android.pictureinpicture.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Picture-in-Picture compatibility across different API levels.
 * Provides centralized feature detection for graceful degradation on older devices.
 */
@Singleton
class PipCompatibilityManager @Inject constructor() {
    fun hasPictureInPictureSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun hasRemoteActionSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    /**
     * Checks if the current activity can enter Picture-in-Picture mode.
     * Considers both system support and activity-specific requirements.
     */
    fun canEnterPictureInPicture(activity: Activity): Boolean {
        if (!hasPictureInPictureSupport()) {
            return false
        }

        // Check if the device supports PiP feature
        val packageManager = activity.packageManager
        val hasPipFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        return hasPipFeature
    }

    /**
     * Safely attempts to enter Picture-in-Picture mode with proper error handling.
     * Returns true if successful, false if PiP is not available or entry failed.
     */
    fun enterPictureInPictureSafely(activity: Activity, pipParamsProvider: () -> Any?): Boolean {
        if (!canEnterPictureInPicture(activity)) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            val params = pipParamsProvider()
            if (params != null && params is android.app.PictureInPictureParams) {
                activity.enterPictureInPictureMode(params)
                true
            } else {
                @Suppress("DEPRECATION")
                activity.enterPictureInPictureMode()
                true
            }
        } catch (e: SecurityException) { // PiP might be disabled by device policy
            false
        } catch (e: IllegalStateException) { // Activity might not be in a valid state for PiP
            false
        } catch (e: Exception) { // Any other unexpected errors
            false
        }
    }

    @StringRes
    fun getPictureInPictureStatusMessageRes(): Int {
        return when {
            !hasPictureInPictureSupport() -> R.string.pip_requires_api_26
            else -> R.string.pip_available
        }
    }
    
    /**
     * Checks if enhanced PiP features (Android 12+) are available.
     * Enhanced features include stashing, better animations, and improved gesture handling.
     */
    fun hasEnhancedPictureInPictureSupport(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S // Android 12+ (API 31)
    }
}