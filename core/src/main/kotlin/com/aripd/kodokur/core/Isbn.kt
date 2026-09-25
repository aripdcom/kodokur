package com.aripd.kodokur.core

/**
 * Doğrulanmış bir ISBN. İçeride her zaman ISBN-13 olarak tutulur; 978 önekli
 * olanların ISBN-10 karşılığı da vardır (979 önekinin yoktur).
 */
class Isbn private constructor(val isbn13: String) {

    /** 978 önekliyse ISBN-10 karşılığı, değilse null. */
    val isbn10: String?
        get() = if (isbn13.startsWith("978")) {
            isbn13.substring(3, 12).let { it + CheckDigits.isbn10(it) }
        } else {
            null
        }

    private val parts: List<String>? by lazy { IsbnRanges.split(isbn13) }

    /** "978-975-08-0171-6" biçimi; aralık bilinmiyorsa tiresiz. */
    val hyphenated13: String get() = parts?.joinToString("-") ?: isbn13

    /** ISBN-10'un tireli biçimi; aralık bilinmiyorsa tiresiz, ISBN-10 yoksa null. */
    val hyphenated10: String?
        get() {
            val plain = isbn10 ?: return null
            val parts = parts ?: return plain
            return (parts.subList(1, 4) + plain.last().toString()).joinToString("-")
        }

    /** Kayıt grubunun adı: "Türkiye", "English language"… */
    val agency: String? get() = IsbnRanges.group(isbn13)?.agency

    override fun equals(other: Any?): Boolean = other is Isbn && other.isbn13 == isbn13
    override fun hashCode(): Int = isbn13.hashCode()
    override fun toString(): String = hyphenated13

    companion object {
        private val LABEL = Regex("^ISBN(-1[03])?:?", RegexOption.IGNORE_CASE)

        /** Aralık tablosunun tarihi. */
        val rangesDate: String get() = IsbnRanges.date

        /**
         * ISBN-13 ya da ISBN-10 metnini doğrular. Tireler, boşluklar ve baştaki
         * "ISBN" / "ISBN-13:" etiketi yok sayılır. Sağlama hanesi tutmazsa null.
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
