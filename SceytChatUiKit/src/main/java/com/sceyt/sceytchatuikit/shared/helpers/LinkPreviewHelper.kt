package com.sceyt.sceytchatuikit.shared.helpers

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.sceytchatuikit.persistence.mappers.toLinkPreviewDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class LinkPreviewHelper : SceytKoinComponent {
    private val attachmentMiddleWare: PersistenceAttachmentsMiddleWare by inject()
    private var scope: CoroutineScope

    constructor(context: Context) {
        scope = (context as? AppCompatActivity)?.lifecycleScope ?: initDefaultScope()
    }

    constructor(scope: CoroutineScope) {
        this.scope = scope
    }

    constructor() {
        scope = initDefaultScope()
    }

    private fun initDefaultScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getPreview(attachment: SceytAttachment,
                   successListener: PreviewCallback.Success? = null,
                   errorListener: PreviewCallback.Error? = null) {

        Log.i("LinkPreviewHelper", "getPreview: attachment.url: ${attachment.url}")

        scope.launch(Dispatchers.IO) {
            val link = attachment.url ?: return@launch withContext(Dispatchers.Main) {
                errorListener?.error(null)
            }
            val response = attachmentMiddleWare.getLinkPreviewData(link, attachment.messageTid)

            withContext(Dispatchers.Main) {
                when (response) {
                    is SceytResponse.Error -> errorListener?.error(response.message)
                    is SceytResponse.Success -> {
                        response.data?.let {
                            successListener?.success(it.toLinkPreviewDetails(link))
                        } ?: errorListener?.error(null)
                    }
                }
            }
        }
    }

    /** Be careful, after closing scope, you can't launch any other coroutines.*/
    fun close() {
        scope.cancel()
    }

    sealed interface PreviewCallback {
        fun interface Success : PreviewCallback {
            fun success(linkDetails: LinkPreviewDetails)
        }

        fun interface Error : PreviewCallback {
            fun error(message: String?)
        }
    }
}