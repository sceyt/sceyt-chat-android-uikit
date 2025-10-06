package com.sceyt.chatuikit.presentation.components.invite_link.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytBottomSheetShareInviteQrBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getApplicationIcon
import com.sceyt.chatuikit.presentation.helpers.InviteLinkQRGenerator

class BottomSheetShareInviteQr : BottomSheetDialogFragment() {
    private lateinit var binding: SceytBottomSheetShareInviteQrBinding

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

        initViews()
        initQrCode()
    }

    private fun initViews() {
        binding.btnShareQr.setOnClickListener { dismiss() }
    }

    private fun initQrCode() {
        val logo = requireContext().getApplicationIcon()
        val qr = InviteLinkQRGenerator.generateQrWithRoundedLogo(
            content = "https://link.sceyt.com/abcdefg1234567",
            size = 800,
            logoDrawable = logo,
            logoCornerRadius = 12.dpToPx().toFloat()
        )
        binding.icQr.setImageBitmap(qr)
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
        const val TAG = "BottomSheetShareInviteQr"

        fun show(fragmentManager: FragmentManager) {
            val bottomSheet = BottomSheetShareInviteQr()
            bottomSheet.show(fragmentManager, TAG)
        }
    }
}

