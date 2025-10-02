package com.sceyt.chatuikit.presentation.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytDialogLoadingBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import androidx.core.graphics.drawable.toDrawable

class SceytLoadingDialog(context: Context) : Dialog(context) {
    private lateinit var binding: SceytDialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytDialogLoadingBinding.inflate(LayoutInflater.from(context))
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setContentView(binding.root)
        setCancelable(false)

        binding.progressBar.indeterminateDrawable.setTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        )
    }
}