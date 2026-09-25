package com.aripd.kodokur.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.aripd.kodokur.R
import com.aripd.kodokur.core.Content
import com.aripd.kodokur.platform.Actions
import java.util.Locale

/** Sonuç ekranının ve geçmiş satırlarının içerik türüne göre değişen kısmı. */

@StringRes
fun Content.kindLabel(): Int = when (this) {
    is Content.Book -> R.string.kind_book
    is Content.Periodical -> R.string.kind_periodical
    is Content.Product -> R.string.kind_product
    is Content.Link -> R.string.kind_link
    is Content.Wifi -> R.string.kind_wifi
    is Content.Email -> R.string.kind_email
    is Content.Phone -> R.string.kind_phone
    is Content.Sms -> R.string.kind_sms
    is Content.Geo -> R.string.kind_geo
    is Content.Contact -> R.string.kind_contact
    is Content.Text -> R.string.kind_text
}

fun Content.kindIcon(): ImageVector = when (this) {
    is Content.Book, is Content.Periodical -> KodokurIcons.Book
    is Content.Product -> Icons.Filled.ShoppingCart
    is Content.Link -> KodokurIcons.OpenInNew
    is Content.Wifi -> KodokurIcons.Wifi
    is Content.Email -> Icons.Filled.Email
    is Content.Phone -> Icons.Filled.Phone
    is Content.Sms -> @Suppress("DEPRECATION") Icons.Filled.Send
    is Content.Geo -> Icons.Filled.Place
    is Content.Contact -> Icons.Filled.Person
    is Content.Text -> KodokurIcons.Text
}

/** Büyük puntoyla gösterilen tek satır. */
fun Content.headline(): String = when (this) {
    is Content.Book -> isbn.hyphenated13
    is Content.Periodical -> "ISSN ${issn.formatted}"
    is Content.Product -> gtin
    is Content.Link -> url
    is Content.Wifi -> ssid
    is Content.Email -> address
    is Content.Phone -> number
    is Content.Sms -> number
    is Content.Geo -> coordinates()
    is Content.Contact -> name ?: organization ?: phones.firstOrNull() ?: emails.firstOrNull().orEmpty()
    is Content.Text -> text
}

/** Numara gibi okunan başlıklar eşit aralıklı yazıyla gösterilir. */
val Content.isCode: Boolean
    get() = this is Content.Book || this is Content.Periodical || this is Content.Product

private fun Content.Geo.coordinates() =
    String.format(Locale.ROOT, "%.6f, %.6f", latitude, longitude)

/** [copyable] değilse (biçim, zaman gibi üst bilgi) satırda kopyalama düğmesi çıkmaz. */
class Detail(
    @StringRes val label: Int,
    val value: String,
    val sensitive: Boolean = false,
    val copyable: Boolean = true,
)

fun Content.details(): List<Detail> = buildList {
    when (val c = this@details) {
        is Content.Book -> {
            add(Detail(R.string.label_isbn13, c.isbn.hyphenated13))
            c.isbn.hyphenated10?.let { add(Detail(R.string.label_isbn10, it)) }
            c.isbn.agency?.let { add(Detail(R.string.label_group, it)) }
            c.price?.let { add(Detail(R.string.label_price, it)) }
        }
        is Content.Periodical -> {
            add(Detail(R.string.label_issn, c.issn.formatted))
            c.issue?.let { add(Detail(R.string.label_issue, it)) }
        }
        is Content.Product -> add(Detail(R.string.label_gtin, c.gtin))
        is Content.Link -> Unit
        is Content.Wifi -> {
            add(Detail(R.string.label_network, c.ssid))
            c.password?.let { add(Detail(R.string.label_password, it, sensitive = true)) }
            c.security?.let { add(Detail(R.string.label_security, it)) }
        }
        is Content.Email -> {
            add(Detail(R.string.label_to, c.address))
            c.subject?.let { add(Detail(R.string.label_subject, it)) }
            c.body?.let { add(Detail(R.string.label_message, it)) }
        }
        is Content.Phone -> Unit
        is Content.Sms -> c.body?.let { add(Detail(R.string.label_message, it)) }
        is Content.Geo -> add(Detail(R.string.label_coordinates, c.coordinates()))
        is Content.Contact -> {
            c.name?.let { add(Detail(R.string.label_name, it)) }
            c.organization?.let { add(Detail(R.string.label_organization, it)) }
            c.phones.forEach { add(Detail(R.string.label_phone, it)) }
            c.emails.forEach { add(Detail(R.string.label_email, it)) }
            c.url?.let { add(Detail(R.string.label_website, it)) }
        }
        is Content.Text -> Unit
    }
}

