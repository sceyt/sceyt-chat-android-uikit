package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.extensions.getMimeTypeTakeFirstPart
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.messagebox.MessageBox
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindView
import com.sceyt.chat.util.FileUtils
import java.io.File

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels { MyViewModelFactory() }
    private var channelId: Long = 0L
    private var isGroup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)


        viewModel.bindView(binding.messagesListView, lifecycleOwner = this)
        viewModel.loadMessages(0, false)


        binding.messagesListView.setMessageClickListener(MessageClickListeners.ReactionLongClickListener { view, reactionItem ->
            Toast.makeText(this, "ReactionLongClick  " + reactionItem.messageItem.message.body, Toast.LENGTH_SHORT).show()
        })

        binding.messageBox.messageBoxActionCallback = object : MessageBox.MessageBoxActionCallback {
            override fun sendMessage(message: Message) {
                //   cancelReplay {
                viewModel.sendMessage(message)
                /// messagesList.scrollToPosition(listAdapter.itemCount - 1)
            }

            override fun editMessage(message: Message) {
                // viewModel.editMessage(message)
                //cancelReplay()
            }

            override fun addAttachments() {
                /* UIUtils.openChooserForUploadImage(requireContext()) { chooseType ->
                     when (chooseType) {
                         UIUtils.ProfilePhotoChooseType.CAMERA -> {
                             dispatchTakePictureIntent()
                         }
                         UIUtils.ProfilePhotoChooseType.GALLERY -> {
                             pickFile(chooseType)
                         }
                         else -> {
                             handleDocumentClicked()
                         }
                     }
                 }*/
                pickFile()
            }
        }
    }

    private fun pickFile() {
        if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            )
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            startActivityForResult(intent, 133)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                232
            )
        }
    }

    private fun handleDocumentClicked() {
        if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            val mimetypes = arrayOf(
                "application/*",
                "audio/*",
                "font/*",
                "message/*",
                "model/*",
                "multipart/*",
                "text/*"
            )
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                133
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                12
            )
        }
    }

    private fun getDataFromIntent() {
        channelId = intent.getLongExtra("channelId", 0)
        isGroup = intent.getBooleanExtra("isGroup", false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 133 && resultCode == RESULT_OK) {
            if (data?.clipData != null) {
                val files = mutableListOf<String>()
                for (i in 0 until (data.clipData?.itemCount ?: 0)) {
                    val uri = data.clipData?.getItemAt(i)?.uri
                    getPathFromAttachment(uri)?.let { path ->
                        files.add(path)
                    }
                }
                if (files.isNotEmpty())
                    addAttachmentFile(*files.toTypedArray())
            } else
                getPathFromAttachment(data?.data)?.let { path ->
                    addAttachmentFile(path)
                }
        }
    }

    private fun getPathFromAttachment(uri: Uri?): String? {
        uri ?: return null
        try {
            return FileUtils(this).getPath(uri)
        } catch (ex: Exception) {
        }
        return null
    }

    private fun addAttachmentFile(vararg filePath: String) {
        val attachments = mutableListOf<Attachment>()

        filePath.forEach { item ->
            val attachment = Attachment.Builder(item, getAttachmentType(item))
                .setName(File(item).name)
                .setMetadata(Gson().toJson(AttachmentMetadata(item)))
                .setUpload(true)
                .build()

            attachments.add(attachment)
        }
        binding.messageBox.addAttachments(attachments)
    }

    private fun getAttachmentType(path: String?): String {
        return when (val type = getMimeTypeTakeFirstPart(path)) {
            "image", "video" -> type
            else -> "file"
        }
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        @SuppressWarnings("unchecked")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            getDataFromIntent()
            return MessageListViewModel(channelId, isGroup) as T
        }
    }
}