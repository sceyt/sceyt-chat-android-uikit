package com.sceyt.chatuikit.presentation.components.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.VideoRecordEvent
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.databinding.SceytActivityCustomCameraBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.extensions.setSafeOnClickListener
import com.sceyt.chatuikit.presentation.components.camera.CameraState.AllowedMode
import com.sceyt.chatuikit.presentation.components.camera.CameraState.CameraMode
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import com.sceyt.chatuikit.styles.camera.CustomCameraStyle
import kotlinx.coroutines.launch

class CustomCameraActivity : AppCompatActivity() {

    private lateinit var binding: SceytActivityCustomCameraBinding
    private lateinit var style: CustomCameraStyle
    private val viewModel: CustomCameraViewModel by viewModels {
        CustomCameraViewModelFactory {
            intent.getStringExtra(EXTRA_ALLOWED_MODE)?.let { AllowedMode.valueOf(it) } ?: AllowedMode.BOTH
        }
    }

    private lateinit var cameraController: CameraXController
    private lateinit var fileFactory: MediaFileFactory
    private lateinit var navigator: CameraNavigator
    private lateinit var permissionCoordinator: PermissionCoordinator
    private lateinit var filePickerHelper: FilePickerHelper

    private var touchController: PreviewTouchController? = null
    private var pendingAudioStart = false
    private var audioPermissionJustGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = CustomCameraStyle.Builder(this, null).build()
        binding = SceytActivityCustomCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.applyStyle()

        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding.root.applySystemWindowInsetsPadding(applyTop = true, applyRight = true, applyLeft = true, applyBottom = true)

        fileFactory = MediaFileFactory(this)
        navigator = CameraNavigator(this, previewLauncher)
        permissionCoordinator = PermissionCoordinator(this, permissionLauncher)
        filePickerHelper = FilePickerHelper(this)

        cameraController = CameraXController(
            appContext = applicationContext,
            lifecycleOwner = this,
            previewView = binding.previewView
        )

        observeState()
        setupUiListeners()

