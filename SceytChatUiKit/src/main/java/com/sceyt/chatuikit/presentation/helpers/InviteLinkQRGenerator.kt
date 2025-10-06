package com.sceyt.chatuikit.presentation.helpers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object InviteLinkQRGenerator {


    fun generateQrWithRoundedLogo(
            content: String,
            size: Int = 800,
            logoDrawable: Drawable? = null,
            logoCornerRadius: Float = 28f, // adjust roundness
    ): Bitmap? {
        if (content.isBlank()) return null
        // === Generate QR matrix ===
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )

        val bitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        // === Convert BitMatrix to Bitmap ===
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val qrBitmap = createBitmap(size, size)
        qrBitmap.setPixels(pixels, 0, size, 0, 0, size, size)

        // === No logo? return just QR ===
        if (logoDrawable == null) return qrBitmap

        // === Prepare logo with Telegram-style proportions ===
        val logoSize = (size * 0.22).toInt() // logo covers ~22% of QR (similar to Telegram)
        val logoBitmap = logoDrawable.toBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)

        // === Round the logo corners ===
        val roundedLogo = createBitmap(logoSize, logoSize)
        val canvas = Canvas(roundedLogo)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(0f, 0f, logoSize.toFloat(), logoSize.toFloat())
        canvas.drawRoundRect(rect, logoCornerRadius, logoCornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(logoBitmap, 0f, 0f, paint)

        // === Draw logo on QR (centered with proper padding) ===
        val qrCanvas = Canvas(qrBitmap)
        
        // Calculate center position
        val centerX = size / 2
        val centerY = size / 2
        val logoLeft = centerX - logoSize / 2
        val logoTop = centerY - logoSize / 2

        // Draw white rounded background behind logo with generous padding (Telegram style)
        // The padding ensures QR patterns have proper distance from the logo
        val bgPadding = (logoSize * 0.15f).toInt() // ~15% padding around logo
        val bgSize = logoSize + (bgPadding * 2)
        val bgLeft = centerX - bgSize / 2
        val bgTop = centerY - bgSize / 2
        
        val bgRect = RectF(
            bgLeft.toFloat(),
            bgTop.toFloat(),
            (bgLeft + bgSize).toFloat(),
            (bgTop + bgSize).toFloat()
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        qrCanvas.drawRoundRect(bgRect, logoCornerRadius + 8f, logoCornerRadius + 8f, bgPaint)

        // Draw logo itself centered
        qrCanvas.drawBitmap(roundedLogo, logoLeft.toFloat(), logoTop.toFloat(), null)
        return qrBitmap
    }
}