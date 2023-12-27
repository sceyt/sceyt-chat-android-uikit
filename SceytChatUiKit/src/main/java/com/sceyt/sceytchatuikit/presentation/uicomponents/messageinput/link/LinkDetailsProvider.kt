package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.link

import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.repositories.AttachmentsRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.mappers.toLinkPreviewDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class LinkDetailsProvider : SceytKoinComponent {
    private val attachmentRepository: AttachmentsRepository by inject()
    private var scope: CoroutineScope
    private var loadDetailsJob: Job? = null
    private var loadedLinks = mutableMapOf<String, LinkPreviewDetails>()

    constructor() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    constructor(scope: CoroutineScope) {
        this.scope = scope
    }

    fun loadLinkDetails(link: String, callback: (LinkPreviewDetails?) -> Unit) {
        loadDetailsJob?.cancel()
        if (loadedLinks.containsKey(link)) {
            callback(loadedLinks[link])
            return
        }
        loadDetailsJob = scope.launch(Dispatchers.IO) {
            val result = attachmentRepository.getLinkPreviewData(link)
            if (result is SceytResponse.Success && result.data != null) {
                val linkPreviewDetails = result.data.toLinkPreviewDetails(link)
                withContext(Dispatchers.Main) { callback(linkPreviewDetails) }
                loadedLinks[link] = linkPreviewDetails
            } else withContext(Dispatchers.Main) { callback(null) }
        }
    }

    fun cancel() {
        loadDetailsJob?.cancel()
    }
}