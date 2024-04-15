package com.sceyt.chatuikit.presentation.common

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import com.sceyt.chatuikit.databinding.SceytProgressDialogBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig

class SceytProgressDialogLoading(context: Context) : Dialog(context) {
    private lateinit var binding: SceytProgressDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytProgressDialogBinding.inflate(LayoutInflater.from(context))
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(binding.root)
        setCancelable(false)

        binding.progressBar.indeterminateDrawable.setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}