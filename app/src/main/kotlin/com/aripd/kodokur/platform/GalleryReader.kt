package com.aripd.kodokur.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.aripd.kodokur.core.BarcodeDecoder
import com.aripd.kodokur.core.LumaImage
import com.aripd.kodokur.core.Scan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the code in an image from the system photo picker. No permission needed. */
object GalleryReader {

    sealed interface Outcome {
        data class Found(val scan: Scan) : Outcome
        data object NotFound : Outcome
        data object Failed : Outcome
    }

    /** The long side is scaled down to this: decoding a full 12 MP photo is slow and unnecessary. */
    private const val MAX_SIDE = 2048

    suspend fun read(context: Context, uri: Uri): Outcome = withContext(Dispatchers.Default) {
        val bitmap = try {
            load(context, uri)
        } catch (e: Exception) {
            Log.w("Kodokur", "Could not open image", e)
            null
        } ?: return@withContext Outcome.Failed

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val scan = BarcodeDecoder(thorough = true).decode(LumaImage.fromArgb(pixels, w, h))
        if (scan != null) Outcome.Found(scan) else Outcome.NotFound
    }

    private fun load(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
