package cl.bovinoapp.drongpx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    object AwaitingPermission : UiState
    object Ready : UiState
    object Processing : UiState
    data class Success(val waypointName: String, val detail: String) : UiState
    data class Error(val message: String, val rawContent: String?) : UiState
}

class MainActivity : ComponentActivity() {

    private var uiState by mutableStateOf<UiState>(UiState.Ready)
    private var resumeShouldRescan = false

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        val content = result.contents
        if (content == null) {
            uiState = UiState.Ready
        } else {
            handleScanned(content)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchScan() else uiState = UiState.AwaitingPermission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ScannerScreen(
                    state = uiState,
                    onScanClick = ::requestCameraAndScan,
                )
            }
        }
        requestCameraAndScan()
    }

    override fun onResume() {
        super.onResume()
        if (resumeShouldRescan) {
            resumeShouldRescan = false
            launchScan()
        }
    }

    private fun requestCameraAndScan() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchScan()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScan() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setBeepEnabled(true)
            .setOrientationLocked(false)
            .setPrompt("Apunta al QR del dron")
        scanLauncher.launch(options)
    }

    private fun handleScanned(rawContent: String) {
        uiState = UiState.Processing
        lifecycleScope.launch {
            try {
                val coordinate = CoordinateParser.parse(rawContent)
                if (coordinate == null) {
                    uiState = UiState.Error(
                        message = "No se encontraron coordenadas válidas en el QR.",
                        rawContent = rawContent,
                    )
                    return@launch
                }

                val draft = GpxBuilder.build(coordinate, rawContent)

                val (waypoint, opened) = withContext(Dispatchers.IO) {
                    val (file, waypoint) = GpxStorage.writePrivateGpx(this@MainActivity, draft)
                    GpxStorage.copyToPublicDownloads(this@MainActivity, waypoint)
                    val uri = GpxStorage.contentUriFor(this@MainActivity, file)
                    val result = MaverickLauncher.open(this@MainActivity, uri, coordinate, waypoint.waypointName)
                    waypoint to result
                }

                when (opened) {
                    is MaverickLauncher.Result.Opened -> {
                        resumeShouldRescan = true
                        uiState = UiState.Success(
                            waypointName = waypoint.waypointName,
                            detail = "Guardado: ${waypoint.fileName}",
                        )
                    }
                    MaverickLauncher.Result.NotFound -> {
                        uiState = UiState.Error(
                            message = "Maverick no está instalado y no se encontró otra app para abrir el GPX.",
                            rawContent = null,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = UiState.Error(
                    message = "No se pudo guardar el GPX: ${e.message ?: e.javaClass.simpleName}",
                    rawContent = rawContent,
                )
            }
        }
    }
}

@Composable
private fun ScannerScreen(state: UiState, onScanClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "DronGPX",
                color = Color.Yellow,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer()
            when (state) {
                UiState.Ready -> {
                    StatusText("Listo para escanear")
                    BigButton("Escanear", onScanClick)
                }
                UiState.AwaitingPermission -> {
                    StatusText("Se necesita permiso de cámara para escanear el QR del dron.")
                    BigButton("Conceder permiso", onScanClick)
                }
                UiState.Processing -> {
                    StatusText("Procesando…")
                }
                is UiState.Success -> {
                    StatusText("Abierto en Maverick:")
                    StatusText(state.waypointName, Color.Green)
                    StatusText(state.detail)
                }
                is UiState.Error -> {
                    StatusText(state.message, Color.Red)
                    state.rawContent?.let { StatusText("Contenido del QR: $it") }
                    BigButton("Reintentar", onScanClick)
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String, color: Color = Color.White) {
    Text(
        text = text,
        color = color,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(72.dp),
    ) {
        Text(text = text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
}
