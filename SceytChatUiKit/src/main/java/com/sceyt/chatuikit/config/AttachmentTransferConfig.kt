package com.sceyt.chatuikit.config

import com.sceyt.chatuikit.data.constants.SceytConstants

/**
 * Configures attachment download locations.
 *
 * @property imageDownloadDirectoryName directory under the app's files directory for downloaded images.
 * @property videoDownloadDirectoryName directory under the app's files directory for downloaded videos.
 * @property fileDownloadDirectoryName directory under the app's files directory for other downloaded files.
 */
data class AttachmentTransferConfig(
    val imageDownloadDirectoryName: String = SceytConstants.ImageFilesDirName,
    val videoDownloadDirectoryName: String = SceytConstants.VideoFilesDirName,
    val fileDownloadDirectoryName: String = SceytConstants.FileFilesDirName,
)