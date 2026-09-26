package com.aripd.kodokur.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A scan in the history: when, and what was scanned. */
data class Record(val timeMillis: Long, val scan: Scan)

/**
 * Exports the history to CSV (RFC 4180, UTF-8 with BOM: Excel reads Turkish
 * characters correctly only when it sees the BOM). ISBN columns are included for
 * library inventories; they are empty on non-book rows.
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
     * Quotes if needed. Text starting with =, +, -, @ counts as a formula in a
     * spreadsheet; a ' is prepended so text from a QR code cannot run a formula.
     * Numbers (ISBN, "-12") are left as is.
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
