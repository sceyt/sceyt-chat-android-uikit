package com.sceyt.chatuikit.media.audio

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class AudioPlayerStateCollector(
    private val state: StateFlow<AudioPlayerState> = AudioPlayerHelper.state,
    private val onStateChanged: (AudioPlayerState) -> Unit
) {
    private var scope: CoroutineScope? = null
    private var collectionJob: Job? = null

    fun start(view: View) {
        val lifecycleOwner = view.findViewTreeLifecycleOwner()
        if (lifecycleOwner == null) {
            start()
        } else {
            start(lifecycleOwner)
        }
    }

    internal fun start(lifecycleOwner: LifecycleOwner) {
        if (collectionJob?.isActive == true) return
        collectionJob = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.collect(onStateChanged)
            }
        }
    }

    fun start() {
        if (collectionJob?.isActive == true) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        collectionJob = newScope.launch {
            state.collect(onStateChanged)
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        scope?.cancel()
        scope = null
    }
}
