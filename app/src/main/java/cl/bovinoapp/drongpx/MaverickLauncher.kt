package cl.bovinoapp.drongpx

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale

object MaverickLauncher {

    private const val PACKAGE = "com.codesector.maverick.full"
    private const val IMPORT_ACTIVITY = "com.codesector.maverick.Import"
    private const val IMPORT_GEO_ACTIVITY = "com.codesector.maverick.ImportGeoPoint"

    enum class OpenedVia { GPX, GPX_OCTET_STREAM, GEO, CHOOSER }

    sealed interface Result {
        data class Opened(val via: OpenedVia) : Result
        object NotFound : Result
    }

    fun open(context: Context, gpxUri: Uri, coordinate: Coordinate, waypointName: String): Result {
        if (tryGpxImport(context, gpxUri, "application/gpx+xml")) {
            return Result.Opened(OpenedVia.GPX)
        }
        if (tryGpxImport(context, gpxUri, "application/octet-stream")) {
            return Result.Opened(OpenedVia.GPX_OCTET_STREAM)
        }
        if (tryGeoImport(context, coordinate, waypointName)) {
            return Result.Opened(OpenedVia.GEO)
        }
        if (tryChooser(context, gpxUri)) {
            return Result.Opened(OpenedVia.CHOOSER)
        }
        return Result.NotFound
    }

    private fun tryGpxImport(context: Context, uri: Uri, mimeType: String): Boolean =
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                setClassName(PACKAGE, IMPORT_ACTIVITY)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }.isSuccess

    private fun tryGeoImport(context: Context, coordinate: Coordinate, waypointName: String): Boolean =
        runCatching {
            val coordText = "%.6f,%.6f".format(Locale.US, coordinate.lat, coordinate.lon)
            val label = Uri.encode(waypointName)
            val geoUri = Uri.parse("geo:$coordText?q=$coordText($label)")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                setClassName(PACKAGE, IMPORT_GEO_ACTIVITY)
            }
            context.startActivity(intent)
        }.isSuccess

    private fun tryChooser(context: Context, gpxUri: Uri): Boolean =
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(gpxUri, "application/gpx+xml")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Abrir con…").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.isSuccess
}
