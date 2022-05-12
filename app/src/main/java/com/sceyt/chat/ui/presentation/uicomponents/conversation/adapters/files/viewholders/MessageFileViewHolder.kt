package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sceyt.chat.ui.databinding.SceytUiMessageFileItemBinding
import com.sceyt.chat.ui.extensions.toPrettySize
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem

class MessageFileViewHolder(
        private val binding: SceytUiMessageFileItemBinding
) : BaseFileViewHolder(binding.root) {

    init {
        with(binding.root) {
            setOnCreateContextMenuListener { menu, v, menuInfo ->
                return@setOnCreateContextMenuListener
            }
        }
    }

    override fun bindTo(item: FileListItem) {
        val file = (item as? FileListItem.File)?.file ?: return

        binding.apply {
            tvFileName.text = file.name
            //todo temporary
            tvFileSize.text = file.uploadedFileSize.toPrettySize()
/*
            if (isIncoming) {
                tvFileSize.text = attachment.uploadedFileSize.toPrettySize()
            } else {
                val size = if (attachment.uploadedFileSize == 0L) {
                    FileCompressorUtil.getFileSize(attachment.url)
                } else attachment.uploadedFileSize

                tvFileSize.text = size.toPrettySize()
            }*/

            /* uploadMutableLiveData.observe(itemView.context as LifecycleOwner) {
                 if (attachment.url != null && it[attachment.url] != null) {
                     if (it[attachment.url]?.progress != null) {
                         val intProgress = ((it[attachment.url]?.progress ?: 1f) * 100).toInt()
                         if (intProgress < 100) {
                             isLoading = true
                             mIsUploadingOrDownloading = true
                             mProgress = intProgress
                             circularProgressbar.progress = intProgress
                         } else {
                             isLoading = false
                             mIsUploadingOrDownloading = false
                         }
                     }
                 }
             }*/

            /*    val loadData = MessagesAdapter.getFileLoadData(attachment.name)
                if (!loadData.isLoading)
                    downloadWithUrl(attachment)

                isLoading = loadData.isLoading
                circularProgressbar.progress = loadData.progress.toInt()

                root.setOnClickListener {
                    if (!MessagesAdapter.getFileLoadData(attachment.name).isLoading)
                        handleClick(FileProvider.getUriForFile(itemView.context, BuildConfig.APPLICATION_ID + ".provider",
                            File(itemView.context.externalCacheDir, attachment.name)), itemView.context)
                }*/

            root.setOnLongClickListener {
                // callbacks.onLongClick(it)
                return@setOnLongClickListener false
            }
        }
    }

    private fun handleClick(uri: Uri?, context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "You may not have a proper app for viewing this content", Toast.LENGTH_SHORT).show()
        }
    }
}