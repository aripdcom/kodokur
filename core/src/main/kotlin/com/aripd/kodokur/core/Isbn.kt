package com.aripd.kodokur.core

/**
 * A validated ISBN. Always stored internally as ISBN-13; those with a 978 prefix
 * also have an ISBN-10 equivalent (the 979 prefix does not).
 */
class Isbn private constructor(val isbn13: String) {

    /** The ISBN-10 equivalent if the prefix is 978, otherwise null. */
    val isbn10: String?
        get() = if (isbn13.startsWith("978")) {
            isbn13.substring(3, 12).let { it + CheckDigits.isbn10(it) }
        } else {
            null
        }

    private val parts: List<String>? by lazy { IsbnRanges.split(isbn13) }

    /** "978-975-08-0171-6" format; unhyphenated if the range is unknown. */
    val hyphenated13: String get() = parts?.joinToString("-") ?: isbn13

    /** Hyphenated ISBN-10; unhyphenated if the range is unknown, null if there is no ISBN-10. */
    val hyphenated10: String?
        get() {
            val plain = isbn10 ?: return null
            val parts = parts ?: return plain
            return (parts.subList(1, 4) + plain.last().toString()).joinToString("-")
        }

    /** Name of the registration group: "Türkiye", "English language"… */
    val agency: String? get() = IsbnRanges.group(isbn13)?.agency

    override fun equals(other: Any?): Boolean = other is Isbn && other.isbn13 == isbn13
    override fun hashCode(): Int = isbn13.hashCode()
    override fun toString(): String = hyphenated13

    companion object {
        private val LABEL = Regex("^ISBN(-1[03])?:?", RegexOption.IGNORE_CASE)

        /** Date of the range table. */
        val rangesDate: String get() = IsbnRanges.date

        /**
         * Validates ISBN-13 or ISBN-10 text. Hyphens, whitespace and a leading
         * "ISBN" / "ISBN-13:" label are ignored. Null if the check digit does not match.
         */
        fun parse(text: String): Isbn? {
            val code = text.trim().replace(LABEL, "")
                .filter { it != '-' && !it.isWhitespace() }
                .uppercase()
            return when (code.length) {
                13 -> code.takeIf {
                    (it.startsWith("978") || it.startsWith("979")) && CheckDigits.isValidGs1(it)
                }?.let(::Isbn)

                10 -> {
                    val body = code.substring(0, 9)
                    if (body.all { it in '0'..'9' } && CheckDigits.isbn10(body) == code[9]) {
                        val ean = "978$body"
                        Isbn(ean + CheckDigits.gs1(ean))
                    } else {
                        null
                    }
                }

                else -> null
            }
        }
    }
}
