package com.example.android.pictureinpicture.util

import kotlinx.coroutines.delay

class TestFrameScheduler : FrameScheduler {
    override suspend fun awaitNextFrame() {
        delay(16) // ~60fps
    }
}