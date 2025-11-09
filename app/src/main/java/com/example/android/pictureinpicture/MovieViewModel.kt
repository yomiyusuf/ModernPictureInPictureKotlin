package com.example.android.pictureinpicture

import androidx.lifecycle.ViewModel
import com.example.android.pictureinpicture.repository.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    timerRepository: TimerRepository
): ViewModel() {
    val time: StateFlow<String> = timerRepository.time
}