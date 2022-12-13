package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.isNull
import com.sceyt.sceytchatuikit.extensions.runOnMainThread
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.presentation.customviews.SceytVideoControllerView
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory

class MessageFilesAdapter(private val files: ArrayList<FileListItem>,
                          private var viewHolderFactory: FilesViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<FileListItem>>() {

    companion object{
        private val progressUpdateListener: HashMap<String, (TransferData) -> Unit> = hashMapOf()

        internal fun setListener(key: String, listener: (TransferData) -> Unit) {
            Log.i("sdfsdf", "setListener")
            //uploadProgressListener = null
            progressUpdateListener[key] = listener
            //  fileTransferData?.let { uploadProgressListener?.invoke(it) }
        }

        internal fun removeListener(key: String) {
            progressUpdateListener.remove(key)
        }

        fun update(data: TransferData) {
            Log.i("sdfsdf", "update    $data")
            // fileTransferData = data
         /*   val key = if (data.state == ProgressState.Uploading || data.state == ProgressState.PendingUpload
                    || data.state == ProgressState.Uploaded) {
                data.attachmentTid
            } else {
                data.url
            }*/

            val key: String = if (data.attachmentTid.isNull() || data.attachmentTid==0L) {
                data.url.toString()
            } else {
                data.attachmentTid.toString()
            }

            runOnMainThread {
                progressUpdateListener[key]?.invoke(data)
            }
            //uploadProgressListener?.invoke(data)
        }
    }
    val videoControllersList = arrayListOf<SceytVideoControllerView>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<FileListItem>, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<FileListItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun onItemDetached() {
        videoControllersList.forEach { it.release() }
        videoControllersList.clear()
    }

    fun getData() = files
}