package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.videoview.scalableVideoView

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RawRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.videoview.scalableVideoView.Size
import java.io.FileDescriptor
import java.io.IOException

class ScalableVideoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
) : TextureView(context, attrs, defStyle), SurfaceTextureListener, OnVideoSizeChangedListener {
    val mediaPlayer = MediaPlayer()
    private var mScalableType = ScalableType.NONE


    val currentPosition: Int
        get() = mediaPlayer.currentPosition
    val duration: Int
        get() = mediaPlayer.duration
    private val videoHeight: Int
        get() = mediaPlayer.videoHeight
    private val videoWidth: Int
        get() = mediaPlayer.videoWidth

    var isLooping: Boolean
        get() = mediaPlayer.isLooping
        set(looping) {
            mediaPlayer.isLooping = looping
        }
    val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    var onSurfaceUpdated: (() -> Unit)? = null

    init {
        if (attrs != null) {
            val a =
                    context.obtainStyledAttributes(attrs, R.styleable.ScalableVideoView, defStyle, 0)
            val scaleType =
                    a.getInt(R.styleable.ScalableVideoView_scalableType, ScalableType.NONE.ordinal)
            a.recycle()
            mScalableType = ScalableType.values().getOrNull(scaleType) ?: ScalableType.NONE
        }
        mediaPlayer.setOnVideoSizeChangedListener(this)
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int,
    ) {
        val surface = Surface(surfaceTexture)
        mediaPlayer.setSurface(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        Log.d("VideoView", "On surface updated")
        onSurfaceUpdated?.invoke()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            if (isPlaying) {
                pause()
            }
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
        }
    }

    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
        scaleVideoSize(width, height)
    }

    private fun scaleVideoSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth == 0 || videoHeight == 0) {
            return
        }
        val viewSize = Size(width, height)
        val videoSize = Size(videoWidth, videoHeight)
        val scaleManager = ScaleManager(viewSize, videoSize)
        val matrix = scaleManager.getScaleMatrix(mScalableType)
        matrix?.let { setTransform(it) }
    }

    fun setRawData(@RawRes id: Int) {
        try {
            val afd = resources.openRawResourceFd(id)
            setDataSource(afd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAssetData(assetName: String) {
        try {
            val manager = context.assets
            val afd = manager.openFd(assetName)
            setDataSource(afd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDataSource(afd: AssetFileDescriptor) {
        try {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDataSource(path: String) {
        try {
            reset()
            mediaPlayer.setDataSource(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDataSource(context: Context, uri: Uri, headers: Map<String?, String?>?) {
        try {
            reset()
            mediaPlayer.setDataSource(context, uri, headers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDataSource(context: Context, uri: Uri) {
        try {
            reset()
            mediaPlayer.setDataSource(context, uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDataSource(fd: FileDescriptor, offset: Long, length: Long) {
        try {
            reset()
            mediaPlayer.setDataSource(fd, offset, length)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDataSource(fd: FileDescriptor) {
        try {
            reset()
            mediaPlayer.setDataSource(fd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setScalableType(scalableType: ScalableType) {
        mScalableType = scalableType
        scaleVideoSize(videoWidth, videoHeight)
    }

    fun prepare(listener: OnPreparedListener? = null) {
        try {
            mediaPlayer.setOnPreparedListener(listener)
            mediaPlayer.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun prepareAsync(listener: OnPreparedListener? = null) {
        try {
            mediaPlayer.setOnPreparedListener(listener)
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnErrorListener(listener: OnErrorListener?) {
        mediaPlayer.setOnErrorListener(listener)
    }

    fun setOnCompletionListener(listener: OnCompletionListener?) {
        mediaPlayer.setOnCompletionListener(listener)
    }

    fun setOnInfoListener(listener: OnInfoListener?) {
        mediaPlayer.setOnInfoListener(listener)
    }

    fun pause() {
        mediaPlayer.pause()
    }

    fun seekTo(msec: Int) {
        mediaPlayer.seekTo(msec)
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer.setVolume(leftVolume, rightVolume)
    }

    fun start() {
        mediaPlayer.start()
    }

    fun stop() {
        mediaPlayer.stop()
    }

    fun reset() {
        mediaPlayer.reset()
    }

    fun release() {
        reset()
        mediaPlayer.release()
    }
}