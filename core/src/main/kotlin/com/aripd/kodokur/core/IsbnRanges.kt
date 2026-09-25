package com.aripd.kodokur.core

/**
 * ISBN International'ın aralık tablosu: bir ISBN'in hangi gruba (dil/ülke)
 * ait olduğunu ve yayıncı bölümünün kaç hane sürdüğünü söyler; tireleme
 * buna dayanır. Tablo pakete gömülüdür (uygulama ağa bağlanmaz) ve
 * `tools/isbn_araliklari.py` ile tazelenir.
 */
internal object IsbnRanges {

    class Rule(val low: Int, val high: Int, val length: Int)

    /** [digits] tiresiz önek ("978975"); [group] ISBN'deki grup bölümü ("975"). */
    class Group(val digits: String, val group: String, val agency: String, val rules: List<Rule>)

    // Mutlak yol: R8 sınıfı başka pakete taşısa da kaynak bulunur.
    private const val RESOURCE = "/com/aripd/kodokur/core/isbn-ranges.txt"

    /** Tablonun kaynaktaki tarihi; hakkında ekranında gösterilir. */
    val date: String by lazy { load().first }

    private val groups: List<Group> by lazy { load().second }

    private fun load(): Pair<String, List<Group>> {
        val stream = IsbnRanges::class.java.getResourceAsStream(RESOURCE)
            ?: error("ISBN aralık tablosu pakette yok: $RESOURCE")
        var date = ""
        val groups = ArrayList<Group>(300)
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.startsWith("#")) {
                    if (date.isEmpty()) date = line.split(" · ").getOrNull(1).orEmpty()
                    continue
                }
                if (line.isBlank()) continue
                val (prefix, agency, rules) = line.split('\t')
                groups += Group(
                    digits = prefix.replace("-", ""),
                    group = prefix.substringAfter('-'),
                    agency = agency,
                    rules = rules.split(',').map { rule ->
                        val (range, length) = rule.split(':')
                        val (low, high) = range.split('-')
                        Rule(low.toInt(), high.toInt(), length.toInt())
                    },
                )
            }
        }
        return date to groups
    }

    /** Grup önekleri birbirinin öneki olmadığından ilk eşleşme tek eşleşmedir. */
    fun group(isbn13: String): Group? = groups.firstOrNull { isbn13.startsWith(it.digits) }

    /**
     * ISBN-13'ü beş bölüme ayırır: önek, grup, yayıncı, yayın, sağlama.
     * Grup bilinmiyorsa ya da aralık henüz tanımlanmamışsa null.
     */
    fun split(isbn13: String): List<String>? {
        val group = group(isbn13) ?: return null
        val rest = isbn13.substring(group.digits.length, 12)
        val key = rest.padEnd(7, '0').take(7).toInt()
        val length = group.rules.firstOrNull { key in it.low..it.high }?.length ?: return null
        if (length == 0 || length >= rest.length) return null
        return listOf(
            isbn13.substring(0, 3),
            group.group,
            rest.substring(0, length),
            rest.substring(length),
            isbn13.substring(12),
        )
    }
}
