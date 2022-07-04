package com.sceyt.chat.ui.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extensions.glideRequestListener
import com.sceyt.chat.ui.sceytconfigs.SearchInputViewStyle.backgroundColor


class SceytAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private var isGroup = false
    private var fullName: String? = null
    private var imageUrl: String? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textSize = 50
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null
    private var avatarBackgroundColor: Int = 0

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytAvatarView)
            isGroup = a.getBoolean(R.styleable.SceytAvatarView_sceytAvatarViewIsGroup, false)
            fullName = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewFullName)
            imageUrl = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewImageUrl)
            textSize = a.getDimensionPixelSize(R.styleable.SceytAvatarView_sceytAvatarViewTextSize, 50)
            avatarBackgroundColor = a.getColor(R.styleable.SceytAvatarView_sceytAvatarBackgroundColor, getAvatarRandomColor())
            a.recycle()
        }
        scaleType = ScaleType.CENTER_CROP
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (visibility != VISIBLE) return
        if (imageUrl.isNullOrBlank()) {
            setImageResource(0)
            drawBackgroundColor(canvas)
            drawName(canvas)
        }
    }

    private fun drawName(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = textSize.toFloat()
        textPaint.color = Color.WHITE

        val xPos = (width / 2).toFloat()
        val yPos = (height / 2 - (textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(getAvatarText(fullName ?: ""), xPos, yPos, textPaint)
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = avatarBackgroundColor
        })
    }

    private fun getAvatarText(title: String): String {
        if (title.trim().isBlank()) return ""
        val strings = title.trim().split(" ")
        return if (strings.size > 1) {
            return ("${strings[0].first()}${strings[1].first()}").uppercase()
        } else strings[0].first().uppercase()
    }

    private fun getAvatarRandomColor(): Int {
        val colors = arrayOf("#FF3E74", "#4F6AFF", "#FBB019", "#00CC99", "#9F35E7", "#63AFFF")
        var colorIndex: Int = (0..6).random()

        if (colorIndex >= colors.size)
            colorIndex -= colors.size

        return colors[colorIndex].toColorInt()
    }

    private fun loadAvatarImage() {
        if (!imageUrl.isNullOrBlank()) {
            avatarLoadCb?.invoke(true)

            Glide.with(context.applicationContext)
                .load(imageUrl)
                .override(width)
                .transition(DrawableTransitionOptions.withCrossFade(100))
                .error(R.drawable.sceyt_bg_circle_gray)
                .placeholder(R.drawable.sceyt_bg_circle_gray)
                .listener(glideRequestListener {
                    avatarLoadCb?.invoke(false)
                })
                .circleCrop()
                .into(this)
        }
    }

    fun setNameAndImageUrl(name: String?, url: String?) {
        fullName = name
        imageUrl = url
        invalidate()
        loadAvatarImage()
    }

    fun setImageUrl(url: String?) {
        imageUrl = url
        invalidate()
        loadAvatarImage()
    }

    fun setAvatarImageLoadListener(cb: (Boolean) -> Unit) {
        avatarLoadCb = cb
    }

    fun setAvatarBackgroundColor(color: Int) {
        avatarBackgroundColor = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, measuredWidth)
    }
}