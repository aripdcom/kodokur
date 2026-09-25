package com.aripd.kodokur.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ZXing ile kod üretip gri görüntüye çevirir ve çözücüden geri okur. */
class BarcodeDecoderTest {

    private fun render(text: String, format: BarcodeFormat, width: Int, height: Int, margin: Int = 40): LumaImage {
        val matrix = MultiFormatWriter().encode(text, format, width, height)
        val w = matrix.width + 2 * margin
        val h = matrix.height + 2 * margin
        val pixels = ByteArray(w * h) { 0xFF.toByte() }
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) pixels[(y + margin) * w + x + margin] = 0
        }
        return LumaImage(pixels, w, h)
    }

    private val decoder = BarcodeDecoder()

    @Test
    fun `EAN-13 kitap barkodu`() {
        val scan = decoder.decode(render("9789750801716", BarcodeFormat.EAN_13, 400, 160))
        assertEquals(Scan("9789750801716", Symbology.EAN_13), scan)
    }

    @Test
    fun `QR ve Code 128`() {
        assertEquals(
            Scan("WIFI:T:WPA;S:Ev;P:gizli;;", Symbology.QR_CODE),
            decoder.decode(render("WIFI:T:WPA;S:Ev;P:gizli;;", BarcodeFormat.QR_CODE, 300, 300)),
        )
        assertEquals(
            Scan("KODOKUR-128", Symbology.CODE_128),
            decoder.decode(render("KODOKUR-128", BarcodeFormat.CODE_128, 400, 120)),
        )
    }

    @Test
    fun `dik tutulan çizgili barkod çevrilerek okunur`() {
        val sideways = render("9780306406157", BarcodeFormat.EAN_13, 400, 160).rotated(90)
        assertNull(decoder.decode(sideways, alsoSideways = false))
        assertEquals("9780306406157", decoder.decode(sideways)?.text)
    }

    @Test
    fun `kamera karesi gibi döndürülmüş ve satır adımlı düzlem`() {
        val image = render("9780306406157", BarcodeFormat.EAN_13, 400, 160)
        // Kamera sensörü görüntüyü 90° yatık verir ve satırları doldurma baytıyla hizalar.
        val sensor = image.rotated(270)
        val stride = sensor.width + 16
        val plane = ByteArray(stride * sensor.height)
        for (y in 0 until sensor.height) System.arraycopy(sensor.pixels, y * sensor.width, plane, y * stride, sensor.width)
        val upright = LumaImage.fromPlane(plane, sensor.width, sensor.height, stride).rotated(90)
        assertEquals("9780306406157", decoder.decode(upright, alsoSideways = false)?.text)
    }

    @Test
    fun `boş görüntüde sonuç yok`() {
        assertNull(decoder.decode(LumaImage(ByteArray(200 * 200) { 0x7F }, 200, 200)))
    }

    @Test
    fun `döndürme ters çevrilebilir`() {
        val image = LumaImage(ByteArray(6) { it.toByte() }, 3, 2)
        for (d in listOf(90, 180, 270)) {
            val back = image.rotated(d).rotated(360 - d)
            assertEquals(image.pixels.toList(), back.pixels.toList())
            assertEquals(3, back.width)
        }
        assertEquals(listOf<Byte>(3, 0, 4, 1, 5, 2), image.rotated(90).pixels.toList())
    }
}
