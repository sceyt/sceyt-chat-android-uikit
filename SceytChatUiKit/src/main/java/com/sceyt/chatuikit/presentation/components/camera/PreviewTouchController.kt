package com.sceyt.chatuikit.presentation.components.camera

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView

class PreviewTouchController(
    private val previewView: PreviewView,
    private val onPinchZoom: (scaleFactor: Float) -> Unit,
    private val onTapFocus: (x: Float, y: Float, action: FocusMeteringAction) -> Unit,
    private val onZoomInteracted: () -> Unit
) {
    private var isPinching = false
    private var lastPinchEndTime = 0L
    private val tapFocusBlockAfterPinchMs = 250L

    private val scaleDetector = ScaleGestureDetector(
        previewView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isPinching = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                onPinchZoom(detector.scaleFactor)
                onZoomInteracted()
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isPinching = false
                lastPinchEndTime = SystemClock.elapsedRealtime()
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        previewView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            val now = SystemClock.elapsedRealtime()
            val blockTapFocus = isPinching || (now - lastPinchEndTime) < tapFocusBlockAfterPinchMs

            if (!blockTapFocus &&
                event.pointerCount == 1 &&
                event.action == MotionEvent.ACTION_UP
            ) {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                onTapFocus(event.x, event.y, action)
            }

            true
        }
    }

    fun detach() {
        previewView.setOnTouchListener(null)
    }
}