package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.videoview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import com.sceyt.sceytchatuikit.databinding.SceytMediaControllerBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.applySystemWindowInsetsMargin
import java.lang.ref.WeakReference
import java.util.*


/**
 * A view containing controls for a MediaPlayer. Typically contains the
 * buttons like "Play/Pause", "Rewind", "Fast Forward" and a progress
 * slider. It takes care of synchronizing the controls with the state
 * of the MediaPlayer.
 *
 *
 * The way to use this class is to instantiate it programatically.
 * The MediaController will create a default set of controls
 * and put them in a window floating above your application. Specifically,
 * the controls will float above the view specified with setAnchorView().
 * The window will disappear if left idle for three seconds and reappear
 * when the user touches the anchor view.
 *
 *
 * Functions like show() and hide() have no effect when MediaController
 * is created in an xml layout.
 *
 * MediaController will hide and
 * show the buttons according to these rules:
 *
 *  *  The "previous" and "next" buttons are hidden until setPrevNextListeners()
 * has been called
 *  *  The "previous" and "next" buttons are visible but disabled if
 * setPrevNextListeners() was called with null listeners
 *  *  The "rewind" and "fastforward" buttons are shown unless requested
 * otherwise by using the MediaController(Context, boolean) constructor
 * with the boolean set to false
 *
 */
class VideoControllerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = SceytMediaControllerBinding.inflate(LayoutInflater.from(context), this, true)
    var isShowing = false
        private set
    private var isDragging = false
    var formatBuilder: StringBuilder? = null
    var formatter: Formatter? = null
    var player: MediaPlayerControl? = null
        set(value) {
            field = value
            updatePausePlay()
        }
    private var anchor: ViewGroup? = null
    private val controllerHandler = MessageHandler(this)


    val vc = ViewConfiguration.get(context)
    val mSlop = vc.scaledTouchSlop
    private var onMediaClickCallback: OnMediaClickCallback? = null
    private var playingListener: PlayingListener? = null


    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private val seekListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {
            show(3600000)
            isDragging = true

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            controllerHandler.removeMessages(SHOW_PROGRESS)
        }

        override fun onProgressChanged(bar: SeekBar, progress: Int, fromuser: Boolean) {
            if (player == null) {
                return
            }
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return
            }
            val duration = player?.duration?.toLong() ?: 0
            val newPosition = duration * progress / 100L
            player?.seekTo(newPosition.toInt())
            Log.i("sdfsfsdf",newPosition.toString())
            if (player != null) binding.tvCurrentPosition.text = stringForTime(newPosition.toInt())
        }

        override fun onStopTrackingTouch(bar: SeekBar) {
            isDragging = false
            /*  val duration = player?.duration?.toLong() ?: 0
             val newPosition = duration * bar.progress / 100L
             player?.seekTo(newPosition.toInt())
             if (player != null) binding.tvCurrentPosition.text = stringForTime(newPosition.toInt())
             //setProgress()
             updatePausePlay()
             show(sDefaultTimeout)

             // Ensure that progress is properly updated in the future,
             // the call to show() does not guarantee this because it is a
             // no-op if we are already showing.
             controllerHandler.sendEmptyMessage(SHOW_PROGRESS)*/
        }
    }


    init {
        formatBuilder = StringBuilder()
        formatter = Formatter(formatBuilder, Locale.getDefault())

        binding.btnPlayPauseBottom.setOnClickListener {
            doPauseResume()
        }

        binding.seekBar.setOnSeekBarChangeListener(seekListener)
        hide()

        setOnClickListener {
            if (isShowing) {
                hide()
            } else {
                show(sDefaultTimeout)
            }
            onMediaClickCallback?.onMediaClick()
        }

        onMediaClickCallback = context as? OnMediaClickCallback

        binding.timerView.applySystemWindowInsetsMargin(applyBottom = true)
    }

    fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            show(0)
        } else {
            hide()
        }
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    fun setAnchorView(view: ViewGroup?) {
        anchor = view
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private fun disableUnsupportedButtons() {
        if (player == null) {
            return
        }
        try {
            if (player?.canPause()?.not() == true) {
                binding.btnPlayPauseBottom.isEnabled = false
            }
        } catch (ex: IncompatibleClassChangeError) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }
    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    @JvmOverloads
    fun show(timeout: Int = sDefaultTimeout) {
        if (!isShowing && anchor != null) {
            binding.btnPlayPauseBottom.visibility = View.VISIBLE
            binding.timerView.visibility = View.VISIBLE
            setProgress()
            binding.btnPlayPauseBottom.requestFocus()
            disableUnsupportedButtons()
            isShowing = true
        }
        updatePausePlay()
        controllerHandler.sendEmptyMessage(SHOW_PROGRESS)
    }

    /**
     * Remove the controller from the screen.
     */
    fun hide() {
        if (anchor == null) {
            return
        }
        try {
            binding.btnPlayPauseBottom.visibility = View.GONE
            binding.timerView.visibility = View.GONE
            controllerHandler.removeMessages(SHOW_PROGRESS)
        } catch (ex: IllegalArgumentException) {
            Log.w("MediaController", "already removed")
        }
        isShowing = false
    }

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        formatBuilder!!.setLength(0)
        return if (hours > 0) {
            formatter!!.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            formatter!!.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun setProgress(): Int {
        if (player == null || isDragging) {
            return 0
        }
        val position = player!!.currentPosition
        val duration = player!!.duration
        if (duration > 0) {
            // use long to avoid overflow
            val pos = 100L * position / duration
            binding.seekBar.progress = pos.toInt()
        }
//            val percent = player!!.bufferPercentage
//        binding.seekBar!!.secondaryProgress = percent * 10
        binding.tvEndPosition.text = stringForTime(duration)
        binding.tvCurrentPosition.text = stringForTime(position)
        return position
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (player == null) {
            return true
        }
        val keyCode = event.keyCode
        val uniqueDown = (event.repeatCount == 0
                && event.action == KeyEvent.ACTION_DOWN)
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                if (uniqueDown) {
                    doPauseResume()
                    show(sDefaultTimeout)
                    binding.btnPlayPauseBottom.requestFocus()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (uniqueDown && !player!!.isPlaying) {
                    player!!.start()
                    updatePausePlay()
                    show(sDefaultTimeout)
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (uniqueDown && player?.isPlaying == true) {
                    player?.pause()
                    updatePausePlay()
                    show(sDefaultTimeout)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE -> {
                // don't show the controls for volume adjustment
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU -> {
                if (uniqueDown) {
                    hide()
                }
                return true
            }
            else -> {
                show(sDefaultTimeout)
                return super.dispatchKeyEvent(event)
            }
        }
    }

    private val pauseListener = OnClickListener {
        doPauseResume()
        show(sDefaultTimeout)
    }


    fun updatePausePlay() {
        if (player == null) {
            return
        }
        binding.btnPlayPauseBottom.isSelected = player?.isPlaying == true
        playingListener?.playing(player?.isPlaying == true)
    }

    private fun doPauseResume() {
        if (player == null) {
            return
        }
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.start()
        }
        updatePausePlay()
    }


    override fun setEnabled(enabled: Boolean) {
        binding.btnPlayPauseBottom.isEnabled = enabled
        binding.seekBar.isEnabled = enabled
        disableUnsupportedButtons()
        super.setEnabled(enabled)
    }

    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        event.className = VideoControllerView::class.java.name
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = VideoControllerView::class.java.name
    }

    fun reloadUI() {
        setProgress()
    }

    fun setPlayingListener(listener: PlayingListener) {
        playingListener = listener
    }

    fun interface PlayingListener {
        fun playing(playing: Boolean)
    }

    interface MediaPlayerControl {
        fun start()
        fun pause()
        val duration: Int
        val currentPosition: Int

        fun seekTo(pos: Int)
        val isPlaying: Boolean

        fun canPause(): Boolean
    }

    private class MessageHandler constructor(view: VideoControllerView) :
            Handler(Looper.getMainLooper()) {
        private val mView: WeakReference<VideoControllerView>

        init {
            mView = WeakReference(view)
        }

        override fun handleMessage(message: Message) {
            val view = mView.get()
            if (view?.player == null) {
                return
            }
            when (message.what) {
                FADE_OUT -> view.hide()
                SHOW_PROGRESS -> {
                    view.setProgress()
                    view.updatePausePlay()
                    if (!view.isDragging && view.isShowing && view.player?.isPlaying == true) {
                        val msg = obtainMessage(SHOW_PROGRESS)
                        sendMessageDelayed(msg, 60)
                    }
                }
            }
        }
    }

    companion object {
        private const val sDefaultTimeout = 3000
        private const val FADE_OUT = 1
        private const val SHOW_PROGRESS = 2
    }
}
