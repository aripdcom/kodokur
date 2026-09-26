package com.aripd.kodokur.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Gs1Test {

    private val gs = Gs1.GS
    private val today = LocalDate.of(2026, 9, 26)

    // Medicine DataMatrix: GTIN, expiry, batch (variable, ends with GS), serial.
    private val medicine = "01086995040123471727123110ABC123${gs}21SN0001"

    @Test
    fun `medicine code fields`() {
        val data = Gs1.parse(medicine, flagged = true)!!
        assertEquals("08699504012347", data.gtin)
        assertEquals("8699504012347", data.ean13)
        assertEquals(Gs1Date(LocalDate.of(2027, 12, 31), dayGiven = true), data.expiry)
        assertEquals("ABC123", data.batch)
        assertEquals("SN0001", data.serial)
        assertTrue(data.others.isEmpty())
    }

    @Test
    fun `parenthesized notation yields the same data`() {
        val parenthesized = Gs1.parse("(01)08699504012347(17)271231(10)ABC123(21)SN0001", flagged = false)
        assertEquals(Gs1.parse(medicine, flagged = true), parenthesized)
    }

    @Test
    fun `day 00 is the last day of the month`() {
        val date = Gs1Date.parse("280200", today)!!
        assertEquals(LocalDate.of(2028, 2, 29), date.date)
        assertFalse(date.dayGiven)
        assertNull(Gs1Date.parse("271301", today))
        assertNull(Gs1Date.parse("270231", today))
    }

    @Test
    fun `sliding century rule`() {
        assertEquals(2075, Gs1Date.parse("751231", today)!!.date.year)
        assertEquals(1977, Gs1Date.parse("770101", today)!!.date.year)
        assertEquals(1999, Gs1Date.parse("991231", today)!!.date.year)
        assertEquals(2026, Gs1Date.parse("260101", today)!!.date.year)
    }

    @Test
    fun `is expired`() {
        val expiry = Gs1Date.parse("260925", today)!!
        assertTrue(expiry.isPast(today))
        assertFalse(Gs1Date.parse("260926", today)!!.isPast(today))
    }

    @Test
    fun `measure and unknown identifier`() {
        val data = Gs1.parse("0104006381333931310300150010L1", flagged = true)!!
        assertEquals(listOf(Gs1Element("3103", "001500")), data.others)
        assertEquals("L1", data.batch)
        assertNull(Gs1.parse("01040063813339319X", flagged = true))
    }

    @Test
    fun `GTIN with invalid check digit is rejected`() {
        assertNull(Gs1.parse("0108699504012348172712311", flagged = true))
    }

    @Test
    fun `unflagged plain number is not GS1`() {
        assertNull(Gs1.parse("0108699504012347172712311", flagged = false))
        assertNull(Gs1.parse("12345", flagged = false))
        assertNull(Gs1.parse("https://aripd.com", flagged = false))
        assertNotNull(Gs1.parse("0108699504012347${gs}10ABC", flagged = false))
    }

    @Test
    fun `content parser distinguishes GS1 from product`() {
        val content = ContentParser.parse(Scan(medicine, Symbology.DATA_MATRIX, gs1 = true))
        assertTrue(content is Content.Gs1)
        // A DataBar carrying only a GTIN is a retail product.
        assertEquals(
            Content.Product("04006381333931"),
            ContentParser.parse(Scan("0104006381333931", Symbology.RSS_14, gs1 = true)),
        )
    }

    /**
     * ZXing's writer enters GS1 mode in different ways: DataMatrix only with
     * DATA_MATRIX_COMPACT (a GS in the data becomes FNC1); in Code 128, FNC1 is written
     * as the ñ character. In both cases the decoded text matches a real scanner's.
     */
    private fun render(text: String, format: BarcodeFormat, w: Int, h: Int): LumaImage {
        val hints = mapOf(EncodeHintType.GS1_FORMAT to true, EncodeHintType.DATA_MATRIX_COMPACT to true)
        val m = MultiFormatWriter().encode(text, format, w, h, hints)
        val margin = 40
        val width = m.width + 2 * margin
        val height = m.height + 2 * margin
        val px = ByteArray(width * height) { 0xFF.toByte() }
        for (y in 0 until m.height) for (x in 0 until m.width) if (m[x, y]) px[(y + margin) * width + x + margin] = 0
        return LumaImage(px, width, height)
    }

    @Test
    fun `GS1 DataMatrix comes from the scanner flagged as GS1`() {
        val scan = BarcodeDecoder().decode(render(medicine, BarcodeFormat.DATA_MATRIX, 300, 300))!!
        assertEquals(Symbology.DATA_MATRIX, scan.symbology)
        assertTrue("GS1 flag", scan.gs1)
        assertEquals("SN0001", (ContentParser.parse(scan) as Content.Gs1).data.serial)
    }

    @Test
    fun `GS1-128 barcode`() {
        val scan = BarcodeDecoder().decode(render("ñ0104006381333931ñ10L1", BarcodeFormat.CODE_128, 600, 150))!!
        assertTrue(scan.gs1)
        assertEquals("L1", (ContentParser.parse(scan) as Content.Gs1).data.batch)
    }

    @Test
    fun `plain DataMatrix is not GS1`() {
        val m = MultiFormatWriter().encode("merhaba", BarcodeFormat.DATA_MATRIX, 200, 200)
        val w = m.width + 80
        val px = ByteArray(w * (m.height + 80)) { 0xFF.toByte() }
        for (y in 0 until m.height) for (x in 0 until m.width) if (m[x, y]) px[(y + 40) * w + x + 40] = 0
        val scan = BarcodeDecoder().decode(LumaImage(px, w, m.height + 80))!!
        assertFalse(scan.gs1)
        assertEquals(Content.Text("merhaba"), ContentParser.parse(scan))
    }
}
