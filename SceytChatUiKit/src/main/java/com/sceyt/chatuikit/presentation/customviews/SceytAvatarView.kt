package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.extensions.roundUp
import kotlin.math.abs


class SceytAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private var isGroup = false
    private var fullName: String? = null
    private var imageUrl: String? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var imagePaint: Paint
    private lateinit var backgroundPaint: Paint
    private var textSize = 0
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null
    private var avatarBackgroundColor: Int = 0
    private var defaultAvatarResId: Int = 0

    init {
        var enableRipple = true
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SceytAvatarView)
            isGroup = a.getBoolean(R.styleable.SceytAvatarView_sceytAvatarViewIsGroup, false)
            fullName = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewFullName)
            imageUrl = a.getString(R.styleable.SceytAvatarView_sceytAvatarViewImageUrl)
            textSize = a.getDimensionPixelSize(R.styleable.SceytAvatarView_sceytAvatarViewTextSize, textSize)
            avatarBackgroundColor = a.getColor(R.styleable.SceytAvatarView_sceytAvatarColor, 0)
            defaultAvatarResId = a.getResourceId(R.styleable.SceytAvatarView_sceytAvatarDefaultIcon, defaultAvatarResId)
            enableRipple = a.getBoolean(R.styleable.SceytAvatarView_sceytAvatarEnableRipple, true)
            a.recycle()
        }
        scaleType = ScaleType.CENTER_CROP
        if (enableRipple) {
            val ripple = context.getCompatDrawable(R.drawable.sceyt_bg_ripple_circle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = ripple
            } else background = ripple
        }
        initPaints()
    }

    private fun initPaints() {
        imagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    override fun draw(canvas: Canvas) {
        if (visibility != VISIBLE) return
        if (imageUrl.isNullOrBlank()) {
            setImageResource(0)
            if (defaultAvatarResId == 0) {
                drawBackgroundColor(canvas)
                drawName(canvas)
            } else {
                if (avatarBackgroundColor != 0)
                    drawBackgroundColor(canvas)

                drawDefaultImage(canvas)
            }
        }
        super.draw(canvas)
    }

    private fun drawName(canvas: Canvas) {
        textPaint.textSize = if (textSize > 0) textSize.toFloat() else width * 0.38f
        textPaint.color = Color.WHITE

        val staticLayout = getStaticLayout(getAvatarText(fullName ?: ""))

        val xPos = (width - staticLayout.width) / 2f
        canvas.save()
        canvas.translate(xPos, (height - staticLayout.height) / 2f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawBackgroundColor(canvas: Canvas) {
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), backgroundPaint.apply {
            color = if (avatarBackgroundColor == 0) getAvatarRandomColor() else avatarBackgroundColor
        })
    }

    private fun getAvatarText(title: String): CharSequence {
        if (title.isBlank()) return ""
        val strings = title.trim().split(" ").filter { it.isNotBlank() }
        if (strings.isEmpty()) return ""
        val data = if (isInEditMode)
            Pair(strings[0].take(1), true) else strings[0].getFirstCharIsEmoji()
        val firstChar = data.first
        val isEmoji = data.second
        if (isEmoji)
            return if (isInEditMode)
                firstChar else firstChar.processEmojiCompat() ?: title.take(1)

        val text = if (strings.size > 1) {
            val secondChar = strings[1].getFirstCharIsEmoji().first
            "${firstChar}${secondChar}".uppercase()
        } else firstChar.toString().uppercase()

        return if (isInEditMode) text else text.processEmojiCompat() ?: title.take(1)
    }

    private fun getAvatarRandomColor(): Int {
        val colors = SceytChatUIKit.theme.avatarColors
        return colors[abs((fullName ?: "").hashCode()) % colors.size].toColorInt()
    }

    @Suppress("DEPRECATION")
    private fun getStaticLayout(title: CharSequence): StaticLayout {
        val width = Layout.getDesiredWidth(title, textPaint).roundUp()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(title, 0, title.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false).build()
        } else StaticLayout(title, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)
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
                .listener(com.sceyt.chatuikit.extensions.glideRequestListener {
                    avatarLoadCb?.invoke(false)
                })
                .circleCrop()
                .into(this)
        }
    }

    private fun drawDefaultImage(canvas: Canvas) {
        val defaultImage = context.getCompatDrawable(defaultAvatarResId) ?: return
        val bitmap = defaultImage.toBitmap()
        updateShader(bitmap)
        val circleCenter = (width) / 2f
        canvas.drawCircle(circleCenter, circleCenter, circleCenter, imagePaint)
    }

    private fun updateShader(bitmap: Bitmap) {
        // Create Shader
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        // Center Image in Shader
        val matrix = Matrix()
        matrix.setScale(width / bitmap.width.toFloat(), width / bitmap.height.toFloat())
        shader.setLocalMatrix(matrix)
        // Set Shader in Paint
        imagePaint.shader = shader
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

    @Suppress("UNUSED")
    fun setAvatarColor(color: Int) {
        avatarBackgroundColor = color
        invalidate()
    }

    @Suppress("UNUSED")
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