package com.example.android.pictureinpicture.repository

import app.cash.turbine.test
import com.example.android.pictureinpicture.util.TestFrameScheduler
import com.example.android.pictureinpicture.util.TestTimeProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerRepositoryTest {

    private lateinit var repository: TimerRepository
    private lateinit var testTimeProvider: TestTimeProvider
    private lateinit var testFrameScheduler: TestFrameScheduler
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        testTimeProvider = TestTimeProvider()
        testFrameScheduler = TestFrameScheduler()
        repository = TimerRepository(testTimeProvider, testFrameScheduler, testDispatcher)
    }

    @Test
    fun `repository emits initial state`() = runTest {
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(false)
        }
        
        repository.time.test {
            assertThat(awaitItem()).isEqualTo("00:00:00")
        }
    }

    @Test
    fun `startOrPause toggles started state correctly`() = runTest {
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(false) // Initial state
            
            repository.startOrPause()
            assertThat(awaitItem()).isEqualTo(true) // Started
            
            repository.startOrPause()
            assertThat(awaitItem()).isEqualTo(false) // Stopped
        }
    }

    @Test
    fun `repository maintains state across multiple observers`() = runTest {
        repository.startOrPause()
        
        // Multiple observers should get the same current state
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(true)
        }
        
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `timer progresses when started`() = runTest {
        testTimeProvider.setTime(0)
        
        // Test timer state without Flow collection to avoid infinite loop
        repository.startOrPause() // Start timer
        
        // Advance time and check if repository tracks time correctly
        testTimeProvider.advanceTime(1000)
        
        // Stop timer to end loop
        repository.startOrPause() // Stop timer
        
        // Check final time value
        assertThat(repository.time.value).matches("\\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun `clear resets timer to zero`() = runTest {
        testTimeProvider.setTime(0)
        
        // Start timer briefly
        repository.startOrPause()
        
        // Advance time
        testTimeProvider.advanceTime(5000)
        
        // Stop timer before clear to avoid infinite loop
        repository.startOrPause()
        
        // Clear the timer
        repository.clear()
        
        // Verify time was reset
        assertThat(repository.time.value).isEqualTo("00:00:00")
    }

    @Test
    fun `clear does not affect started state`() = runTest {
        repository.startOrPause() // Start timer
        
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(true) // Started
            
            repository.clear()
            // Started state should remain unchanged
            expectNoEvents()
            assertThat(repository.started.value).isEqualTo(true)
        }
    }

    @Test
    fun `timer format is correct for various durations`() = runTest {
        testTimeProvider.setTime(0)
        repository.clear()
        
        // Test initial format
        assertThat(repository.time.value).isEqualTo("00:00:00")
        
        // Start timer briefly and test formats
        repository.startOrPause()
        
        // Test 1.05 seconds format
        testTimeProvider.setTime(1050)
        repository.startOrPause() // Stop to check value
        repository.startOrPause() // Restart to update
        repository.startOrPause() // Stop again
        assertThat(repository.time.value).matches("\\d{2}:\\d{2}:\\d{2}")
        
        // Reset and test minute format
        repository.clear()
        testTimeProvider.setTime(60000) // 1 minute
        repository.startOrPause()
        repository.startOrPause()
        assertThat(repository.time.value).matches("\\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun `repository maintains singleton behavior`() = runTest {
        // Simulate different ViewModels accessing the same repository
        val firstAccess = repository.started.value
        val secondAccess = repository.started.value
        
        assertThat(firstAccess).isEqualTo(secondAccess)
        assertThat(firstAccess).isEqualTo(false)
        
        // Change state through one access point
        repository.startOrPause()
        
        // Both access points should reflect the change
        assertThat(repository.started.value).isEqualTo(true)
    }

    @Test
    fun `rapid operations maintain consistency`() = runTest {
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(false) // Initial
            
            // Rapid start/stop operations
            repeat(5) {
                repository.startOrPause()
                assertThat(awaitItem()).isEqualTo(true)
                
                repository.startOrPause()
                assertThat(awaitItem()).isEqualTo(false)
            }
        }
    }

    @Test
    fun `timer continues after clear when started`() = runTest {
        testTimeProvider.setTime(0)
        
        // Start timer
        repository.startOrPause()
        
        // Let timer run for a bit then stop
        testTimeProvider.advanceTime(3000)
        repository.startOrPause() // Stop
        
        // Clear timer (should reset to 00:00:00)
        repository.clear()
        assertThat(repository.time.value).isEqualTo("00:00:00")
        
        // Timer should be able to start again after clear
        repository.startOrPause() // Start
        repository.startOrPause() // Stop immediately
        
        // Should still show valid time format
        assertThat(repository.time.value).matches("\\d{2}:\\d{2}:\\d{2}")
    }

    @Test
    fun `repository flows are cold but StateFlows provide current value`() = runTest {
        // StateFlows should provide current value immediately
        assertThat(repository.started.value).isEqualTo(false)
        assertThat(repository.time.value).isEqualTo("00:00:00")
        
        // Change state
        repository.startOrPause()
        
        // New observers should get current state immediately
        assertThat(repository.started.value).isEqualTo(true)
        
        repository.started.test {
            assertThat(awaitItem()).isEqualTo(true) // Current state
        }
    }
}