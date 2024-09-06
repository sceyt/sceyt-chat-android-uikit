package com.sceyt.chatuikit.presentation.uicomponents.locationpreview

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytActivityLocationPreviewBinding
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground

open class LocationPreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytActivityLocationPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            SceytActivityLocationPreviewBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(
            statusBarColor = SceytChatUIKit.theme.statusBarColor,
            navigationBarColor = SceytChatUIKit.theme.primaryColor)

    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    companion object {
        fun launchActivity(context: Context) {
            context.launchActivity<LocationPreviewActivity>(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold)
        }
    }

}