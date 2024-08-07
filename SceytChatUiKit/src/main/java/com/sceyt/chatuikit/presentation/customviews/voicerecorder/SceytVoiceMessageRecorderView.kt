package com.sceyt.chatuikit.presentation.customviews.voicerecorder

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytRecordViewBinding
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.animatorListener
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.durationToMinSecShort
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import com.sceyt.chatuikit.extensions.permissionIgnored
import com.sceyt.chatuikit.extensions.runOnMainThread
import com.sceyt.chatuikit.extensions.screenWidthPx
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.sceytstyles.MessageInputStyle
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

class SceytVoiceMessageRecorderView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

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
    private lateinit var style: MessageInputStyle

    init {
        init()
    }

    private fun init() {
        binding = SceytRecordViewBinding.inflate(LayoutInflater.from(context), this)
        layoutTransition = LayoutTransition().apply { setDuration(200) }

        post {
            context.maybeComponentActivity()?.lifecycle?.addObserver(lifecycleEventObserver)
        }

        AudioPlayerHelper.addToggleCallback(TAG) {
            runOnMainThread {
                if (isRecording)
                    forceStopRecording()
            }
        }
    }

    private fun SceytRecordViewBinding.initViews() {
        showDefaultRecordButton()
        setupRecorder()
        applyStyle()

        btnCancel.setOnClickListener {
            isLocked = false
            stopRecording(RecordingBehaviour.CANCELED)
        }

        lockViewStopButton.setOnClickListener {
            isLocked = false
            stopRecording(RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.initViews()
    }

    internal fun setStyle(style: MessageInputStyle) {
        this.style = style
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun SceytRecordViewBinding.setupRecorder() {
        imageViewAudio.setOnTouchListener(OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                ACTION_DOWN -> {
                    if (isLocked) {
                        // If its already locked, unlock and send
                        isLocked = false
                    } else {
                        if (!context.checkAndAskPermissions(requestVoicePermissionLauncher, Manifest.permission.RECORD_AUDIO)
                                || layoutTransition.isRunning)
                            return@OnTouchListener false

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
                    stopRecordAndShowPreviewIfNeeded()
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
            imageViewAudio.translationY = 0f
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

    private fun stopRecordAndShowPreviewIfNeeded() {
        if (isRecording) {
            isLocked = false
            binding.stopRecording(RecordingBehaviour.LOCK_DONE_SHOW_PREVIEW)
        }
    }

    fun forceStopRecording() {
        if (isRecording) {
            stopTrackingAction = true
            isRecording = false
            isLocked = false
            binding.stopRecording(RecordingBehaviour.CANCELED)
        }
    }

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        if (event != Lifecycle.Event.ON_RESUME && !hasWindowFocus())
            stopRecordAndShowPreviewIfNeeded()
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
                imageViewAudio.animate()
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
        imageViewAudio.animate().apply {
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
        textViewTime.visibility = View.GONE
        icPoint.visibility = View.GONE
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
        binding.imageViewAudio.animate()
            .scaleXBy(1.2f)
            .scaleYBy(1.2f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator())
            .start()
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

    private val buttonZ get() = dpToPx(2f).toFloat()
    private val paddingNormal get() = dpToPx(7f)
    private val paddingRecording get() = dpToPx(12f)

    private fun SceytRecordViewBinding.showDefaultRecordButton() {
        colorAnimation?.cancel()
        imageViewAudio.translationZ = 0.0f
        imageViewAudio.cardElevation = 0.0f
        recording.isInvisible = true
        with(imageAudio) {
            setPadding(paddingNormal)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.accentColor))
            setImageDrawable(style.voiceRecordIcon)
            setColorFilter(getCompatColor(R.color.sceyt_color_on_primary))
        }
    }

    private fun SceytRecordViewBinding.showRecordingRecordButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        recording.isVisible = true
        with(imageAudio) {
            setPadding(paddingRecording)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.accentColor))
            setImageDrawable(style.voiceRecordIcon)
            setColorFilter(getCompatColor(R.color.sceyt_color_on_primary))
        }
    }

    private fun SceytRecordViewBinding.showRecordingLockedButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        with(imageAudio) {
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.accentColor))
            setImageDrawable(style.sendVoiceMessageIcon)
            setPadding(paddingRecording, paddingRecording,
                (paddingRecording - 2), paddingRecording)
        }
    }

    private fun SceytRecordViewBinding.showDeleteRecordButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        with(imageAudio) {
            setPadding(paddingRecording)
            backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytChatUIKit.theme.accentColor))
            setImageResource(R.drawable.sceyt_ic_delete_record)
            animateColor(this, getCompatColor(SceytChatUIKit.theme.accentColor), getCompatColor(R.color.sceyt_color_error))
        }
    }

    private fun SceytRecordViewBinding.showRecordingFromDeleteButton() {
        imageViewAudio.translationZ = buttonZ
        imageViewAudio.cardElevation = buttonZ
        imageAudio.setPadding(paddingRecording)
        imageAudio.setImageDrawable(style.voiceRecordIcon)
        imageAudio.setColorFilter(context.getCompatColor(R.color.sceyt_color_on_primary))
        animateColor(imageAudio, context.getCompatColor(R.color.sceyt_color_error), context.getCompatColor(SceytChatUIKit.theme.accentColor))
    }

    private fun animateColor(view: View, colorFrom: Int, colorTo: Int) {
        colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation?.duration = 200 // milliseconds
        colorAnimation?.addUpdateListener { animator ->
            view.backgroundTintList = ColorStateList.valueOf(animator.animatedValue as Int)
        }
        colorAnimation?.start()
    }

    private fun SceytRecordViewBinding.applyStyle() {
        recording.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColor))
        imageViewLockArrow.setTintColorRes(SceytChatUIKit.theme.accentColor)
        lockViewStopButton.setTintColorRes(SceytChatUIKit.theme.accentColor)
        imageViewLock.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
        btnCancel.setTextColor(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        textViewTime.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
        textViewSlideCancel.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
    }

    private fun showPermissionSettingsDialog() {
        SceytDialog.showSceytDialog(context,
            titleId = R.string.sceyt_voice_permission_disabled_title,
            descId = R.string.sceyt_voice_permission_disabled_desc,
            positiveBtnTitleId = R.string.sceyt_settings,
            replaceLastDialog = false,
            positiveCb = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            })
    }

    private val requestVoicePermissionLauncher = if (isInEditMode) null else context.maybeComponentActivity()?.run {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            initPermissionLauncher {
                onVoicePermissionResult(it)
            }
        } else null
    }

    private fun onVoicePermissionResult(isGranted: Boolean) {
        if (!isGranted && context.permissionIgnored(Manifest.permission.RECORD_AUDIO))
            showPermissionSettingsDialog()
    }

    fun setListener(listener: RecordingListener) {
        recordingListener = listener
    }

    fun setRecorderHeight(height: Int) {
        binding.recording.updateLayoutParams<ViewGroup.LayoutParams> { this.height = height }
    }
}