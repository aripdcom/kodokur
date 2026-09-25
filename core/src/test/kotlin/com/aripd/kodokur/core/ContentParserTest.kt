package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentParserTest {

    private fun parse(text: String, symbology: Symbology = Symbology.QR_CODE, addOn: String? = null) =
        ContentParser.parse(Scan(text, symbology, addOn))

    @Test
    fun `kitap barkodu ve fiyat eki`() {
        val book = parse("9780306406157", Symbology.EAN_13, addOn = "52495") as Content.Book
        assertEquals("978-0-306-40615-7", book.isbn.hyphenated13)
        assertEquals("$24.95", book.price)
        assertEquals(null, (parse("9780306406157", Symbology.EAN_13, "90000") as Content.Book).price)
        assertEquals("£7.99", (parse("9780306406157", Symbology.EAN_13, "00799") as Content.Book).price)
    }

    @Test
    fun `dergi barkodu ve sayı eki`() {
        val magazine = parse("9770317847001", Symbology.EAN_13, addOn = "07") as Content.Periodical
        assertEquals("0317-8471", magazine.issn.formatted)
        assertEquals("07", magazine.issue)
    }

    @Test
    fun `market ürünü`() {
        assertEquals(Content.Product("4006381333931"), parse("4006381333931", Symbology.EAN_13))
        assertEquals(Content.Product("03600029145"), parse("03600029145", Symbology.UPC_A))
    }

    @Test
    fun `QR içinde metin olarak ISBN`() {
        assertTrue(parse("ISBN 978-975-08-0171-6") is Content.Book)
        // Sağlaması tutmayan numara düz metindir.
        assertEquals(Content.Text("978-975-08-0171-5"), parse("978-975-08-0171-5"))
    }

    @Test
    fun `bağlantılar`() {
        assertEquals(Content.Link("https://aripd.com/a?b=1"), parse("https://aripd.com/a?b=1"))
        assertEquals(Content.Link("https://www.aripd.com"), parse("www.aripd.com"))
        assertEquals(Content.Link("http://x.org"), parse("URLTO:http://x.org"))
        assertEquals(Content.Text("https://a b"), parse("https://a b"))
    }

    @Test
    fun `Wi-Fi ve kaçışlı karakterler`() {
        assertEquals(
            Content.Wifi("Ev Ağı", "pa;ss:w\\rd", "WPA", hidden = true),
            parse("WIFI:T:WPA;S:Ev Ağı;P:pa\\;ss\\:w\\\\rd;H:true;;"),
        )
        assertEquals(Content.Wifi("Kafe", null, "nopass", hidden = false), parse("WIFI:S:Kafe;T:nopass;P:;;"))
        assertEquals(Content.Text("WIFI:T:WPA;;"), parse("WIFI:T:WPA;;"))
    }

    @Test
    fun `e-posta biçimleri`() {
        assertEquals(
            Content.Email("a@b.com", "Merhaba dünya", null),
            parse("mailto:a@b.com?subject=Merhaba%20d%C3%BCnya"),
        )
        assertEquals(Content.Email("a@b.com", "Konu", "Gövde"), parse("MATMSG:TO:a@b.com;SUB:Konu;BODY:Gövde;;"))
    }

    @Test
    fun `telefon, SMS, konum`() {
        assertEquals(Content.Phone("+902121234567"), parse("tel:+902121234567"))
        assertEquals(Content.Sms("+90555", "Selam"), parse("SMSTO:+90555:Selam"))
        assertEquals(Content.Sms("+90555", null), parse("sms:+90555"))
        assertEquals(Content.Geo(41.0082, 28.9784), parse("geo:41.0082,28.9784?q=Sultanahmet"))
        assertEquals(Content.Text("geo:91,0"), parse("geo:91,0"))
    }

    @Test
    fun `kişi kartları`() {
        assertEquals(
            Content.Contact("Cem Selman", "ARIPD", listOf("+905551112233", "02121112233"), listOf("cem@aripd.com"), null),
            parse("MECARD:N:Selman,Cem;ORG:ARIPD;TEL:+905551112233;TEL:02121112233;EMAIL:cem@aripd.com;;"),
        )
        val vcard = "BEGIN:VCARD\r\nVERSION:3.0\r\nN:Selman;Cem;;;\r\nFN:Cem Selman\r\n" +
            "ORG:ARIPD\r\nTEL;TYPE=CELL:+905551112233\r\nEMAIL:cem@aripd.com\r\n" +
            "URL:https://aripd.com\r\nEND:VCARD"
        assertEquals(
            Content.Contact("Cem Selman", "ARIPD", listOf("+905551112233"), listOf("cem@aripd.com"), "https://aripd.com"),
            parse(vcard),
        )
    }

    @Test
    fun `tanınmayan metin`() {
        assertEquals(Content.Text("Merhaba"), parse("  Merhaba  "))
        assertEquals(Content.Text("ABC-123"), parse("ABC-123", Symbology.CODE_128))
    }
}
