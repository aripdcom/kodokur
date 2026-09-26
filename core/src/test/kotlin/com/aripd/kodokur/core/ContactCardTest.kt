package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Business cards in QR codes: vCard 2.1–4.0 and MECARD. */
class ContactCardTest {

    private fun parse(text: String) = ContentParser.parse(Scan(text, Symbology.QR_CODE)) as Content.Contact

    @Test
    fun `vCard 3 title, address, note and escapes`() {
        val card = parse(
            "BEGIN:VCARD\r\nVERSION:3.0\r\nN:Selman;Cem;;;\r\nFN:Cem Selman\r\n" +
                "ORG:ARIPD\\, Ltd.;Yazılım\r\nTITLE:Kurucu\r\n" +
                "TEL;TYPE=CELL:+905551112233\r\nitem1.EMAIL;TYPE=INTERNET:cem@aripd.com\r\n" +
                "ADR;TYPE=WORK:;;Bağdat Cd. 1\\;Kat 2;Kadıköy;İstanbul;34710;Türkiye\r\n" +
                "NOTE:Satır bir\\nSatır iki\r\nURL:https://aripd.com\r\nEND:VCARD",
        )
        assertEquals("Cem Selman", card.name)
        assertEquals("ARIPD, Ltd., Yazılım", card.organization)
        assertEquals("Kurucu", card.title)
        assertEquals(listOf("cem@aripd.com"), card.emails)
        assertEquals("Bağdat Cd. 1;Kat 2, Kadıköy, İstanbul, 34710, Türkiye", card.address)
        assertEquals("Satır bir\nSatır iki", card.note)
        assertEquals("https://aripd.com", card.url)
    }

    @Test
    fun `vCard 2_1 quoted-printable Turkish and soft line break`() {
        // "Gül Şahin", "Ürün Müdürü", "Çankaya, Ankara" — UTF-8 bytes as =XX.
        val card = parse(
            "BEGIN:VCARD\nVERSION:2.1\n" +
                "N;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=C5=9Eahin;G=C3=BCl;;;\n" +
                "TITLE;CHARSET=UTF-8;QUOTED-PRINTABLE:=C3=9Cr=C3=BCn M=C3=BCd=C3=BC=\nr=C3=BC\n" +
                "ADR;WORK;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:;;;=C3=87ankaya;Ankara;;\n" +
                "TEL;CELL:+905321234567\nEND:VCARD",
        )
        assertEquals("Gül Şahin", card.name)
        assertEquals("Ürün Müdürü", card.title)
        assertEquals("Çankaya, Ankara", card.address)
        assertEquals(listOf("+905321234567"), card.phones)
    }

    @Test
    fun `quoted-printable Latin-5 charset`() {
        // ISO-8859-9: ş = 0xFE, ı = 0xFD
        assertEquals("Işık", ContentParser.decodeQuotedPrintable("I=FE=FDk", "ISO-8859-9"))
    }

    @Test
    fun `MECARD address and note`() {
        val card = parse("MECARD:N:Selman,Cem;ADR:Bağdat Cd. 1,Kadıköy,İstanbul;NOTE:Fuar standı B12;TEL:+905551112233;;")
        assertEquals("Cem Selman", card.name)
        assertEquals("Bağdat Cd. 1, Kadıköy, İstanbul", card.address)
        assertEquals("Fuar standı B12", card.note)
    }
}
