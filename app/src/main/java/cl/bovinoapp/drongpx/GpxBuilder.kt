package cl.bovinoapp.drongpx

import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DronWaypoint(
    val fileName: String,
    val waypointName: String,
    val gpxContent: String,
)

object GpxBuilder {

    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss", Locale.US)
    private val NAME_STAMP = DateTimeFormatter.ofPattern("dd-MM HH mm ss", Locale.US)

    fun build(coordinate: Coordinate, rawQrContent: String, now: ZonedDateTime = ZonedDateTime.now()): DronWaypoint {
        val fileName = "dron_${now.format(FILE_STAMP)}.gpx"
        val waypointName = "Dron ${now.format(NAME_STAMP)}"
        val isoTime = now.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)

        val content = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<gpx version="1.1" creator="DronGPX" xmlns="http://www.topografix.com/GPX/1/1">
  <wpt lat="${"%.6f".format(Locale.US, coordinate.lat)}" lon="${"%.6f".format(Locale.US, coordinate.lon)}">
    <time>$isoTime</time>
    <name>${escapeXml(waypointName)}</name>
    <desc>${escapeXml(rawQrContent)}</desc>
  </wpt>
</gpx>
"""
        return DronWaypoint(fileName, waypointName, content)
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
