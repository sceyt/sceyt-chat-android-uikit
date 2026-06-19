package com.sceyt.chatuikit.presentation.components.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class CameraXController(
    private val appContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    interface Callbacks {
        fun onZoomRatioChanged(ratio: Float, min: Float, max: Float)
        fun onHasFlashUnit(hasFlash: Boolean)
        fun onVideoEvent(event: VideoRecordEvent)
    }

    private var provider: ProcessCameraProvider? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var recording: Recording? = null

    fun getCamera(): Camera? = camera
    fun isRecording(): Boolean = recording != null

    fun init(onReady: () -> Unit, onError: (Throwable) -> Unit) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            try {
                provider = future.get()
                onReady()
            } catch (t: Throwable) {
                onError(t)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    @SuppressLint("RestrictedApi")
    fun bind(
        lensFacing: Int,
        mode: CameraState.CameraMode,
        flashMode: Int,
        callbacks: Callbacks
    ) {
        val p = provider ?: return
        p.unbindAll()

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.Builder(recorder)
            .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
            .build()

        camera = when (mode) {
            CameraState.CameraMode.PHOTO -> p.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture
            )

            CameraState.CameraMode.VIDEO -> p.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                videoCapture
            )
        }

        callbacks.onHasFlashUnit(camera?.cameraInfo?.hasFlashUnit() == true)

        camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { z ->
            callbacks.onZoomRatioChanged(z.zoomRatio, z.minZoomRatio, z.maxZoomRatio)
        }

        applyFlash(mode, flashMode)
    }

    fun unbindAll() {
        preview?.surfaceProvider = null
        provider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        videoCapture = null
        recording = null
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun startFocusAndMetering(action: FocusMeteringAction) {
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun applyFlash(mode: CameraState.CameraMode, flashMode: Int) {
        when (mode) {
            CameraState.CameraMode.PHOTO -> imageCapture?.flashMode = flashMode
            CameraState.CameraMode.VIDEO -> {
                val torch = flashMode != ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(torch)
            }
        }
    }

    fun takePhoto(
        file: File,
        shouldMirror: Boolean,
        onSaved: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val ic = imageCapture ?: return

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = shouldMirror
        }
        val output = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(metadata)
            .build()
        ic.takePicture(
            output,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        file: File,
        withAudio: Boolean,
        callbacks: Callbacks
    ) {
        val vc = videoCapture ?: return
        if (recording != null) return

        val output = FileOutputOptions.Builder(file).build()
        recording = vc.output
            .prepareRecording(appContext, output)
            .apply {
                if (withAudio && hasAudioPermission()) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(appContext)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                }
                callbacks.onVideoEvent(event)
            }
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun pauseRecording() {
        recording?.pause()
    }

    fun resumeRecording() {
        recording?.resume()
    }
}
