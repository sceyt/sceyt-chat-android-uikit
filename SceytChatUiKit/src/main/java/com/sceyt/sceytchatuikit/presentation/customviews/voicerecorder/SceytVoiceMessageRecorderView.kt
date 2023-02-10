package com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent.*
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.setPadding
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytRecordViewBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import java.util.*
import kotlin.math.abs

class SceytVoiceMessageRecorderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: SceytRecordViewBinding
    private val animBlink by lazy { AnimationUtils.loadAnimation(context, R.anim.sceyt_anim_blink) }
    private val animJump by lazy { AnimationUtils.loadAnimation(context, R.anim.sceyt_anim_jump) }
    private val animJumpFast by lazy { AnimationUtils.loadAnimation(context, R.anim.sceyt_anim_jump_fast) }
    private var stopTrackingAction = false
    var isRecording = false
        private set
    private var audioTotalTime = 0
    private var timerTask: TimerTask? = null
    private var audioTimer: Timer? = null
    private var lastX = 0f
    private var lastY = 0f
    private var firstX = 0f
    private var firstY = 0f
    private val directionOffset = 0f
    private var cancelOffset = 0f
    private var lockOffset = 0f
    private var isLocked = false
    private var userBehaviour = UserBehaviour.NONE
    private var recordingListener: RecordingListener? = null
    private var isLayoutDirectionRightToLeft = false
    private var colorAnimation: ValueAnimator? = null

    init {
        init()
    }

    private fun init() {
        binding = SceytRecordViewBinding.inflate(LayoutInflater.from(context), this, true)
        with(binding) {
            showDefaultRecordButton()
            setupRecorder()
            setupStyle()

            btnCancel.setOnClickListener {
                isLocked = false
                stopRecording(RecordingBehaviour.CANCELED)
            }

            lockViewStopButton.setOnClickListener {
                isLocked = false
                stopRecording(RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun SceytRecordViewBinding.setupRecorder() {
        imageViewAudio.setOnTouchListener(OnTouchListener { view, motionEvent ->
            if (!context.hasPermissions(Manifest.permission.RECORD_AUDIO)) {
                context.asComponentActivity().requestPermissionsSafety(arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                return@OnTouchListener false
            }

            when (motionEvent.action) {
                ACTION_DOWN -> {
                    if (isLocked) {
                        // If its already locked, unlock and send
                        isLocked = false
                    } else {
                        cancelOffset = screenWidthPx() / 2.8f
                        lockOffset = screenWidthPx() / 2.5f
                        if (firstX == 0f) {
                            firstX = motionEvent.rawX
                        }
                        if (firstY == 0f) {
                            firstY = motionEvent.rawY
                        }
                        startRecord()
                    }
                }
                ACTION_UP -> {
                    if (isRecording)
                        stopRecording(RecordingBehaviour.RELEASED)
                }
                ACTION_CANCEL -> {
                    stopRecording(RecordingBehaviour.CANCELED)
                }
                ACTION_MOVE -> {
                    if (stopTrackingAction) {
                        return@OnTouchListener true
                    }
                    var direction = UserBehaviour.NONE
                    val motionX = abs(firstX - motionEvent.rawX)
                    val motionY = abs(firstY - motionEvent.rawY)
                    if (if (isLayoutDirectionRightToLeft) motionX > directionOffset && lastX > firstX && lastY > firstY else motionX > directionOffset && lastX < firstX && lastY < firstY) {
                        if (if (isLayoutDirectionRightToLeft) motionX > motionY else motionX > motionY) {
                            direction = UserBehaviour.CANCELING
                        } else if (motionY > motionX && lastY < firstY) {
                            direction = UserBehaviour.LOCKING
                        }
                    } else if (if (isLayoutDirectionRightToLeft) motionX > motionY && motionX > directionOffset && lastX > firstX else motionX > motionY && motionX > directionOffset && lastX < firstX) {
                        direction = UserBehaviour.CANCELING
                    } else if (motionY > motionX && motionY > directionOffset && lastY < firstY) {
                        direction = UserBehaviour.LOCKING
                    }
                    if (direction == UserBehaviour.CANCELING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.rawY + imageViewAudio.width / 2f > firstY) {
                            userBehaviour = UserBehaviour.CANCELING
                        }
                        if (userBehaviour == UserBehaviour.CANCELING) {
                            translateX(-(firstX - motionEvent.rawX))
                        }
                    } else if (direction == UserBehaviour.LOCKING) {
                        if (userBehaviour == UserBehaviour.NONE || motionEvent.rawX + imageViewAudio.width / 2f > firstX) {
                            userBehaviour = UserBehaviour.LOCKING
                        }
                        if (userBehaviour == UserBehaviour.LOCKING) {
                            translateY(-(firstY - motionEvent.rawY))
                        }
                    }
                    lastX = motionEvent.rawX
                    lastY = motionEvent.rawY
                }
            }
            view.onTouchEvent(motionEvent)
            true
        })
    }

    private fun SceytRecordViewBinding.translateY(y: Float) {
        if (y < -lockOffset) {
            locked()
            binding.imageViewAudio.translationY = 0f
            return
        }
        if (layoutLock.visibility != View.VISIBLE) {
            layoutLock.visibility = View.VISIBLE

            showRecordingFromDeleteButton()
        }
        imageViewAudio.translationY = y
        layoutLock.translationY = y / 2
        imageViewAudio.translationX = 0f
    }

    private fun SceytRecordViewBinding.translateX(x: Float) {
        if (if (isLayoutDirectionRightToLeft) x > cancelOffset else x < -cancelOffset) {
            canceled()
            imageViewAudio.translationX = 0f
            textViewSlideCancel.translationX = 0f
            return
        }
        imageViewAudio.translationX = x
        textViewSlideCancel.translationX = x
        layoutLock.translationY = 0f
        imageViewAudio.translationY = 0f
        if (abs(x) < imageAudio.width) {
            if (layoutLock.visibility != View.VISIBLE) {
                layoutLock.visibility = View.VISIBLE

                showRecordingFromDeleteButton()
            }
        } else {
            if (layoutLock.visibility != View.GONE) {
                layoutLock.visibility = View.GONE

                showDeleteRecordButton()
            }
        }
    }

    private fun locked() {
        stopTrackingAction = true
        binding.stopRecording(RecordingBehaviour.LOCKED)
        isLocked = true
    }

    private fun canceled() {
        stopTrackingAction = true
        binding.stopRecording(RecordingBehaviour.CANCELED)
    }

    fun forceStopRecording() {
        if (isRecording) {
            stopTrackingAction = true
            isRecording = false
            isLocked = false
            binding.stopRecording(RecordingBehaviour.CANCELED)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus && isRecording) {
            isLocked = false
            binding.stopRecording(RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW)
        }
    }

    private fun SceytRecordViewBinding.stopRecording(recordingBehaviour: RecordingBehaviour) {
        stopTrackingAction = true
        firstX = 0f
        firstY = 0f
        lastX = 0f
        lastY = 0f
        userBehaviour = UserBehaviour.NONE
        textViewSlideCancel.translationX = 0f
        textViewSlideCancel.visibility = View.GONE
        layoutLock.visibility = View.GONE
        layoutLock.translationY = 0f
        imageViewLockArrow.clearAnimation()
        imageViewLock.clearAnimation()

        if (isLocked)
            return

        when (recordingBehaviour) {
            RecordingBehaviour.LOCKED -> {
                lockViewContainer.visibility = View.VISIBLE
                btnCancel.visibility = View.VISIBLE
                binding.imageViewAudio.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator())
                    .start()
                showRecordingLockedButton()
                recordingListener?.onRecordingLocked()
            }
            RecordingBehaviour.CANCELED -> {
                isRecording = false
                moveToInitialState()
                recordingListener?.onRecordingCanceled()
            }
            RecordingBehaviour.RELEASED, RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW, RecordingBehaviour.LOCK_DONE_SEND_IMMEDIATELY -> {
                isRecording = false
                moveToInitialState()
                val shouldShowPreview = recordingBehaviour == RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW
                recordingListener?.onRecordingCompleted(shouldShowPreview)
            }
        }
    }

    private fun SceytRecordViewBinding.moveToInitialState() {
        isRecording = false
        binding.imageViewAudio.animate().apply {
            scaleX(1f)
            scaleY(1f)
            translationX(0f)
            translationY(0f)
            duration = 100
            setListener(animatorListener(onAnimationEnd = {
                showDefaultRecordButton()
                setListener(null)
            }))
            interpolator = LinearInterpolator()
            start()
        }

        icPoint.clearAnimation()
        textViewTime.visibility = View.INVISIBLE
        icPoint.visibility = View.INVISIBLE
        lockViewContainer.visibility = View.GONE
        layoutEffect2.visibility = View.GONE
        layoutEffect1.visibility = View.GONE
        btnCancel.visibility = View.GONE
        timerTask?.cancel()
    }

    private fun SceytRecordViewBinding.startRecord() {
        recordingListener?.onRecordingStarted()

        isRecording = true

        showRecordingRecordButton()

        stopTrackingAction = false
        binding.imageViewAudio.animate().scaleXBy(0.7f).scaleYBy(0.7f).setDuration(200).setInterpolator(OvershootInterpolator()).start()
        textViewTime.visibility = View.VISIBLE
        layoutLock.visibility = View.VISIBLE
        textViewSlideCancel.visibility = View.VISIBLE
        icPoint.visibility = View.VISIBLE
        layoutEffect2.visibility = View.VISIBLE
        layoutEffect1.visibility = View.VISIBLE
        icPoint.startAnimation(animBlink)
        imageViewLockArrow.clearAnimation()
        imageViewLock.clearAnimation()
        imageViewLockArrow.startAnimation(animJumpFast)
        imageViewLock.startAnimation(animJump)

        if (audioTimer == null)
            audioTimer = Timer()

        timerTask = object : TimerTask() {
            override fun run() {
                textViewTime.post {
                    textViewTime.text = (audioTotalTime * 1000L).durationToMinSecShort()
                    audioTotalTime++
                }
            }
        }
        audioTotalTime = 0
        audioTimer?.schedule(timerTask, 0L, 1000L)
    }


    private val buttonZ get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)
    private val paddingNormal get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11f, context.resources.displayMetrics)
    private val paddingRecording get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, context.resources.displayMetrics)

    private fun SceytRecordViewBinding.showDefaultRecordButton() {
        colorAnimation?.cancel()
        imageViewAudio.translationZ = 0.0f
        imageViewAudio.cardElevation = 0.0f
        imageAudio.background = null
        imageAudio.setPadding(paddingNormal.toInt())
        imageAudio.setImageResource(R.drawable.sceyt_ic_voice)
        imageAudio.setColorFilter("#B2B6BE".toColorInt())
    }

    private fun SceytRecordViewBinding.showRecordingRecordButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ

        with(imageAudio) {
            setPadding(paddingRecording.toInt())
            setBackgroundResource(R.drawable.sceyt_bg_circle)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
            setImageResource(R.drawable.sceyt_ic_voice)
            setColorFilter(getCompatColor(R.color.sceyt_color_white))
        }
    }

    private fun SceytRecordViewBinding.showRecordingLockedButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        with(imageAudio) {
            setBackgroundResource(R.drawable.sceyt_bg_circle)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
            setImageResource(MessageInputViewStyle.sendMessageIcon)
            setPadding(paddingRecording.toInt(), paddingRecording.toInt(),
                (paddingRecording - 2).toInt(), paddingRecording.toInt())
        }
    }

    private fun SceytRecordViewBinding.showDeleteRecordButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        with(imageAudio) {
            setPadding(paddingRecording.toInt())
            setBackgroundResource(R.drawable.sceyt_bg_circle)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
            setImageResource(R.drawable.sceyt_ic_delete)
            animateColor(this, getCompatColor(SceytKitConfig.sceytColorAccent), getCompatColor(R.color.sceyt_color_red))
        }
    }

    private fun SceytRecordViewBinding.showRecordingFromDeleteButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        imageAudio.setPadding(paddingRecording.toInt())
        imageAudio.setImageResource(R.drawable.sceyt_ic_voice)
        imageAudio.setColorFilter(context.getCompatColor(R.color.sceyt_color_white))
        animateColor(imageAudio, context.getCompatColor(R.color.sceyt_color_red), context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    private fun animateColor(view: View, colorFrom: Int, colorTo: Int) {
        colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation?.duration = 200 // milliseconds
        colorAnimation?.addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
        colorAnimation?.start()
    }

    private fun SceytRecordViewBinding.setupStyle() {
        imageViewLockArrow.setColorFilter(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        lockViewStopButton.setColorFilter(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }

    fun setListener(listener: RecordingListener) {
        recordingListener = listener
    }
}