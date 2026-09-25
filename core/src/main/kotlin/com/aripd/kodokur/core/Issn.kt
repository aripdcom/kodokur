package com.aripd.kodokur.core

/**
 * Süreli yayın numarası. Dergi ve gazetelerin barkodu 977 önekli bir EAN-13'tür:
 * 977 + ISSN'in ilk yedi hanesi + iki haneli yayın varyantı + GS1 sağlaması.
 * ISSN'in kendi sağlama hanesi barkodda yoktur, yeniden hesaplanır.
 */
class Issn private constructor(val digits: String) {

    /** "0317-8471" biçimi. */
    val formatted: String get() = "${digits.substring(0, 4)}-${digits.substring(4)}"

    override fun equals(other: Any?): Boolean = other is Issn && other.digits == digits
    override fun hashCode(): Int = digits.hashCode()
    override fun toString(): String = formatted

    companion object {
        /** 977 önekli, sağlaması tutan bir EAN-13'ten ISSN; değilse null. */
        fun fromEan13(ean: String): Issn? {
            if (ean.length != 13 || !ean.startsWith("977") || !CheckDigits.isValidGs1(ean)) return null
            val body = ean.substring(3, 10)
            return Issn(body + CheckDigits.issn(body))
        }

        /** "0317-8471" ya da "03178471"; sağlaması tutmazsa null. */
        fun parse(text: String): Issn? {
            val code = text.trim().removePrefix("ISSN").trim().replace("-", "").uppercase()
            if (code.length != 8) return null
            val body = code.substring(0, 7)
            if (!body.all { it in '0'..'9' } || CheckDigits.issn(body) != code[7]) return null
            return Issn(code)
        }
    }
}
