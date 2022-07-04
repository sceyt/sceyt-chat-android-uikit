package com.sceyt.chat.ui.presentation.uicomponents.creategroup

import android.animation.LayoutTransition
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ActivityCreateGroupBinding
import com.sceyt.chat.ui.extensions.isNightTheme
import com.sceyt.chat.ui.extensions.statusBarIconsColorWithBackground

class CreateGroupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupBinding
    private var abool: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        statusBarIconsColorWithBackground(isNightTheme())
        super.onCreate(savedInstanceState)

        setContentView(ActivityCreateGroupBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)
        binding.layoutDetails.layoutTransition = LayoutTransition().apply { enableTransitionType(LayoutTransition.CHANGING) }

        binding.switchChannelMode.setOnClickListener {
            abool = abool.not()
            binding.switchChannelMode.isChecked = abool

            binding.groupURI.isVisible = abool
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }
}