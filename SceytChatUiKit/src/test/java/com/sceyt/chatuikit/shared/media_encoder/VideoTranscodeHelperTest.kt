package com.sceyt.chatuikit.shared.media_encoder

import android.app.Application
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.koin.SceytKoinApp
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class VideoTranscodeHelperTest {
    private lateinit var application: Application
    private lateinit var compressor: MockedStatic<CustomVideoCompressor>
    private val destinations = mutableListOf<String>()
    private val jobs = mutableListOf<Job>()
    private var activeListener: CompressionListener? = null

    @Before
    fun setUp() {
        stopKoin()
        application = RuntimeEnvironment.getApplication()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<Application> { application } })
        }
        compressor = Mockito.mockStatic(CustomVideoCompressor::class.java) { invocation ->
            when (invocation.method.name) {
                "start" -> {
                    destinations += invocation.getArgument<String>(3)
                    activeListener = invocation.getArgument<CompressionListener>(5)
                    Job().also { jobs += it }
                }

                "cancel" -> null
                else -> invocation.callRealMethod()
            }
        }
    }

    @After
    fun tearDown() {
        while (activeListener != null) finish(cancelled = true)
        compressor.close()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `resume before cancellation completes queues the replacement`() {
        val originalResults = mutableListOf<TranscodeResultEnum>()
        val replacementResults = mutableListOf<TranscodeResultEnum>()
        val original = transcode("/video.mp4", "original.mp4", originalResults)

        VideoTranscodeHelper.cancel(original)
        transcode("/video.mp4", "replacement.mp4", replacementResults)

        assertThat(destinations).hasSize(1)
        finish(cancelled = true)

        assertThat(destinations).hasSize(2)
        assertThat(File(destinations.last()).name).isEqualTo("replacement.mp4")
        assertThat(originalResults).containsExactly(TranscodeResultEnum.Cancelled)
        assertThat(replacementResults).isEmpty()

        finish()

        assertThat(replacementResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `pausing a queued replacement keeps only the latest resume`() {
        val discardedResults = mutableListOf<TranscodeResultEnum>()
        val latestResults = mutableListOf<TranscodeResultEnum>()
        val original = transcode("/video.mp4", "original.mp4", mutableListOf())
        VideoTranscodeHelper.cancel(original)
        val discarded = transcode("/video.mp4", "discarded.mp4", discardedResults)
        VideoTranscodeHelper.cancel(discarded)
        transcode("/video.mp4", "latest.mp4", latestResults)

        finish(cancelled = true)

        assertThat(destinations).hasSize(2)
        assertThat(File(destinations.last()).name).isEqualTo("latest.mp4")
        finish()

        assertThat(discardedResults).isEmpty()
        assertThat(latestResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `same source requests each receive a result sequentially`() {
        val firstResults = mutableListOf<TranscodeResultEnum>()
        val secondResults = mutableListOf<TranscodeResultEnum>()
        transcode("/video.mp4", "first.mp4", firstResults)
        transcode("/video.mp4", "second.mp4", secondResults)

        assertThat(destinations).hasSize(1)
        finish()

        assertThat(destinations).hasSize(2)
        assertThat(firstResults).containsExactly(TranscodeResultEnum.Success)
        assertThat(secondResults).isEmpty()
        finish()

        assertThat(secondResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `cancelling active request preserves queued request with same source`() {
        val firstResults = mutableListOf<TranscodeResultEnum>()
        val secondResults = mutableListOf<TranscodeResultEnum>()
        val first = transcode("/video.mp4", "first.mp4", firstResults)
        transcode("/video.mp4", "second.mp4", secondResults)

        VideoTranscodeHelper.cancel(first)
        finish(cancelled = true)

        assertThat(destinations).hasSize(2)
        assertThat(firstResults).containsExactly(TranscodeResultEnum.Cancelled)
        assertThat(secondResults).isEmpty()
        finish()

        assertThat(secondResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `cancelling queued request does not cancel active request with same source`() {
        val firstResults = mutableListOf<TranscodeResultEnum>()
        val discardedResults = mutableListOf<TranscodeResultEnum>()
        val nextResults = mutableListOf<TranscodeResultEnum>()
        transcode("/video.mp4", "first.mp4", firstResults)
        val discarded = transcode("/video.mp4", "discarded.mp4", discardedResults)
        transcode("/video.mp4", "next.mp4", nextResults)

        VideoTranscodeHelper.cancel(discarded)

        compressor.verify({ CustomVideoCompressor.cancel() }, Mockito.never())
        assertThat(destinations).hasSize(1)
        finish()

        assertThat(destinations).hasSize(2)
        assertThat(File(destinations.last()).name).isEqualTo("next.mp4")
        assertThat(firstResults).containsExactly(TranscodeResultEnum.Success)
        assertThat(discardedResults).isEmpty()
        finish()

        assertThat(nextResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `cancelling specific requests preserves other queued sources`() {
        val nextResults = mutableListOf<TranscodeResultEnum>()
        val discardedResults = mutableListOf<TranscodeResultEnum>()
        val first = transcode("/first.mp4", "first.mp4", mutableListOf())
        transcode("/next.mp4", "next.mp4", nextResults)
        val discarded = transcode("/first.mp4", "discarded.mp4", discardedResults)

        VideoTranscodeHelper.cancel(first)
        VideoTranscodeHelper.cancel(discarded)
        finish(cancelled = true)

        assertThat(File(destinations.last()).name).isEqualTo("next.mp4")
        finish()

        assertThat(destinations).hasSize(2)
        assertThat(nextResults).containsExactly(TranscodeResultEnum.Success)
        assertThat(discardedResults).isEmpty()
    }

    @Test
    fun `cancelled run that never reports still starts the queued replacement`() {
        val originalResults = mutableListOf<TranscodeResultEnum>()
        val replacementResults = mutableListOf<TranscodeResultEnum>()
        val original = transcode("/video.mp4", "original.mp4", originalResults)
        val originalListener = checkNotNull(activeListener)
        activeListener = null

        VideoTranscodeHelper.cancel(original)
        transcode("/video.mp4", "replacement.mp4", replacementResults)
        jobs.first().cancel()

        assertThat(destinations).hasSize(2)
        assertThat(File(destinations.last()).name).isEqualTo("replacement.mp4")
        assertThat(originalResults).containsExactly(TranscodeResultEnum.Cancelled)

        originalListener.onCancelled()

        assertThat(destinations).hasSize(2)
        assertThat(originalResults).containsExactly(TranscodeResultEnum.Cancelled)
        assertThat(replacementResults).isEmpty()

        finish()

        assertThat(replacementResults).containsExactly(TranscodeResultEnum.Success)
    }

    @Test
    fun `finished run reports once when its job completes afterwards`() {
        val firstResults = mutableListOf<TranscodeResultEnum>()
        val secondResults = mutableListOf<TranscodeResultEnum>()
        transcode("/first.mp4", "first.mp4", firstResults)
        transcode("/second.mp4", "second.mp4", secondResults)

        finish()
        jobs.first().cancel()

        assertThat(firstResults).containsExactly(TranscodeResultEnum.Success)
        assertThat(destinations).hasSize(2)
        assertThat(secondResults).isEmpty()

        finish()

        assertThat(secondResults).containsExactly(TranscodeResultEnum.Success)
    }

    private fun transcode(
        path: String,
        name: String,
        results: MutableList<TranscodeResultEnum>,
    ): File {
        val destination = File(application.cacheDir, name)
        VideoTranscodeHelper.transcodeAsResultWithCallback(
            destination = destination,
            path = path,
        ) { results += it.resultType }
        return destination
    }

    private fun finish(cancelled: Boolean = false) {
        val listener = checkNotNull(activeListener)
        activeListener = null
        if (cancelled) listener.onCancelled() else listener.onSuccess()
    }
}
