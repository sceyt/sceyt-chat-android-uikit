package com.sceyt.chat.demo.call

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sceyt.calluikit.model.CallPhase
import com.sceyt.calluikit.ui.SceytCallUiKit
import com.sceyt.calluikit.ui.attributes.CallActiveCallBannerAttributes
import com.sceyt.calluikit.ui.navigation.CallDestination
import com.sceyt.calluikit.ui.navigation.navigate
import com.sceyt.calluikit.ui.theme.SceytCallThemeProvider

/** Embeds the UI Kit banner above [pushDownViewId], moving content with its animated height. */
fun ComponentActivity.attachActiveCallBanner(
    root: ConstraintLayout,
    pushDownViewId: Int,
) {
    val controller = SceytCallUiKit.controller
    val bannerView = ComposeView(this).apply {
        id = View.generateViewId()
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val callState by controller.callState.collectAsStateWithLifecycle()
            val duration by controller.callDuration.collectAsStateWithLifecycle()
            SceytCallThemeProvider(theme = SceytCallUiKit.theme) {
                SceytCallUiKit.renderers.activeCallBanner.Render(
                    attributes = CallActiveCallBannerAttributes(callState, duration),
                    onToggleMute = { controller.media.toggleMute() },
                    onEndCall = {
                        if (callState.phase == CallPhase.Incoming) {
                            controller.rejectCall()
                        } else {
                            controller.leaveCall()
                        }
                    },
                    onClick = {
                        SceytCallUiKit.navigator.navigate(
                            this@attachActiveCallBanner,
                            CallDestination.Ongoing(),
                        )
                    },
                    modifier = Modifier,
                )
            }
        }
    }

    root.addView(
        bannerView,
        ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        ),
    )

    ConstraintSet().apply {
        clone(root)
        constrainWidth(bannerView.id, ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(bannerView.id, ConstraintSet.WRAP_CONTENT)
        connect(bannerView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(bannerView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(bannerView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(pushDownViewId, ConstraintSet.TOP, bannerView.id, ConstraintSet.BOTTOM)
        applyTo(root)
    }
}
