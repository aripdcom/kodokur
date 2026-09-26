package com.aripd.kodokur.core

/** Okunan metnin ne olduğu; sonuç ekranı hangi eylemleri sunacağına buna bakar. */
sealed interface Content {

    /** 978/979 önekli EAN-13 ya da metin olarak yazılmış geçerli bir ISBN. */
    data class Book(val isbn: Isbn, val price: String?) : Content

    /** 977 önekli EAN-13: dergi, gazete. [issue] 2 haneli ekten sayı numarası. */
    data class Periodical(val issn: Issn, val issue: String?) : Content

    /** GS1 verisi: ilaç karekodu, GS1-128, GS1 DataBar (GTIN + SKT, parti, seri…). */
    data class Gs1(val data: Gs1Data) : Content

    /** Öbür EAN/UPC kodları: market ürünleri. */
    data class Product(val gtin: String) : Content

    data class Link(val url: String) : Content

    data class Wifi(
        val ssid: String,
        val password: String?,
        /** "WPA", "WEP", "SAE", "nopass"… QR'da yazdığı gibi. */
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
        /** Unvan (vCard TITLE). */
        val title: String? = null,
        /** Posta adresi, bileşenleri virgülle birleşmiş. */
        val address: String? = null,
        val note: String? = null,
    ) : Content

    data class Text(val text: String) : Content
}
