package com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment

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
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.asFragmentActivity
import com.sceyt.sceytchatuikit.extensions.checkAndAskPermissions
import com.sceyt.sceytchatuikit.extensions.getFileUriWithProvider
import com.sceyt.sceytchatuikit.extensions.getPermissionsForMangeStorage
import com.sceyt.sceytchatuikit.extensions.initAttachmentLauncher
import com.sceyt.sceytchatuikit.extensions.initCameraLauncher
import com.sceyt.sceytchatuikit.extensions.initPermissionLauncher
import com.sceyt.sceytchatuikit.extensions.initVideoCameraLauncher
import com.sceyt.sceytchatuikit.extensions.oneOfPermissionsIgnored
import com.sceyt.sceytchatuikit.extensions.permissionIgnored
import com.sceyt.sceytchatuikit.imagepicker.GalleryMediaPicker
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.shared.utils.FileUtil
import com.sceyt.sceytchatuikit.shared.utils.ImageUriPathUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class ChooseAttachmentHelper {
    private lateinit var context: Context
    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var requestVideoCameraPermissionLauncher: ActivityResultLauncher<String>
    private var requestSceytGalleryPermissionLauncher: ActivityResultLauncher<String>
    private var takePhotoLauncher: ActivityResultLauncher<Uri>
    private var takeVideoLauncher: ActivityResultLauncher<Uri>
    private var addAttachmentLauncher: ActivityResultLauncher<Intent>
    private var allowMultiple: Boolean = true
    private var onlyImages: Boolean = true

    private var chooseFilesCb: ((List<String>) -> Unit)? = null
    private var takePictureCb: ((String) -> Unit)? = null
    private var takeVideoCb: ((String) -> Unit)? = null
    private lateinit var scope: CoroutineScope
    private val debounceHelper by lazy { DebounceHelper(300L, scope) }
    private var placeToSavePathsList: MutableSet<String> = mutableSetOf()

    constructor(activity: ComponentActivity) {
        with(activity) {
            this@ChooseAttachmentHelper.context = activity
            scope = activity.lifecycleScope

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }
            requestVideoCameraPermissionLauncher = initPermissionLauncher {
                onVideoCameraPermissionResult(it)
            }

            requestSceytGalleryPermissionLauncher = initPermissionLauncher {
                onSceytGalleryPermissionResult(it)
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
        }
    }

    constructor(fragment: Fragment) {
        with(fragment) {
            scope = fragment.lifecycleScope

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    this@ChooseAttachmentHelper.context = requireContext()
                }
            }

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }

            requestVideoCameraPermissionLauncher = initPermissionLauncher {
                onVideoCameraPermissionResult(it)
            }

            requestSceytGalleryPermissionLauncher = initPermissionLauncher {
                onSceytGalleryPermissionResult(it)
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
        }
    }

    fun takePicture(result: (uri: String) -> Unit) {
        takePictureCb = result
        if (context.checkAndAskPermissions(requestCameraPermissionLauncher,
                    Manifest.permission.CAMERA)) {
            takePhotoLauncher.launch(getPhotoFileUri())
        }
    }

    fun takeVideo(result: (uri: String) -> Unit) {
        takeVideoCb = result
        if (context.checkAndAskPermissions(requestVideoCameraPermissionLauncher, Manifest.permission.CAMERA)) {
            takeVideoLauncher.launch(getVideoFileUri())
        }
    }

    fun chooseFromGallery(allowMultiple: Boolean, onlyImages: Boolean, result: (uris: List<String>) -> Unit) {
        chooseFilesCb = result
        this.onlyImages = onlyImages
        this.allowMultiple = allowMultiple
        openGallery()
    }

    fun chooseMultipleFiles(allowMultiple: Boolean, result: (uris: List<String>) -> Unit) {
        chooseFilesCb = result
        this.allowMultiple = allowMultiple
        pickFile()
    }

    fun openSceytGallery(pickerListener: GalleryMediaPicker.PickerListener, vararg selections: String) {
        val permissions = getPermissionsForMangeStorage()
        if (context.checkAndAskPermissions(requestSceytGalleryPermissionLauncher, *permissions)) {
            openSceytGalleryPicker(pickerListener, *selections)
        } else GalleryMediaPicker.pickerListener = pickerListener
    }

    private fun onTakePhotoResult(success: Boolean) {
        if (success) {
            placeToSavePathsList.lastOrNull()?.let { path ->
                takePictureCb?.invoke(path)
            }
        }
    }

    private fun onTakeVideoResult(success: Boolean) {
        if (success) {
            placeToSavePathsList.lastOrNull()?.let { path ->
                takeVideoCb?.invoke(path)
            }
        }
    }

    private fun onChooseFileResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val uris = mutableListOf<Uri>()
                for (i in 0 until (data.clipData?.itemCount ?: 0)) {
                    data.clipData?.getItemAt(i)?.uri?.let { uri ->
                        uris.add(uri)
                    }
                }
                scope.launch(Dispatchers.IO) {
                    val paths = getPathFromFile(*uris.toTypedArray())
                    if (paths.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            placeToSavePathsList.addAll(paths)
                            chooseFilesCb?.invoke(paths)
                        }
                    } else withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.sceyt_could_not_open_file), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                scope.launch(Dispatchers.IO) {
                    val paths = getPathFromFile(data?.data)
                    if (paths.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            placeToSavePathsList.addAll(paths)
                            chooseFilesCb?.invoke(paths)
                        }
                    } else withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.sceyt_could_not_open_file), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun getPathFromFile(vararg uris: Uri?): List<String> {
        val paths = mutableListOf<String>()
        val filteredUris = uris.filterNotNull()
        if (filteredUris.isEmpty()) return emptyList()

        filteredUris.forEach { uri ->
            try {
                debounceHelper.submitSuspendable {
                    withContext(Dispatchers.Main) { SceytLoader.showLoading(context) }
                }

                var realFile: File? = null
                try {
                    val path = FileUtil(context).getPath(uri)
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
                            val copiedFile = ImageUriPathUtil.copyFile(context, uri.toString(), name)
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
            takePhotoLauncher.launch(getPhotoFileUri())
        } else if (context.permissionIgnored(Manifest.permission.CAMERA))
            showPermissionDeniedDialog(R.string.sceyt_camera_permission_disabled_title, R.string.sceyt_camera_permission_disabled_desc)
    }

    private fun onVideoCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            takeVideoLauncher.launch(getVideoFileUri())
        } else if (context.permissionIgnored(Manifest.permission.CAMERA))
            showPermissionDeniedDialog(R.string.sceyt_camera_permission_disabled_title, R.string.sceyt_camera_permission_disabled_desc)
    }

    private fun onSceytGalleryPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openSceytGalleryPicker()
        } else if (context.oneOfPermissionsIgnored(*getPermissionsForMangeStorage()))
            showPermissionDeniedDialog(R.string.sceyt_media_permission_disabled_title, R.string.sceyt_media_permission_disabled_desc)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        if (onlyImages)
            intent.type = "image/*"
        addAttachmentLauncher.launch(intent)
    }

    private fun openSceytGalleryPicker(pickerListener: GalleryMediaPicker.PickerListener? = GalleryMediaPicker.pickerListener,
                                       vararg selections: String) {
        GalleryMediaPicker.instance(selections = selections).apply {
            GalleryMediaPicker.pickerListener = pickerListener
        }.show(context.asFragmentActivity().supportFragmentManager, GalleryMediaPicker.TAG)
    }

    private fun pickFile() {
        val mimetypes = arrayOf(
            "application/*",
            "audio/*",
            "font/*",
            "message/*",
            "model/*",
            "multipart/*",
            "text/*")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, false)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addAttachmentLauncher.launch(intent)
    }

    fun setSaveUrlsPlace(savePathsTo: MutableSet<String>) {
        placeToSavePathsList = savePathsTo
    }

    private fun showPermissionDeniedDialog(titleId: Int, descId: Int) {
        SceytDialog.showSceytDialog(context,
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
        return context.getFileUriWithProvider(file).also { placeToSavePathsList.add(file.path) }
    }

    private fun getVideoFileUri(): Uri {
        val directory = File(context.filesDir, "Videos")
        if (!directory.exists()) directory.mkdir()
        val file = File.createTempFile("Video_${UUID.randomUUID()}", ".mp4", directory)
        return context.getFileUriWithProvider(file).also { placeToSavePathsList.add(file.path) }
    }
}