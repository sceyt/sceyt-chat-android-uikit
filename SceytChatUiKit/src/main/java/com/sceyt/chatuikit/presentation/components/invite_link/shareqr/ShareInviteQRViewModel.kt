package com.sceyt.chatuikit.presentation.components.invite_link.shareqr

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getApplicationIcon
import com.sceyt.chatuikit.extensions.getFileUriWithProvider
import com.sceyt.chatuikit.extensions.toFile
import com.sceyt.chatuikit.presentation.helpers.InviteLinkQRGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class LinkQrData(
        val link: String,
        val size: Int = 800,
        val logoCornerRadius: Float = 12.dpToPx().toFloat(),
) : Parcelable

enum class ErrorType {
    GenerateQr,
    SaveQr
}

data class UIState(
        val qrBitmap: Bitmap? = null,
        val qrFile: File? = null, // If file already generated, no need to generate again
        val isShareEnabled: Boolean = false,
        val dismissDialog: Boolean = false,
        val error: ErrorType? = null,
)

class ShareInviteQRViewModel(
        private val application: Application,
        private val linkQrData: LinkQrData,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    init {
        generateQrCode()
    }

    private fun generateQrCode() {
        viewModelScope.launch {
            val logo = application.getApplicationIcon()
            val bitmap = InviteLinkQRGenerator.generateQrWithRoundedLogo(
                content = linkQrData.link,
                size = linkQrData.size,
                logoDrawable = logo,
                logoCornerRadius = linkQrData.logoCornerRadius
            )
            _uiState.update {
                it.copy(
                    qrBitmap = bitmap,
                    isShareEnabled = true,
                    error = if (bitmap == null) ErrorType.GenerateQr else null
                )
            }
        }

    }

    fun onShareQrClick(launchingContext: Context) {
        val state = _uiState.value
        if (!state.isShareEnabled) return

        _uiState.update {
            it.copy(isShareEnabled = false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = state.qrBitmap
            if (bitmap == null) {
                _uiState.update {
                    it.copy(
                        isShareEnabled = true,
                        error = ErrorType.GenerateQr
                    )
                }
                return@launch
            }

            val file = state.qrFile?.let {
                if (it.exists())
                    it else bitmap.toFile(application)
            } ?: bitmap.toFile(application)

            ShareCompat.IntentBuilder(launchingContext)
                .setStream(application.getFileUriWithProvider(file))
                .setType("image/jpeg")
                .startChooser()
        }
    }
}