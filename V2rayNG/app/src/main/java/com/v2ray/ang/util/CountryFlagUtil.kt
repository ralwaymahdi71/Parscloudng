package com.v2ray.ang.util

/**
 * Best-effort extraction of a country name/flag from a server's remark text.
 *
 * Free server lists almost always encode the country in the remark, e.g.
 * "Germany263", "🇩🇪 DE-Frankfurt-01", "US New York #2". This util tries a
 * few cheap heuristics to turn that into a display name + flag emoji, and
 * falls back gracefully (empty flag, original remark as name) when it can't
 * confidently tell.
 *
 * This never touches the raw config link — it only reads the human-readable
 * remark that the existing config parsers already extract.
 */
object CountryFlagUtil {

    data class CountryInfo(val countryName: String?, val flagEmoji: String)

    // ISO 3166-1 alpha-2 code -> English country name, for the common set of
    // countries that show up in free VPN lists. Not exhaustive by design.
    private val CODE_TO_NAME: Map<String, String> = mapOf(
        "DE" to "Germany", "US" to "United States", "GB" to "United Kingdom",
        "UK" to "United Kingdom", "FR" to "France", "NL" to "Netherlands",
        "JP" to "Japan", "SG" to "Singapore", "HK" to "Hong Kong",
        "KR" to "South Korea", "CA" to "Canada", "AU" to "Australia",
        "RU" to "Russia", "TR" to "Turkey", "IR" to "Iran", "AE" to "UAE",
        "IN" to "India", "BR" to "Brazil", "IT" to "Italy", "ES" to "Spain",
        "SE" to "Sweden", "CH" to "Switzerland", "FI" to "Finland",
        "PL" to "Poland", "UA" to "Ukraine", "TW" to "Taiwan", "CN" to "China",
        "VN" to "Vietnam", "ID" to "Indonesia", "MY" to "Malaysia",
        "TH" to "Thailand", "PH" to "Philippines", "AT" to "Austria",
        "BE" to "Belgium", "DK" to "Denmark", "NO" to "Norway",
        "IE" to "Ireland", "PT" to "Portugal", "CZ" to "Czechia",
        "RO" to "Romania", "GR" to "Greece", "IL" to "Israel",
        "SA" to "Saudi Arabia", "EG" to "Egypt", "ZA" to "South Africa",
        "MX" to "Mexico", "AR" to "Argentina", "LU" to "Luxembourg",
        "MD" to "Moldova", "LT" to "Lithuania", "LV" to "Latvia",
        "EE" to "Estonia", "BG" to "Bulgaria", "HU" to "Hungary",
    )

    // English country name (lowercase, no punctuation) -> ISO code, so remarks
    // like "Germany263" or "United_States-01" resolve to the same flag.
    private val NAME_TO_CODE: Map<String, String> =
        CODE_TO_NAME.entries.associate { (code, name) -> name.lowercase() to code } +
            mapOf(
                "usa" to "US", "united states of america" to "US",
                "uk" to "GB", "great britain" to "GB", "england" to "GB",
                "uae" to "AE", "united arab emirates" to "AE",
                "holland" to "NL", "korea" to "KR", "south korea" to "KR",
                "hongkong" to "HK", "türkiye" to "TR", "turkiye" to "TR",
            )

    /**
     * @param remark The server's remark/name as extracted by the existing config parser.
     */
    fun extract(remark: String?): CountryInfo {
        if (remark.isNullOrBlank()) return CountryInfo(null, "")

        // 1) The remark already contains an emoji flag (two regional-indicator
        //    symbols) — just reuse it, don't guess.
        val existingFlag = Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]").find(remark)
        if (existingFlag != null) {
            return CountryInfo(null, existingFlag.value)
        }

        val cleaned = remark.trim()

        // 2) A 2-letter ISO code as its own token, e.g. "DE-Frankfurt-01" or "[DE] node1".
        val codeToken = Regex("(?:^|[^A-Za-z])([A-Za-z]{2})(?:[^A-Za-z]|$)")
            .findAll(cleaned)
            .map { it.groupValues[1].uppercase() }
            .firstOrNull { it in CODE_TO_NAME }
        if (codeToken != null) {
            return CountryInfo(CODE_TO_NAME[codeToken], codeToFlagEmoji(codeToken))
        }

        // 3) A known country name appearing anywhere in the remark, e.g. "Germany263".
        val lower = cleaned.lowercase()
        val nameMatch = NAME_TO_CODE.entries
            .filter { (name, _) -> lower.contains(name) }
            .maxByOrNull { (name, _) -> name.length } // prefer the longest/most specific match
        if (nameMatch != null) {
            val code = nameMatch.value
            return CountryInfo(CODE_TO_NAME[code] ?: nameMatch.key.replaceFirstChar { it.uppercase() }, codeToFlagEmoji(code))
        }

        return CountryInfo(null, "")
    }

    private fun codeToFlagEmoji(isoCode: String): String {
        if (isoCode.length != 2) return ""
        val base = 0x1F1E6 // regional indicator symbol letter A
        return isoCode.uppercase().map { c ->
            String(Character.toChars(base + (c - 'A')))
        }.joinToString("")
    }
}
