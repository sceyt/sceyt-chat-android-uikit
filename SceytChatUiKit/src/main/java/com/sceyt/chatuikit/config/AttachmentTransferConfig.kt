package com.sceyt.chatuikit.config

import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.shared.media_encoder.VideoQuality

/**
 * Configures attachment download locations and video preparation for uploads.
 *
 * @property imageDownloadDirectoryName directory under the app's files directory for downloaded images.
 * @property videoDownloadDirectoryName directory under the app's files directory for downloaded videos.
 * @property fileDownloadDirectoryName directory under the app's files directory for other downloaded files.
 * @property videoTranscodeQuality quality used when transcoding videos before upload.
 */
data class AttachmentTransferConfig(
    val imageDownloadDirectoryName: String = SceytConstants.ImageFilesDirName,
    val videoDownloadDirectoryName: String = SceytConstants.VideoFilesDirName,
    val fileDownloadDirectoryName: String = SceytConstants.FileFilesDirName,
    val videoTranscodeQuality: VideoQuality = VideoQuality.MEDIUM,
)
