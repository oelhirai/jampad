package com.jampad.data.di

import com.jampad.data.audio.AudioPlayer
import com.jampad.data.audio.AudioRecorder
import com.jampad.data.audio.LoopEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideLoopEngine(
        recorder: AudioRecorder,
        player: AudioPlayer,
    ): LoopEngine = LoopEngine(recorder, player)
}
