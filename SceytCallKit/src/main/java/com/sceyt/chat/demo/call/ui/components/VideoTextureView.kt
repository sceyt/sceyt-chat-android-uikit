package com.sceyt.chat.demo.call.ui.components

import android.content.Context
import android.content.res.Resources
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.TextureView
import com.callclient.call.providers.GlobalEGLProvider
import org.webrtc.EglBase
import org.webrtc.EglRenderer
import org.webrtc.GlRectDrawer
import org.webrtc.RendererCommon
import org.webrtc.ThreadUtils
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class VideoTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    TextureView(context, attrs, defStyle), TextureView.SurfaceTextureListener, VideoSink {

    private val resourceName: String = getResourceName()
    private val videoLayoutMeasure = RendererCommon.VideoLayoutMeasure()
    private var rendererEvents: RendererCommon.RendererEvents? = null
    private val eglRenderer: EglRenderer = EglRenderer(resourceName)
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var isFirstFrameRendered = false
    private var rotatedFrameWidth = 0
    private var rotatedFrameHeight = 0
    private var frameRotation = 0
    private var retryAttempt = AtomicInteger(0)

    init {
        surfaceTextureListener = this
    }

    /**
     * Initialise the renderer. Should be called from the main thread.
     *
     * @param sharedContext [org.webrtc.EglBase.Context]
     * @param configType Configuration type to use
     */
    private fun init(sharedContext: EglBase.Context, configType: IntArray = EglBase.CONFIG_PLAIN) {
        ThreadUtils.checkIsOnMainThread()
        eglRenderer.init(sharedContext, configType, GlRectDrawer())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Do not setup the renderer when using developer tools to avoid EGL14 runtime exceptions
        if (!isInEditMode) {
            retryAttempt.set(0)
            tryInitWithRetry()
        }
    }

    override fun onDetachedFromWindow() {
        uiThreadHandler.removeCallbacks(retryRunnable)
        eglRenderer.release()
        super.onDetachedFromWindow()
    }

    /**
     * Try to initialize with retry if it fails
     */
    private fun tryInitWithRetry() {
        tryInit().fold(
            onSuccess = {
                Log.d(
                    "VideoTextureView",
                    "EGL initialization successful on attempt ${retryAttempt.get() + 1}"
                )
                // Reset retry counter on success
                retryAttempt.set(0)
            },
            onFailure = { error ->
                val currentAttempt = retryAttempt.get()
                if (currentAttempt >= 3) {
                    Log.e(
                        "VideoTextureView",
                        "VideoTextureView, Failed to initialize EGL after $currentAttempt attempts, errod: ${error.message.orEmpty()}",
                    )
                    return
                }

                Log.e(
                    "VideoTextureView",
                    "Failed to initialize EGL on attempt ${currentAttempt + 1} error: ${error.message.orEmpty()}",
                )
                GlobalEGLProvider.recreate()
                uiThreadHandler.postDelayed(retryRunnable, 300)
            })
    }

    private val retryRunnable = Runnable {
        // Increment for next attempt and try initialization
        retryAttempt.incrementAndGet()
        tryInitWithRetry()
    }

    private fun tryInit() = runCatching {
        init(GlobalEGLProvider.baseContext)
    }

    fun setMirror(mirror: Boolean) {
        eglRenderer.setMirror(mirror)
    }

    fun setScalingType(scalingType: RendererCommon.ScalingType?) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(scalingType)
        requestLayout()
    }

    fun setScalingType(
        scalingTypeMatchOrientation: RendererCommon.ScalingType?,
        scalingTypeMismatchOrientation: RendererCommon.ScalingType?,
    ) {
        ThreadUtils.checkIsOnMainThread()
        videoLayoutMeasure.setScalingType(
            scalingTypeMatchOrientation,
            scalingTypeMismatchOrientation
        )
        requestLayout()
    }

    /**
     * Called when a new frame is received. Sends the frame to be rendered.
     *
     * @param videoFrame The [org.webrtc.VideoFrame] received from WebRTC connection to draw on the screen.
     */
    override fun onFrame(videoFrame: VideoFrame) {
        eglRenderer.onFrame(videoFrame)
        updateFrameData(videoFrame)
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        ThreadUtils.checkIsOnMainThread()
        val size =
            videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight)
        // When the surface size is set to wrap content, we need to calculate the correct aspect ratio.
        when (MeasureSpec.getMode(heightSpec)) {
            MeasureSpec.AT_MOST -> {
                val resolution =
                    updateSurfaceSize(rotatedFrameWidth, rotatedFrameHeight, size.x, size.y)
                setMeasuredDimension(resolution.width, resolution.height)
            }

            else -> setMeasuredDimension(size.x, size.y)
        }
    }

    // This is custom implementation to calculate the correct aspect ratio when the surface size is set to wrap content.
    private fun updateSurfaceSize(
        rotatedFrameWidth: Int,
        rotatedFrameHeight: Int,
        width: Int,
        height: Int
    ): Size {
        if (rotatedFrameHeight >= rotatedFrameWidth)
            return Size(width, height)

        val conf = rotatedFrameWidth / width.toFloat()
        val newH = (rotatedFrameHeight / conf).toInt()
        return Size(width, newH)
    }

    private fun updateFrameData(videoFrame: VideoFrame) {
        if (!isFirstFrameRendered) {
            rendererEvents?.onFirstFrameRendered()
            isFirstFrameRendered = true
        }

        if (videoFrame.rotatedWidth != rotatedFrameWidth ||
            videoFrame.rotatedHeight != rotatedFrameHeight ||
            videoFrame.rotation != frameRotation
        ) {
            rotatedFrameWidth = videoFrame.rotatedWidth
            rotatedFrameHeight = videoFrame.rotatedHeight
            frameRotation = videoFrame.rotation

            post { requestLayout() }

            uiThreadHandler.post {
                rendererEvents?.onFrameResolutionChanged(
                    rotatedFrameWidth,
                    rotatedFrameHeight,
                    frameRotation,
                )
            }
        }
    }

    /**
     * After the view is laid out we need to set the correct layout aspect ratio to the renderer so that the image
     * is scaled correctly.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        eglRenderer.setLayoutAspectRatio((right - left) / (bottom.toFloat() - top))
    }

    fun setRendererEventListener(listener: RendererCommon.RendererEvents) {
        rendererEvents = listener
    }

    /** Pause the egl renderer by reducing fps to 0. */
    fun pauseVideo() {
        eglRenderer.pauseVideo()
    }

    /** Resume the egl renderer by reducing fps to positive. */
    fun resumeVideo() {
        eglRenderer.disableFpsReduction()
    }

    /**
     * [SurfaceTextureListener] callback that lets us know when a surface texture is ready and we can draw on it.
     */
    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        eglRenderer.createEglSurface(surfaceTexture)
    }

    /**
     * [SurfaceTextureListener] callback that lets us know when a surface texture is destroyed we need to stop drawing
     * on it.
     */
    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        val completionLatch = CountDownLatch(1)
        eglRenderer.releaseEglSurface { completionLatch.countDown() }
        ThreadUtils.awaitUninterruptibly(completionLatch)
        return true
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    private fun getResourceName(): String {
        return try {
            resources.getResourceEntryName(id) + ": "
        } catch (e: Resources.NotFoundException) {
            ""
        }
    }
}