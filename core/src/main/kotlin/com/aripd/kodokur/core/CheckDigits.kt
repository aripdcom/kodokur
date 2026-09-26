package com.aripd.kodokur.core

/** Check digits: GS1 (EAN/UPC, ISBN-13), ISBN-10 and ISSN. */
internal object CheckDigits {

    /** GS1 mod-10: weights 3, 1, 3, 1… starting from the right. [body] excludes the check digit. */
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

    /** ISBN-10: weights 10…2, mod 11; 'X' when the result is 10. [body] is nine digits. */
    fun isbn10(body: String): Char = mod11(body, firstWeight = 10)

    /** ISSN: weights 8…2, mod 11; 'X' when the result is 10. [body] is seven digits. */
    fun issn(body: String): Char = mod11(body, firstWeight = 8)

    private fun mod11(body: String, firstWeight: Int): Char {
        var sum = 0
        for (i in body.indices) sum += (body[i] - '0') * (firstWeight - i)
        val check = (11 - sum % 11) % 11
        return if (check == 10) 'X' else '0' + check
    }
}
