package com.sceyt.chatuikit.presentation.components.poll_results

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.ActivityPollResultsBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.styles.StyleRegistry

open class PollResultsActivity : AppCompatActivity() {
    protected open lateinit var binding: ActivityPollResultsBinding
    protected open lateinit var style: PollResultsStyle
    private lateinit var message: SceytMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = PollResultsStyle.Builder(this, null).build()
        StyleRegistry.register(style)

        enableEdgeToEdge()
        binding = ActivityPollResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsAndWindowColor(binding.root)

        getDataFromIntent()
        loadPollResultsFragment()
        applyStyle()
    }

    protected open fun getDataFromIntent() {
        message = requireNotNull(intent?.parcelable(MESSAGE))
    }

    protected open fun loadPollResultsFragment() {
        val fragment = PollResultsFragment.newInstance(
            message = message,
            styleId = style.styleId
        )
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
    }

    override fun finish() {
        super.finish()
        overrideTransitions(
            R.anim.sceyt_anim_slide_hold,
            R.anim.sceyt_anim_slide_out_right,
            false)
    }

    override fun onDestroy() {
        super.onDestroy()
        StyleRegistry.unregister(style.styleId)
    }

    companion object {
        private const val MESSAGE = "POLL_MESSAGE"

        fun launch(
                context: Context,
                message: SceytMessage
        ) {
            context.launchActivity<PollResultsActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold,
            ) {
                putExtra(MESSAGE, message)
            }
        }
    }
}