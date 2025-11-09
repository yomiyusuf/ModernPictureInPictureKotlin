package com.example.android.pictureinpicture.repository

import com.example.android.pictureinpicture.util.FrameScheduler
import com.example.android.pictureinpicture.util.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timeProvider: TimeProvider,
    private val frameScheduler: FrameScheduler,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private var job: Job? = null
    
    private val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
    
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
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "00:00:00"
    )
    
    /**
     * Starts the stopwatch if it is not yet started, or pauses it if it is already started.
     */
    fun startOrPause() {
        if (_started.value) {
            _started.value = false
            job?.cancel()
        } else {
            _started.value = true
            job = repositoryScope.launch { start() }
        }
    }
    
    private suspend fun start() {
        startUptimeMillis = timeProvider.uptimeMillis() - timeMillis.value
        while (repositoryScope.isActive) {
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