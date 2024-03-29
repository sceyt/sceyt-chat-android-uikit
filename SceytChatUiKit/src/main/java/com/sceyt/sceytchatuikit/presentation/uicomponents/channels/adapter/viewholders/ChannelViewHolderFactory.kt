package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

open class ChannelViewHolderFactory(context: Context) {
    protected val layoutInflater = LayoutInflater.from(context)
    protected val channelClickListenersImpl = ChannelClickListenersImpl()
    private var attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)? = null
    var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
        private set

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Default.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding: SceytItemChannelBinding = if (cachedViews.isNullOrEmpty()) {
            cacheViews(parent.context)
            SceytItemChannelBinding.inflate(layoutInflater, parent, false)
        } else {
            if (cachedViews!!.size < SceytKitConfig.CHANNELS_LOAD_SIZE / 2)
                cacheViews(parent.context, SceytKitConfig.CHANNELS_LOAD_SIZE / 2)

            cachedViews!!.pop().also {
                it.root.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
        return ChannelViewHolder(binding, channelClickListenersImpl, attachDetachListener, userNameBuilder)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        channelClickListenersImpl.setListener(listener)
    }

    fun setChannelAttachDetachListener(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        attachDetachListener = listener
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    protected val clickListeners get() = channelClickListenersImpl as ChannelClickListeners.ClickListeners

    protected fun getAttachDetachListener() = attachDetachListener

    companion object {
        private lateinit var asyncLayoutInflater: AsyncLayoutInflater
        private var cachedViews: Stack<SceytItemChannelBinding>? = Stack<SceytItemChannelBinding>()
        private var cacheJob: Job? = null

        fun cacheViews(context: Context, count: Int = SceytKitConfig.CHANNELS_LOAD_SIZE) {
            asyncLayoutInflater = AsyncLayoutInflater(context)

            cacheJob = (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
                for (i in 0..count) {
                    asyncLayoutInflater.inflate(R.layout.sceyt_item_channel, null) { view, _, _ ->
                        cachedViews?.push(SceytItemChannelBinding.bind(view))
                    }
                }
            }
        }

        fun clearCache() {
            cacheJob?.cancel()
            cachedViews?.clear()
            cachedViews = null
        }
    }

    open fun getItemViewType(item: ChannelListItem, position: Int): Int {
        return when (item) {
            is ChannelListItem.ChannelItem -> ChannelType.Default.ordinal
            is ChannelListItem.LoadingMoreItem -> ChannelType.Loading.ordinal
        }
    }

    enum class ChannelType {
        Loading, Default
    }
}