package com.sceyt.chatuikit.presentation.uicomponents.locationpreview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytActivityLocationPreviewBinding
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.uicomponents.locationpreview.fragments.LocationPreviewFragment

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

        initFragment()
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    private fun initFragment() {
        val locationPreviewFragment = getLocationPreviewFragment()
        supportFragmentManager.commit {
            replace(R.id.fragment_container_location, locationPreviewFragment.also {
                it.setupLocationListener { result ->
                    setLocationResult(result)
                }
            })
        }
    }

    private fun setLocationResult(result: LocationResult) {
        val intent = Intent().apply {
            putExtra(EXTRA_LOCATION_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    open fun getLocationPreviewFragment() = LocationPreviewFragment()

    companion object {
        const val EXTRA_LOCATION_RESULT = "location_result"

        fun getIntent(context: Context): Intent {
            return context.createIntent<LocationPreviewActivity>()
        }
    }

}