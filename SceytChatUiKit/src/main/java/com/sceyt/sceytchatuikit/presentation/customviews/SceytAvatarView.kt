package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import kotlin.math.abs


class SceytAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private var isGroup = false
    private var fullName: String? = null
    private var imageUrl: String? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textSize = 0
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null
    private var avatarBackgroundColor: Int = 0
    private var defaultAvatarResId: Int = 0

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytAvatarView)
            isGroup = a.getBoolean(R.styleable.SceytAvatarView_sceytAvatarViewIsGroup, false)
            fullName = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewFullName)
            imageUrl = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewImageUrl)
            textSize = a.getDimensionPixelSize(R.styleable.SceytAvatarView_sceytAvatarViewTextSize, textSize)
            avatarBackgroundColor = a.getColor(R.styleable.SceytAvatarView_sceytAvatarColor, 0)
            defaultAvatarResId = a.getResourceId(R.styleable.SceytAvatarView_sceytAvatarDefaultIcon, defaultAvatarResId)
            a.recycle()
        }
        scaleType = ScaleType.CENTER_CROP
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (visibility != VISIBLE) return
        if (imageUrl.isNullOrBlank()) {
            if (defaultAvatarResId == 0) {
                setImageResource(0)
                drawBackgroundColor(canvas)
                drawName(canvas)
            } else
                loadDefaultImage()
        }
    }

    private fun drawName(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = if (textSize > 0) textSize.toFloat() else width * 0.38f
        textPaint.color = Color.WHITE

        val xPos = (width / 2).toFloat()
        val yPos = (height / 2 - (textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(getAvatarText(fullName ?: ""), xPos, yPos, textPaint)
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (avatarBackgroundColor == 0) getAvatarRandomColor() else avatarBackgroundColor
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
        val colors = UserStyle.avatarColors
        return colors[abs((fullName ?: "").hashCode()) % colors.size].toColorInt()
    }

    private fun loadAvatarImage(oldImageUrl: String?) {
        if (!imageUrl.isNullOrBlank() && imageUrl != oldImageUrl) {
            avatarLoadCb?.invoke(true)

            Glide.with(context.applicationContext)
                .load(imageUrl)
                .override(width)
                .transition(DrawableTransitionOptions.withCrossFade(100))
                .error(R.drawable.sceyt_bg_circle_gray)
                .placeholder(context.getCompatDrawable(R.drawable.sceyt_bg_circle_gray))
                .listener(com.sceyt.sceytchatuikit.extensions.glideRequestListener {
                    avatarLoadCb?.invoke(false)
                })
                .circleCrop()
                .into(this)
        }
    }

    private fun loadDefaultImage() {
        if (isInEditMode) {
            if (defaultAvatarResId != 0)
                setImageResource(defaultAvatarResId)
        } else
            Glide.with(context.applicationContext)
                .load(defaultAvatarResId)
                .override(width)
                .circleCrop()
                .into(this)
    }

    fun setNameAndImageUrl(name: String?, url: String?, @DrawableRes defaultIcon: Int = defaultAvatarResId) {
        val oldImageUrl = imageUrl
        fullName = name
        imageUrl = url
        defaultAvatarResId = defaultIcon
        invalidate()
        loadAvatarImage(oldImageUrl)
    }

    fun setImageUrl(url: String?, @DrawableRes defaultIcon: Int = defaultAvatarResId) {
        val oldImageUrl = imageUrl
        imageUrl = url
        defaultAvatarResId = defaultIcon
        invalidate()
        loadAvatarImage(oldImageUrl)
    }

    fun setAvatarImageLoadListener(cb: (Boolean) -> Unit) {
        avatarLoadCb = cb
    }

    fun setAvatarColor(color: Int) {
        avatarBackgroundColor = color
        invalidate()
    }

    fun setDefaultIcon(@DrawableRes id: Int) {
        defaultAvatarResId = id
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, measuredWidth)
    }
}