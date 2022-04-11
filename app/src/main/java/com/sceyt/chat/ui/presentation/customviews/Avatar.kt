package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.sceyt.chat.ui.R
import kotlin.math.abs


class Avatar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private var isGroup = false
    private var fullName: String? = null
    private var imageUrl: String? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.Avatar)
            isGroup = a.getBoolean(R.styleable.Avatar_isGroup, false)
            fullName = a.getString(R.styleable.Avatar_fullName)
            imageUrl = a.getString(R.styleable.Avatar_imageUrl)
            a.recycle()
        }
        scaleType = ScaleType.CENTER_CROP
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (!imageUrl.isNullOrBlank()) {
            val drawableTransitionOptions = DrawableTransitionOptions().crossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build())

            Glide.with(context)
                .load(imageUrl)
                .transition(drawableTransitionOptions)
                .error(R.color.colorAccent)
                .placeholder(R.drawable.bg_circle_gray)
                /* .listener(glideRequestListener {
                     // progress.isVisible=false
                 })*/
                .circleCrop()
                .into(this)
        } else {
            setImageResource(0)
            drawBackgroundColor(canvas)
            drawName(canvas)
        }
        /*val progress = ProgressBar(context)
        progress.setBackgroundColor(Color.YELLOW)
        progress.layoutParams = ViewGroup.LayoutParams(width / 2, width / 2)
        progress.draw(canvas)*/
    }

    private fun drawName(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 50f
        textPaint.color = Color.WHITE

        val xPos = (width / 2).toFloat()
        val yPos = (height / 2 - (textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(getAvatarText(fullName ?: ""), xPos, yPos, textPaint)
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = getAvatarColor(arrayListOf(1, 2, 4, 5, 45, 5, 6, 4, 3, 2, 4, 56).random().toLong()).toColorInt()
        })
    }

    private fun getAvatarText(title: String): String {
        if (title.trim().isBlank()) return ""
        val strings = title.trim().split(" ")
        return if (strings.size > 1) {
            return ("${strings[0].first()}${strings[1].first()}").uppercase()
        } else strings[0].first().uppercase()
    }

    private fun getAvatarColor(channelCreateTime: Long): String {

        val colors = arrayOf("#FF3E74", "#4F6AFF", "#FBB019", "#00CC99", "#9F35E7", "#63AFFF")

        var colorIndex: Int = abs(channelCreateTime % 10).toInt()

        if (colorIndex >= colors.size) {
            colorIndex -= colors.size
        }

        return colors[colorIndex]
    }

    fun setImageUrl(url: String?) {
        imageUrl = url
        invalidate()
    }

    fun setName(name: String) {
        fullName = name
        invalidate()
    }

    fun setNameAndImageUrl(name: String, url: String?) {
        fullName = name
        imageUrl = url
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, measuredWidth)
    }
}