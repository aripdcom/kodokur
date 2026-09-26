package com.aripd.kodokur.platform

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.PersistableBundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.aripd.kodokur.R
import com.aripd.kodokur.core.Content
import java.io.File
import java.time.LocalDate

/**
 * Dışarıya devredilen her şey. Uygulamanın INTERNET izni yok: bağlantı, arama,
 * harita, arama/SMS/e-posta ilgili uygulamaya niyetle (intent) gider; hiçbiri
 * izin gerektirmez (ACTION_DIAL aramayı başlatmaz, yalnızca numarayı çevirir).
 */
object Actions {

    const val SOURCE_URL = "https://github.com/aripdcom/kodokur"
    const val PRIVACY_URL = "https://kodokur.aripd.com/privacy.html"

    fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
        false
    }

    fun browse(context: Context, url: String) = start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /**
     * Panoya kopyalar. [sensitive] (parola) Android 13+ panosu önizlemesinde
     * gizlenir. Android 13+ kopyalamayı kendisi gösterdiği için bildirim yalnızca
     * öncesinde çıkar.
     */
    fun copy(context: Context, text: String, sensitive: Boolean = false) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(context.getString(R.string.app_name), text)
        if (sensitive) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(send, null))
    }

    fun webSearch(context: Context, query: String) {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        try {
            context.startActivity(search)
        } catch (_: ActivityNotFoundException) {
            browse(context, "https://duckduckgo.com/?q=" + Uri.encode(query))
        }
    }

    fun dial(context: Context, number: String) =
        start(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))

    fun email(context: Context, address: String, subject: String?, body: String?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            body?.let { putExtra(Intent.EXTRA_TEXT, it) }
        }
        start(context, intent)
    }

    fun sms(context: Context, number: String, body: String?) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
        body?.let { intent.putExtra("sms_body", it) }
        start(context, intent)
    }

    fun map(context: Context, latitude: Double, longitude: Double) =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")))

    /** Kişi ekleme formunu doldurup açar; kaydı kullanıcı yapar, izin gerekmez. */
    fun addContact(context: Context, contact: Content.Contact) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            contact.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            contact.organization?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            contact.title?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
            contact.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
            contact.note?.let { putExtra(ContactsContract.Intents.Insert.NOTES, it) }
            val phoneKeys = listOf(
                ContactsContract.Intents.Insert.PHONE,
                ContactsContract.Intents.Insert.SECONDARY_PHONE,
                ContactsContract.Intents.Insert.TERTIARY_PHONE,
            )
            contact.phones.zip(phoneKeys).forEach { (value, key) -> putExtra(key, value) }
            val emailKeys = listOf(
                ContactsContract.Intents.Insert.EMAIL,
                ContactsContract.Intents.Insert.SECONDARY_EMAIL,
                ContactsContract.Intents.Insert.TERTIARY_EMAIL,
            )
            contact.emails.zip(emailKeys).forEach { (value, key) -> putExtra(key, value) }
        }
        start(context, intent)
    }

    /**
     * Android 11+ "ağ ekle" penceresi: kullanıcı onaylar, ağ kaydedilir. İzin
     * gerekmez. WEP ve kurumsal (EAP) ağlar öneri olarak eklenemez; onlar için
     * parolayı kopyalama kalır.
     */
    fun canConnect(wifi: Content.Wifi): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && suggestion(wifi) != null

    fun connect(context: Context, wifi: Content.Wifi) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val suggestion = suggestion(wifi) ?: return
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            .putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
        start(context, intent)
    }

    private fun suggestion(wifi: Content.Wifi): WifiNetworkSuggestion? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val builder = WifiNetworkSuggestion.Builder().setSsid(wifi.ssid)
            val password = wifi.password
            when (wifi.security?.uppercase()) {
                "WPA", "WPA2" -> builder.setWpa2Passphrase(password ?: return null)
                "SAE", "WPA3" -> builder.setWpa3Passphrase(password ?: return null)
                // T alanı yoksa: parola varsa pratikte WPA2, yoksa açık ağ.
                null, "" -> if (password != null) builder.setWpa2Passphrase(password)
                "NOPASS" -> Unit
                else -> return null
            }
            if (wifi.hidden) builder.setIsHiddenSsid(true)
            builder.build()
        } catch (_: IllegalArgumentException) {
            // Kurallara uymayan parola (WPA2 için 8–63 karakter) ya da SSID.
            null
        }
    }

    /**
     * Geçmişi CSV dosyası olarak paylaşır. Dosya önbellekteki paylaşım klasörüne
     * yazılır (eskiler silinir); alıcıya yalnızca bu dosya için okuma yetkisi verilir.
     */
    fun shareCsv(context: Context, csv: String) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "kodokur-${LocalDate.now()}.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(context, Intent.createChooser(send, null))
    }
}
