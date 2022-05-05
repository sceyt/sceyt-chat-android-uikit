package com.sceyt.chat.ui.presentation.uicomponents.conversatin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.SceytUiItemIncTextMessageBinding

class MessagesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return object : RecyclerView.ViewHolder(SceytUiItemIncTextMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        setReplayCountLineMargins(holder.itemView)
    }

    private fun setReplayCountLineMargins(itemView: View) {
        with(itemView) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val messageInfo = itemView.findViewById<View>(R.id.messageBody)
            messageInfo.measure(widthSpec, heightSpec)
            val messageInfoHeight: Int = messageInfo.measuredHeight
            val tvReplayCount = itemView.findViewById<TextView>(R.id.tvReplayCount)
            tvReplayCount.measure(widthSpec, heightSpec)
            val tvReplayCountHeight: Int = tvReplayCount.measuredHeight

            val toReplayLine = findViewById<View>(R.id.toReplayLine)
            (toReplayLine.layoutParams as ConstraintLayout.LayoutParams).setMargins(0,
                messageInfoHeight , 0, tvReplayCountHeight / 2 - tvReplayCount.paddingTop)
            toReplayLine.requestLayout()
        }
    }

    override fun getItemCount(): Int {
        return 120
    }
}