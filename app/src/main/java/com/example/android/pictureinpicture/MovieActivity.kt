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

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.util.Linkify
import android.util.Rational
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import com.example.android.pictureinpicture.databinding.MovieActivityBinding
import com.example.android.pictureinpicture.util.PipCompatibilityManager
import com.example.android.pictureinpicture.widget.MovieView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Demonstrates usage of Picture-in-Picture when using [MediaSessionCompat].
 */
@AndroidEntryPoint
class MovieActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "MediaSessionPlaybackActivity"

        private const val MEDIA_ACTIONS_PLAY_PAUSE =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE

        private const val MEDIA_ACTIONS_ALL =
            MEDIA_ACTIONS_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        private const val PLAYLIST_SIZE = 2
    }

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: MovieActivityBinding

    @Inject
    lateinit var pipCompatibilityManager: PipCompatibilityManager
    
    private lateinit var session: MediaSessionCompat

    /**
     * Callbacks from the [MovieView] showing the video playback.
     */
    private val movieListener = object : MovieView.MovieListener() {

        override fun onMovieStarted() {
            // We are playing the video now. Update the media session state and the PiP window will
            // update the actions.
            updatePlaybackState(
                PlaybackStateCompat.STATE_PLAYING,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieStopped() {
            // The video stopped or reached its end. Update the media session state and the PiP
            // window will update the actions.
            updatePlaybackState(
                PlaybackStateCompat.STATE_PAUSED,
                binding.movie.getCurrentPosition(),
                binding.movie.getVideoResourceId()
            )
        }

        override fun onMovieMinimized() {
            // The MovieView wants us to minimize it. We enter Picture-in-Picture mode now.
            minimize()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MovieActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Linkify.addLinks(binding.explanation, Linkify.ALL)
        binding.pip.setOnClickListener { minimize() }
        binding.switchExample.setOnClickListener {
            startActivity(Intent(this@MovieActivity, MainActivity::class.java))
            finish()
        }

        // Configure parameters for the picture-in-picture mode. We do this at the first layout of
        // the MovieView because we use its layout position and size.
        if (pipCompatibilityManager.hasPictureInPictureSupport()) {
            binding.movie.doOnLayout { updatePictureInPictureParams() }
        }

        // Set up the video; it automatically starts.
        binding.movie.setMovieListener(movieListener)
        
        // Collect timer state from the shared repository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieViewModel.time.collect { time ->
                    binding.timerDisplay.text = time
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        session = MediaSessionCompat(this, TAG)
        session.isActive = true
        MediaControllerCompat.setMediaController(this, session.controller)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, binding.movie.title)
            .build()
        session.setMetadata(metadata)

        session.setCallback(MediaSessionCallback(binding.movie))

        val state = if (binding.movie.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        updatePlaybackState(
            state,
            MEDIA_ACTIONS_ALL,
            binding.movie.getCurrentPosition(),
            binding.movie.getVideoResourceId()
        )
    }

    override fun onStop() {
        super.onStop()
        // On entering Picture-in-Picture mode, onPause is called, but not onStop.
        // For this reason, this is the place where we should pause the video playback.
        binding.movie.pause()
        session.release()
    }

    override fun onRestart() {
        super.onRestart()
        if (!pipCompatibilityManager.isActivityInPictureInPictureMode(this)) {
            // Show the video controls so the video can be easily resumed.
            binding.movie.showControls()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustFullScreen(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            adjustFullScreen(resources.configuration)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide the controls in picture-in-picture mode.
            binding.movie.hideControls()
        } else {
            // Show the video controls if the video is not playing
            if (!binding.movie.isPlaying) {
                binding.movie.showControls()
            }
        }
    }

    private fun createPictureInPictureParams(): PictureInPictureParams? {
        // Calculate the aspect ratio of the PiP screen.
        val width = binding.movie.width
        val height = binding.movie.height
        
        if (width <= 0 || height <= 0) {
            // Use default 16:9 aspect ratio if video dimensions are invalid
            return pipCompatibilityManager.createPictureInPictureParams(
                aspectRatio = Rational(16, 9),
                sourceRectHint = null
            )
        }
        
        val aspectRatio = Rational(width, height)
        
        // Get the visible rectangle for smooth animation hints
        val visibleRect = Rect()
        binding.movie.getGlobalVisibleRect(visibleRect)

        return pipCompatibilityManager.createPictureInPictureParams(
            aspectRatio = aspectRatio,
            sourceRectHint = visibleRect
        )
    }

    private fun updatePictureInPictureParams(): PictureInPictureParams? {
        val params = createPictureInPictureParams()
        pipCompatibilityManager.setPictureInPictureParamsSafely(this, params)
        return params
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Automatically enter PiP mode when user navigates away (home button, gesture navigation)
        if (pipCompatibilityManager.hasPictureInPictureSupport() && 
            pipCompatibilityManager.canEnterPictureInPicture(this)) {
            
            pipCompatibilityManager.enterPictureInPictureSafely(this) {
                createPictureInPictureParams()
            }
        }
    }

    /**
     * Enters Picture-in-Picture mode.
     */
    private fun minimize() {
        if (!pipCompatibilityManager.hasPictureInPictureSupport()) {
            return
        }
        
        if (!pipCompatibilityManager.canEnterPictureInPicture(this)) {
            return
        }
        
        pipCompatibilityManager.enterPictureInPictureSafely(this) {
            createPictureInPictureParams()
        }
    }

    /**
     * Adjusts immersive full-screen flags depending on the screen orientation.

     * @param config The current [Configuration].
     */
    private fun adjustFullScreen(config: Configuration) {
        val insetsController = ViewCompat.getWindowInsetsController(window.decorView)
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            binding.scroll.visibility = View.GONE
            binding.movie.setAdjustViewBounds(false)
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            binding.scroll.visibility = View.VISIBLE
            binding.movie.setAdjustViewBounds(true)
        }
    }

    /**
     * Overloaded method that persists previously set media actions.

     * @param state The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     * @param mediaId The media id related to the video in the media session.
     */
    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        position: Int,
        mediaId: Int
    ) {
        val actions = session.controller.playbackState.actions
        updatePlaybackState(state, actions, position, mediaId)
    }

    private fun updatePlaybackState(
        @PlaybackStateCompat.State state: Int,
        playbackActions: Long,
        position: Int,
        mediaId: Int
    ) {
        val builder = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setActiveQueueItemId(mediaId.toLong())
            .setState(state, position.toLong(), 1.0f)
        session.setPlaybackState(builder.build())
    }

    /**
     * Updates the [MovieView] based on the callback actions. <br></br>
     * Simulates a playlist that will disable actions when you cannot skip through the playlist in a
     * certain direction.
     */
    private inner class MediaSessionCallback(
        private val movieView: MovieView
    ) : MediaSessionCompat.Callback() {

        private var indexInPlaylist: Int = 1

        override fun onPlay() {
            movieView.play()
        }

        override fun onPause() {
            movieView.pause()
        }

        override fun onSkipToNext() {
            movieView.startVideo()
            if (indexInPlaylist < PLAYLIST_SIZE) {
                indexInPlaylist++
                if (indexInPlaylist >= PLAYLIST_SIZE) {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                } else {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_ALL,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                }
            }
        }

        override fun onSkipToPrevious() {
            movieView.startVideo()
            if (indexInPlaylist > 0) {
                indexInPlaylist--
                if (indexInPlaylist <= 0) {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                } else {
                    updatePlaybackState(
                        PlaybackStateCompat.STATE_PLAYING,
                        MEDIA_ACTIONS_ALL,
                        movieView.getCurrentPosition(),
                        movieView.getVideoResourceId()
                    )
                }
            }
        }
    }
}
