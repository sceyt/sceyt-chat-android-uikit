package com.sceyt.chatuikit.shared.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.persistence.mappers.getBlurredBytesAndSizeAsString
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapUtilTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `high bit depth bitmap converts to rgba bytes`() {
        val bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGBA_F16).apply { eraseColor(Color.RED) }

        val bytes = BitmapUtil.bitmapToRgba(bitmap)

        assertThat(bytes.size).isEqualTo(8)
        assertThat(bytes.take(4).map { it.toInt() and 0xff }).containsExactly(255, 0, 0, 255).inOrder()
        assertThat(bitmap.isRecycled).isFalse()
    }

    @Test
    fun `high bit depth image keeps its dimensions and blurred thumb`() {
        val file = temporary.newFile("hdr.png")
        val source = Bitmap.createBitmap(64, 32, Bitmap.Config.RGBA_F16).apply { eraseColor(Color.BLUE) }
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertThat(BitmapFactory.decodeFile(file.path).config).isEqualTo(Bitmap.Config.RGBA_F16)

        val metadata = getBlurredBytesAndSizeAsString(
            context = RuntimeEnvironment.getApplication(),
            metadata = null,
            filePath = file.path,
            type = AttachmentTypeEnum.Image.value,
        )

        val json = JSONObject(checkNotNull(metadata))
        assertThat(json.getInt(SceytConstants.Width)).isEqualTo(64)
        assertThat(json.getInt(SceytConstants.Height)).isEqualTo(32)
        assertThat(json.getString(SceytConstants.Thumb)).isNotEmpty()
    }
}
