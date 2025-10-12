package com.sceyt.chatuikit.presentation.components.invite_link.shareqr

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytBottomSheetShareInviteQrBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.dismissSafety
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.invite_link.BottomSheetShareInviteQRStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class BottomSheetShareInviteQR : BottomSheetDialogFragment(), SceytKoinComponent {
    protected lateinit var binding: SceytBottomSheetShareInviteQrBinding
    protected lateinit var style: BottomSheetShareInviteQRStyle
    protected val viewModel by viewModel<ShareInviteQRViewModel>(
        parameters = {
            parametersOf(requireArguments().parcelable<LinkQrData>(LINK_DATA_KEY))
        }
    )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            BottomSheetShareInviteQRStyle.Builder(context, null).build()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytBottomSheetShareInviteQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()
        initViews()
        initViewModel()
    }

    protected open fun initViews() {
        binding.btnShareQr.setOnClickListener {
            onShareQrClick()
        }
    }

    protected open fun initViewModel() {
        viewModel.uiState.onEach { state ->
            state.qrBitmap?.let { bitmap ->
                binding.icQr.setImageBitmap(bitmap)
            }
            state.error?.let { errorType ->
                when (errorType) {
                    ErrorType.GenerateQr -> {
                        customToastSnackBar("Failed to generate QR code")
                    }

                    ErrorType.SaveQr -> {
                        customToastSnackBar("Failed to save QR code")
                    }
                }
            }
        }.launchIn(lifecycleScope)
    }

    protected open fun onShareQrClick() {
        viewModel.onShareQrClick(requireActivity())
        dismissSafety()
    }

    protected open fun applyStyle() = with(binding) {
        style.backgroundStyle.apply(root)

        // Apply text styles
        style.titleTextStyle.apply(tvTitle)
        style.descriptionTextStyle.apply(tvDescription)

        // Apply button style
        style.shareButtonStyle.apply(btnShareQr)

        // Set texts
        tvTitle.text = style.titleText
        tvDescription.text = style.descriptionText
        btnShareQr.text = style.shareButtonText
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).isDraggable = false
            }
        }
    }

    companion object {
        private val TAG = BottomSheetShareInviteQR::class.simpleName
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"
        private const val LINK_DATA_KEY = "linkDataKey"

        fun show(
                fragmentManager: FragmentManager,
                linkQrData: LinkQrData,
                styleId: String? = null,
        ) {
            val existingSheet = fragmentManager.findFragmentByTag(TAG) as? BottomSheetShareInviteQR
            if (existingSheet != null && existingSheet.isAdded) {
                return
            }

            val bottomSheet = BottomSheetShareInviteQR().setBundleArguments {
                putString(STYLE_ID_KEY, styleId)
                putParcelable(LINK_DATA_KEY, linkQrData)
            }
            bottomSheet.show(fragmentManager, TAG)
        }
    }
}

