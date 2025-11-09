package com.example.android.pictureinpicture.di

import com.example.android.pictureinpicture.util.AndroidFrameScheduler
import com.example.android.pictureinpicture.util.FrameScheduler
import com.example.android.pictureinpicture.util.SystemTimeProvider
import com.example.android.pictureinpicture.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    abstract fun bindTimeProvider(
        systemTimeProvider: SystemTimeProvider
    ): TimeProvider

    @Binds
    abstract fun bindFrameScheduler(
        androidFrameScheduler: AndroidFrameScheduler
    ): FrameScheduler

    companion object {
        @Provides
        @Singleton
        fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Main
    }
}