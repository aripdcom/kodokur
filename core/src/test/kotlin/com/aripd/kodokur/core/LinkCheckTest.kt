package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkCheckTest {

    private fun inspect(url: String) = LinkCheck.inspect(url)!!

    @Test
    fun `güvenli sıradan bağlantı uyarı taşımaz`() {
        val info = inspect("https://www.aripd.com/kodokur?a=1#b")
        assertEquals("www.aripd.com", info.host)
        assertTrue(info.secure)
        assertTrue(info.warnings.isEmpty())
    }

    @Test
    fun `şifresiz bağlantı`() {
        assertEquals(setOf(LinkWarning.NOT_ENCRYPTED), inspect("http://aripd.com").warnings)
    }

    @Test
    fun `kullanıcı adı hilesi gerçek hedefi gösterir`() {
        val info = inspect("https://www.bankam.com.tr@kotu.example/giris")
        assertEquals("kotu.example", info.host)
        assertEquals(setOf(LinkWarning.HIDDEN_DESTINATION), info.warnings)
    }

    @Test
    fun `Kiril harfli benzer alan adı`() {
        // İlk harf Kiril "а" (U+0430).
        val info = inspect("https://аpple.com/")
        assertEquals("аpple.com", info.host)
        assertTrue(info.asciiHost.startsWith("xn--"))
        assertEquals(setOf(LinkWarning.LOOKALIKE_HOST), info.warnings)
    }

    @Test
    fun `punycode yazılmış alan adı da yakalanır`() {
        val info = inspect("https://xn--pple-43d.com")
        assertEquals("аpple.com", info.host)
        assertTrue(LinkWarning.LOOKALIKE_HOST in info.warnings)
    }

    @Test
    fun `IP adresi ve port`() {
        assertEquals(setOf(LinkWarning.IP_ADDRESS, LinkWarning.NOT_ENCRYPTED), inspect("http://192.168.1.1:8080/").warnings)
        assertEquals(setOf(LinkWarning.IP_ADDRESS), inspect("https://[2001:db8::1]/").warnings)
        assertEquals("aripd.com", inspect("https://ARIPD.com.:443/").host)
    }

    @Test
    fun `http olmayan metin incelenmez`() {
        assertNull(LinkCheck.inspect("ftp://aripd.com"))
        assertNull(LinkCheck.inspect("https:///yol"))
    }
}