        if (permissionCoordinator.hasCameraPermission()) {
            initializeCamera()
        } else {
            permissionCoordinator.requestBasePermissions()
        }
    }

    private val previewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val filePath = result.data?.getStringExtra(CameraMediaPreviewActivity.EXTRA_RESULT_URI)
            val isVideo = result.data?.getBooleanExtra(CameraMediaPreviewActivity.EXTRA_RESULT_IS_VIDEO, false) ?: false
            if (filePath != null) navigator.returnResult(filePath, isVideo)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val cameraGranted = perms[Manifest.permission.CAMERA] ?: permissionCoordinator.hasCameraPermission()
        val audioGranted = perms[Manifest.permission.RECORD_AUDIO] ?: permissionCoordinator.hasAudioPermission()
        if (cameraGranted) {
            initializeCamera()
            if (pendingAudioStart) {
                pendingAudioStart = false
                if (audioGranted) {
                    audioPermissionJustGranted = true
                }
            }
        } else {
            permissionCoordinator.handleCameraDenied()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                UiBindings.render(binding, state, style)
                binding.modeSelector.setSelectedMode(state.currentMode)
            }
        }
    }

    private fun initializeCamera() {
        cameraController.init(
            onReady = { bindUseCasesFromState() },
            onError = { finish() }
        )
    }

    private fun bindUseCasesFromState() {
        val state = viewModel.state.value

        cameraController.bind(
            lensFacing = state.lensFacing,
            mode = state.currentMode,
            flashMode = state.flashMode,
            callbacks = cameraCallbacks
        )

        attachTouchController()
    }

    private val cameraCallbacks = object : CameraXController.Callbacks {
        override fun onZoomRatioChanged(ratio: Float, min: Float, max: Float) {
            viewModel.setZoomRatio(ratio)
        }

        override fun onHasFlashUnit(hasFlash: Boolean) {
            viewModel.setHasFlashUnit(hasFlash)
        }

        override fun onVideoEvent(event: VideoRecordEvent) {
            when (event) {
                is VideoRecordEvent.Start -> viewModel.startRecording()
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) viewModel.stopRecording()
                }
            }
        }
    }

    private fun attachTouchController() {
        touchController?.detach()
        touchController = PreviewTouchController(
            previewView = binding.previewView,
            onPinchZoom = { scale ->
                val current = cameraController.getCamera()?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                cameraController.setZoomRatio(current * scale)
            },
            onTapFocus = { x, y, action ->
                cameraController.startFocusAndMetering(action)
                showFocusIndicator(x, y)
            },
            onZoomInteracted = { viewModel.onZoomInteracted() }
        ).also { it.attach() }
    }

    private fun setupUiListeners() {
        binding.btnClose.setOnClickListener { finish() }

        binding.btnSwitchCamera.setOnClickListener {
            viewModel.switchCamera()
            bindUseCasesFromState()
        }

        binding.btnFlash.setOnClickListener {
            viewModel.toggleFlashMode()
            val s = viewModel.state.value
            cameraController.applyFlash(s.currentMode, s.flashMode)
        }

        binding.modeSelector.onModeSelected = { mode ->
            viewModel.setMode(mode)
            bindUseCasesFromState()
        }

        binding.btnCapture.setSafeOnClickListener {
            when (viewModel.state.value.currentMode) {
                CameraMode.PHOTO -> capturePhoto()
                CameraMode.VIDEO -> toggleVideoRecording()
            }
        }

        binding.btnGallery.setOnClickListener {
            openGalleryForAllowedMode()
        }

        binding.btnPauseResume.setOnClickListener { togglePauseResume() }
    }

    private fun capturePhoto() {
        val photoFile = fileFactory.createPhotoFile()
        cameraController.takePhoto(
            file = photoFile,
            onSaved = { navigator.openPhotoPreview(photoFile.absolutePath) },
            onError = { /* handle */ }
        )
    }

    private fun openGalleryForAllowedMode() {
        val filter = when (viewModel.state.value.allowedMode) {
            AllowedMode.PHOTO_ONLY -> BottomSheetMediaPicker.PickerFilterType.Image
            AllowedMode.VIDEO_ONLY -> BottomSheetMediaPicker.PickerFilterType.Video
            AllowedMode.BOTH -> BottomSheetMediaPicker.PickerFilterType.All
        }

        filePickerHelper.openMediaPicker(
            pickerListener = BottomSheetMediaPicker.PickerListener { items ->
                val first = items.firstOrNull() ?: return@PickerListener
                if (first.mediaType == BottomSheetMediaPicker.MediaType.Video) {
                    navigator.openVideoPreview(first.realPath)
                } else {
                    navigator.openPhotoPreview(first.realPath)
                }
            },
            filter = filter,
            maxSelectCount = 1
        )
    }

    private fun toggleVideoRecording() {
        if (cameraController.isRecording()) {
            cameraController.stopRecording()
            return
        }

        if (audioPermissionJustGranted) {
            audioPermissionJustGranted = false
            return
        }

        if (!permissionCoordinator.hasAudioPermission()) {
            pendingAudioStart = true
            permissionCoordinator.requestAudioPermission()
            return
        }

        startVideoRecording(withAudio = true)
    }

    private fun startVideoRecording(withAudio: Boolean) {
        val videoFile = fileFactory.createVideoFile()

        cameraController.startRecording(
            file = videoFile,
            withAudio = withAudio,
            callbacks = object : CameraXController.Callbacks {
                override fun onZoomRatioChanged(ratio: Float, min: Float, max: Float) = Unit
                override fun onHasFlashUnit(hasFlash: Boolean) = Unit

                override fun onVideoEvent(event: VideoRecordEvent) {
                    when (event) {
                        is VideoRecordEvent.Start -> viewModel.startRecording()
                        is VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {
                                viewModel.stopRecording()
                                navigator.openVideoPreview(videoFile.absolutePath)
                            } else {
                                viewModel.stopRecording()
                            }
                        }
                    }
                }
            }
        )
    }

    private fun togglePauseResume() {
        val state = viewModel.state.value
        if (!state.isRecording) return

        if (state.isPaused) {
            cameraController.resumeRecording()
            viewModel.resumeRecording()
        } else {
            cameraController.pauseRecording()
            viewModel.pauseRecording()
        }
    }

    private fun showFocusIndicator(x: Float, y: Float) {
        binding.focusIndicator.apply {
            this.x = x - width / 2
            this.y = y - height / 2
            alpha = 1f
            isVisible = true

            animate()
                .alpha(0f)
                .setDuration(style.focusIndicatorStyle.animDurationMs)
                .withEndAction { isVisible = false }
                .start()
        }
    }

    override fun onDestroy() {
        touchController?.detach()
        cameraController.unbindAll()
        super.onDestroy()
    }

    private fun SceytActivityCustomCameraBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        modeSelector.setBackgroundColor(style.modeSelectorStyle.backgroundColor)

        style.captureStyle.buttonBackground?.let { btnCapture.background = it }
        style.recordingTimeStyle.background?.let { tvRecordingTime.background = it }
        style.zoomStyle.background?.let { tvZoomLevel.background = it }
        style.focusIndicatorStyle.focusIcon?.let {
            focusIndicator.setImageDrawable(it)
        }

        style.closeIcon?.let {
            btnClose.background = it
            btnClose.setImageDrawable(null)
        }
        style.switchCameraIcon?.let {
            btnSwitchCamera.background = it
            btnSwitchCamera.setImageDrawable(null)
        }
        style.galleryIcon?.let {
            btnGallery.background = it
            btnGallery.setImageDrawable(null)
        }

        style.recordingTimeStyle.textStyle.apply(tvRecordingTime)
        style.zoomStyle.textStyle.apply(tvZoomLevel)

        modeSelector.setModeTexts(style.modeSelectorStyle.photoText, style.modeSelectorStyle.videoText)
        modeSelector.setModeTextStyle(style.modeSelectorStyle.textStyle)
        modeSelector.setModeTextColors(style.modeSelectorStyle.selectedTextColor, style.modeSelectorStyle.unselectedTextColor)
        modeSelector.setHighlightBackground(style.modeSelectorStyle.highlightBackground)
    }


    companion object {
        const val EXTRA_RESULT_URI = "result_uri"
        const val EXTRA_IS_VIDEO = "is_video"
        private const val EXTRA_ALLOWED_MODE = "allowed_mode"

        fun newIntent(context: Context, allowedMode: AllowedMode? = null): Intent {
            return Intent(context, CustomCameraActivity::class.java).apply {
                if (allowedMode != null) {
                    putExtra(EXTRA_ALLOWED_MODE, allowedMode.name)
                }
            }
        }

        fun launch(context: Context) {
            context.startActivity(Intent(context, CustomCameraActivity::class.java))
        }

        fun launchForPhoto(context: Context) {
            context.startActivity(Intent(context, CustomCameraActivity::class.java).apply {
                putExtra(EXTRA_ALLOWED_MODE, AllowedMode.PHOTO_ONLY.name)
            })
        }

        fun launchForVideo(context: Context) {
            context.startActivity(Intent(context, CustomCameraActivity::class.java).apply {
                putExtra(EXTRA_ALLOWED_MODE, AllowedMode.VIDEO_ONLY.name)
            })
        }
    }
}
