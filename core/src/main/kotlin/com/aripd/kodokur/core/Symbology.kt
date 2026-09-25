package com.aripd.kodokur.core

/**
 * Barkod türleri. Adlar ZXing'in `BarcodeFormat` adlarıyla aynıdır; geçmiş
 * kaydında bu ad saklanır, ekranda [label] gösterilir.
 */
enum class Symbology(val label: String, val isProductCode: Boolean = false) {
    EAN_13("EAN-13", isProductCode = true),
    EAN_8("EAN-8", isProductCode = true),
    UPC_A("UPC-A", isProductCode = true),
    UPC_E("UPC-E", isProductCode = true),
    CODE_128("Code 128"),
    CODE_39("Code 39"),
    CODE_93("Code 93"),
    CODABAR("Codabar"),
    ITF("ITF"),
    RSS_14("GS1 DataBar"),
    RSS_EXPANDED("GS1 DataBar Expanded"),
    QR_CODE("QR"),
    DATA_MATRIX("Data Matrix"),
    PDF_417("PDF417"),
    AZTEC("Aztec"),
    MAXICODE("MaxiCode");

    companion object {
        fun fromName(name: String): Symbology? = entries.firstOrNull { it.name == name }
    }
}
