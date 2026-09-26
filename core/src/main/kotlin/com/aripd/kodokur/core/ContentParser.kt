package com.aripd.kodokur.core

/**
 * Okunan metni [Content]'e çevirir. Yaygın QR biçimlerini tanır (bağlantı, Wi-Fi,
 * e-posta, telefon, SMS, konum, kişi kartı); tanımadığı her şey düz metindir.
 */
object ContentParser {

    private val URL = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE)
    private val ISBN_TEXT = Regex("^(ISBN(-1[03])?:?\\s*)?[0-9][0-9\\- ]{8,16}[0-9Xx]$", RegexOption.IGNORE_CASE)
    private val GEO = Regex("^geo:(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)

    fun parse(scan: Scan): Content {
        val text = scan.text.trim()

        if (scan.symbology.isProductCode) {
            if (scan.symbology == Symbology.EAN_13) {
                Isbn.parse(text)?.let { return Content.Book(it, scan.suggestedPrice) }
                Issn.fromEan13(text)?.let { issn ->
                    return Content.Periodical(issn, scan.addOn?.takeIf { it.length == 2 })
                }
            }
            return Content.Product(text)
        }

        // İlaç karekodu ve öbür GS1 kodları. İşaretsiz metin yalnızca açık GS1
        // yazımındaysa (parantezli ya da GS ayraçlı) kabul edilir, bkz. Gs1.parse.
        Gs1.parse(text, scan.gs1)?.let { data ->
            // Tek alan olarak yalnız GTIN taşıyan DataBar, market ürünüdür.
            return if (data.elements.size == 1 && data.gtin != null) Content.Product(data.gtin!!) else Content.Gs1(data)
        }

        // QR'a ya da Code 128'e metin olarak yazılmış ISBN.
        if (ISBN_TEXT.matches(text)) Isbn.parse(text)?.let { return Content.Book(it, null) }

        return parseText(text)
    }

    private fun parseText(text: String): Content {
        val upper = text.uppercase()
        return when {
            URL.matches(text) ->
                Content.Link(if (upper.startsWith("WWW.")) "https://$text" else text)
            upper.startsWith("URLTO:") ->
                Content.Link(text.substring(6)).takeIf { URL.matches(it.url) } ?: Content.Text(text)
            upper.startsWith("WIFI:") -> parseWifi(text) ?: Content.Text(text)
            upper.startsWith("MAILTO:") -> parseMailto(text)
            upper.startsWith("MATMSG:") -> parseMatmsg(text)
            upper.startsWith("TEL:") -> Content.Phone(text.substring(4).trim())
            upper.startsWith("SMSTO:") || upper.startsWith("SMS:") -> parseSms(text)
            upper.startsWith("GEO:") -> parseGeo(text) ?: Content.Text(text)
            upper.startsWith("MECARD:") -> parseMecard(text)
            upper.startsWith("BEGIN:VCARD") -> parseVcard(text)
            else -> Content.Text(text)
        }
    }

    // --- Noktalı virgülle ayrılan KEY:değer biçimleri (WIFI, MATMSG, MECARD) ---

    /**
     * "WIFI:T:WPA;S:ağ;P:parola;;" gövdesini alanlara böler. Ters bölü kaçışı
     * (\; \, \: \\ \") çözülür. Aynı anahtar birden çok kez gelebilir (MECARD'da TEL).
     */
    private fun fields(body: String): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>()
        val key = StringBuilder()
        val value = StringBuilder()
        var inValue = false
        var escaped = false
        fun flush() {
            if (inValue && key.isNotEmpty()) result += key.toString().uppercase() to value.toString()
            key.clear()
            value.clear()
            inValue = false
        }
        for (c in body) {
            val target = if (inValue) value else key
            when {
                escaped -> { target.append(c); escaped = false }
                c == '\\' -> escaped = true
                c == ';' -> flush()
                c == ':' && !inValue -> inValue = true
                else -> target.append(c)
            }
        }
        flush()
        return result
    }

    private fun List<Pair<String, String>>.first(key: String): String? =
        firstOrNull { it.first == key }?.second?.takeIf { it.isNotEmpty() }

    private fun parseWifi(text: String): Content.Wifi? {
        val f = fields(text.substring(5))
        val ssid = f.first("S") ?: return null
        return Content.Wifi(
            ssid = ssid,
            password = f.first("P"),
            security = f.first("T"),
            hidden = f.first("H").equals("true", ignoreCase = true),
        )
    }

    private fun parseMatmsg(text: String): Content {
        val f = fields(text.substring(7))
        val to = f.first("TO") ?: return Content.Text(text)
        return Content.Email(to, f.first("SUB"), f.first("BODY"))
    }

    private fun parseMecard(text: String): Content {
        val f = fields(text.substring(7))
        val name = f.first("N")?.let { n ->
            // "Soyad,Ad" → "Ad Soyad"
            val parts = n.split(',').map(String::trim).filter(String::isNotEmpty)
            if (parts.size == 2) "${parts[1]} ${parts[0]}" else parts.joinToString(" ")
        }
        return Content.Contact(
            name = name,
            organization = f.first("ORG"),
            phones = f.filter { it.first == "TEL" && it.second.isNotEmpty() }.map { it.second },
            emails = f.filter { it.first == "EMAIL" && it.second.isNotEmpty() }.map { it.second },
            url = f.first("URL"),
            // MECARD adresi tek alan; bileşenler virgülle ayrılır.
            address = f.first("ADR")?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
                ?.joinToString(", ")?.takeIf { it.isNotEmpty() },
            note = f.first("NOTE"),
        )
    }

    // --- URI biçimleri ---

    private fun parseMailto(text: String): Content {
        val rest = text.substring(7)
        val address = decodePercent(rest.substringBefore('?')).trim()
        val query = rest.substringAfter('?', "").split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) null else pair.substring(0, eq).lowercase() to decodePercent(pair.substring(eq + 1))
        }.toMap()
        if (address.isEmpty() && query["to"].isNullOrEmpty()) return Content.Text(text)
        return Content.Email(address.ifEmpty { query["to"]!! }, query["subject"], query["body"])
    }

    private fun parseSms(text: String): Content {
        val rest = text.substringAfter(':')
        val number = rest.substringBefore(':').substringBefore('?').trim()
        if (number.isEmpty()) return Content.Text(text)
        val body = when {
            rest.contains(':') -> rest.substringAfter(':')
            rest.contains("?body=", ignoreCase = true) -> decodePercent(rest.substringAfter('=', ""))
            else -> null
        }?.takeIf { it.isNotEmpty() }
        return Content.Sms(number, body)
    }

    private fun parseGeo(text: String): Content.Geo? {
        val m = GEO.find(text) ?: return null
        val lat = m.groupValues[1].toDouble()
        val lon = m.groupValues[2].toDouble()
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return Content.Geo(lat, lon)
    }

    /**
     * vCard 2.1–4.0. Katlanmış satırlar açılır; quoted-printable değerler (eski
     * telefonların ve kartvizit üreticilerinin Türkçe karakterleri =C3=BC diye
     * yazdığı biçim) CHARSET'e göre çözülür. Yapılı alanlar (N, ORG, ADR) kaçışsız
     * ";" işaretlerinden bölünür, kaçışlar ondan sonra açılır.
     */
    private fun parseVcard(text: String): Content {
        var fn: String? = null
        var n: String? = null
        var org: String? = null
        var url: String? = null
        var title: String? = null
        var address: String? = null
        var note: String? = null
        val phones = ArrayList<String>()
        val emails = ArrayList<String>()
        for (line in vcardLines(text)) {
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val params = line.substring(0, colon).split(';')
            val key = params.first().substringAfterLast('.').uppercase() // "item1.TEL" → TEL
            var raw = line.substring(colon + 1).trim()
            if (params.any { it.uppercase().let { p -> p == "QUOTED-PRINTABLE" || p == "ENCODING=QUOTED-PRINTABLE" } }) {
                val charset = params.firstOrNull { it.uppercase().startsWith("CHARSET=") }?.substringAfter('=')
                raw = decodeQuotedPrintable(raw, charset)
            }
            if (raw.isEmpty()) continue
            val parts = splitUnescaped(raw).map(::unescapeVcard)
            val value = unescapeVcard(raw)
            when (key) {
                "FN" -> fn = value
                "N" -> n = listOfNotNull(parts.getOrNull(1), parts.getOrNull(0))
                    .filter(String::isNotBlank).joinToString(" ")
                "ORG" -> org = parts.filter(String::isNotBlank).joinToString(", ")
                "TITLE" -> title = value
                "TEL" -> phones += value.removePrefix("tel:")
                "EMAIL" -> emails += value
                "URL" -> url = url ?: value
                "ADR" -> address = address ?: parts.filter(String::isNotBlank).joinToString(", ")
                "NOTE" -> note = value
            }
        }
        return Content.Contact(
            name = fn?.takeIf { it.isNotBlank() } ?: n?.takeIf { it.isNotBlank() },
            organization = org?.takeIf { it.isNotBlank() },
            phones = phones,
            emails = emails,
            url = url,
            title = title,
            address = address?.takeIf { it.isNotBlank() },
            note = note,
        )
    }

    /** Satırları açar: boşlukla başlayan devam satırı ve quoted-printable'ın "=" ile biten yumuşak sonu. */
    private fun vcardLines(text: String): List<String> {
        val physical = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = ArrayList<String>()
        for (line in physical) {
            val last = out.lastOrNull()
            when {
                last != null && (line.startsWith(" ") || line.startsWith("\t")) ->
                    out[out.size - 1] = last + line.substring(1)
                last != null && last.endsWith("=") && isQuotedPrintable(last) ->
                    out[out.size - 1] = last.dropLast(1) + line
                else -> out += line
            }
        }
        return out
    }

    private fun isQuotedPrintable(line: String): Boolean =
        line.substringBefore(':').uppercase().contains("QUOTED-PRINTABLE")

    internal fun decodeQuotedPrintable(value: String, charset: String?): String {
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '=' && i + 2 < value.length) {
                val b = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (b != null) {
                    bytes.write(b)
                    i += 3
                    continue
                }
            }
            bytes.write(c.toString().toByteArray(Charsets.UTF_8))
            i++
        }
        val cs = try {
            charset?.let { java.nio.charset.Charset.forName(it) } ?: Charsets.UTF_8
        } catch (_: Exception) {
            Charsets.UTF_8
        }
        return String(bytes.toByteArray(), cs)
    }

    /** Kaçışsız ";" işaretlerinden böler (kaçışlar korunur). */
    private fun splitUnescaped(value: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var escaped = false
        for (c in value) {
            when {
                escaped -> { cur.append('\\').append(c); escaped = false }
                c == '\\' -> escaped = true
                c == ';' -> { out += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
        }
        if (escaped) cur.append('\\')
        out += cur.toString()
        return out
    }

    private fun unescapeVcard(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                val next = value[i + 1]
                out.append(if (next == 'n' || next == 'N') '\n' else next)
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString().trim()
    }

    private fun decodePercent(s: String): String = try {
        java.net.URLDecoder.decode(s.replace("+", "%2B"), Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        s
    }
}
