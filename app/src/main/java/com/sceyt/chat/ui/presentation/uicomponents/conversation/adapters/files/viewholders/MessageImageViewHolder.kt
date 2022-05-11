package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import android.view.View
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.databinding.RecyclerviewMessageImageItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem


class MessageImageViewHolder(
        private val binding: RecyclerviewMessageImageItemBinding) : BaseFileViewHolder(binding.root) {

    init {
        with(itemView) {
            setOnCreateContextMenuListener { menu, v, menuInfo ->
                return@setOnCreateContextMenuListener
            }
        }
    }

    override fun bindTo(item: FileListItem) {
        val file = (item as? FileListItem.Image)?.file ?: return

        // todo temporary
        Glide.with(binding.root)
            .load(file.url)
            .into(binding.fileImage)

        /*  itemView.imageCont.apply {
              cardElevation = 0F
          }

          itemView.apply {
              if (isIncoming) {
                  GlideImageLoader(fileImage, circularProgressbar, progress_circular_cont)
                      .load(item.url, RequestOptions())
              } else if (!isIncoming && !File(item.name).exists()) {
                  Glide.with(this)
                      .load(item.url)
                      .into(fileImage)
              } else {
                  Glide.with(this)
                      .load(File(item.name))
                      .diskCacheStrategy(DiskCacheStrategy.ALL)
                      .into(fileImage)
              }
              uploadMutableLiveData.observe(context as LifecycleOwner) {
                  if (item.url != null && it[item.url] != null) {
                      if (it[item.url]?.progress != null) {
                          progress_circular_cont.visibility = View.VISIBLE
                          val intProgress = (it[item.url]?.progress!! * 100).toInt()
                          if (intProgress < 100) {
                              progress_circular_cont.visibility = View.VISIBLE
                              circularProgressbar.progress = intProgress
                          } else {
                              progress_circular_cont.visibility = View.GONE
                          }
                      }
                  }
              }

              cancel_attachment.setOnClickListener {
                  itemClickListener.onClick(item)
              }

              setOnLongClickListener {
                  callbacks.onLongClick(it)
                  return@setOnLongClickListener false
              }*/
    }

    interface Callbacks {
        open fun onLongClick(view: View) {}
    }
}