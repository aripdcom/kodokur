package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IssnTest {

    @Test
    fun `977 önekli EAN-13'ten ISSN`() {
        assertEquals("0317-8471", Issn.fromEan13("9770317847001")?.formatted)
        assertEquals("1050-124X", Issn.fromEan13("9771050124008")?.formatted)
    }

    @Test
    fun `977 olmayan ya da sağlaması bozuk EAN reddedilir`() {
        assertNull(Issn.fromEan13("9780306406157"))
        assertNull(Issn.fromEan13("9770317847002"))
    }

    @Test
    fun `metinden ISSN`() {
        assertEquals("1050-124X", Issn.parse("ISSN 1050-124x")?.formatted)
        assertNull(Issn.parse("0317-8472"))
    }
}
