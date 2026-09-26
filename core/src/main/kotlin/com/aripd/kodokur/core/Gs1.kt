package com.aripd.kodokur.core

import java.time.LocalDate
import java.time.YearMonth

/**
 * GS1 verisi: ilaç karekodu (Türkiye'de İlaç Takip Sistemi, GS1 DataMatrix),
 * GS1-128 lojistik barkodları, GS1 DataBar. İçerik "uygulama tanımlayıcısı" (AI)
 * ile başlayan alanlardan oluşur: (01) GTIN, (17) son kullanma, (10) parti,
 * (21) seri numarası…
 */
class Gs1Data(val elements: List<Gs1Element>) {

    private fun value(ai: String) = elements.firstOrNull { it.ai == ai }?.value

    /** (01) Ticari ürün numarası, 14 hane. */
    val gtin: String? get() = value("01")

    /** GTIN'in barkod üzerindeki karşılığı: baştaki 0 atılınca EAN-13. */
    val ean13: String? get() = gtin?.takeIf { it.startsWith("0") }?.substring(1)

    val expiry: Gs1Date? get() = value("17")?.let(Gs1Date::parse)
    val bestBefore: Gs1Date? get() = value("15")?.let(Gs1Date::parse)
    val production: Gs1Date? get() = value("11")?.let(Gs1Date::parse)
    val batch: String? get() = value("10")
    val serial: String? get() = value("21")
    val count: String? get() = value("30") ?: value("37")

    /** Yukarıdakilerin dışında kalan alanlar; ham hâlleriyle gösterilir. */
    val others: List<Gs1Element>
        get() = elements.filter { it.ai !in KNOWN }

    override fun equals(other: Any?) = other is Gs1Data && other.elements == elements
    override fun hashCode() = elements.hashCode()
    override fun toString() = elements.joinToString("") { "(${it.ai})${it.value}" }

    private companion object {
        val KNOWN = setOf("01", "17", "15", "11", "10", "21", "30", "37")
    }
}

data class Gs1Element(val ai: String, val value: String)

/**
 * GS1 tarihi (YYAAGG). Gün "00" ise ayın son günü kastedilir; o zaman [dayGiven]
 * false olur ve tarih ay/yıl olarak gösterilir.
 */
data class Gs1Date(val date: LocalDate, val dayGiven: Boolean) {

    /** Bugün bu tarihten sonraysa süresi geçmiştir (tarihin kendisi geçerli gündür). */
    fun isPast(today: LocalDate): Boolean = today.isAfter(date)

    companion object {
        /**
         * GS1'in kayan yüzyıl kuralı: iki haneli yıl, bugünün yılından 51–99 ileri
         * düşüyorsa önceki yüzyıl, 50'den fazla geride kalıyorsa sonraki yüzyıl.
         */
        fun parse(text: String, today: LocalDate = LocalDate.now()): Gs1Date? {
            if (text.length != 6 || !text.all(Char::isDigit)) return null
            val yy = text.substring(0, 2).toInt()
            val mm = text.substring(2, 4).toInt()
            val dd = text.substring(4, 6).toInt()
            if (mm !in 1..12) return null
            val current = today.year % 100
            val century = today.year - current
            val diff = yy - current
            val year = when {
                diff in 51..99 -> century - 100 + yy
                diff in -99..-50 -> century + 100 + yy
                else -> century + yy
            }
            val month = YearMonth.of(year, mm)
            return when {
                dd == 0 -> Gs1Date(month.atEndOfMonth(), dayGiven = false)
                dd <= month.lengthOfMonth() -> Gs1Date(month.atDay(dd), dayGiven = true)
                else -> null
            }
        }
    }
}

/**
 * GS1 element dizisi ayrıştırıcısı. İki yazımı tanır:
 * - ham: "0108690…17261231" + [GS] ayraçlı değişken alanlar (tarayıcının verdiği)
 * - parantezli: "(01)08690…(17)261231(10)ABC" (insanın okuduğu, DataBar Expanded)
 */
object Gs1 {

    /** Grup ayracı (FNC1'in veri içindeki karşılığı). */
    const val GS = '\u001D'

    /** AI → (sabit veri uzunluğu ya da null, en fazla uzunluk). */
    private class Spec(val fixed: Int?, val max: Int)

