package com.aripd.kodokur.core

/**
 * The ISBN International range table: tells which group (language/country) an
 * ISBN belongs to and how many digits the publisher element spans; hyphenation
 * relies on it. The table is bundled in the package (the app does not go online)
 * and refreshed with `tools/isbn_ranges.py`.
 */
internal object IsbnRanges {

    class Rule(val low: Int, val high: Int, val length: Int)

    /** [digits] is the unhyphenated prefix ("978975"); [group] is the group element of the ISBN ("975"). */
    class Group(val digits: String, val group: String, val agency: String, val rules: List<Rule>)

    // Absolute path: the resource is found even if R8 moves the class to another package.
    private const val RESOURCE = "/com/aripd/kodokur/core/isbn-ranges.txt"

    /** The table's date at the source; shown on the About screen. */
    val date: String by lazy { load().first }

    private val groups: List<Group> by lazy { load().second }

    private fun load(): Pair<String, List<Group>> {
        val stream = IsbnRanges::class.java.getResourceAsStream(RESOURCE)
            ?: error("ISBN range table missing from the package: $RESOURCE")
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

    /** No group prefix is a prefix of another, so the first match is the only match. */
    fun group(isbn13: String): Group? = groups.firstOrNull { isbn13.startsWith(it.digits) }

    /**
     * Splits an ISBN-13 into five elements: prefix, group, publisher, title, check digit.
     * Null if the group is unknown or the range is not defined yet.
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
