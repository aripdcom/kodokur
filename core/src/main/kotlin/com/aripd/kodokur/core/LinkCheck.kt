package com.aripd.kodokur.core

import java.net.IDN
import java.util.Locale

/** Bir bağlantıyı açmadan önce kullanıcıya söylenmesi gerekenler. */
enum class LinkWarning {
    /** http: bağlantı şifresiz; araya giren sayfayı değiştirebilir. */
    NOT_ENCRYPTED,

    /**
     * Alan adında Latin dışı harf ya da punycode (xn--) var. "аpple.com"daki ilk
     * harf Kiril olabilir: göze aynı görünen başka bir site.
     */
    LOOKALIKE_HOST,

    /** Alan adı yerine çıplak IP adresi; tanınan bir sitenin adresi değildir. */
    IP_ADDRESS,

    /**
     * Adreste "@" var: "https://banka.com@kotu.site" tarayıcıyı banka.com'a değil
     * kotu.site'a götürür, öndeki kısım yalnızca kullanıcı adıdır.
     */
    HIDDEN_DESTINATION,
}

/**
 * Bağlantının gerçekte gittiği yer. [host] tarayıcının gideceği alan adı (küçük
 * harf, Unicode); [asciiHost] punycode karşılığı, farklıysa gösterilir.
 */
data class LinkInfo(
    val url: String,
    val secure: Boolean,
    val host: String,
    val asciiHost: String,
    val warnings: Set<LinkWarning>,
)

object LinkCheck {

    private val URL = Regex("^(https?)://([^/?#]*)(.*)$", RegexOption.IGNORE_CASE)
    private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    /** http(s) bağlantısını inceler; biçim tanınmazsa null. */
    fun inspect(url: String): LinkInfo? {
        val m = URL.find(url.trim()) ?: return null
        val scheme = m.groupValues[1].lowercase(Locale.ROOT)
        val authority = m.groupValues[2]
        val warnings = mutableSetOf<LinkWarning>()

        // userinfo@host:port — tarayıcı son "@"tan sonrasına gider.
        val at = authority.lastIndexOf('@')
        if (at >= 0) warnings += LinkWarning.HIDDEN_DESTINATION
        var hostPort = authority.substring(at + 1)

        val host = if (hostPort.startsWith("[")) {
            // IPv6: [::1]:8080
            warnings += LinkWarning.IP_ADDRESS
            hostPort.substringBefore(']').removePrefix("[")
        } else {
            hostPort = hostPort.substringBefore(':')
            hostPort.trimEnd('.').lowercase(Locale.ROOT)
        }
        if (host.isEmpty()) return null

        val ascii = try {
            IDN.toASCII(host, IDN.ALLOW_UNASSIGNED)
        } catch (_: IllegalArgumentException) {
            host
        }.lowercase(Locale.ROOT)
        val unicode = try {
            IDN.toUnicode(ascii, IDN.ALLOW_UNASSIGNED)
        } catch (_: IllegalArgumentException) {
            host
        }

        if (IPV4.matches(host)) warnings += LinkWarning.IP_ADDRESS
        if (host.any { it.code > 0x7F } || ascii.split('.').any { it.startsWith("xn--") }) {
            warnings += LinkWarning.LOOKALIKE_HOST
        }
        if (scheme == "http") warnings += LinkWarning.NOT_ENCRYPTED

        return LinkInfo(
            url = url.trim(),
            secure = scheme == "https",
            host = unicode,
            asciiHost = ascii,
            warnings = warnings,
        )
    }
}
