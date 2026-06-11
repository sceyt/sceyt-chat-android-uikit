package com.sceyt.chatuikit.shared.helpers.picker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.asFragmentActivity
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.copyFile
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.getPermissionsForMangeStorage
import com.sceyt.chatuikit.extensions.initAttachmentLauncher
import com.sceyt.chatuikit.extensions.initCameraLauncher
import com.sceyt.chatuikit.extensions.initCustomCameraLauncher
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.initVideoCameraLauncher
import com.sceyt.chatuikit.extensions.oneOfPermissionsIgnored
import com.sceyt.chatuikit.extensions.permissionIgnored
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigateForResult
import com.sceyt.chatuikit.presentation.common.dialogs.SceytDialog
import com.sceyt.chatuikit.presentation.common.dialogs.SceytLoader
import com.sceyt.chatuikit.presentation.components.camera.CameraState
import com.sceyt.chatuikit.presentation.components.camera.CustomCameraActivity
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker.Companion.MAX_SELECT_MEDIA_COUNT
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import com.sceyt.chatuikit.shared.utils.FilePathUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class FilePickerHelper {
    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestVideoCameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestSceytGalleryPermissionLauncher: ActivityResultLauncher<String>? = null
    private var requestCustomCameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var takePhotoLauncher: ActivityResultLauncher<Uri>? = null
    private var takeVideoLauncher: ActivityResultLauncher<Uri>? = null
    private var addAttachmentLauncher: ActivityResultLauncher<Intent>? = null
    private var customCameraLauncher: ActivityResultLauncher<Intent>? = null
    private var allowMultiple: Boolean = true
    private var onlyImages: Boolean = true
    private var sceytGalleryFilter = BottomSheetMediaPicker.PickerFilterType.All
    private var sceytGalleryMaxSelectCount: Int = MAX_SELECT_MEDIA_COUNT
    private var chooseFilesCb: ((List<String>) -> Unit)? = null
    private var parentDirToCopyProvider: () -> File = { context.cacheDir }
    private var takePictureCb: ((String) -> Unit)? = null
    private var takeVideoCb: ((String) -> Unit)? = null
    private var customCameraCb: ((String, Boolean) -> Unit)? = null
    private var pendingCustomCameraMode: CameraState.AllowedMode? = null
    private var scope: CoroutineScope
    private var placeToSavePathsList: MutableSet<Pair<AttachmentTypeEnum, String>> = mutableSetOf()
    private val contextProvider: () -> Context

    private val context: Context
        get() = contextProvider()

    constructor(activity: ComponentActivity) {
        with(activity) {
            contextProvider = { this }
            scope = activity.lifecycleScope

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                return

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }
            requestVideoCameraPermissionLauncher = initPermissionLauncher {
                onVideoCameraPermissionResult(it)
            }

            requestSceytGalleryPermissionLauncher = initPermissionLauncher {
                onSceytGalleryPermissionResult(it)
            }

            requestCustomCameraPermissionLauncher = initPermissionLauncher {
                onCustomCameraPermissionResult(it)
            }

            takePhotoLauncher = initCameraLauncher {
                onTakePhotoResult(it)
            }

            takeVideoLauncher = initVideoCameraLauncher {
                onTakeVideoResult(it)
            }

            addAttachmentLauncher = initAttachmentLauncher {
                onChooseFileResult(it)
            }

            customCameraLauncher = initCustomCameraLauncher {
                onCustomCameraResult(it)
            }
        }
    }

    constructor(fragment: Fragment) {
        with(fragment) {
            contextProvider = { requireContext() }
            scope = fragment.lifecycleScope

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                return

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }

            requestVideoCameraPermissionLauncher = initPermissionLauncher {
                onVideoCameraPermissionResult(it)
            }

            requestSceytGalleryPermissionLauncher = initPermissionLauncher {
                onSceytGalleryPermissionResult(it)
            }

            requestCustomCameraPermissionLauncher = initPermissionLauncher {
                onCustomCameraPermissionResult(it)
            }

            takePhotoLauncher = initCameraLauncher {
                onTakePhotoResult(it)
            }

            takeVideoLauncher = initVideoCameraLauncher {
                onTakeVideoResult(it)
            }

            addAttachmentLauncher = initAttachmentLauncher {
                onChooseFileResult(it)
            }

            customCameraLauncher = initCustomCameraLauncher {
                onCustomCameraResult(it)
            }
        }
    }

    fun takePicture(result: (uri: String) -> Unit) {
        takePictureCb = result
        openCustomCamera(CameraState.AllowedMode.PHOTO_ONLY) { filePath, isVideo ->
            if (!isVideo) takePictureCb?.invoke(filePath)
        }
    }

    fun takeVideo(result: (uri: String) -> Unit) {
        takeVideoCb = result
        openCustomCamera(CameraState.AllowedMode.VIDEO_ONLY) { filePath, isVideo ->
            if (isVideo) takeVideoCb?.invoke(filePath)
        }
    }

    fun chooseFromGallery(
        allowMultiple: Boolean,
        onlyImages: Boolean,
        parentDirToCopyProvider: () -> File = { context.cacheDir },
        result: (uri: List<String>) -> Unit
    ) {
        chooseFilesCb = result
        this.parentDirToCopyProvider = parentDirToCopyProvider
        this.onlyImages = onlyImages
        this.allowMultiple = allowMultiple
        openGallery()
    }

    fun chooseMultipleFiles(
        allowMultiple: Boolean,
        mimetypes: Array<String>? = null,
        parentDirToCopyProvider: () -> File = { context.cacheDir },
        result: (uri: List<String>) -> Unit
    ) {
        chooseFilesCb = result
        this.parentDirToCopyProvider = parentDirToCopyProvider
        this.allowMultiple = allowMultiple
        pickFile(mimetypes)
    }

    fun openMediaPicker(
        filter: BottomSheetMediaPicker.PickerFilterType = sceytGalleryFilter,
        maxSelectCount: Int = sceytGalleryMaxSelectCount,
        vararg selections: String,
    ) {
        val permissions = getPermissionsForMangeStorage()
        sceytGalleryFilter = filter
        sceytGalleryMaxSelectCount = maxSelectCount
        if (context.checkAndAskPermissions(requestSceytGalleryPermissionLauncher, *permissions)) {
            openSceytGalleryPicker(
                filter = filter,
                maxSelectCount = maxSelectCount,
                selections = selections
            )
        }
    }

    fun openCustomCamera(result: (filePath: String, isVideo: Boolean) -> Unit) {
        openCustomCamera(CameraState.AllowedMode.BOTH, result)
    }

    fun openCustomCamera(
        allowedMode: CameraState.AllowedMode,
        result: (filePath: String, isVideo: Boolean) -> Unit
    ) {
        customCameraCb = result
        pendingCustomCameraMode = allowedMode
        if (context.checkAndAskPermissions(
                requestCustomCameraPermissionLauncher, Manifest.permission.CAMERA
            )
        ) {
            launchCustomCamera(allowedMode)
            pendingCustomCameraMode = null
        }
    }

    private fun launchCustomCamera(allowedMode: CameraState.AllowedMode) {
        customCameraLauncher?.let {
            SceytChatUIKit.navigator.navigateForResult(
                context = context,
                launcher = it,
                destination = Destination.CustomCamera(allowedMode)
            )
        }
    }

    private fun onTakePhotoResult(success: Boolean) {
        if (success) {
            placeToSavePathsList.lastOrNull()?.let { (_, path) ->
                takePictureCb?.invoke(path)
            }
        }
    }

    private fun onTakeVideoResult(success: Boolean) {
        if (success) {
            placeToSavePathsList.lastOrNull()?.let { (_, path) ->
                takeVideoCb?.invoke(path)
            }
        }
    }

    private fun onChooseFileResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val parentDir = parentDirToCopyProvider()
            val data = result.data
            if (data?.clipData != null) {
                val uris = mutableListOf<Uri>()
                for (i in 0 until (data.clipData?.itemCount ?: 0)) {
                    data.clipData?.getItemAt(i)?.uri?.let { uri ->
                        uris.add(uri)
                    }
                }
                scope.launch(Dispatchers.IO) {
                    val paths = getPathFromFile(parentDir, *uris.toTypedArray())
                    if (paths.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            placeToSavePathsList.addAll(paths.map { AttachmentTypeEnum.File to it })
                            chooseFilesCb?.invoke(paths)
                        }
                    } else withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.sceyt_could_not_open_file),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    val paths = getPathFromFile(parentDir, data?.data)
                    if (paths.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            placeToSavePathsList.addAll(paths.map { AttachmentTypeEnum.File to it })
                            chooseFilesCb?.invoke(paths)
                        }
                    } else withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.sceyt_could_not_open_file),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private suspend fun getPathFromFile(
        parentDir: File,
        vararg uris: Uri?
    ): List<String> {
        val paths = mutableListOf<String>()
        val filteredUris = uris.filterNotNull()
        if (filteredUris.isEmpty()) return emptyList()
        val debounceHelper by lazy { DebounceHelper(300L, scope) }

        filteredUris.forEach { uri ->
            try {
                debounceHelper.submitSuspendable {
                    withContext(Dispatchers.Main) { SceytLoader.showLoading(context) }
                }

                var realFile: File? = null
                try {
                    val path = FilePathUtil.getFilePathFromUri(
                        context = context,
                        parentDirToCopy = parentDir,
                        uri = uri
                    ) ?: return@forEach
                    FileInputStream(File(path))
                    realFile = File(path)
                } catch (ex: Exception) {
                    SceytLog.e(TAG, "error to get path with reason ${ex.message}")
                } finally {
                    if (realFile != null && realFile.exists()) {
                        paths.add(realFile.path)
                    } else {
                        val name = DocumentFile.fromSingleUri(context, uri)?.name
                        if (name != null) {
                            val copiedFile = copyFile(context, uri.toString(), name)
                            paths.add(copiedFile.path)
                        }
                    }
                }
            } catch (e: Exception) {
                SceytLog.e(TAG, "error to copy file with reason ${e.message}")
            }
        }
        debounceHelper.cancelLastDebounce()

        withContext(Dispatchers.Main) { SceytLoader.hideLoading() }
        return paths
    }

    private fun onCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            takePhotoLauncher?.launch(getPhotoFileUri())
        } else if (context.permissionIgnored(Manifest.permission.CAMERA))
            showPermissionDeniedDialog(
                titleId = R.string.sceyt_camera_permission_disabled_title,
                descId = R.string.sceyt_camera_permission_disabled_desc
            )
    }

    private fun onVideoCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            takeVideoLauncher?.launch(getVideoFileUri())
        } else if (context.permissionIgnored(Manifest.permission.CAMERA))
            showPermissionDeniedDialog(
                titleId = R.string.sceyt_camera_permission_disabled_title,
                descId = R.string.sceyt_camera_permission_disabled_desc
            )
    }

    private fun onSceytGalleryPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openSceytGalleryPicker()
        } else if (context.oneOfPermissionsIgnored(*getPermissionsForMangeStorage()))
            showPermissionDeniedDialog(
                titleId = R.string.sceyt_media_permission_disabled_title,
                descId = R.string.sceyt_media_permission_disabled_desc
            )
    }

    private fun onCustomCameraResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val filePath = result.data?.getStringExtra(CustomCameraActivity.EXTRA_RESULT_URI)
            val isVideo =
                result.data?.getBooleanExtra(CustomCameraActivity.EXTRA_IS_VIDEO, false) ?: false

            filePath?.let { path ->
                placeToSavePathsList.add(
                    (if (isVideo) AttachmentTypeEnum.Video else AttachmentTypeEnum.Image) to path
                )
                customCameraCb?.invoke(path, isVideo)
            }
        }
    }

    private fun onCustomCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            launchCustomCamera(pendingCustomCameraMode ?: CameraState.AllowedMode.BOTH)
            pendingCustomCameraMode = null
        } else if (context.permissionIgnored(Manifest.permission.CAMERA))
            showPermissionDeniedDialog(
                R.string.sceyt_camera_permission_disabled_title,
                R.string.sceyt_camera_permission_disabled_desc
            )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        if (onlyImages)
            intent.type = "image/*"
        addAttachmentLauncher?.launch(intent)
    }

    private fun openSceytGalleryPicker(
        filter: BottomSheetMediaPicker.PickerFilterType = sceytGalleryFilter,
        maxSelectCount: Int = sceytGalleryMaxSelectCount,
        vararg selections: String,
    ) {
        BottomSheetMediaPicker.instance(
            selections = selections,
            fileFilter = filter,
            maxSelectCount = maxSelectCount
        ).show(context.asFragmentActivity().supportFragmentManager, BottomSheetMediaPicker.TAG)
    }

    private fun pickFile(mimetypes: Array<String>?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        if (!mimetypes.isNullOrEmpty())
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addAttachmentLauncher?.launch(intent)
    }

    fun setSaveUrlsPlace(savePathsTo: MutableSet<Pair<AttachmentTypeEnum, String>>) {
        placeToSavePathsList = savePathsTo
    }

    private fun showPermissionDeniedDialog(titleId: Int, descId: Int) {
        SceytDialog.showDialog(
            context = context,
            titleId = titleId,
            descId = descId,
            positiveBtnTitleId = R.string.sceyt_settings,
            replaceLastDialog = false,
            positiveCb = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            })
    }

    private fun getPhotoFileUri(): Uri {
        val directory = File(context.filesDir, "Photos")
        if (!directory.exists()) directory.mkdir()
        val file = File.createTempFile("Photo_${UUID.randomUUID()}", ".jpg", directory)
        return context.getFileUriWithProvider(file).also {
            placeToSavePathsList.add(AttachmentTypeEnum.Image to file.path)
        }
    }

    private fun getVideoFileUri(): Uri {
        val directory = File(context.filesDir, "Videos")
        if (!directory.exists()) directory.mkdir()
        val file = File.createTempFile("Video_${UUID.randomUUID()}", ".mp4", directory)
        return context.getFileUriWithProvider(file).also {
            placeToSavePathsList.add(AttachmentTypeEnum.Video to file.path)
        }
    }
}
