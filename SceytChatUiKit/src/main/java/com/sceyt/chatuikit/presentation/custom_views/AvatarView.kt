package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.withTranslation
import androidx.core.text.toSpannable
import com.google.android.material.imageview.ShapeableImageView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.presentation.common.EmojiProcessor
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.presentation.helpers.AvatarImageLoader
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_BORDER_WIDTH
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
        defStyleAttr: Int = 0,
) : ShapeableImageView(context, attrs, defStyleAttr) {
    private var imageUrl: String? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var imagePaint: Paint
    private lateinit var backgroundPaint: Paint
    private lateinit var borderPaint: Paint
    private var textStyle = TextStyle()
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null
    private var shape: Shape = Shape.Circle
    private var borderWidth: Float = 0f
    private var borderColor: Int = 0

    @ColorInt
    private var avatarBackgroundColor: Int = 0
    private var defaultAvatar: DefaultAvatar? = null
    private var initials: CharSequence = ""
    private var placeholder: AvatarPlaceholder? = null
    private var errorPlaceholder: AvatarErrorPlaceHolder? = null
    private val defaultPlaceholder by lazy {
        context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSecondary).toDrawable()
    }
    private val emojiProcessor by lazy { EmojiProcessor() }

    init {
        var enableRipple = true
        context.obtainStyledAttributes(attrs, R.styleable.AvatarView).use { array ->
            val name = array.getString(R.styleable.AvatarView_sceytUiAvatarFullName)
            textStyle = TextStyle.Builder(array)
                .setSize(R.styleable.AvatarView_sceytUiAvatarTextSize, UNSET_SIZE)
                .setColor(R.styleable.AvatarView_sceytUiAvatarTextColor, Color.WHITE)
                .setFont(R.styleable.AvatarView_sceytUiAvatarTextFont)
                .setStyle(R.styleable.AvatarView_sceytUiAvatarTextStyle)
                .build()
            avatarBackgroundColor = array.getColor(R.styleable.AvatarView_sceytUiAvatarColor, avatarBackgroundColor)
            borderWidth = array.getDimension(R.styleable.AvatarView_sceytUiAvatarBorderWidth, 0f)
            borderColor = array.getColor(R.styleable.AvatarView_sceytUiAvatarBorderColor, 0)
            val defaultAvatarResId = array.getResourceId(R.styleable.AvatarView_sceytUiAvatarDefaultIcon, 0)
            enableRipple = array.getBoolean(R.styleable.AvatarView_sceytUiAvatarEnableRipple, true)

            defaultAvatar = when {
                defaultAvatarResId != 0 -> defaultAvatarResId.toDefaultAvatar()
                !name.isNullOrBlank() -> {
                    initials = getInitials(name)
                    DefaultAvatar.FromInitials(name)
                }

                else -> null
            }
            val shapeValue = array.getInt(R.styleable.AvatarView_sceytUiAvatarShape, 0)
            val cornerRadius = array.getDimension(R.styleable.AvatarView_sceytUiAvatarCornerRadius, 0f)
            shape = if (shapeValue == 1) {
                Shape.RoundedCornerShape(cornerRadius)
            } else Shape.Circle

            scaleType = array.getInt(R.styleable.AvatarView_android_scaleType, -1).takeIf {
                it != -1
            }?.let { ScaleType.entries.getOrNull(it) } ?: ScaleType.CENTER_CROP
        }

        if (enableRipple) {
            val ripple = context.getCompatDrawable(R.drawable.sceyt_bg_ripple_circle)
            foreground = ripple
        }

        initPaints()
        initShape(shape)
        setDefaultImageIfNeeded(defaultAvatar)
    }

    private fun initPaints() {
        imagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        backgroundPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = borderColor
        }
    }

    private fun initShape(shape: Shape) {
        shape.applyTo(this)
    }

    override fun draw(canvas: Canvas) {
        if (visibility != VISIBLE) return
        if (imageUrl.isNullOrBlank()) {
            val default = defaultAvatar
            if (default is DefaultAvatar.FromInitials) {
                drawInitialsAndBackground(canvas, default)
            } else {
                drawBackgroundColor(canvas, avatarBackgroundColor)
            }
        }
        super.draw(canvas)

        // Draw border on top of everything
        if (borderWidth > 0 && borderColor != 0) {
            drawBorder(canvas)
        }
    }

    private fun drawInitials(canvas: Canvas, name: CharSequence) {
        textPaint.textSize = if (textStyle.size > 0) textStyle.size.toFloat() else width * 0.38f
        textPaint.color = Color.WHITE

        val spannable = name.toSpannable()
        textStyle.apply(context, spannable)
        val staticLayout = getStaticLayout(spannable)

        val xPos = (width - staticLayout.width) / 2f
        canvas.withTranslation(xPos, (height - staticLayout.height) / 2f) {
            staticLayout.draw(this)
        }
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

    private fun drawBorder(canvas: Canvas) {
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor

        val halfBorderWidth = borderWidth / 2f

        when (val avatarShape = shape) {
            Shape.Circle -> {
                val radius = (width / 2).toFloat() - halfBorderWidth
                canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, borderPaint)
            }

            is Shape.RoundedCornerShape -> {
                val rect = RectF(
                    halfBorderWidth,
                    halfBorderWidth,
                    width.toFloat() - halfBorderWidth,
                    height.toFloat() - halfBorderWidth
                )
                val radii = floatArrayOf(
                    avatarShape.topLeft, avatarShape.topLeft,
                    avatarShape.topRight, avatarShape.topRight,
                    avatarShape.bottomRight, avatarShape.bottomRight,
                    avatarShape.bottomLeft, avatarShape.bottomLeft
                )
                val path = Path().apply {
                    addRoundRect(rect, radii, Path.Direction.CW)
                }
                canvas.drawPath(path, borderPaint)
            }

            Shape.UnsetShape -> {
                val rect = RectF(
                    halfBorderWidth,
                    halfBorderWidth,
                    width.toFloat() - halfBorderWidth,
                    height.toFloat() - halfBorderWidth
                )
                canvas.drawRect(rect, borderPaint)
            }
        }
    }

    private fun getInitials(title: CharSequence): CharSequence {
        val text = extractInitials(title)
        initials = emojiProcessor.processEmojiSafe(text) { processed ->
            initials = processed
            postInvalidate()
        }
        return initials
    }

    private fun extractInitials(title: CharSequence): CharSequence {
        if (title.isBlank()) return ""
        val words = title.trim().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        val (firstChar, firstIsEmoji) = words[0].getFirstCharIsEmoji()
        if (firstIsEmoji) return firstChar

        if (words.size > 1) {
            val (secondChar, secondIsEmoji) = words[1].getFirstCharIsEmoji()
            return SpannableStringBuilder()
                .append(firstChar.toString().uppercase())
                .append(if (secondIsEmoji) secondChar else secondChar.toString().uppercase())
        }
        return firstChar.toString().uppercase()
    }

    private fun getAvatarRandomColor(initials: CharSequence): Int {
        val colors = if (isInEditMode)
            listOf(1) else SceytChatUIKit.config.defaultAvatarBackgroundColors.getColors(context)
        return colors[abs(initials.toString().hashCode()) % colors.size]
    }

    private fun getStaticLayout(title: CharSequence): StaticLayout {
        val width = Layout.getDesiredWidth(title, textPaint).roundUp()
        return StaticLayout.Builder.obtain(title, 0, title.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun loadAvatarImage(oldImageUrl: String?, preloadForOffline: Boolean = true) {
        if (!imageUrl.isNullOrBlank() && imageUrl != oldImageUrl) {
            AvatarImageLoader.loadAvatar(
                context = context.applicationContext,
                imageUrl = imageUrl,
                imageView = this,
                placeholder = placeholder ?: AvatarPlaceholder.FromDrawable(defaultPlaceholder),
                errorPlaceholder = errorPlaceholder
                        ?: AvatarErrorPlaceHolder.FromDrawable(defaultPlaceholder),
                loadCallback = { loading ->
                    avatarLoadCb?.invoke(loading)
                },
                preloadForOffline = preloadForOffline
            )
        } else {
            avatarLoadCb?.invoke(false)
        }
    }

    private fun setDefaultImageIfNeeded(avatar: DefaultAvatar?) {
        if (!imageUrl.isNullOrBlank()) return
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

            else -> {
                setImageResource(0)
            }
        }
    }

    private fun setInitialsIfNeeded(defaultAvatar: DefaultAvatar?) {
        (defaultAvatar as? DefaultAvatar.FromInitials)?.let {
            initials = getInitials(it.name)
        }
    }

    private fun drawInitialsAndBackground(canvas: Canvas, avatar: DefaultAvatar.FromInitials) {
        val initials = getInitials(avatar.name)
        val color = if (avatarBackgroundColor == 0)
            getAvatarRandomColor(avatar.name) else avatarBackgroundColor

        drawBackgroundColor(canvas, color)
        drawInitials(canvas, initials)
    }

    fun setImageUrl(url: String?, preloadForOffline: Boolean = true) {
        val oldImageUrl = imageUrl
        imageUrl = url
        setDefaultImageIfNeeded(defaultAvatar)
        loadAvatarImage(oldImageUrl, preloadForOffline)
        invalidate()
    }

    @Suppress("Unused")
    fun setDefaultAvatar(avatar: DefaultAvatar) {
        defaultAvatar = avatar
        setInitialsIfNeeded(defaultAvatar)
        setDefaultImageIfNeeded(defaultAvatar)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        emojiProcessor.unregister()
    }

    fun applyStyle(style: AvatarStyle) {
        textStyle = style.textStyle

        if (style.avatarBackgroundColor != UNSET_COLOR) {
            avatarBackgroundColor = style.avatarBackgroundColor
        }

        if (style.borderWidth != UNSET_BORDER_WIDTH) {
            borderWidth = style.borderWidth
            borderPaint.strokeWidth = style.borderWidth
        }

        if (style.borderColor != UNSET_COLOR) {
            borderColor = style.borderColor
            borderPaint.color = style.borderColor
        }

        shape = style.shape
        initShape(shape)
    }

    sealed interface DefaultAvatar {
        data class FromBitmap(val bitmap: Bitmap) : DefaultAvatar
        data class FromDrawable(val drawable: Drawable?) : DefaultAvatar
        data class FromDrawableRes(@param:DrawableRes val id: Int) : DefaultAvatar
        data class FromInitials(val name: CharSequence) : DefaultAvatar
    }

    sealed interface AvatarPlaceholder {
        data class FromDrawable(val drawable: Drawable?) : AvatarPlaceholder
        data class FromDrawableRes(@param:DrawableRes val id: Int) : AvatarPlaceholder
    }

    sealed interface AvatarErrorPlaceHolder {
        data class FromBitmap(val bitmap: Bitmap) : AvatarErrorPlaceHolder
        data class FromDrawable(val drawable: Drawable?) : AvatarErrorPlaceHolder
        data class FromDrawableRes(@param:DrawableRes val id: Int) : AvatarErrorPlaceHolder
    }

    fun appearanceBuilder() = AppearanceBuilder()

    inner class AvatarAppearance(
            val style: AvatarStyle,
            val imageUrl: String?,
            val defaultAvatar: DefaultAvatar?,
            val placeholder: AvatarPlaceholder?,
            val errorPlaceholder: AvatarErrorPlaceHolder?,
            val preloadForOffline: Boolean,
    ) {

        fun applyToAvatar() {
            val oldImageUrl = this@AvatarView.imageUrl
            this@AvatarView.imageUrl = imageUrl
            this@AvatarView.defaultAvatar = defaultAvatar
            this@AvatarView.placeholder = placeholder
            this@AvatarView.errorPlaceholder = errorPlaceholder

            style.apply(this@AvatarView)
            setInitialsIfNeeded(defaultAvatar)
            setDefaultImageIfNeeded(defaultAvatar)
            loadAvatarImage(oldImageUrl, preloadForOffline)
        }
    }

    @Suppress("Unused")
    inner class AppearanceBuilder {
        private var imageUrl = this@AvatarView.imageUrl
        private var preloadForOffline: Boolean = true
        private var defaultAvatar = this@AvatarView.defaultAvatar
        private var placeholder: AvatarPlaceholder? = null
        private var errorPlaceholder: AvatarErrorPlaceHolder? = null
        private var avatarStyle = AvatarStyle(
            textStyle = this@AvatarView.textStyle,
            shape = this@AvatarView.shape,
            avatarBackgroundColor = this@AvatarView.avatarBackgroundColor
        )

        fun setStyle(style: AvatarStyle) = apply {
            avatarStyle = style
        }

        fun setImageUrl(url: String?) = apply {
            imageUrl = url
        }

        fun setPreloadForOffline(preload: Boolean) = apply {
            preloadForOffline = preload
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

        fun setDefaultAvatar(name: CharSequence) = apply {
            defaultAvatar = DefaultAvatar.FromInitials(name)
        }

        fun setPlaceholder(placeholder: AvatarPlaceholder) = apply {
            this.placeholder = placeholder
        }

        fun setErrorPlaceholder(errorPlaceholder: AvatarErrorPlaceHolder) = apply {
            this.errorPlaceholder = errorPlaceholder
        }

        fun setBorder(width: Float, @ColorInt color: Int) = apply {
            avatarStyle = avatarStyle.copy(borderWidth = width, borderColor = color)
        }

        fun setBorderWidth(width: Float) = apply {
            avatarStyle = avatarStyle.copy(borderWidth = width)
        }

        fun setBorderColor(@ColorInt color: Int) = apply {
            avatarStyle = avatarStyle.copy(borderColor = color)
        }

        fun build() = AvatarAppearance(
            style = avatarStyle,
            imageUrl = imageUrl,
            defaultAvatar = defaultAvatar,
            placeholder = placeholder,
            errorPlaceholder = errorPlaceholder,
            preloadForOffline = preloadForOffline
        )
    }
}

fun @receiver:DrawableRes Int.toDefaultAvatar(): DefaultAvatar {
    return DefaultAvatar.FromDrawableRes(this)
}


