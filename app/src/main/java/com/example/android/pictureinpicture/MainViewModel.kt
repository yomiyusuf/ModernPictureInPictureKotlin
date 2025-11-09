/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pictureinpicture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.pictureinpicture.util.FrameScheduler
import com.example.android.pictureinpicture.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val timeProvider: TimeProvider,
    private val frameScheduler: FrameScheduler,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
): ViewModel() {

    private var job: Job? = null

    private var startUptimeMillis = timeProvider.uptimeMillis()
    private val timeMillis = MutableStateFlow(0L)

    private val _started = MutableStateFlow(false)

    val started: StateFlow<Boolean> = _started.asStateFlow()
    val time: StateFlow<String> = timeMillis.map { millis ->
        val minutes = millis / 1000 / 60
        val m = minutes.toString().padStart(2, '0')
        val seconds = (millis / 1000) % 60
        val s = seconds.toString().padStart(2, '0')
        val hundredths = (millis % 1000) / 10
        val h = hundredths.toString().padStart(2, '0')
        "$m:$s:$h"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "00:00:00"
    )

    /**
     * Starts the stopwatch if it is not yet started, or pauses it if it is already started.
     */
    fun startOrPause() {
        if (_started.value == true) {
            _started.value = false
            job?.cancel()
        } else {
            _started.value = true
            job = viewModelScope.launch(dispatcher) { start() }
        }
    }

    private suspend fun CoroutineScope.start() {
        startUptimeMillis = timeProvider.uptimeMillis() - timeMillis.value
        while (isActive) {
            timeMillis.value = timeProvider.uptimeMillis() - startUptimeMillis
            frameScheduler.awaitNextFrame()
        }
    }

    /**
     * Clears the stopwatch to 00:00:00.
     */
    fun clear() {
        startUptimeMillis = timeProvider.uptimeMillis()
        timeMillis.value = 0L
    }
}
