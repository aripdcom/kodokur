package com.aripd.kodokur.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Generates codes with ZXing, renders them to a grayscale image and reads them back through the decoder. */
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
    fun `EAN-13 book barcode`() {
        val scan = decoder.decode(render("9789750801716", BarcodeFormat.EAN_13, 400, 160))
        assertEquals(Scan("9789750801716", Symbology.EAN_13), scan)
    }

    @Test
    fun `QR and Code 128`() {
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
    fun `perpendicular linear barcode is read by rotating`() {
        val sideways = render("9780306406157", BarcodeFormat.EAN_13, 400, 160).rotated(90)
        assertNull(decoder.decode(sideways, alsoSideways = false))
        assertEquals("9780306406157", decoder.decode(sideways)?.text)
    }

    @Test
    fun `rotated plane with row stride like a camera frame`() {
        val image = render("9780306406157", BarcodeFormat.EAN_13, 400, 160)
        // The camera sensor delivers the image tilted 90° and aligns rows with padding bytes.
        val sensor = image.rotated(270)
        val stride = sensor.width + 16
        val plane = ByteArray(stride * sensor.height)
        for (y in 0 until sensor.height) System.arraycopy(sensor.pixels, y * sensor.width, plane, y * stride, sensor.width)
        val upright = LumaImage.fromPlane(plane, sensor.width, sensor.height, stride).rotated(90)
        assertEquals("9780306406157", decoder.decode(upright, alsoSideways = false)?.text)
    }

    @Test
    fun `blank image yields no result`() {
        assertNull(decoder.decode(LumaImage(ByteArray(200 * 200) { 0x7F }, 200, 200)))
    }

    @Test
    fun `rotation is reversible`() {
        val image = LumaImage(ByteArray(6) { it.toByte() }, 3, 2)
        for (d in listOf(90, 180, 270)) {
            val back = image.rotated(d).rotated(360 - d)
            assertEquals(image.pixels.toList(), back.pixels.toList())
            assertEquals(3, back.width)
        }
        assertEquals(listOf<Byte>(3, 0, 4, 1, 5, 2), image.rotated(90).pixels.toList())
    }
}
