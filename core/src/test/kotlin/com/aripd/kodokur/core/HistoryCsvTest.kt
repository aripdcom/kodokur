package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class HistoryCsvTest {

    @Test
    fun `başlık, ISBN sütunları ve tırnaklama`() {
        val csv = HistoryCsv.export(
            listOf(
                Record(0L, Scan("9780306406157", Symbology.EAN_13, "52495")),
                Record(61_000L, Scan("a,\"b\"\nc", Symbology.QR_CODE)),
            ),
            ZoneOffset.UTC,
        )
        assertEquals(
            "﻿time,format,content,add_on,isbn13,isbn10\r\n" +
                "1970-01-01 00:00:00,EAN-13,9780306406157,52495,9780306406157,0306406152\r\n" +
                "1970-01-01 00:01:01,QR,\"a,\"\"b\"\"\nc\",,,\r\n",
            csv,
        )
    }

    @Test
    fun `formül gibi başlayan metin etkisizleştirilir, sayılar kalır`() {
        assertEquals("'=HYPERLINK(1)", HistoryCsv.field("=HYPERLINK(1)"))
        assertEquals("'@SUM", HistoryCsv.field("@SUM"))
        assertEquals("-12", HistoryCsv.field("-12"))
        assertEquals("+90", HistoryCsv.field("+90"))
    }
}
