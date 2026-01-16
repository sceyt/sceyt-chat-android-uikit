package com.sceyt.chatuikit.presentation.components.camera.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.components.camera.CameraState.AllowedMode
import com.sceyt.chatuikit.presentation.components.camera.CameraState.CameraMode
import com.sceyt.chatuikit.styles.common.TextStyle
import kotlin.math.abs
import androidx.core.view.isVisible

class CameraModeSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val modesContainer: LinearLayout
    private val modeHighlight: View
    private val tvPhoto: TextView
    private val tvVideo: TextView
    private var currentMode = CameraMode.PHOTO
    private var allowedMode = AllowedMode.BOTH
    private var selectedTextColor = ContextCompat.getColor(context, android.R.color.white)
    private var unselectedTextColor = ColorUtils.setAlphaComponent(selectedTextColor, (0.6f * 255).toInt())

    var onModeSelected: ((CameraMode) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 50
        private val swipeVelocityThreshold = 50

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = (e2.x) - (e1?.x ?: 0f)
            val diffY = (e2.y) - (e1?.y ?: 0f)

            if (abs(diffX) > abs(diffY) && abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                if (diffX < 0) {
                    if (currentMode == CameraMode.PHOTO) {
                        selectMode(CameraMode.VIDEO, animate = true)
                    }
                } else {
                    if (currentMode == CameraMode.VIDEO) {
                        selectMode(CameraMode.PHOTO, animate = true)
                    }
                }
                return true
            }
            return false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = e.x
            val photoRect = IntArray(2)
            val videoRect = IntArray(2)
            tvPhoto.getLocationOnScreen(photoRect)
            tvVideo.getLocationOnScreen(videoRect)

            val viewLocation = IntArray(2)
            this@CameraModeSelectorView.getLocationOnScreen(viewLocation)

            val photoLeft = photoRect[0] - viewLocation[0]
            val photoRight = photoLeft + tvPhoto.width
            val videoLeft = videoRect[0] - viewLocation[0]
            val videoRight = videoLeft + tvVideo.width

            when (x) {
                in photoLeft.toFloat()..photoRight.toFloat() -> {
                    if (currentMode != CameraMode.PHOTO) {
                        selectMode(CameraMode.PHOTO, animate = true)
                    }
                }
                in videoLeft.toFloat()..videoRight.toFloat() -> {
                    if (currentMode != CameraMode.VIDEO) {
                        selectMode(CameraMode.VIDEO, animate = true)
                    }
                }
            }
            return true
        }
    })

    init {
        inflate(context, R.layout.sceyt_view_camera_mode_selector, this)
        modesContainer = findViewById(R.id.modesContainer)
        modeHighlight = findViewById(R.id.modeHighlight)
        tvPhoto = findViewById(R.id.tvPhoto)
        tvVideo = findViewById(R.id.tvVideo)

        isClickable = true
        isFocusable = true

        post {
            updateHighlightSize()
            updateSelection(CameraMode.PHOTO, animate = false)
        }
    }

    private fun selectMode(mode: CameraMode, animate: Boolean) {
        if (currentMode == mode) return
        currentMode = mode
        updateSelection(mode, animate)
        onModeSelected?.invoke(mode)
    }

    fun setSelectedMode(mode: CameraMode, animate: Boolean = false) {
        if (currentMode == mode) return
        currentMode = mode
        updateSelection(mode, animate)
    }

    fun setModeTexts(photoText: String, videoText: String) {
        tvPhoto.text = photoText
        tvVideo.text = videoText
        updateHighlightSizeAndSelection()
    }

    fun setAllowedMode(allowedMode: AllowedMode) {
        this.allowedMode = allowedMode
        when (allowedMode) {
            AllowedMode.BOTH -> {
                tvPhoto.visibility = VISIBLE
                tvVideo.visibility = VISIBLE
            }
            AllowedMode.PHOTO_ONLY -> {
                tvPhoto.visibility = VISIBLE
                tvVideo.visibility = GONE
                if (currentMode != CameraMode.PHOTO) setSelectedMode(CameraMode.PHOTO, animate = false)
            }
            AllowedMode.VIDEO_ONLY -> {
                tvPhoto.visibility = GONE
                tvVideo.visibility = VISIBLE
                if (currentMode != CameraMode.VIDEO) setSelectedMode(CameraMode.VIDEO, animate = false)
            }
        }
        updateHighlightSizeAndSelection()
    }

    fun setModeTextStyle(textStyle: TextStyle) {
        textStyle.apply(tvPhoto)
        textStyle.apply(tvVideo)
        updateHighlightSizeAndSelection()
    }

    fun setModeTextColors(selectedColor: Int, unselectedColor: Int) {
        selectedTextColor = selectedColor
        unselectedTextColor = unselectedColor
        updateSelection(currentMode, animate = false)
    }

    fun setHighlightBackground(background: Drawable?) {
        if (background != null) {
            modeHighlight.background = background
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allowedMode != AllowedMode.BOTH) return false
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    private fun updateSelection(mode: CameraMode, animate: Boolean) {
        val isPhoto = mode == CameraMode.PHOTO

        tvPhoto.setTextColor(if (isPhoto) selectedTextColor else unselectedTextColor)
        tvVideo.setTextColor(if (!isPhoto) selectedTextColor else unselectedTextColor)

        if (animate) {
            animateCentering(isPhoto)
        } else {
            centerSelectedItem(isPhoto)
        }
    }

    private fun updateHighlightSizeAndSelection() {
        post {
            updateHighlightSize()
            updateSelection(currentMode, animate = false)
        }
    }

    private fun updateHighlightSize() {
        val photoWidth = if (tvPhoto.isVisible) tvPhoto.width else 0
        val videoWidth = if (tvVideo.isVisible) tvVideo.width else 0
        val photoHeight = if (tvPhoto.isVisible) tvPhoto.height else 0
        val videoHeight = if (tvVideo.isVisible) tvVideo.height else 0
        val maxWidth = maxOf(photoWidth, videoWidth)
        val maxHeight = maxOf(photoHeight, videoHeight)
        if (maxWidth <= 0 || maxHeight <= 0) return

        val params = modeHighlight.layoutParams
        if (params.width != maxWidth || params.height != maxHeight) {
            params.width = maxWidth
            params.height = maxHeight
            modeHighlight.layoutParams = params
        }
    }

    private fun centerSelectedItem(isPhoto: Boolean) {
        modesContainer.post {
            val translation = calculateTranslation(isPhoto)
            modesContainer.translationX = translation
        }
    }

    private fun animateCentering(isPhoto: Boolean) {
        modesContainer.post {
            val targetTranslation = calculateTranslation(isPhoto)

            ValueAnimator.ofFloat(modesContainer.translationX, targetTranslation).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    modesContainer.translationX = animator.animatedValue as Float
                }
                start()
            }
        }
    }

    private fun calculateTranslation(isPhoto: Boolean): Float {
        val containerWidth = modesContainer.width
        if (containerWidth <= 0) return 0f

        val viewWidth = width - paddingLeft - paddingRight
        val viewCenterX = paddingLeft + viewWidth / 2f

        val selectedView = if (isPhoto) tvPhoto else tvVideo
        if (selectedView.visibility != VISIBLE) return 0f
        val selectedCenterInContainer = selectedView.left + selectedView.width / 2f

        val containerLeft = (width - containerWidth) / 2f

        val currentSelectedCenterX = containerLeft + selectedCenterInContainer

        return viewCenterX - currentSelectedCenterX
    }
}
