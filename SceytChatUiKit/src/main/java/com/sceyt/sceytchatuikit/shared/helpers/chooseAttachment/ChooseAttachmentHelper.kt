package com.sceyt.sceytchatuikit.shared.helpers.chooseAttachment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.extensions.initAttachmentLauncher
import com.sceyt.sceytchatuikit.extensions.initCameraLauncher
import kotlinx.coroutines.runBlocking
import java.io.File

class ChooseAttachmentHelper {
    private lateinit var context: Context
    private var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var requestGalleryPermissionLauncher: ActivityResultLauncher<String>
    private var requestFilesPermissionLauncher: ActivityResultLauncher<String>
    private var takePhotoLauncher: ActivityResultLauncher<Uri>
    private var addAttachmentLauncher: ActivityResultLauncher<Intent>
    private var allowMultiple: Boolean = true
    private var onlyImages: Boolean = true

    private var takePhotoPath: String? = null
    private var chooseFilesCb: ((List<String>) -> Unit)? = null
    private var takePictureCb: ((String) -> Unit)? = null

    constructor(activity: ComponentActivity) {
        with(activity) {
            this@ChooseAttachmentHelper.context = activity

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }
            requestGalleryPermissionLauncher = initPermissionLauncher {
                onGalleryPermissionResult(it)
            }
            requestFilesPermissionLauncher = initPermissionLauncher {
                onFilesPermissionResult(it)
            }

            takePhotoLauncher = initCameraLauncher {
                onTakePhotoResult(it)
            }

            addAttachmentLauncher = initAttachmentLauncher {
                onChooseFileResult(it)
            }
        }
    }

    constructor(fragment: Fragment) {
        with(fragment) {
            lifecycleScope.launchWhenResumed {
                this@ChooseAttachmentHelper.context = requireContext()
            }

            requestCameraPermissionLauncher = initPermissionLauncher {
                onCameraPermissionResult(it)
            }
            requestGalleryPermissionLauncher = initPermissionLauncher {
                onGalleryPermissionResult(it)
            }
            requestFilesPermissionLauncher = initPermissionLauncher {
                onFilesPermissionResult(it)
            }

            takePhotoLauncher = initCameraLauncher {
                onTakePhotoResult(it)
            }

            addAttachmentLauncher = initAttachmentLauncher {
                onChooseFileResult(it)
            }
        }
    }

    fun takePicture(result: (uri: String) -> Unit) {
        takePictureCb = result
        if (context.checkAndAskPermissions(requestCameraPermissionLauncher,
                    android.Manifest.permission.CAMERA)) {
            takePhotoLauncher.launch(getPhotoFileUri())
        }
    }

    fun chooseFromGallery(allowMultiple: Boolean, onlyImages: Boolean, result: (uris: List<String>) -> Unit) {
        chooseFilesCb = result
        this.onlyImages = onlyImages
        this.allowMultiple = allowMultiple
        if (context.checkAndAskPermissions(requestGalleryPermissionLauncher,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openGallery()
        }
    }

    fun chooseMultipleFiles(allowMultiple: Boolean, result: (uris: List<String>) -> Unit) {
        chooseFilesCb = result
        this.allowMultiple = allowMultiple
        if (context.checkAndAskPermissions(requestFilesPermissionLauncher,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            pickFile()
        }
    }

    private fun onTakePhotoResult(success: Boolean) {
        if (success) {
            takePhotoPath?.let { path ->
                takePictureCb?.invoke(path)
            }.also { takePhotoPath = null }
        }
    }

    private fun onChooseFileResult(result: ActivityResult) {
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val paths = mutableListOf<String>()
                for (i in 0 until (data.clipData?.itemCount ?: 0)) {
                    val uri = data.clipData?.getItemAt(i)?.uri
                    runBlocking {
                        context.getPathFromFile(uri)?.let { path ->
                            paths.add(path)
                        }
                    }
                }
                if (paths.isNotEmpty())
                    chooseFilesCb?.invoke(paths)
            } else
                context.getPathFromFile(data?.data)?.let { path ->
                    chooseFilesCb?.invoke(arrayListOf(path))
                }
        }
    }

    private fun onCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            takePhotoLauncher.launch(getPhotoFileUri())
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.CAMERA))
            context.shortToast("Please enable camera permission in settings")
    }

    private fun onGalleryPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            openGallery()
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            context.shortToast("Please enable storage permission in settings")
    }

    private fun onFilesPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            pickFile()
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            context.shortToast("Please enable storage permission in settings")
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
        if (onlyImages)
            intent.type = "image/*"
        addAttachmentLauncher.launch(intent)
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
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addAttachmentLauncher.launch(intent)
    }

    private fun getPhotoFileUri(): Uri {
        val directory = File(context.filesDir, "Photos")
        if (!directory.exists()) directory.mkdir()
        val file = File.createTempFile("Photo_${System.currentTimeMillis()}", ".jpg", directory)
        return context.getFileUriWithProvider(file).also { takePhotoPath = file.path }
    }
}