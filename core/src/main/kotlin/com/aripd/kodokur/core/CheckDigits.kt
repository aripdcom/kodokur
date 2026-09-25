package com.aripd.kodokur.core

/** Sağlama haneleri: GS1 (EAN/UPC, ISBN-13), ISBN-10 ve ISSN. */
internal object CheckDigits {

    /** GS1 mod-10: sağdan başlayarak 3, 1, 3, 1… ağırlıkları. [body] sağlama hanesiz. */
    fun gs1(body: String): Char {
        var sum = 0
        for (i in body.indices) {
            val digit = body[body.length - 1 - i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        return '0' + (10 - sum % 10) % 10
    }

    fun isValidGs1(code: String): Boolean =
        code.length >= 2 && code.all { it in '0'..'9' } && gs1(code.dropLast(1)) == code.last()

    /** ISBN-10: ağırlıklar 10…2, mod 11; 10 çıkarsa 'X'. [body] dokuz hane. */
    fun isbn10(body: String): Char = mod11(body, firstWeight = 10)

    /** ISSN: ağırlıklar 8…2, mod 11; 10 çıkarsa 'X'. [body] yedi hane. */
    fun issn(body: String): Char = mod11(body, firstWeight = 8)

    private fun mod11(body: String, firstWeight: Int): Char {
        var sum = 0
        for (i in body.indices) sum += (body[i] - '0') * (firstWeight - i)
        val check = (11 - sum % 11) % 11
        return if (check == 10) 'X' else '0' + check
    }
}
