package com.sceyt.chatuikit.media.audio

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import org.junit.Test
import java.util.concurrent.Executor

class AudioPlayerCoordinatorTest {
    private val factory = FakeAudioPlayerFactory()
    private val coordinator = AudioPlayerCoordinator(
        executor = Executor { it.run() },
        playerFactory = factory::create
    )

    @Test
    fun init_startsPlayerAndPublishesPlayingState() {
        coordinator.init(FILE_A, TID_A)

        assertThat(factory.players).hasSize(1)
        assertThat(coordinator.state.value).isEqualTo(
            AudioPlayerState(
                filePath = FILE_A,
                messageTid = TID_A,
                status = AudioPlayerStatus.Playing,
                duration = DURATION
            )
        )
    }

    @Test
    fun switchingTracks_savesAndStopsPreviousPlayer() {
        coordinator.init(FILE_A, TID_A)
        val firstPlayer = factory.players.single()
        firstPlayer.position = 2_400
        coordinator.setPlaybackSpeed(FILE_A, TID_A, 1.5f)

        coordinator.init(FILE_B, TID_B)

        assertThat(firstPlayer.stopState).isEqualTo(AudioPlaybackState(2_400, 1.5f))
        assertThat(coordinator.getPlaybackState(FILE_A, TID_A))
            .isEqualTo(AudioPlaybackState(2_400, 1.5f))
        assertThat(coordinator.state.value.matches(FILE_B, TID_B)).isTrue()
        assertThat(coordinator.state.value.status).isEqualTo(AudioPlayerStatus.Playing)
    }

    @Test
    fun staleEventFromPreviousTrack_isIgnored() {
        coordinator.init(FILE_A, TID_A)
        val firstPlayer = factory.players.single()
        coordinator.init(FILE_B, TID_B)

        firstPlayer.emit(AudioPlayerStatus.Completed, position = 0)

        assertThat(coordinator.state.value.matches(FILE_B, TID_B)).isTrue()
        assertThat(coordinator.state.value.status).isEqualTo(AudioPlayerStatus.Playing)
    }

    @Test
    fun completedTrack_keepsSpeedAndCanReplay() {
        coordinator.init(FILE_A, TID_A)
        val player = factory.players.single()
        coordinator.setPlaybackSpeed(FILE_A, TID_A, 1.5f)

        player.emit(AudioPlayerStatus.Completed, position = 0)

        assertThat(coordinator.state.value.status).isEqualTo(AudioPlayerStatus.Completed)
        assertThat(coordinator.getPlaybackState(FILE_A, TID_A))
            .isEqualTo(AudioPlaybackState(0, 1.5f))

        coordinator.toggle(FILE_A, TID_A)

        assertThat(coordinator.state.value.status).isEqualTo(AudioPlayerStatus.Playing)
        assertThat(coordinator.state.value.speed).isEqualTo(1.5f)
    }

    @Test
    fun returningToPreviousTrack_restoresPositionAndSpeed() {
        coordinator.init(FILE_A, TID_A)
        factory.players.single().position = 3_200
        coordinator.setPlaybackSpeed(FILE_A, TID_A, 2f)
        coordinator.init(FILE_B, TID_B)

        coordinator.init(FILE_A, TID_A)

        val restoredPlayer = factory.players.last()
        assertThat(restoredPlayer.sourceFilePath).isEqualTo(FILE_A)
        assertThat(restoredPlayer.speed).isEqualTo(2f)
        assertThat(restoredPlayer.seekPositions).containsExactly(3_200L)
    }

    @Test
    fun initializationFailure_publishesErrorAndClearsCurrentPlayer() {
        factory.initializeResult = false

        coordinator.init(FILE_A, TID_A)

        assertThat(coordinator.state.value.status).isEqualTo(AudioPlayerStatus.Error)
        assertThat(coordinator.getCurrentPlayer()).isNull()
    }

    private class FakeAudioPlayerFactory {
        val players = mutableListOf<FakeAudioPlayer>()
        var initializeResult = true

        fun create(
            filePath: String,
            messageTid: MessageTid,
            onStateChanged: (AudioPlayerState) -> Unit
        ): AudioPlayer {
            return FakeAudioPlayer(
                sourceFilePath = filePath,
                messageTid = messageTid,
                initializeResult = initializeResult,
                onStateChanged = onStateChanged
            ).also(players::add)
        }
    }

    private class FakeAudioPlayer(
        val sourceFilePath: String,
        private val messageTid: MessageTid,
        private val initializeResult: Boolean,
        private val onStateChanged: (AudioPlayerState) -> Unit
    ) : AudioPlayer {
        var position = 0L
        var speed = 1f
        var status = AudioPlayerStatus.Initializing
        var stopState: AudioPlaybackState? = null
        val seekPositions = mutableListOf<Long>()

        override fun initialize(): Boolean = initializeResult

        override fun play() {
            emit(AudioPlayerStatus.Playing)
        }

        override fun pause() {
            emit(AudioPlayerStatus.Paused)
        }

        override fun stop(savedState: AudioPlaybackState?) {
            stopState = savedState
            emit(AudioPlayerStatus.Stopped, savedState?.position ?: 0)
        }

        override fun seekToPosition(position: Long) {
            this.position = position
            seekPositions += position
            emit(status)
        }

        override fun togglePlayPause() {
            emit(
                if (status == AudioPlayerStatus.Playing) {
                    AudioPlayerStatus.Paused
                } else {
                    AudioPlayerStatus.Playing
                }
            )
        }

        override fun setPlaybackSpeed(speed: Float) {
            this.speed = speed
            emit(status)
        }

        override fun getPlaybackSpeed(): Float = speed

        override fun getPlaybackPosition(): Long = position

        override fun getAudioDuration(): Long = DURATION

        override fun getFilePath(): String = sourceFilePath

        override fun getMessageTid(): MessageTid = messageTid

        override fun isPlaying(): Boolean = status == AudioPlayerStatus.Playing

        fun emit(status: AudioPlayerStatus, position: Long = this.position) {
            this.status = status
            this.position = position
            onStateChanged(
                AudioPlayerState(
                    filePath = sourceFilePath,
                    messageTid = messageTid,
                    status = status,
                    position = position,
                    duration = DURATION,
                    speed = speed
                )
            )
        }
    }

    private companion object {
        const val FILE_A = "/voice/a.aac"
        const val FILE_B = "/voice/b.aac"
        const val TID_A = 10L
        const val TID_B = 20L
        const val DURATION = 8_000L
    }
}