    private val SPECS: Map<String, Spec> = buildMap {
        fun fixed(ai: String, n: Int) = put(ai, Spec(n, n))
        fun variable(ai: String, max: Int) = put(ai, Spec(null, max))
        fixed("00", 18); fixed("01", 14); fixed("02", 14)
        variable("10", 20)
        for (ai in listOf("11", "12", "13", "15", "16", "17")) fixed(ai, 6)
        fixed("20", 2)
        variable("21", 20); variable("22", 20)
        variable("30", 8); variable("37", 8)
        for (ai in listOf("240", "241", "250", "251", "253", "400", "401", "403")) variable(ai, 30)
        variable("254", 20)
        fixed("402", 17)
        for (d in 0..7) fixed("41$d", 13)
        variable("420", 20); variable("421", 12); fixed("422", 3)
        for (d in 0..6) variable("71$d", 20) // ulusal sağlık geri ödeme numaraları
        fixed("7003", 10); variable("7004", 4)
        // 31nn–36nn: ölçüler (ağırlık, uzunluk…), son hane ondalık konumu
        for (a in 31..36) for (b in 0..9) for (d in 0..9) fixed("$a$b$d", 6)
        variable("8003", 30); variable("8004", 30); fixed("8005", 6); fixed("8006", 18)
        variable("8007", 34); variable("8008", 12); variable("8020", 25)
        variable("90", 30)
        for (d in 1..9) variable("9$d", 90)
    }

    private val PAREN = Regex("""\((\d{2,4})\)([^()]*)""")

    /**
     * [text]'i GS1 olarak ayrıştırır. [flagged]: tarayıcı kodun GS1 olduğunu
     * bildirdi (sembol tanımlayıcı). İşaretsiz metin yalnızca açıkça GS1 biçimindeyse
     * (parantezli ya da GS ayraçlı, 01 ile başlayan, sağlaması tutan GTIN) kabul
     * edilir; rastgele bir sayı dizisi ilaç kodu gibi görünmesin.
     */
    fun parse(text: String, flagged: Boolean): Gs1Data? {
        val raw = text.trim().removePrefix("]d2").removePrefix("]C1").removePrefix("]Q3").removePrefix("]e0")
        val elements = if (raw.startsWith("(")) parseParenthesized(raw) else parseRaw(raw)
        if (elements.isNullOrEmpty()) return null
        val gtin = elements.firstOrNull { it.ai == "01" }?.value
        if (gtin != null && !CheckDigits.isValidGs1(gtin)) return null
        if (!flagged) {
            val explicit = raw.startsWith("(") || GS in raw
            if (!explicit || elements.first().ai != "01" || elements.size < 2) return null
        }
        return Gs1Data(elements)
    }

    private fun parseParenthesized(text: String): List<Gs1Element>? {
        val matches = PAREN.findAll(text).toList()
        if (matches.isEmpty() || matches.sumOf { it.value.length } != text.length) return null
        return matches.map { m ->
            val ai = m.groupValues[1]
            val value = m.groupValues[2]
            val spec = SPECS[ai] ?: return null
            if (spec.fixed != null && value.length != spec.fixed) return null
            if (value.isEmpty() || value.length > spec.max) return null
            Gs1Element(ai, value)
        }
    }

    private fun parseRaw(text: String): List<Gs1Element>? {
        val out = ArrayList<Gs1Element>()
        var i = 0
        while (i < text.length) {
            if (text[i] == GS) { i++; continue }
            val ai = (2..4).map { n -> text.substring(i, minOf(i + n, text.length)) }
                .firstOrNull { it.length >= 2 && it.all(Char::isDigit) && it in SPECS } ?: return null
            val spec = SPECS.getValue(ai)
            i += ai.length
            val value = if (spec.fixed != null) {
                if (i + spec.fixed > text.length) return null
                text.substring(i, i + spec.fixed).also { i += spec.fixed }
            } else {
                val end = text.indexOf(GS, i).let { if (it < 0) text.length else it }
                if (end - i > spec.max) return null
                text.substring(i, end).also { i = end }
            }
            if (value.isEmpty() || GS in value) return null
            out += Gs1Element(ai, value)
        }
        return out
    }
}
