package com.sceyt.chat.ui.presentation.uicomponents.messageinput

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.databinding.SceytMessageInputViewBinding
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.isTextMessage
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentItem
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.attachments.AttachmentFileViewHolder
import com.sceyt.chat.ui.sceytconfigs.MessageInputViewStyle
import com.sceyt.chat.ui.utils.UIUtils
import com.sceyt.chat.ui.utils.ViewUtil
import java.io.File

class MessageInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: SceytMessageInputViewBinding
    private var takePhotoPath: String? = null

    var messageBoxActionCallback: MessageBoxActionCallback? = null
    var message: Message? = null
        set(value) {
            field = value
            if (value != null) {
                binding.messageInput.setText(message?.body)
                binding.messageInput.text?.let { text -> binding.messageInput.setSelection(text.length) }
                context.showSoftInput(binding.messageInput)
            }
        }

    private var replayMessage: Message? = null
    private var replayThreadMessageId: Long? = null

    init {
        binding = SceytMessageInputViewBinding.inflate(LayoutInflater.from(context), this, true)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MessageInputView)
            MessageInputViewStyle.updateWithAttributes(context, a)
            a.recycle()
        }
        init()
        setupAttachmentsList()
    }


    private fun init() {

        binding.setUpStyle()
        determineState()

        binding.messageInput.doOnTextChanged { _, _, _, _ -> determineState() }

        binding.icSendMessage.setOnClickListener {
            val messageBody = binding.messageInput.text.toString().trim()
            if (messageBody != "" || allAttachments.isNotEmpty()) {
                if (message != null) {
                    message?.body = messageBody
                    message?.let {
                        messageBoxActionCallback?.editMessage(it)
                    }
                } else {
                    val messageToSend: Message? = Message.MessageBuilder()
                        .setAttachments(allAttachments.toTypedArray())
                        .setType(getMessageType(allAttachments))
                        .setBody(binding.messageInput.text.toString())
                        .apply {
                            replayMessage?.let {
                                setParentMessageId(it.id)
                                setReplyInThread(replayThreadMessageId != null)
                            } ?: replayThreadMessageId?.let {
                                setParentMessageId(it)
                                setReplyInThread(true)
                            }
                        }.build()

                    if (replayMessage != null)
                        messageToSend?.let { msg -> messageBoxActionCallback?.sendReplayMessage(msg, replayMessage) }
                    else
                        messageToSend?.let { msg -> messageBoxActionCallback?.sendMessage(msg) }
                }
                reset()
            }
        }

        binding.icAddAttachments.setOnClickListener {
            UIUtils.openFileChooser(context) { chooseType ->
                when (chooseType) {
                    UIUtils.ProfilePhotoChooseType.CAMERA -> {
                        if (context.checkAndAskPermissions(requestCameraPermissionLauncher,
                                    android.Manifest.permission.CAMERA)) {
                            takePhotoLauncher?.launch(getPhotoFileUri())
                        }
                    }
                    UIUtils.ProfilePhotoChooseType.GALLERY -> {
                        if (context.checkAndAskPermissions(requestGalleryPermissionLauncher,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            openGallery()
                        }
                    }
                    else -> {
                        if (context.checkAndAskPermissions(requestFilesPermissionLauncher,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            pickFile()
                        }
                    }
                }
            }
            messageBoxActionCallback?.addAttachments()
        }

        binding.layoutReplayMessage.icCancelReplay.setOnClickListener {
            cancelReplay()
        }
    }

    private fun SceytMessageInputViewBinding.setUpStyle() {
        icAddAttachments.setImageResource(MessageInputViewStyle.attachmentIcon)
        icSendMessage.setImageResource(MessageInputViewStyle.sendMessageIcon)
        messageInput.setTextColor(context.getCompatColor(MessageInputViewStyle.inputTextColor))
        messageInput.hint = MessageInputViewStyle.inputHintText
        messageInput.setHintTextColor(context.getCompatColor(MessageInputViewStyle.inputHintTextColor))
    }

    fun replayMessage(message: Message) {
        replayMessage = message
        with(binding.layoutReplayMessage) {
            ViewUtil.expandHeight(root, 1, 200)
            tvName.text = message.from.fullName.trim()
            tvMessageBody.text = if (message.isTextMessage())
                message.body.trim() else context.getString(R.string.attachment)
        }
    }

    fun cancelReplay(readyCb: (() -> Unit?)? = null) {
        if (replayMessage == null)
            readyCb?.invoke()
        else {
            replayMessage = null
            ViewUtil.collapseHeight(binding.layoutReplayMessage.root, 200) {
                binding.layoutReplayMessage.root.isVisible = false
                context.asAppCompatActivity()?.lifecycleScope?.launchWhenResumed { readyCb?.invoke() }
            }
        }
    }

    private fun getPhotoFileUri(): Uri {
        val directory = File(context.filesDir, "Photos")
        if (!directory.exists()) directory.mkdir()
        val file = File.createTempFile("Photo_${System.currentTimeMillis()}", ".jpg", directory)
        return context.getFileUriWithProvider(file).also { takePhotoPath = file.path }
    }

    private fun getMessageType(attachments: List<Attachment>): String {
        if (attachments.isNotEmpty() && attachments.size == 1) {
            if (attachments[0].type.isEqualsVideoOrImage())
                return "media"
        }
        return "text"
    }

    private fun reset() {
        message = null
        replayMessage = null
        allAttachments.clear()
        attachmentsAdapter.clear()
        binding.messageInput.text = null
        determineState()
    }

    private fun determineState() {
        if (binding.messageInput.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()) {
            binding.icSendMessage.alpha = 0.5f
        } else
            binding.icSendMessage.alpha = 1f
    }

    private fun addAttachments(attachments: List<Attachment>) {
        allAttachments.addAll(attachments)
        attachmentsAdapter.addItems(attachments.map { AttachmentItem(it) })
        determineState()
    }

    private fun setupAttachmentsList() {
        attachmentsAdapter = AttachmentsAdapter(allAttachments.map { AttachmentItem(it) } as ArrayList<AttachmentItem>,
            object : AttachmentFileViewHolder.Callbacks {
                override fun itemRemoved(item: AttachmentItem) {
                    attachmentsAdapter.removeItem(item)
                    determineState()
                }
            })

        binding.rvAttachments.adapter = attachmentsAdapter
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
        addAttachments(attachments)
    }

    private val takePhotoLauncher = context.asAppCompatActivity()?.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            takePhotoPath?.let { path ->
                addAttachmentFile(path)
            }.also { takePhotoPath = null }
        }
    }

    private val addAttachmentLauncher = context.asAppCompatActivity()?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data
            if (data?.clipData != null) {
                val paths = mutableListOf<String>()
                for (i in 0 until (data.clipData?.itemCount ?: 0)) {
                    val uri = data.clipData?.getItemAt(i)?.uri
                    context.getPathFromFile(uri)?.let { path ->
                        paths.add(path)
                    }
                }
                if (paths.isNotEmpty())
                    addAttachmentFile(*paths.toTypedArray())
            } else
                context.getPathFromFile(data?.data)?.let { path ->
                    addAttachmentFile(path)
                }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addAttachmentLauncher?.launch(intent)
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addAttachmentLauncher?.launch(intent)
    }

    private fun getAttachmentType(path: String?): String {
        return when (val type = getMimeTypeTakeFirstPart(path)) {
            "image", "video" -> type
            else -> "file"
        }
    }

    private val requestCameraPermissionLauncher = context.asAppCompatActivity()?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            takePhotoLauncher?.launch(getPhotoFileUri())
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.CAMERA))
            context.shortToast("Please enable camera permission in settings")
    }

    private val requestGalleryPermissionLauncher = context.asAppCompatActivity()?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openGallery()
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            context.shortToast("Please enable storage permission in settings")
    }

    private val requestFilesPermissionLauncher = context.asAppCompatActivity()?.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pickFile()
        } else if (context.checkDeniedOneOfPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE))
            context.shortToast("Please enable storage permission in settings")
    }

    interface MessageBoxActionCallback {
        fun sendMessage(message: Message)
        fun sendReplayMessage(message: Message, parent: Message?)
        fun editMessage(message: Message)
        fun addAttachments()
    }
}