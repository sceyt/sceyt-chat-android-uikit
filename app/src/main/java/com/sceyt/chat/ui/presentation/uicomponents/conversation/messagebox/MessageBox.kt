package com.sceyt.chat.ui.presentation.uicomponents.conversation.messagebox

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ViewMessageBoxBinding
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.messagebox.attachments.AttachmentFileViewHolder

class MessageBox(context: Context, attributeSet: AttributeSet) :
        LinearLayoutCompat(context, attributeSet) {

    var messageBoxActionCallback: MessageBoxActionCallback? = null

    private lateinit var attachmentsAdapter: AttachmentsAdapter
    private var allAttachments = mutableListOf<Attachment>()
    private val binding: ViewMessageBoxBinding

    var message: Message? = null
        set(value) {
            field = value

            if (value != null) {
                binding.editor.setText(message?.body)

                binding.editor.text?.let { text -> binding.editor.setSelection(text.length) }
                binding.editor.requestFocus()
                val imm: InputMethodManager? =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.toggleSoftInput(
                    InputMethodManager.SHOW_FORCED,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }

    var replayMessage: Message? = null
    var replayThreadMessageId: Long? = null

    init {
        binding = ViewMessageBoxBinding.inflate(LayoutInflater.from(context), this, true)
        setupAttachmentsList()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        determineState()

        binding.editor.doOnTextChanged { text, start, before, count -> determineState() }

        binding.sendMessage.setOnClickListener {

            val messageBody = binding.editor.text.toString().trim()
            if (messageBody != "" || allAttachments.isNotEmpty()) {
                if (message != null) {
                    message?.body = messageBody
                    message?.let {
                        messageBoxActionCallback?.editMessage(it)
                    }
                } else {
                    val messageToSend: Message?
                    if (message != null) {
                        message?.body = binding.editor.text.toString()

                        messageToSend = message
                    } else {
                        messageToSend = Message.MessageBuilder()
                            .setAttachments(allAttachments.toTypedArray())
                            .setType(getMessageType(allAttachments))
                            .setBody(binding.editor.text.toString())
                            .apply {
                                replayMessage?.let {
                                    setParentMessageId(it.id)
                                    setReplyInThread(replayThreadMessageId != null)
                                } ?: replayThreadMessageId?.let {
                                    setParentMessageId(it)
                                    setReplyInThread(true)
                                }
                            }.build().apply {
                                if (replayMessage != null)
                                    parent.body = replayMessage?.body
                            }
                    }
                    messageToSend?.let { msg -> messageBoxActionCallback?.sendMessage(msg) }
                }

                reset()
            }
        }

        binding.addAttachments.setOnClickListener {
            messageBoxActionCallback?.addAttachments()
        }
    }

    private fun getMessageType(attachments: List<Attachment>): String {
        if (attachments.isNotEmpty() && attachments.size == 1) {
            if (attachments[0].type.isEqualsVideoOrImage())
                return "media"
        }
        return "text"
    }

    fun reset() {
        message = null
        replayMessage = null
        allAttachments.clear()
        attachmentsAdapter.notifyDataSetChanged()
        binding.editor.text = null
        determineState()
    }

    private fun determineState() {
        if (binding.editor.text?.trim().isNullOrEmpty() && allAttachments.isEmpty()) {
            binding.sendMessage.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
        } else {
            binding.sendMessage.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary))
        }
    }

    fun addAttachments(attachments: List<Attachment>) {
        allAttachments.addAll(attachments)
        attachmentsAdapter.notifyDataSetChanged()
        determineState()
    }

    private fun setupAttachmentsList() {
        val listManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        attachmentsAdapter = AttachmentsAdapter(object : AttachmentFileViewHolder.Callbacks {
            override fun itemRemoved(item: Attachment?) {
                allAttachments.remove(item)
                attachmentsAdapter.notifyDataSetChanged()
                determineState()
            }
        }).apply {
            submitList(allAttachments)
        }

        binding.attachmentsList.apply {
            layoutManager = listManager
            adapter = attachmentsAdapter
        }
    }

    interface MessageBoxActionCallback {
        fun sendMessage(message: Message)
        fun editMessage(message: Message)
        fun addAttachments()
    }
}