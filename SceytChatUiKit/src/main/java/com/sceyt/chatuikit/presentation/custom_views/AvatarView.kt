package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.getFirstCharIsEmoji
import com.sceyt.chatuikit.extensions.processEmojiCompat
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import kotlin.math.abs

class AvatarView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var name: CharSequence? = null
    private var imageUrl: String? = null
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var imagePaint: Paint
    private lateinit var backgroundPaint: Paint
    private var textSize = 0
    private var avatarLoadCb: ((loading: Boolean) -> Unit?)? = null

    @ColorInt
    private var avatarBackgroundColor: Int = 0
    private var defaultAvatar: DefaultAvatar? = null

    init {
        var enableRipple = true
        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.AvatarView)
            name = a.getString(R.styleable.AvatarView_sceytUiAvatarFullName) ?: name
            imageUrl = a.getString(R.styleable.AvatarView_sceytUiAvatarImageUrl)
            textSize = a.getDimensionPixelSize(R.styleable.AvatarView_sceytUiAvatarTextSize, textSize)
            avatarBackgroundColor = a.getColor(R.styleable.AvatarView_sceytUiAvatarColor, avatarBackgroundColor)
            val defaultAvatarResId = a.getResourceId(R.styleable.AvatarView_sceytUiAvatarDefaultIcon, 0)
            if (defaultAvatarResId != 0) {
                defaultAvatar = defaultAvatarResId.toDefaultAvatar()
            }
            enableRipple = a.getBoolean(R.styleable.AvatarView_sceytUiAvatarEnableRipple, true)
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
            val default = defaultAvatar
            if (default == null) {
                val initials = getInitials(name ?: "")
                drawBackgroundColor(canvas, avatarBackgroundColor)
                drawInitials(canvas, initials)
            } else {
                // Id default avatar is DefaultAvatar.Initial then drawDefaultImage
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
        textPaint.textSize = if (textSize > 0) textSize.toFloat() else width * 0.38f
        textPaint.color = Color.WHITE

        val staticLayout = getStaticLayout(name)

        val xPos = (width - staticLayout.width) / 2f
        canvas.save()
        canvas.translate(xPos, (height - staticLayout.height) / 2f)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawBackgroundColor(canvas: Canvas, @ColorInt color: Int) {
        if (color == 0) return
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), (width / 2).toFloat(), backgroundPaint.apply {
            this.color = color
        })
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
                .error(R.drawable.sceyt_bg_circle_gray)
                .placeholder(context.getCompatDrawable(R.drawable.sceyt_bg_circle_gray))
                .listener(com.sceyt.chatuikit.extensions.glideRequestListener {
                    avatarLoadCb?.invoke(false)
                })
                .circleCrop()
                .into(this)
        }
    }

    private fun drawDefaultImage(canvas: Canvas, avatar: DefaultAvatar) {
        when (avatar) {
            is DefaultAvatar.FromBitmap -> {
                drawCircleBitmap(avatar.bitmap, canvas)
            }

            is DefaultAvatar.FromDrawable -> {
                drawCircleDrawable(avatar.drawable, canvas)
            }

            is DefaultAvatar.FromDrawableRes -> {
                val drawable = context.getCompatDrawable(avatar.id) ?: return
                drawCircleDrawable(drawable, canvas)
            }

            is DefaultAvatar.Initial -> {
                val color = if (avatarBackgroundColor == 0)
                    getAvatarRandomColor(avatar.initial) else avatarBackgroundColor
                drawBackgroundColor(canvas, color)
                drawInitials(canvas, avatar.initial)
            }
        }
    }

    private fun drawCircleDrawable(drawable: Drawable, canvas: Canvas) {
        // Get the drawable's width and height
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        // Find the center and radius for the circle
        val radius = minOf(width, height) / 2f
        val cx = width / 2f
        val cy = height / 2f

        // Create a path for the circular shape
        val path = Path()
        path.addCircle(cx, cy, radius, Path.Direction.CW)

        // Clip the canvas to the circle path
        canvas.save()
        canvas.clipPath(path)

        // Scale and position the drawable within the circle
        val matrix = Matrix()
        matrix.setScale(width / drawableWidth.toFloat(), height / drawableHeight.toFloat())
        // matrix.postTranslate(cx - drawableWidth / 2f, cy - drawableHeight / 2f)
        canvas.concat(matrix)

        // Set the bounds and draw the drawable on the canvas
        drawable.setBounds(0, 0, drawableWidth, drawableHeight)
        drawable.draw(canvas)

        // Restore the canvas to remove the clipping
        canvas.restore()
    }

    private fun drawCircleBitmap(bitmap: Bitmap, canvas: Canvas) {
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

    fun setImageUrl(url: String?) {
        val oldImageUrl = imageUrl
        imageUrl = url
        invalidate()
        loadAvatarImage(oldImageUrl)
    }

    fun setAvatarImageLoadListener(cb: (Boolean) -> Unit) {
        avatarLoadCb = cb
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = measuredWidth
        setMeasuredDimension(measuredWidth, measuredWidth)
    }

    sealed class DefaultAvatar {
        data class FromBitmap(val bitmap: Bitmap) : DefaultAvatar()
        data class FromDrawable(val drawable: Drawable) : DefaultAvatar()
        data class FromDrawableRes(@DrawableRes val id: Int) : DefaultAvatar()
        data class Initial(
                val initial: CharSequence
        ) : DefaultAvatar()
    }

    inner class StyleBuilder {
        private var name: CharSequence? = this@AvatarView.name
        private var imageUrl: String? = this@AvatarView.imageUrl
        private var textSize = this@AvatarView.textSize

        @ColorInt
        private var avatarBackgroundColor: Int = this@AvatarView.avatarBackgroundColor
        private var defaultAvatar: DefaultAvatar? = this@AvatarView.defaultAvatar

        fun setName(name: CharSequence) = apply {
            this.name = name
        }

        fun setImageUrl(url: String?) = apply {
            this.imageUrl = url
        }

        fun setTextSize(size: Int) = apply {
            this.textSize = size
        }

        fun setAvatarBackgroundColor(@ColorInt color: Int) = apply {
            avatarBackgroundColor = color
        }

        fun setAvatarBackgroundColorRes(@ColorRes color: Int) = apply {
            avatarBackgroundColor = if (color != 0)
                context.getCompatColor(color) else 0
        }

        fun setDefaultAvatar(avatar: DefaultAvatar) = apply {
            defaultAvatar = avatar
        }

        fun setDefaultAvatar(@DrawableRes id: Int) = apply {
            defaultAvatar = id.toDefaultAvatar()
        }

        fun setDefaultAvatar(drawable: Drawable) = apply {
            defaultAvatar = DefaultAvatar.FromDrawable(drawable)
        }

        fun setDefaultAvatar(bitmap: Bitmap) = apply {
            defaultAvatar = DefaultAvatar.FromBitmap(bitmap)
        }

        fun build() {
            val oldImageUrl = this@AvatarView.imageUrl
            this@AvatarView.name = name
            this@AvatarView.imageUrl = imageUrl
            this@AvatarView.textSize = textSize
            this@AvatarView.avatarBackgroundColor = avatarBackgroundColor
            this@AvatarView.defaultAvatar = defaultAvatar

            invalidate()
            loadAvatarImage(oldImageUrl)
        }
    }

    fun styleBuilder() = StyleBuilder()
}

fun @receiver:DrawableRes Int.toDefaultAvatar(): DefaultAvatar {
    return DefaultAvatar.FromDrawableRes(this)
}