/**
 * Bir eylem düğmesi. [external] ise tarayıcıya ya da başka bir uygulamaya çıkar;
 * ekranda "Kodokur internete çıkmaz" notu bunlar için gösterilir.
 */
class ResultAction(
    @StringRes val label: Int,
    val icon: ImageVector,
    val external: Boolean = false,
    val run: (Context) -> Unit,
)

fun Content.actions(raw: String): List<ResultAction> = when (val c = this) {
    is Content.Book -> listOf(
        ResultAction(R.string.act_open_library, KodokurIcons.OpenInNew, external = true) {
            Actions.browse(it, "https://openlibrary.org/isbn/${c.isbn.isbn13}")
        },
        ResultAction(R.string.act_google_books, KodokurIcons.OpenInNew, external = true) {
            Actions.browse(it, "https://books.google.com/books?vid=ISBN${c.isbn.isbn13}")
        },
        ResultAction(R.string.act_search_web, Icons.Filled.Search, external = true) {
            Actions.webSearch(it, "ISBN ${c.isbn.isbn13}")
        },
    )
    is Content.Periodical -> listOf(
        ResultAction(R.string.act_issn_portal, KodokurIcons.OpenInNew, external = true) {
            Actions.browse(it, "https://portal.issn.org/resource/ISSN/${c.issn.formatted}")
        },
        ResultAction(R.string.act_search_web, Icons.Filled.Search, external = true) {
            Actions.webSearch(it, "ISSN ${c.issn.formatted}")
        },
    )
    is Content.Product -> listOf(
        ResultAction(R.string.act_open_food_facts, KodokurIcons.OpenInNew, external = true) {
            Actions.browse(it, "https://world.openfoodfacts.org/product/${c.gtin}")
        },
        ResultAction(R.string.act_search_web, Icons.Filled.Search, external = true) {
            Actions.webSearch(it, c.gtin)
        },
    )
    is Content.Link -> listOf(
        ResultAction(R.string.act_open, KodokurIcons.OpenInNew, external = true) { Actions.browse(it, c.url) },
    )
    is Content.Wifi -> buildList {
        if (Actions.canConnect(c)) {
            add(ResultAction(R.string.act_connect_wifi, KodokurIcons.Wifi) { Actions.connect(it, c) })
        }
        c.password?.let { password ->
            add(ResultAction(R.string.act_copy_password, KodokurIcons.Copy) { Actions.copy(it, password, sensitive = true) })
        }
    }
    is Content.Email -> listOf(
        ResultAction(R.string.act_send_email, Icons.Filled.Email) { Actions.email(it, c.address, c.subject, c.body) },
    )
    is Content.Phone -> listOf(
        ResultAction(R.string.act_dial, Icons.Filled.Phone) { Actions.dial(it, c.number) },
    )
    is Content.Sms -> listOf(
        ResultAction(R.string.act_send_sms, @Suppress("DEPRECATION") Icons.Filled.Send) { Actions.sms(it, c.number, c.body) },
    )
    is Content.Geo -> listOf(
        ResultAction(R.string.act_show_map, Icons.Filled.Place, external = true) { Actions.map(it, c.latitude, c.longitude) },
    )
    is Content.Contact -> listOf(
        ResultAction(R.string.act_add_contact, Icons.Filled.Person) { Actions.addContact(it, c) },
    )
    is Content.Text -> listOf(
        ResultAction(R.string.act_search_web, Icons.Filled.Search, external = true) { Actions.webSearch(it, c.text) },
    )
} + listOf(
    ResultAction(R.string.act_copy, KodokurIcons.Copy) { Actions.copy(it, copyText(raw)) },
    ResultAction(R.string.act_share, Icons.Filled.Share) { Actions.shareText(it, copyText(raw)) },
)

/**
 * Kopyalanan metin: kitapta tiresiz ISBN-13 (katalog ve kitapçı aramalarına
 * olduğu gibi yapışır), dergide ISSN; öbürlerinde ham içerik.
 */
private fun Content.copyText(raw: String): String = when (this) {
    is Content.Book -> isbn.isbn13
    is Content.Periodical -> issn.formatted
    else -> raw
}
