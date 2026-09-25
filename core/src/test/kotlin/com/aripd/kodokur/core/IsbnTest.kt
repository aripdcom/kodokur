package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Beklenen sağlama haneleri ve tirelemeler kodu kullanmadan, elle/bağımsız
// hesaplanmıştır; test, kodun kendi çıktısını doğrulamasın.
class IsbnTest {

    private fun isbn(text: String): Isbn =
        Isbn.parse(text).also { assertNotNull("geçerli olmalı: $text", it) }!!

    @Test
    fun `ISBN-13 ve ISBN-10 aynı kitaba çözülür`() {
        assertEquals(isbn("9780306406157"), isbn("0-306-40615-2"))
        assertEquals("0306406152", isbn("978-0-306-40615-7").isbn10)
    }

    @Test
    fun `X sağlamalı ISBN-10`() {
        val book = isbn("0-8044-2957-X")
        assertEquals("9780804429573", book.isbn13)
        assertEquals("080442957X", book.isbn10)
        assertEquals("0-8044-2957-X", book.hyphenated10)
        assertEquals(book, isbn("080442957x"))
    }

    @Test
    fun `etiket ve boşluklar yok sayılır`() {
        assertEquals("9780306406157", isbn("ISBN 978-0-306-40615-7").isbn13)
        assertEquals("9780306406157", isbn("ISBN-13: 978 0 306 40615 7").isbn13)
        assertEquals("9780306406157", isbn("isbn-10:0306406152").isbn13)
    }

    @Test
    fun `sağlaması tutmayan ya da ISBN olmayan kod reddedilir`() {
        assertNull(Isbn.parse("9780306406158"))
        assertNull(Isbn.parse("0306406153"))
        assertNull(Isbn.parse("4006381333931")) // market ürünü
        assertNull(Isbn.parse("978030640615"))
        assertNull(Isbn.parse("97803064061X7"))
    }

    @Test
    fun `tireleme aralık tablosuna göre`() {
        assertEquals("978-0-306-40615-7", isbn("9780306406157").hyphenated13)
        assertEquals("978-0-8044-2957-3", isbn("9780804429573").hyphenated13)
        assertEquals("978-975-08-0171-6", isbn("9789750801716").hyphenated13)
        assertEquals("978-975-999-999-5", isbn("9789759999995").hyphenated13)
        assertEquals("978-605-360-018-3", isbn("9786053600183").hyphenated13)
        assertEquals("978-625-7000-12-3", isbn("9786257000123").hyphenated13)
        assertEquals("979-10-90636-07-1", isbn("9791090636071").hyphenated13)
        assertEquals("975-08-0171-7", isbn("9789750801716").hyphenated10)
    }

    @Test
    fun `979 önekinin ISBN-10'u yoktur`() {
        val book = isbn("9791090636071")
        assertNull(book.isbn10)
        assertNull(book.hyphenated10)
    }

    @Test
    fun `tanımsız aralıkta tiresiz kalır`() {
        // 978-625'te 0200000-3199999 henüz dağıtılmamış (uzunluk 0).
        assertEquals("9786251000006", isbn("9786251000006").hyphenated13)
    }

    @Test
    fun `kayıt grubu adı`() {
        assertEquals("Türkiye", isbn("9789750801716").agency)
        assertEquals("Türkiye", isbn("9786053600183").agency)
        assertEquals("English language", isbn("9780306406157").agency)
        assertEquals("France", isbn("9791090636071").agency)
    }

    @Test
    fun `aralık tablosu tarihli`() {
        assertTrue(Isbn.rangesDate, Isbn.rangesDate.contains("20"))
    }
}
