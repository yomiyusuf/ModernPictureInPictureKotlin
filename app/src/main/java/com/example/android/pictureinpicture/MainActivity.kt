/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.trackPipAnimationHintView
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.android.pictureinpicture.databinding.MainActivityBinding
import com.example.android.pictureinpicture.util.PipCompatibilityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Intent action for stopwatch controls from Picture-in-Picture mode.  */
private const val ACTION_STOPWATCH_CONTROL = "stopwatch_control"

/** Intent extra for stopwatch controls from Picture-in-Picture mode.  */
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_CLEAR = 1
private const val CONTROL_TYPE_START_OR_PAUSE = 2

private const val REQUEST_CLEAR = 3
private const val REQUEST_START_OR_PAUSE = 4

/**
 * Demonstrates usage of Picture-in-Picture mode on phones and tablets.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: MainActivityBinding
    
    @Inject
    lateinit var pipCompatibilityManager: PipCompatibilityManager

    /**
     * A [BroadcastReceiver] for handling action items on the picture-in-picture mode.
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        // Called when an item is clicked.
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != ACTION_STOPWATCH_CONTROL) {
                return
            }
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                CONTROL_TYPE_START_OR_PAUSE -> viewModel.startOrPause()
                CONTROL_TYPE_CLEAR -> viewModel.clear()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Event handlers
        binding.clear.setOnClickListener { viewModel.clear() }
        binding.startOrPause.setOnClickListener { viewModel.startOrPause() }
        binding.pip.setOnClickListener {
            if (!pipCompatibilityManager.hasPictureInPictureSupport()) {
                Toast.makeText(this, pipCompatibilityManager.getPictureInPictureStatusMessageRes(), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            if (!pipCompatibilityManager.canEnterPictureInPicture(this)) {
                Toast.makeText(this, R.string.pip_disabled, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val success = pipCompatibilityManager.enterPictureInPictureSafely(this) {
                updatePictureInPictureParams(viewModel.started.value)
            }
            
            if (!success) {
                Toast.makeText(this, R.string.pip_failed, Toast.LENGTH_SHORT).show()
            }
        }
        binding.switchExample.setOnClickListener {
            startActivity(Intent(this@MainActivity, MovieActivity::class.java))
        }
        // Collect state from the viewModel using Flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.time.collect { time ->
                    binding.time.text = time
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.started.collect { started ->
                    binding.startOrPause.setImageResource(
                        if (started) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp
                    )
                    if (pipCompatibilityManager.hasPictureInPictureSupport()) {
                        updatePictureInPictureParams(started)
                    }
                }
            }
        }
        
        // Configure PiP button visibility and explanation text based on device support
        if (pipCompatibilityManager.hasPictureInPictureSupport()) {
            binding.pip.visibility = View.VISIBLE
            // Use default explanation text (from layout-v26) that mentions PiP
        } else {
            binding.pip.visibility = View.GONE
            // The layout already uses legacy explanation text via layout resource qualifiers
        }

        // Use trackPipAnimationHint view to make a smooth enter/exit pip transition.
        // See https://android.devsite.corp.google.com/develop/ui/views/picture-in-picture#smoother-transition
        if (pipCompatibilityManager.hasPictureInPictureSupport()) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    trackPipAnimationHintView(binding.stopwatchBackground)
                }
            }
        }

        // Handle events from the action icons on the picture-in-picture mode.
        if (pipCompatibilityManager.hasRemoteActionSupport()) {
            registerReceiver(broadcastReceiver, IntentFilter(ACTION_STOPWATCH_CONTROL))
        }
    }

    // This is called when the activity gets into or out of the picture-in-picture mode.
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide in-app buttons. They cannot be interacted in the picture-in-picture mode, and
            // their features are provided as the action icons.
            binding.clear.visibility = View.GONE
            binding.startOrPause.visibility = View.GONE
        } else {
            binding.clear.visibility = View.VISIBLE
            binding.startOrPause.visibility = View.VISIBLE
        }
    }

    /**
     * Updates the parameters of the picture-in-picture mode for this activity based on the current
     * [started] state of the stopwatch.
     */
    private fun updatePictureInPictureParams(started: Boolean): PictureInPictureParams? {
        if (!pipCompatibilityManager.hasPictureInPictureSupport()) {
            return null
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
                // Set action items for the picture-in-picture mode. These are the only custom controls
                // available during the picture-in-picture mode.
                .setActions(
                    listOfNotNull(
                        // "Clear" action.
                        createRemoteAction(
                            R.drawable.ic_refresh_24dp,
                            R.string.clear,
                            REQUEST_CLEAR,
                            CONTROL_TYPE_CLEAR
                        ),
                        if (started) {
                            // "Pause" action when the stopwatch is already started.
                            createRemoteAction(
                                R.drawable.ic_pause_24dp,
                                R.string.pause,
                                REQUEST_START_OR_PAUSE,
                                CONTROL_TYPE_START_OR_PAUSE
                            )
                        } else {
                            // "Start" action when the stopwatch is not started.
                            createRemoteAction(
                                R.drawable.ic_play_arrow_24dp,
                                R.string.start,
                                REQUEST_START_OR_PAUSE,
                                CONTROL_TYPE_START_OR_PAUSE
                            )
                        }
                    )
                )
                // Set the aspect ratio of the picture-in-picture mode.
                .setAspectRatio(Rational(16, 9))
            
            // Seamless resize control (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Disables the seamless resize. The seamless resize works great for videos where the
                // content can be arbitrarily scaled, but you can disable this for non-video content so
                // that the picture-in-picture mode is resized with a cross fade animation.
                builder.setSeamlessResizeEnabled(false)
            }
            
            // Enhanced features for Android 12+ (API 31+)
            if (pipCompatibilityManager.hasEnhancedPictureInPictureSupport()) {
                // Enable automatic PiP entry for improved gesture navigation support
                builder.setAutoEnterEnabled(true)
                
                // Add source rect hint for smoother animations
                val sourceRect = Rect().apply {
                    binding.stopwatchBackground.getGlobalVisibleRect(this)
                }
                builder.setSourceRectHint(sourceRect)
            }
            
            val params = builder.build()
            setPictureInPictureParams(params)
            return params
        }
        return null
    }

    /**
     * Creates a [RemoteAction]. It is used as an action icon on the overlay of the
     * picture-in-picture mode.
     */
    private fun createRemoteAction(
        @DrawableRes iconResId: Int,
        @StringRes titleResId: Int,
        requestCode: Int,
        controlType: Int
    ): RemoteAction? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            RemoteAction(
                Icon.createWithResource(this, iconResId),
                getString(titleResId),
                getString(titleResId),
                PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    Intent(ACTION_STOPWATCH_CONTROL)
                        .putExtra(EXTRA_CONTROL_TYPE, controlType),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            null
        }
    }
}
