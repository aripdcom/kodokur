package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IssnTest {

    @Test
    fun `ISSN from EAN-13 with 977 prefix`() {
        assertEquals("0317-8471", Issn.fromEan13("9770317847001")?.formatted)
        assertEquals("1050-124X", Issn.fromEan13("9771050124008")?.formatted)
    }

    @Test
    fun `non-977 EAN or EAN with invalid check digit is rejected`() {
        assertNull(Issn.fromEan13("9780306406157"))
        assertNull(Issn.fromEan13("9770317847002"))
    }

    @Test
    fun `ISSN from text`() {
        assertEquals("1050-124X", Issn.parse("ISSN 1050-124x")?.formatted)
        assertNull(Issn.parse("0317-8472"))
    }
}
