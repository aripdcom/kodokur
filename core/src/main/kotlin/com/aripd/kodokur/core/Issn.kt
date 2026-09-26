package com.aripd.kodokur.core

/**
 * International Standard Serial Number. Magazine and newspaper barcodes are an EAN-13
 * with a 977 prefix: 977 + first seven ISSN digits + two-digit variant + GS1 check digit.
 * The ISSN's own check digit is not in the barcode; it is recomputed.
 */
class Issn private constructor(val digits: String) {

    /** "0317-8471" format. */
    val formatted: String get() = "${digits.substring(0, 4)}-${digits.substring(4)}"

    override fun equals(other: Any?): Boolean = other is Issn && other.digits == digits
    override fun hashCode(): Int = digits.hashCode()
    override fun toString(): String = formatted

    companion object {
        /** ISSN from an EAN-13 with a 977 prefix and a valid check digit; otherwise null. */
        fun fromEan13(ean: String): Issn? {
            if (ean.length != 13 || !ean.startsWith("977") || !CheckDigits.isValidGs1(ean)) return null
            val body = ean.substring(3, 10)
            return Issn(body + CheckDigits.issn(body))
        }

        /** "0317-8471" or "03178471"; null if the check digit does not match. */
        fun parse(text: String): Issn? {
            val code = text.trim().removePrefix("ISSN").trim().replace("-", "").uppercase()
            if (code.length != 8) return null
            val body = code.substring(0, 7)
            if (!body.all { it in '0'..'9' } || CheckDigits.issn(body) != code[7]) return null
            return Issn(code)
        }
    }
}
