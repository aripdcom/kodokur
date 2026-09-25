package com.aripd.kodokur.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Geçmişteki bir okuma: ne zaman, ne okundu. */
data class Record(val timeMillis: Long, val scan: Scan)

/**
 * Geçmişi CSV'ye döker (RFC 4180, UTF-8, BOM'lu: Excel Türkçe karakterleri
 * ancak BOM görünce doğru okur). Kitaplık envanteri için ISBN sütunları hazır
 * gelir; kitap olmayan satırlarda boştur.
 */
object HistoryCsv {

    private val TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun export(records: List<Record>, zone: ZoneId = ZoneId.systemDefault()): String {
        val sb = StringBuilder("﻿")
        row(sb, listOf("time", "format", "content", "add_on", "isbn13", "isbn10"))
        for (r in records) {
            val isbn = (ContentParser.parse(r.scan) as? Content.Book)?.isbn
            row(
                sb,
                listOf(
                    TIME.format(Instant.ofEpochMilli(r.timeMillis).atZone(zone)),
                    r.scan.symbology.label,
                    r.scan.text,
                    r.scan.addOn.orEmpty(),
                    isbn?.isbn13.orEmpty(),
                    isbn?.isbn10.orEmpty(),
                ),
            )
        }
        return sb.toString()
    }

    private fun row(sb: StringBuilder, cells: List<String>) {
        cells.joinTo(sb, ",") { field(it) }
        sb.append("\r\n")
    }

    /**
     * Gerekirse tırnaklar. =, +, -, @ ile başlayan metin tablo programında formül
     * sayılır; QR'dan gelen bir metin formül çalıştırmasın diye başına ' eklenir.
     * Sayılar (ISBN, "-12") olduğu gibi kalır.
     */
    internal fun field(value: String): String {
        var v = value
        if (v.isNotEmpty() && v[0] in "=+-@\t\r" && v.toDoubleOrNull() == null) v = "'$v"
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }
}
