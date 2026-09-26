package com.aripd.kodokur.core

/**
 * A single scan. [addOn] is the 2- or 5-digit add-on barcode to the right of an
 * EAN/UPC barcode: on books the 5 digits carry the suggested price, on magazines the
 * 2 digits carry the issue number. [gs1]: the scanner flagged the code as GS1
 * (medicine DataMatrix, GS1-128, DataBar); the fields in the text are parsed with [Gs1].
 */
data class Scan(
    val text: String,
    val symbology: Symbology,
    val addOn: String? = null,
    val gs1: Boolean = false,
) {
    /** Price from the 5-digit add-on ("$24.95", "£7.99"); null if there is no price or the add-on is of another kind. */
    val suggestedPrice: String?
        get() {
            val code = addOn?.takeIf { it.length == 5 && it.all(Char::isDigit) } ?: return null
            // Same rules as ZXing's UPCEANExtension5Support.
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
