package com.sceyt.chat.demo.call.ui.components

import androidx.compose.runtime.Stable
import org.webrtc.VideoTrack

/**
 * Stable wrapper for WebRTC [VideoTrack] to prevent unnecessary recompositions.
 * [VideoTrack] is a Java class not annotated with [@Stable]/[@Immutable], so Compose
 * treats it as unstable and cannot skip composables that receive it as a parameter.
 */
@Stable
class StableVideoTrack(val value: VideoTrack?)