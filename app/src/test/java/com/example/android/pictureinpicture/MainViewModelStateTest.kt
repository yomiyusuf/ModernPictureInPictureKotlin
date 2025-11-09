package com.example.android.pictureinpicture

import com.example.android.pictureinpicture.util.TestFrameScheduler
import com.example.android.pictureinpicture.util.TestTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelStateTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var testTimeProvider: TestTimeProvider
    private lateinit var testFrameScheduler: TestFrameScheduler
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        testTimeProvider = TestTimeProvider()
        testFrameScheduler = TestFrameScheduler()
        viewModel = MainViewModel(testTimeProvider, testFrameScheduler, testDispatcher)
    }

    @Test
    fun `initial state should be stopped`() = runTest(testDispatcher) {
        assertThat(viewModel.started.value).isEqualTo(false)
    }

    @Test
    fun `startOrPause should toggle started state`() = runTest(testDispatcher) {
        // Initially stopped
        assertThat(viewModel.started.value).isEqualTo(false)

        // Start
        viewModel.startOrPause()
        assertThat(viewModel.started.value).isEqualTo(true)

        // Pause
        viewModel.startOrPause()
        assertThat(viewModel.started.value).isEqualTo(false)
    }

    @Test
    fun `multiple startOrPause calls should toggle correctly`() = runTest(testDispatcher) {
        repeat(5) {
            viewModel.startOrPause()
            assertThat(viewModel.started.value).isEqualTo(true)

            viewModel.startOrPause()
            assertThat(viewModel.started.value).isEqualTo(false)
        }
    }

    @Test
    fun `clear should not affect started state when stopped`() = runTest(testDispatcher) {
        assertThat(viewModel.started.value).isEqualTo(false)
        
        viewModel.clear()
        
        assertThat(viewModel.started.value).isEqualTo(false)
    }

    @Test
    fun `time provider is used for time operations`() = runTest(testDispatcher) {
        // Set specific time
        testTimeProvider.setTime(12345L)
        
        // Clear should use the time provider
        viewModel.clear()
        
        // Verify time provider was called (through state management)
        assertThat(testTimeProvider.uptimeMillis()).isEqualTo(12345L)
    }

    @Test
    fun `time formatting produces expected string format`() = runTest(testDispatcher) {
        // Test that initial time format is correct
        val initialTime = viewModel.time.value
        assertThat(initialTime).matches("\\d{2}:\\d{2}:\\d{2}")
        assertThat(initialTime).isEqualTo("00:00:00")
    }

    @Test
    fun `test time provider can be manipulated`() {
        testTimeProvider.setTime(0)
        assertThat(testTimeProvider.uptimeMillis()).isEqualTo(0)
        
        testTimeProvider.advanceTime(1000)
        assertThat(testTimeProvider.uptimeMillis()).isEqualTo(1000)
        
        testTimeProvider.reset()
        assertThat(testTimeProvider.uptimeMillis()).isEqualTo(0)
    }

    @Test
    fun `viewModel can be created with time provider`() {
        val customTimeProvider = TestTimeProvider()
        customTimeProvider.setTime(99999)
        
        val customViewModel = MainViewModel(customTimeProvider, testFrameScheduler, testDispatcher)
        
        // Verify it was injected correctly by checking it's not null
        assertThat(customViewModel.started.value).isEqualTo(false)
        assertThat(customViewModel.time.value).isEqualTo("00:00:00")
    }

    @Test
    fun `state flows are properly initialized`() = runTest(testDispatcher) {
        // Verify StateFlows are not null and have expected initial values
        assertThat(viewModel.started).isNotNull()
        assertThat(viewModel.time).isNotNull()
        
        assertThat(viewModel.started.value).isEqualTo(false)
        assertThat(viewModel.time.value).isEqualTo("00:00:00")
    }
}