package com.sceyt.chat.demo.call.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack

/**
 * Composable wrapper for VideoTextureView.
 * Uses the existing VideoTextureView implementation that handles EGL properly.
 */
@Composable
fun VideoRenderer(
    videoTrack: StableVideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
) {
    var currentRenderer by remember { mutableStateOf<VideoTextureView?>(null) }
    var currentTrack by remember { mutableStateOf<VideoTrack?>(null) }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                VideoTextureView(context).apply {
                    setMirror(mirror)
                    setScalingType(scalingType)
                    currentRenderer = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { renderer ->
                renderer.setMirror(mirror)
                renderer.setScalingType(scalingType)

                val track = videoTrack?.value
                if (track != currentTrack) {
                    currentTrack?.removeSink(renderer)
                    if (!renderer.isInEditMode) {
                        track?.addSink(renderer)
                    }
                    currentTrack = track
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            currentTrack?.removeSink(currentRenderer)
            currentTrack = null
            currentRenderer = null
        }
    }
}

/**
 * Composable for local video preview with mirror effect.
 */
@Composable
fun LocalVideoPreview(
    videoTrack: StableVideoTrack?,
    modifier: Modifier = Modifier
) {
    VideoRenderer(
        videoTrack = videoTrack,
        modifier = modifier,
        mirror = true,
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL
    )
}

/**
 * Composable for remote video (full screen).
 */
@Composable
fun RemoteVideoView(
    videoTrack: StableVideoTrack?,
    modifier: Modifier = Modifier
) {
    VideoRenderer(
        videoTrack = videoTrack,
        modifier = modifier,
        mirror = false,
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT
    )
}