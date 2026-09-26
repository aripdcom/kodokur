package com.aripd.kodokur.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Expected check digits and hyphenations were computed by hand/independently,
// without using the code, so the test does not validate the code's own output.
class IsbnTest {

    private fun isbn(text: String): Isbn =
        Isbn.parse(text).also { assertNotNull("should be valid: $text", it) }!!

    @Test
    fun `ISBN-13 and ISBN-10 resolve to the same book`() {
        assertEquals(isbn("9780306406157"), isbn("0-306-40615-2"))
        assertEquals("0306406152", isbn("978-0-306-40615-7").isbn10)
    }

    @Test
    fun `ISBN-10 with X check digit`() {
        val book = isbn("0-8044-2957-X")
        assertEquals("9780804429573", book.isbn13)
        assertEquals("080442957X", book.isbn10)
        assertEquals("0-8044-2957-X", book.hyphenated10)
        assertEquals(book, isbn("080442957x"))
    }

    @Test
    fun `label and whitespace are ignored`() {
        assertEquals("9780306406157", isbn("ISBN 978-0-306-40615-7").isbn13)
        assertEquals("9780306406157", isbn("ISBN-13: 978 0 306 40615 7").isbn13)
        assertEquals("9780306406157", isbn("isbn-10:0306406152").isbn13)
    }

    @Test
    fun `code with invalid check digit or non-ISBN code is rejected`() {
        assertNull(Isbn.parse("9780306406158"))
        assertNull(Isbn.parse("0306406153"))
        assertNull(Isbn.parse("4006381333931")) // retail product
        assertNull(Isbn.parse("978030640615"))
        assertNull(Isbn.parse("97803064061X7"))
    }

    @Test
    fun `hyphenation follows the range table`() {
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
    fun `979 prefix has no ISBN-10`() {
        val book = isbn("9791090636071")
        assertNull(book.isbn10)
        assertNull(book.hyphenated10)
    }

    @Test
    fun `undefined range stays unhyphenated`() {
        // In 978-625, 0200000-3199999 is not allocated yet (length 0).
        assertEquals("9786251000006", isbn("9786251000006").hyphenated13)
    }

    @Test
    fun `registration group name`() {
        assertEquals("Türkiye", isbn("9789750801716").agency)
        assertEquals("Türkiye", isbn("9786053600183").agency)
        assertEquals("English language", isbn("9780306406157").agency)
        assertEquals("France", isbn("9791090636071").agency)
    }

    @Test
    fun `range table is dated`() {
        assertTrue(Isbn.rangesDate, Isbn.rangesDate.contains("20"))
    }
}
