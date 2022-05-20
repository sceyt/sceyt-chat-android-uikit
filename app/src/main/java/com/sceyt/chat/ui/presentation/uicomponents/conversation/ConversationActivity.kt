package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.UploadData
import com.sceyt.chat.ui.databinding.ActivityConversationBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.presentation.uicomponents.conversation.messagebox.MessageBox
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.bindView
import com.sceyt.chat.ui.utils.FileCompressorUtil
import com.sceyt.chat.ui.utils.FileUtil
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

        /* binding.messagesListView.setMessageClickListener(MessageClickListeners.AddReactionClickListener {
             //Toast.makeText(this, "AddReactionClick  " + it.message.body, Toast.LENGTH_SHORT).show()
         })*/

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
        ){
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            )
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "image/*"
            startActivityForResult(intent, 133)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                232
            )
        }
    }

    private fun getDataFromIntent() {
        channelId = intent.getLongExtra("channelId", 0)
        isGroup = intent.getBooleanExtra("isGroup", false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 133) {
            if (resultCode != Activity.RESULT_OK)
                return

            if (data?.clipData != null) {
                val files = mutableListOf<String>()
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri: Uri = data.clipData!!.getItemAt(i).uri
                    files.add(FileUtils(this).getPath(uri))
                }
                addAttachmentFile(*files.toTypedArray())
            } else {
                val uri = data!!.data?.let {
                    val realPath = FileUtil(this).getPath(it)
                    /*  if (realPath.isNullOrBlank()) return
                      if (File(realPath).exists())
                          addAttachmentFile(realPath)
                      else*/ FileCompressorUtil.compress(this, it)?.let { file ->
                    addAttachmentFile(file.path)
                }
                }
            }
        }
    }

    private fun addAttachmentFile(vararg filePath: String) {
        val attachments = mutableListOf<Attachment>()
        val map = LinkedHashMap<String, UploadData>()
       // viewModel.liveDataUpload.postValue(map)

        filePath.forEach { item ->
            val attachment = Attachment.Builder(item, getAttachmentType(item))
                .setName(File(item).name)
                .setMetadata("{info:\"some info\"}")
                .setUpload(true)
                .build()

            attachment.apply {
                setUploaderProgress(object : ProgressCallback {
                    override fun onResult(pct: Float) {
                        map[item] = UploadData(pct, null)
                   //     viewModel.liveDataUpload.postValue(map)
                    }

                    override fun onError(e: SceytException?) {
                        map[item] = UploadData(null, e)
                      //  viewModel.liveDataUpload.postValue(map)
                    }
                })

                setUploaderCompletion(object : ActionCallback {
                    override fun onSuccess() {
                    }

                    override fun onError(e: SceytException?) {
                    }
                })
            }

            attachments.add(attachment)
        }
        binding.messageBox.addAttachments(attachments)
    }

    private fun getAttachmentType(path: String?): String {
        return when (val type = FileCompressorUtil.getMimeType(path)) {
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