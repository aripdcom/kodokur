package com.aripd.kodokur.core

/**
 * Bir okuma. [addOn], EAN/UPC barkodunun sağındaki 2 ya da 5 haneli ek barkoddur:
 * kitaplarda 5 hane önerilen fiyatı, dergilerde 2 hane sayı numarasını taşır.
 */
data class Scan(
    val text: String,
    val symbology: Symbology,
    val addOn: String? = null,
) {
    /** 5 haneli ekten fiyat ("$24.95", "£7.99"); fiyat yoksa ya da ek başka türse null. */
    val suggestedPrice: String?
        get() {
            val code = addOn?.takeIf { it.length == 5 && it.all(Char::isDigit) } ?: return null
            // ZXing'in UPCEANExtension5Support'u ile aynı kurallar.
            return when (code) {
                "90000" -> null
                "99991" -> "0.00"
                "99990" -> null
                else -> {
                    val currency = when (code[0]) {
                        '0' -> "£"
                        '5' -> "$"
                        else -> return null
                    }
                    val cents = code.substring(1).toInt()
                    "$currency${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
                }
            }
        }
}
