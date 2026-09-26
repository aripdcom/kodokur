package com.aripd.kodokur.core

/** What the scanned text is; the result screen decides which actions to offer based on this. */
sealed interface Content {

    /** An EAN-13 with a 978/979 prefix, or a valid ISBN written as text. */
    data class Book(val isbn: Isbn, val price: String?) : Content

    /** An EAN-13 with a 977 prefix: magazine, newspaper. [issue] is the issue number from the 2-digit add-on. */
    data class Periodical(val issn: Issn, val issue: String?) : Content

    /** GS1 data: medicine DataMatrix, GS1-128, GS1 DataBar (GTIN + expiry, batch, serial…). */
    data class Gs1(val data: Gs1Data) : Content

    /** Other EAN/UPC codes: retail products. */
    data class Product(val gtin: String) : Content

    data class Link(val url: String) : Content

    data class Wifi(
        val ssid: String,
        val password: String?,
        /** "WPA", "WEP", "SAE", "nopass"… as written in the QR code. */
        val security: String?,
        val hidden: Boolean,
    ) : Content

    data class Email(val address: String, val subject: String?, val body: String?) : Content

    data class Phone(val number: String) : Content

    data class Sms(val number: String, val body: String?) : Content

    data class Geo(val latitude: Double, val longitude: Double) : Content

    data class Contact(
        val name: String?,
        val organization: String?,
        val phones: List<String>,
        val emails: List<String>,
        val url: String?,
        /** Job title (vCard TITLE). */
        val title: String? = null,
        /** Postal address, components joined with commas. */
        val address: String? = null,
        val note: String? = null,
    ) : Content

    data class Text(val text: String) : Content
}
