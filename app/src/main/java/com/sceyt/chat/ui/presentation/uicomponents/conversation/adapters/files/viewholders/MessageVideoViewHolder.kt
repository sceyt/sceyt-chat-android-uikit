package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import com.sceyt.chat.ui.databinding.SceytUiMessageVideoItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem


class MessageVideoViewHolder(
        private val binding: SceytUiMessageVideoItemBinding,
) : BaseFileViewHolder(binding.root) {

    init {
        with(binding.root) {
            setOnCreateContextMenuListener { menu, v, menuInfo ->
                return@setOnCreateContextMenuListener
            }
        }
    }

    override fun bindTo(item: FileListItem) {
        val file = (item as? FileListItem.Video)?.file ?: return

        var openUrl: Uri? = null

        binding.apply {
            videoView.setVideoURI(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
            videoView.setOnPreparedListener { mediaPlayer ->
                val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
                val screenRatio = binding.videoView.width / binding.videoView.height.toFloat()
                val scaleX = videoRatio / screenRatio
                if (scaleX >= 1f) {
                    binding.videoView.scaleX = scaleX
                } else {
                    binding.videoView.scaleY = 1f / scaleX
                }
            }
            videoView.start()

            parentLayout.clipToOutline = true


            /* uploadMutableLiveData.observe(itemView.context as LifecycleOwner) {
                 if (item.url != null && it[item.url] != null) {
                     if (it[item.url]?.progress != null) {
                         val intProgress = ((it[item.url]?.progress ?: 1f) * 100)
                         if (intProgress < 100) {
                             isLoading = true
                             updateFileLoadData(item.name, isLoading = true, progress = intProgress.toDouble())
                             circularProgressbar.progress = intProgress.toInt()
                         } else {
                             isLoading = false
                             updateFileLoadData(item.name, isLoading = false, progress = intProgress.toDouble())
                             openUrl = Uri.fromFile(File(itemView.context.externalCacheDir, item.name))
                             startVideo(videoView, openUrl)
                         }
                     }
                 }
             }*/

            /* val loadData = getFileLoadData(item.name)
             if (!loadData.isLoading) {
                 downloadWithUrl(item)
                 startVideo(videoView, Uri.fromFile(File(itemView.context.externalCacheDir, item.name)))
             }

             isLoading = loadData.isLoading
             circularProgressbar.progress = loadData.progress.toInt()

             videoView.setOnPreparedListener { mediaPlayer ->
                 val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
                 val screenRatio = videoView.width / videoView.height.toFloat()
                 val scaleX = videoRatio / screenRatio
                 if (scaleX >= 1f) {
                     videoView.scaleX = scaleX
                 } else {
                     videoView.scaleY = 1f / scaleX
                 }
             }

             root.setOnClickListener {
                 if (!getFileLoadData(item.name).isLoading)
                     openUrl?.path?.let { path ->
                         handleClick(FileProvider.getUriForFile(itemView.context, BuildConfig.APPLICATION_ID + ".provider", File(path)), itemView.context)
                     }
             }*/

            /*  root.setOnLongClickListener {
                  callbacks.onLongClick(it)
                  return@setOnLongClickListener false
              }*/
        }
    }


    private fun startVideo(videoView: VideoView, uri: Uri?) {
        videoView.setVideoURI(uri)
        videoView.setOnErrorListener { _, _, _ -> return@setOnErrorListener true }
        videoView.start()
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