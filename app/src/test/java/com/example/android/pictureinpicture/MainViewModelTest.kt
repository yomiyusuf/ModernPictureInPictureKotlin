package com.example.android.pictureinpicture

import com.example.android.pictureinpicture.repository.TimerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var mockRepository: TimerRepository

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)

        every { mockRepository.started } returns MutableStateFlow(false)
        every { mockRepository.time } returns MutableStateFlow("00:00:00")
        
        viewModel = MainViewModel(mockRepository)
    }

    @Test
    fun `startOrPause delegates to repository`() = runTest {
        viewModel.startOrPause()
        
        verify { mockRepository.startOrPause() }
    }

    @Test
    fun `clear delegates to repository`() = runTest {
        viewModel.clear()
        
        verify { mockRepository.clear() }
    }

    @Test
    fun `started flow exposes repository started flow`() = runTest {
        assertThat(viewModel.started).isEqualTo(mockRepository.started)
    }

    @Test
    fun `time flow exposes repository time flow`() = runTest {
        assertThat(viewModel.time).isEqualTo(mockRepository.time)
    }

    @Test
    fun `delegation works with multiple repository interactions`() = runTest {
        // Perform multiple operations
        viewModel.startOrPause()
        viewModel.clear() 
        viewModel.startOrPause()
        viewModel.clear()

        verify(exactly = 2) { mockRepository.startOrPause() }
        verify(exactly = 2) { mockRepository.clear() }
    }

    @Test
    fun `StateFlows provide initial values`() = runTest {
        // Test that StateFlows are accessible and provide expected initial values
        assertThat(viewModel.started.value).isEqualTo(false)
        assertThat(viewModel.time.value).isEqualTo("00:00:00")
    }

    @Test
    fun `ViewModel can be constructed with repository`() = runTest {
        val anotherMockRepository = mockk<TimerRepository>(relaxed = true)
        every { anotherMockRepository.started } returns MutableStateFlow(false)
        every { anotherMockRepository.time } returns MutableStateFlow("00:00:00")
        
        val anotherViewModel = MainViewModel(anotherMockRepository)
        
        // Verify it was constructed successfully
        assertThat(anotherViewModel.started).isNotNull()
        assertThat(anotherViewModel.time).isNotNull()
    }
}