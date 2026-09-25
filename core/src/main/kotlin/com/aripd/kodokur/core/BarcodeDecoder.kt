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
 * ZXing üzerine ince bir katman. Örnek iş parçacığı güvenli değildir: kamera
 * çözümleyicisi kendi örneğini tek iş parçacığında kullanır.
 *
 * [thorough] galeriden seçilen tek görsel içindir: daha yavaş ama daha inatçı
 * (TRY_HARDER, ters renkli kodlar). Kamerada kapalıdır; her kare hızlı geçmeli,
 * nasılsa bir sonraki kare gelir.
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
     * Görüntüyü olduğu gibi dener; [alsoSideways] ise bulamayınca 90° çevrilmişini
     * de dener. Çizgili (1B) barkodları ZXing yalnızca yatay satırlarda arar;
     * telefona dik tutulan bir kitabın barkodu ancak çevrilince okunur. 2B kodlar
     * yönden bağımsızdır.
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

    private fun Result.toScan(): Scan? {
        val symbology = Symbology.fromName(barcodeFormat.name) ?: return null
        if (text.isNullOrEmpty()) return null
        return Scan(
            text = text,
            symbology = symbology,
            addOn = resultMetadata?.get(ResultMetadataType.UPC_EAN_EXTENSION) as? String,
        )
    }
}
