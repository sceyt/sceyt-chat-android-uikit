package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.text.toSpannable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.applyTo
import kotlin.math.abs

class AvatarView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ShapeableImageView(context, attrs, defStyleAttr) {
    private var name: CharSequence? = null
    private var imageUrl: String? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var imagePaint: Paint
    private lateinit var backgroundPaint: Paint
    private var textStyle = TextStyle()
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null
    private var shape: Shape = Shape.Circle

    @ColorInt
    private var avatarBackgroundColor: Int = 0
    private var defaultAvatar: DefaultAvatar? = null

    init {
        var enableRipple = true
        context.obtainStyledAttributes(attrs, R.styleable.AvatarView).use { array ->
            name = array.getString(R.styleable.AvatarView_sceytUiAvatarFullName) ?: name
            imageUrl = array.getString(R.styleable.AvatarView_sceytUiAvatarImageUrl)
            textStyle = TextStyle.Builder(array)
                .setSize(R.styleable.AvatarView_sceytUiAvatarTextSize, UNSET_SIZE)
                .setColor(R.styleable.AvatarView_sceytUiAvatarTextColor, Color.WHITE)
                .setFont(R.styleable.AvatarView_sceytUiAvatarTextFont)
                .setStyle(R.styleable.AvatarView_sceytUiAvatarTextStyle)
                .build()
            avatarBackgroundColor = array.getColor(R.styleable.AvatarView_sceytUiAvatarColor, avatarBackgroundColor)
            val defaultAvatarResId = array.getResourceId(R.styleable.AvatarView_sceytUiAvatarDefaultIcon, 0)
            if (defaultAvatarResId != 0) {
                defaultAvatar = defaultAvatarResId.toDefaultAvatar()
            }
            enableRipple = array.getBoolean(R.styleable.AvatarView_sceytUiAvatarEnableRipple, true)
        }

        scaleType = ScaleType.CENTER_CROP

        if (enableRipple) {
            val ripple = context.getCompatDrawable(R.drawable.sceyt_bg_ripple_circle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                foreground = ripple
            } else background = ripple
        }

        initPaints()
        initShape(shape)
    }

