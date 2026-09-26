package com.aripd.kodokur.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer

/**
 * A thin layer over ZXing. An instance is not thread-safe: the camera analyzer
 * uses its own instance on a single thread.
 *
 * [thorough] is for a single image picked from the gallery: slower but more
 * persistent (TRY_HARDER, inverted codes). Off for the camera; each frame must
 * go through quickly, and another frame is coming anyway.
 */
class BarcodeDecoder(private val thorough: Boolean = false) {

    private val reader = MultiFormatReader().apply {
        val hints = HashMap<DecodeHintType, Any>()
        hints[DecodeHintType.POSSIBLE_FORMATS] = Symbology.entries.map { BarcodeFormat.valueOf(it.name) }
        if (thorough) {
            hints[DecodeHintType.TRY_HARDER] = true
            hints[DecodeHintType.ALSO_INVERTED] = true
        }
        setHints(hints)
    }

    /**
     * Tries the image as is; with [alsoSideways], if nothing is found, also tries it
     * rotated 90°. ZXing searches linear (1D) barcodes only along horizontal rows;
     * the barcode of a book held perpendicular to the phone reads only once rotated.
     * 2D codes are orientation-independent.
     */
    fun decode(image: LumaImage, alsoSideways: Boolean = true): Scan? =
        decodeOnce(image) ?: if (alsoSideways) decodeOnce(image.rotated(90)) else null

    private fun decodeOnce(image: LumaImage): Scan? {
        val source = PlanarYUVLuminanceSource(
            image.pixels, image.width, image.height, 0, 0, image.width, image.height, false,
        )
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).toScan()
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }

    private companion object {
        val GS1_IDENTIFIERS = setOf("]d2", "]C1", "]Q3", "]e0")
    }

    private fun Result.toScan(): Scan? {
        val symbology = Symbology.fromName(barcodeFormat.name) ?: return null
        if (text.isNullOrEmpty()) return null
        val identifier = resultMetadata?.get(ResultMetadataType.SYMBOLOGY_IDENTIFIER) as? String
        return Scan(
            text = text,
            symbology = symbology,
            addOn = resultMetadata?.get(ResultMetadataType.UPC_EAN_EXTENSION) as? String,
            // ]d2 DataMatrix, ]C1 Code 128, ]Q3 QR, ]e0 DataBar: a GS1 code starting with FNC1.
            gs1 = identifier in GS1_IDENTIFIERS ||
                symbology == Symbology.RSS_14 || symbology == Symbology.RSS_EXPANDED,
        )
    }
}
