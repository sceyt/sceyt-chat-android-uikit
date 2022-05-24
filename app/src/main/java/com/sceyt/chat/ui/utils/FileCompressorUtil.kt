package com.sceyt.chat.ui.utils

import android.content.Context
import android.net.Uri
import com.sceyt.chat.ui.extensions.getFileName
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.*

object FileCompressorUtil {

    fun compress(context: Context, uri: Uri): File? {
        if (!uri.path.isNullOrBlank()) {
            val tmpFile = File(context.cacheDir, getFileName(context, uri))
            if (tmpFile.isDirectory)
                tmpFile.delete()
            return context.contentResolver.openInputStream(uri)?.let { compress(it, tmpFile) }
        }
        return null
    }

    private fun compress(originalStream: InputStream, tmpFile: File): File {
        val fout: OutputStream = FileOutputStream(tmpFile)
        val out = BufferedOutputStream(fout)
        val gzOut = GzipCompressorOutputStream(out)
        val buffer = ByteArray(1024)
        var n: Int
        while (-1 != originalStream.read(buffer).also { n = it }) {
            gzOut.write(buffer, 0, n)
        }
        gzOut.close()
        originalStream.close()

        return tmpFile
    }
}