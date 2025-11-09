package com.example.android.pictureinpicture.util

import kotlinx.coroutines.android.awaitFrame
import javax.inject.Inject

/**
 * Abstraction for frame timing to enable testability without polluting production code.
 */
interface FrameScheduler {
    /**
     * Waits for the next frame or equivalent timing interval.
     */
    suspend fun awaitNextFrame()
}

class AndroidFrameScheduler @Inject constructor() : FrameScheduler {
    override suspend fun awaitNextFrame() {
        awaitFrame()
    }
}