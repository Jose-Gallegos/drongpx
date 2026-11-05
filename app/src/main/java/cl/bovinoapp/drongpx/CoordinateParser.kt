package cl.bovinoapp.drongpx

import android.net.Uri

data class Coordinate(val lat: Double, val lon: Double)

object CoordinateParser {

    private val AT_PATTERN =
        Regex("""@(-?\d{1,3}(?:\.\d+)?)(?![\d.eE]),(-?\d{1,3}(?:\.\d+)?)(?![\d.eE])""")
    private val LOOSE_PAIR_PATTERN =
        Regex("""(?<![-+.\deE])(-?\d{1,3}(?:\.\d+)?)(?![\d.eE])\s*,\s*(?<![-+.\deE])(-?\d{1,3}(?:\.\d+)?)(?![\d.eE])""")

    fun parse(raw: String): Coordinate? {
        val trimmed = raw.trim()
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()

        uri?.let {
            fromQueryParam(it, "query")?.let { c -> return c }
            fromQueryParam(it, "q")?.let { c -> return c }
            if (it.scheme == "geo") {
                fromPairText(it.schemeSpecificPart.substringBefore('?'))?.let { c -> return c }
            }
        }

        AT_PATTERN.find(trimmed)?.let { m ->
            fromParts(m.groupValues[1], m.groupValues[2])?.let { c -> return c }
        }

        LOOSE_PAIR_PATTERN.find(trimmed)?.let { m ->
            fromParts(m.groupValues[1], m.groupValues[2])?.let { c -> return c }
        }

        return null
    }

    private fun fromQueryParam(uri: Uri, key: String): Coordinate? =
        runCatching { uri.getQueryParameter(key) }.getOrNull()?.let { fromPairText(it) }

    private fun fromPairText(text: String): Coordinate? {
        val parts = text.split(",").map { it.trim() }
        if (parts.size < 2) return null
        return fromParts(parts[0], parts[1])
    }

    private fun fromParts(latText: String, lonText: String): Coordinate? {
        val lat = latText.toDoubleOrNull() ?: return null
        val lon = lonText.toDoubleOrNull() ?: return null
        if (!lat.isFinite() || !lon.isFinite()) return null
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        return Coordinate(lat, lon)
    }
}
