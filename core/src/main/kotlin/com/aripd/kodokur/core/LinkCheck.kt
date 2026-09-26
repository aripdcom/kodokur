package com.aripd.kodokur.core

import java.net.IDN
import java.util.Locale

/** What the user should be told before opening a link. */
enum class LinkWarning {
    /** http: the connection is unencrypted; anyone in between can alter the page. */
    NOT_ENCRYPTED,

    /**
     * The domain has non-Latin letters or punycode (xn--). The first letter of
     * "аpple.com" may be Cyrillic: a different site that looks the same.
     */
    LOOKALIKE_HOST,

    /** A bare IP address instead of a domain; not the address of a recognizable site. */
    IP_ADDRESS,

    /**
     * The address contains "@": "https://bank.com@evil.site" takes the browser to
     * evil.site, not bank.com; the part in front is only a user name.
     */
    HIDDEN_DESTINATION,
}

/**
 * Where the link actually goes. [host] is the domain the browser will visit
 * (lowercase, Unicode); [asciiHost] is its punycode form, shown if different.
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

    /** Inspects an http(s) link; null if the format is not recognized. */
    fun inspect(url: String): LinkInfo? {
        val m = URL.find(url.trim()) ?: return null
        val scheme = m.groupValues[1].lowercase(Locale.ROOT)
        val authority = m.groupValues[2]
        val warnings = mutableSetOf<LinkWarning>()

        // userinfo@host:port — the browser goes to what follows the last "@".
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