    private fun initPaints() {
        imagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    private fun initShape(shape: Shape) {
        shape.applyTo(this)
    }

    override fun draw(canvas: Canvas) {
        if (visibility != VISIBLE) return
        if (imageUrl.isNullOrBlank()) {
            setImageResource(0)
            val default = defaultAvatar
            if (default == null) {
                val initials = getInitials(name ?: "")
                drawBackgroundColor(canvas, avatarBackgroundColor)
                drawInitials(canvas, initials)
            } else {
                // If default avatar is DefaultAvatar.Initial then drawDefaultImage
                // will draw the initials and background,
                // otherwise we should tyr to draw the avatarBackgroundColor
                if (default !is DefaultAvatar.Initial)
                    drawBackgroundColor(canvas, avatarBackgroundColor)

                drawDefaultImage(canvas, default)
            }
        }
        super.draw(canvas)
    }

    private fun drawInitials(canvas: Canvas, name: CharSequence) {
        textPaint.textSize = if (textStyle.size > 0) textStyle.size.toFloat() else width * 0.38f
        textPaint.color = Color.WHITE

        val spannable = name.toSpannable()
        textStyle.apply(context, spannable)
        val staticLayout = getStaticLayout(spannable)

        val xPos = (width - staticLayout.width) / 2f
        canvas.save()
        canvas.translate(xPos, (height - staticLayout.height) / 2f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawBackgroundColor(canvas: Canvas, @ColorInt color: Int) {
        if (color == 0) return
        val paint = backgroundPaint.apply { this.color = color }
        when (val avatarShape = shape) {
            Shape.Circle -> {
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), paint)
            }

            is Shape.RoundedCornerShape -> {
                val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                val radii = floatArrayOf(
                    avatarShape.topLeft, avatarShape.topLeft,
                    avatarShape.topRight, avatarShape.topRight,
                    avatarShape.bottomRight, avatarShape.bottomRight,
                    avatarShape.bottomLeft, avatarShape.bottomLeft
                )
                val path = Path().apply {
                    addRoundRect(rect, radii, Path.Direction.CW)
                }
                canvas.drawPath(path, paint)
            }

            Shape.UnsetShape -> {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    private fun getInitials(title: CharSequence): CharSequence {
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

    private fun getAvatarRandomColor(initials: CharSequence): Int {
        val colors = if (isInEditMode)
            listOf(1) else SceytChatUIKit.config.defaultAvatarBackgroundColors.getColors(context)
        return colors[abs(initials.hashCode()) % colors.size]
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
                .error(SceytChatUIKit.theme.colors.backgroundColorSecondary)
                .placeholder(SceytChatUIKit.theme.colors.backgroundColorSecondary)
                .listener(com.sceyt.chatuikit.extensions.glideRequestListener {
                    avatarLoadCb?.invoke(false)
                })
                .into(this)
        } else {
            avatarLoadCb?.invoke(false)
        }
    }

    private fun drawDefaultImage(canvas: Canvas, avatar: DefaultAvatar) {
        when (avatar) {
            is DefaultAvatar.FromBitmap -> {
                setImageBitmap(avatar.bitmap)
            }

            is DefaultAvatar.FromDrawable -> {
                setImageDrawable(avatar.drawable)
            }

            is DefaultAvatar.FromDrawableRes -> {
                setImageResource(avatar.id)
            }

            is DefaultAvatar.Initial -> {
                val color = if (avatarBackgroundColor == 0)
                    getAvatarRandomColor(avatar.initial) else avatarBackgroundColor

                drawBackgroundColor(canvas, color)
                drawInitials(canvas, avatar.initial)
            }
        }
    }

    fun setImageUrl(url: String?) {
        val oldImageUrl = imageUrl
        imageUrl = url
        invalidate()
        loadAvatarImage(oldImageUrl)
    }

    fun setDefaultAvatar(avatar: DefaultAvatar) {
        defaultAvatar = avatar
        invalidate()
    }

    fun setAvatarImageLoadListener(cb: (Boolean) -> Unit) {
        avatarLoadCb = cb
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, measuredWidth)
    }

    fun applyStyle(style: AvatarStyle) {
        textStyle = style.textStyle

        if (style.avatarBackgroundColor != UNSET_COLOR) {
            avatarBackgroundColor = style.avatarBackgroundColor
        }
        shape = style.shape
        initShape(shape)
    }

    sealed class DefaultAvatar {
        data class FromBitmap(val bitmap: Bitmap) : DefaultAvatar()
        data class FromDrawable(val drawable: Drawable?) : DefaultAvatar()
        data class FromDrawableRes(@DrawableRes val id: Int) : DefaultAvatar()
        data class Initial(
                val initial: CharSequence
        ) : DefaultAvatar()
    }

    fun appearanceBuilder() = AppearanceBuilder()

    inner class AvatarAppearance(
            val style: AvatarStyle,
            val name: CharSequence?,
            val imageUrl: String?,
            val defaultAvatar: DefaultAvatar?,
    ) {

        fun applyToAvatar() {
            val oldImageUrl = this@AvatarView.imageUrl
            this@AvatarView.name = name
            this@AvatarView.imageUrl = imageUrl
            this@AvatarView.defaultAvatar = defaultAvatar

            style.apply(this@AvatarView)
            loadAvatarImage(oldImageUrl)
        }
    }

    inner class AppearanceBuilder {
        private var name = this@AvatarView.name
        private var imageUrl = this@AvatarView.imageUrl
        private var defaultAvatar = this@AvatarView.defaultAvatar
        private var avatarStyle = AvatarStyle(
            textStyle = this@AvatarView.textStyle,
            shape = this@AvatarView.shape,
            avatarBackgroundColor = this@AvatarView.avatarBackgroundColor
        )

        fun setStyle(style: AvatarStyle) = apply {
            avatarStyle = style
        }

        fun setName(name: CharSequence) = apply {
            this.name = name
        }

        fun setImageUrl(url: String?) = apply {
            imageUrl = url
        }

        fun setDefaultAvatar(avatar: DefaultAvatar) = apply {
            defaultAvatar = avatar
        }

        fun setDefaultAvatar(@DrawableRes id: Int) = apply {
            defaultAvatar = id.toDefaultAvatar()
        }

        fun setDefaultAvatar(drawable: Drawable?) = apply {
            if (drawable == null) return@apply
            defaultAvatar = DefaultAvatar.FromDrawable(drawable)
        }

        fun setDefaultAvatar(bitmap: Bitmap) = apply {
            defaultAvatar = DefaultAvatar.FromBitmap(bitmap)
        }

        fun build() = AvatarAppearance(
            style = avatarStyle,
            name = name,
            imageUrl = imageUrl,
            defaultAvatar = defaultAvatar
        )
    }
}


fun @receiver:DrawableRes Int.toDefaultAvatar(): DefaultAvatar {
    return DefaultAvatar.FromDrawableRes(this)
}


