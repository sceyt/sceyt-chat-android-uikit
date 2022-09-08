package com.sceyt.sceytchatuikit.shared.helpers

import android.webkit.URLUtil
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URISyntaxException

class LinkPreviewHelper {
    private var scope: CoroutineScope

    constructor(scope: CoroutineScope) {
        this.scope = scope
    }

    constructor() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    fun getPreview(loadId: Long,
                   link: String,
                   successListener: PreviewCallback.Success? = null,
                   errorListener: PreviewCallback.Error? = null) {

        val url = URLUtil.guessUrl(link)
        scope.launch(Dispatchers.IO) {
            val doc: Document
            val previewMetaData = PreviewMetaData(messageId = loadId)
            try {
                doc = Jsoup.connect(URLUtil.guessUrl(url))
                    .timeout(10 * 1000)
                    .get()

                val elements = doc.getElementsByTag("meta")
                var title = doc.select("meta[property=og:title]").attr("content")
                if (title.isNullOrBlank()) {
                    title = doc.title()
                }
                previewMetaData.title = title

                //getDescription
                var description = doc.select("meta[name=description]").attr("content")
                if (description.isNullOrBlank()) {
                    description = doc.select("meta[name=Description]").attr("content")
                }
                if (description.isNullOrBlank()) {
                    description = doc.select("meta[property=og:description]").attr("content")
                }
                if (description.isNullOrBlank()) {
                    description = ""
                }
                previewMetaData.description = description


                // getMediaType
                val mediaTypes = doc.select("meta[name=medium]")
                val type = if (mediaTypes.size > 0) {
                    val media = mediaTypes.attr("content")
                    if (media == "image") "photo" else media
                } else {
                    doc.select("meta[property=og:type]").attr("content")
                }
                previewMetaData.mediaType = type


                //getImages
                val imageElements = doc.select("meta[property=og:image]")
                if (imageElements.size > 0) {
                    val image = imageElements.attr("content")
                    if (image.isNotEmpty()) {
                        previewMetaData.imageUrl = resolveURL(url, image)
                    }
                }
                if (previewMetaData.imageUrl.isNullOrBlank()) {
                    var src = doc.select("link[rel=image_src]").attr("href")
                    if (src.isNotEmpty()) {
                        previewMetaData.imageUrl = resolveURL(url, src)
                    } else {
                        src = doc.select("link[rel=apple-touch-icon]").attr("href")
                        if (src.isNotEmpty()) {
                            previewMetaData.imageUrl = resolveURL(url, src)
                            previewMetaData.favicon = resolveURL(url, src)
                        } else {
                            src = doc.select("link[rel=icon]").attr("href")
                            if (src.isNotEmpty()) {
                                previewMetaData.imageUrl = resolveURL(url, src)
                                previewMetaData.favicon = resolveURL(url, src)
                            } else {
                                src = doc.select("meta[itemprop=image]").attr("content")
                                if (src.isNotEmpty()) {
                                    previewMetaData.favicon = doc.location() + src
                                }
                            }
                        }
                    }
                }

                //Favicon
                var src = doc.select("link[rel=apple-touch-icon]").attr("href")
                if (src.isNotEmpty()) {
                    previewMetaData.favicon = resolveURL(url, src)
                } else {
                    src = doc.select("link[rel=icon]").attr("href")
                    if (src.isNotEmpty()) {
                        previewMetaData.favicon = resolveURL(url, src)
                    } else {
                        src = doc.select("link[rel=shortcut icon]").attr("href")
                        if (src.isNotEmpty()) {
                            previewMetaData.favicon = resolveURL(url, src)
                        }
                    }
                }
                for (element in elements) {
                    if (element.hasAttr("property")) {
                        val strProperty = element.attr("property").trim { it <= ' ' }
                        if (strProperty == "og:url") {
                            previewMetaData.url = element.attr("content")
                        }
                        if (strProperty == "og:site_name") {
                            previewMetaData.siteName = element.attr("content")
                        }
                    }
                }
                if (previewMetaData.url.isNullOrBlank()) {
                    var uri: URI? = null
                    try {
                        uri = URI(url)
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                    }
                    if (uri != null) {
                        previewMetaData.url = uri.host
                    }
                }
                withContext(Dispatchers.Main) {
                    successListener?.success(previewMetaData)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorListener?.error("No Html Received from " + url + " Check your Internet " + e.localizedMessage)
                }
            }
        }

    }

    /** Be careful, after closing scope, you can't launch any other coroutines.*/
    fun close() {
        scope.cancel()
    }

    private fun resolveURL(url: String, part: String): String {
        return if (URLUtil.isValidUrl(part)) {
            part
        } else {
            var baseUri: URI? = null
            try {
                baseUri = URI(url)
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            baseUri = baseUri?.resolve(part)
            baseUri.toString()
        }
    }

    data class PreviewMetaData(
            val messageId: Long,
            var url: String? = null,
            var imageUrl: String? = null,
            var title: String? = null,
            var description: String? = null,
            var siteName: String? = null,
            var mediaType: String? = null,
            var favicon: String? = null
    )

    sealed interface PreviewCallback {
        fun interface Success : PreviewCallback {
            fun success(previewMetaData: PreviewMetaData)
        }

        fun interface Error : PreviewCallback {
            fun error(message: String?)
        }
    }
}