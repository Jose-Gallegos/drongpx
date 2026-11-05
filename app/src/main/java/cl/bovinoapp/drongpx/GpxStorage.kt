package cl.bovinoapp.drongpx

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object GpxStorage {

    private const val DOWNLOADS_SUBFOLDER = "Download/DronGPX"

    /** Writes the GPX, renaming it with a numeric suffix if [waypoint.fileName] is already taken. */
    fun writePrivateGpx(context: Context, waypoint: DronWaypoint): Pair<File, DronWaypoint> {
        val dir = File(context.filesDir, "gpx").apply { mkdirs() }
        val uniqueName = uniqueFileName(dir, waypoint.fileName)
        val file = File(dir, uniqueName)
        file.writeText(waypoint.gpxContent, Charsets.UTF_8)
        val resolved = if (uniqueName == waypoint.fileName) waypoint else waypoint.copy(fileName = uniqueName)
        return file to resolved
    }

    private fun uniqueFileName(dir: File, baseFileName: String): String {
        if (!File(dir, baseFileName).exists()) return baseFileName
        val dotIndex = baseFileName.lastIndexOf('.')
        val stem = if (dotIndex >= 0) baseFileName.substring(0, dotIndex) else baseFileName
        val extension = if (dotIndex >= 0) baseFileName.substring(dotIndex) else ""
        var suffix = 2
        while (File(dir, "$stem-$suffix$extension").exists()) suffix++
        return "$stem-$suffix$extension"
    }

    fun contentUriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copies the GPX into the public Downloads/DronGPX folder. Only possible on API 29+
     * without a storage permission (scoped-storage MediaStore write); on API 26-28 this
     * is skipped since the app requests no storage permission.
     */
    fun copyToPublicDownloads(context: Context, waypoint: DronWaypoint): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, waypoint.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/gpx+xml")
                put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOADS_SUBFOLDER)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                out.write(waypoint.gpxContent.toByteArray(Charsets.UTF_8))
            }
            true
        }.getOrDefault(false)
    }
}
