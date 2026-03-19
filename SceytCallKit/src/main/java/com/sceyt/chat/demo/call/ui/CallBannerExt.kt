package com.sceyt.chat.demo.call.ui

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.ui.components.ActiveCallBanner

/**
 * Adds the [ActiveCallBanner] as a child of [root] (a ConstraintLayout) anchored to the top,
 * and re-constrains [pushDownViewId] so its top sits below the banner.
 * Content shifts down when the banner appears — no overlap.
 */
fun ComponentActivity.attachActiveCallBanner(
    callManager: CallManager,
    root: ConstraintLayout,
    pushDownViewId: Int,
) {
    val bannerView = ComposeView(this).apply {
        id = View.generateViewId()
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
        setContent {
            ActiveCallBanner(
                callManager = callManager,
                onClick = { startActivity(CallActivity.createOngoingIntent(this@attachActiveCallBanner)) },
            )
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
