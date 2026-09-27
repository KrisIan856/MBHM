package com.example.mbhm.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import com.example.mbhm.model.CurfewStatus
import com.example.mbhm.model.IncidentType
import com.example.mbhm.model.PayStatus
import com.example.mbhm.model.ReceiptStatus
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// ─── Color tokens ─────────────────────────────────────────────────────────────

object C {
    val bg = Color(0xFFF5F2EE)
    val card = Color(0xFFFFFFFF)
    val primary = Color(0xFF8B7FC7)
    val primaryLt = Color(0xFFEAE7F5)
    val sage = Color(0xFF7E9478)
    val sageLt = Color(0xFFE4EDE3)
    val text = Color(0xFF2C2825)
    val muted = Color(0xFF8A847E)
    val border = Color(0xFFE2DDD9)
    val danger = Color(0xFFB03030)
    val dangerLt = Color(0xFFFDECEA)
    val warn = Color(0xFFA07A28)
    val warnLt = Color(0xFFFEF3E2)
    val greenText = Color(0xFF3D7055)
    val blueText = Color(0xFF3B6ACB)
    val blueLt = Color(0xFFE8F0FF)
    val blueBar = Color(0xFF6B9BF7)
    val chevron = Color(0xFFC9C3BD)
    val navIdle = Color(0xFFB0AAA4)
}

fun hexColor(value: String): Color {
    val hex = value.removePrefix("#")
    val parsed = hex.toLongOrNull(16) ?: return C.muted
    return Color(0xFF000000 or parsed)
}

fun money(value: Int): String = "₱" + String.format("%,d", value)

fun moneyOrNull(value: Int?): String? = value?.let { money(it) }

fun dialPhone(context: android.content.Context, phone: String) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")
    )
    runCatching { context.startActivity(intent) }
}

fun android.content.Context.fileNameOf(uri: android.net.Uri): String {
    var name = uri.lastPathSegment ?: "file"
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index)
        }
    }
    return name
}

// ─── Avatar ───────────────────────────────────────────────────────────────────

@Composable
fun Avatar(initials: String, color: String, size: Int = 40, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(hexColor(color)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = C.text,
            fontSize = if (size > 44) 18.sp else 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── Status chips ─────────────────────────────────────────────────────────────

@Composable
private fun ChipLabel(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun PayChip(status: PayStatus, modifier: Modifier = Modifier) {
    when (status) {
        PayStatus.PAID -> ChipLabel("✓ Paid", C.sageLt, C.greenText, modifier)
        PayStatus.PARTIAL -> ChipLabel("½ Partial", C.blueLt, C.blueText, modifier)
        PayStatus.PENDING -> ChipLabel("⏳ Pending", C.warnLt, C.warn, modifier)
        PayStatus.OVERDUE -> ChipLabel("⚠ Overdue", C.dangerLt, C.danger, modifier)
    }
}

@Composable
fun ReceiptChip(status: ReceiptStatus, modifier: Modifier = Modifier) {
    when (status) {
        ReceiptStatus.PENDING_REVIEW -> ChipLabel("🕐 Under Review", C.warnLt, C.warn, modifier)
        ReceiptStatus.VERIFIED -> ChipLabel("✓ Verified", C.sageLt, C.greenText, modifier)
        ReceiptStatus.REJECTED -> ChipLabel("✕ Rejected", C.dangerLt, C.danger, modifier)
    }
}

@Composable
fun IncidentBadge(type: IncidentType, modifier: Modifier = Modifier) {
    when (type) {
        IncidentType.MISSED_CURFEW -> ChipLabel("🌙 Curfew", C.dangerLt, C.danger, modifier)
        IncidentType.SOS -> ChipLabel("🆘 SOS", Color(0xFFFFE4E4), Color(0xFF9B0000), modifier)
        IncidentType.MISSED_WORSHIP -> ChipLabel("📅 Worship", C.warnLt, C.warn, modifier)
        IncidentType.MAINTENANCE -> ChipLabel("🔧 Maintenance", Color(0xFFEAF0F5), Color(0xFF3A6080), modifier)
        IncidentType.OTHER -> ChipLabel("•• Other", Color(0xFFF0EAF5), Color(0xFF6B5FA8), modifier)
    }
}

@Composable
fun CurfewChip(status: CurfewStatus, modifier: Modifier = Modifier) {
    when (status) {
        CurfewStatus.COMPLIANT -> ChipLabel("✓ In", C.sageLt, C.greenText, modifier)
        CurfewStatus.LATE -> ChipLabel("⚠ Late", C.dangerLt, C.danger, modifier)
        CurfewStatus.ABSENT -> ChipLabel("✕ Absent", Color(0xFFF5E4E4), Color(0xFF9B0000), modifier)
        CurfewStatus.PENDING -> ChipLabel("⏳ Pending", Color(0xFFF0EEF8), Color(0xFF6B5FA8), modifier)
    }
}

// ─── QR Code display (deterministic pattern from a seed string) ──────────────

private fun buildQrCells(seed: String): Array<BooleanArray> {
    val grid = 21
    val cells = Array(grid) { BooleanArray(grid) }

    fun finder(row: Int, col: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val outer = r == 0 || r == 6 || c == 0 || c == 6
                val inner = r in 2..4 && c in 2..4
                cells[row + r][col + c] = outer || inner
            }
        }
        for (i in -1..7) {
            if (row + i in 0 until grid && col - 1 >= 0) cells[row + i][col - 1] = false
            if (row + i in 0 until grid && col + 7 < grid) cells[row + i][col + 7] = false
            if (col + i in 0 until grid && row - 1 >= 0) cells[row - 1][col + i] = false
            if (col + i in 0 until grid && row + 7 < grid) cells[row + 7][col + i] = false
        }
    }

    finder(0, 0)
    finder(0, grid - 7)
    finder(grid - 7, 0)

    for (i in 8 until grid - 8) {
        cells[6][i] = i % 2 == 0
        cells[i][6] = i % 2 == 0
    }

    var h = 0
    for (ch in seed) h = 31 * h + ch.code
    for (r in 0 until grid) {
        for (c in 0 until grid) {
            val skip = (r < 9 && c < 9) || (r < 9 && c > grid - 9) ||
                (r > grid - 9 && c < 9) || r == 6 || c == 6
            if (skip) continue
            h = 1664525 * h + 1013904223
            cells[r][c] = (h ushr 31) == 1
        }
    }
    return cells
}

@Composable
fun QRCodeDisplay(seed: String, size: Int = 200, modifier: Modifier = Modifier) {
    val cells = remember(seed) { buildQrCells(seed) }
    val pad = 10f
    val grid = 21
    val cellPx = (size - pad * 2) / grid

    Canvas(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, on ->
                if (on) {
                    drawRoundRect(
                        color = Color(0xFF1A1A1A),
                        topLeft = Offset(pad + c * cellPx, pad + r * cellPx),
                        size = Size(cellPx - 0.5f, cellPx - 0.5f),
                        cornerRadius = CornerRadius(0.8f)
                    )
                }
            }
        }
    }
}

// ─── QR Scanner (real camera + ZXing) ────────────────────────────────────────

private enum class ScannerState { IDLE, REQUESTING, ACTIVE, DENIED, ERROR, SUCCESS }

private fun grayFromRgba(
    bytes: ByteArray, width: Int, height: Int, rotation: Int
): Pair<IntArray, IntArray>? {
    fun luma(index: Int): Int {
        val r = bytes[index].toInt() and 0xFF
        val g = bytes[index + 1].toInt() and 0xFF
        val b = bytes[index + 2].toInt() and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    fun pack(v: Int): Int = (0xFF000000.toInt()) or (v shl 16) or (v shl 8) or v

    return when (rotation) {
        0 -> {
            val out = IntArray(width * height)
            var i = 0
            while (i < out.size) {
                out[i] = pack(luma(i * 4))
                i++
            }
            out to intArrayOf(width, height)
        }
        90 -> {
            val gw = height
            val gh = width
            val out = IntArray(gw * gh)
            var ox = 0
            while (ox < gw) {
                var oy = 0
                while (oy < gh) {
                    val sx = oy
                    val sy = height - 1 - ox
                    out[oy * gw + ox] = pack(luma((sy * width + sx) * 4))
                    oy++
                }
                ox++
            }
            out to intArrayOf(gw, gh)
        }
        180 -> {
            val out = IntArray(width * height)
            var oy = 0
            while (oy < height) {
                var ox = 0
                while (ox < width) {
                    val sx = width - 1 - ox
                    val sy = height - 1 - oy
                    out[oy * width + ox] = pack(luma((sy * width + sx) * 4))
                    ox++
                }
                oy++
            }
            out to intArrayOf(width, height)
        }
        270 -> {
            val gw = height
            val gh = width
            val out = IntArray(gw * gh)
            var ox = 0
            while (ox < gw) {
                var oy = 0
                while (oy < gh) {
                    val sx = width - 1 - oy
                    val sy = ox
                    out[oy * gw + ox] = pack(luma((sy * width + sx) * 4))
                    oy++
                }
                ox++
            }
            out to intArrayOf(gw, gh)
        }
        else -> null
    }
}

@Composable
fun QRScanner(onScan: (String) -> Unit, onClose: () -> Unit, label: String? = null) {
    var state by remember { mutableStateOf(ScannerState.IDLE) }
    var errorMsg by remember { mutableStateOf("") }
    var scannedValue by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    fun startCamera() {
        state = ScannerState.REQUESTING
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    try {
                        val buffer = proxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val dims = grayFromRgba(
                            bytes, proxy.width, proxy.height, proxy.imageInfo.rotationDegrees
                        )
                        if (dims != null) {
                            val (pixels, size) = dims
                            val source = RGBLuminanceSource(size[0], size[1], pixels)
                            val hints = mapOf(
                                DecodeHintType.TRY_HARDER to true
                            )
                            val result = MultiFormatReader().decode(
                                BinaryBitmap(HybridBinarizer(source)), hints
                            )
                            val text = result.text
                            if (!text.isNullOrEmpty()) {
                                Handler(Looper.getMainLooper()).post {
                                    if (scannedValue == null) scannedValue = text
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // no QR code in this frame
                    } finally {
                        proxy.close()
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                )
                cameraProvider = provider
                state = ScannerState.ACTIVE
            } catch (e: Exception) {
                state = ScannerState.ERROR
                errorMsg = e.message ?: "Camera unavailable"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else state = ScannerState.DENIED
    }

    fun startTapped() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(scannedValue) {
        val value = scannedValue ?: return@LaunchedEffect
        state = ScannerState.SUCCESS
        stopCamera()
        onScan(value)
    }

    DisposableEffect(Unit) {
        onDispose {
            stopCamera()
            executor.shutdown()
        }
    }

    val scannerPrimary = C.primary
    val transition = rememberInfiniteTransition(label = "scan")
    val scanFraction by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "scanline"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D0D1A))
                .border(
                    2.dp,
                    if (state == ScannerState.ACTIVE) scannerPrimary else Color(0xFFE8E3DC),
                    RoundedCornerShape(20.dp)
                )
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            if (state == ScannerState.ACTIVE) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = 36.dp.toPx()
                    val sw = 4.dp.toPx()
                    val off = 28.dp.toPx()
                    val stroke = sw
                    fun hLine(x: Float, y: Float, len: Float) = drawLine(
                        scannerPrimary, Offset(x, y), Offset(x + len, y), stroke, StrokeCap.Round
                    )
                    fun vLine(x: Float, y: Float, len: Float) = drawLine(
                        scannerPrimary, Offset(x, y), Offset(x, y + len), stroke, StrokeCap.Round
                    )
                    hLine(off, off + stroke / 2, c)
                    vLine(off + stroke / 2, off, c)
                    hLine(size.width - off - c, off + stroke / 2, c)
                    vLine(size.width - off - stroke / 2, off, c)
                    hLine(off, size.height - off - stroke / 2, c)
                    vLine(off + stroke / 2, size.height - off - c, c)
                    hLine(size.width - off - c, size.height - off - stroke / 2, c)
                    vLine(size.width - off - stroke / 2, size.height - off - c, c)
                    drawLine(
                        scannerPrimary,
                        Offset(40.dp.toPx(), size.height * scanFraction),
                        Offset(size.width - 40.dp.toPx(), size.height * scanFraction),
                        strokeWidth = 2.dp.toPx(),
                        alpha = 0.75f
                    )
                }
            }

            when (state) {
                ScannerState.IDLE -> ScannerMessage("📷", "Tap below to start camera")
                ScannerState.REQUESTING -> ScannerMessage("⏳", "Requesting camera…")
                ScannerState.DENIED -> ScannerMessage(
                    "🚫", "Camera access denied", Color(0xFFFF6B6B),
                    "Allow camera in settings, then retry"
                )
                ScannerState.ERROR -> ScannerMessage(
                    "⚠️", "Camera error", Color(0xFFFFA94D), errorMsg
                )
                ScannerState.SUCCESS -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 56.sp)
                        Text(
                            "Scanned!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                ScannerState.ACTIVE -> Unit
            }
        }

        when (state) {
            ScannerState.ACTIVE -> Text(
                "Point camera at the guardian's QR code",
                color = C.muted,
                fontSize = 14.sp
            )
            else -> if (label != null) {
                Text(label, color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }

        when (state) {
            ScannerState.IDLE, ScannerState.DENIED, ScannerState.ERROR -> PrimaryButton(
                text = if (state == ScannerState.IDLE) "📷 Start Camera" else "↺ Retry",
                onClick = { startTapped() },
                modifier = Modifier.fillMaxWidth()
            )
            ScannerState.ACTIVE -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.dangerLt)
                    .clickable {
                        stopCamera()
                        state = ScannerState.IDLE
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel Scan", color = C.danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            else -> Unit
        }
    }
}

@Composable
private fun ScannerMessage(
    icon: String,
    message: String,
    messageColor: Color = Color(0xFFB3B3B3),
    sub: String? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 44.sp)
        Text(
            message,
            color = messageColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (sub != null) {
            Text(
                sub,
                color = Color(0x80FFFFFF),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
fun Header(
    title: String,
    onBack: (() -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(C.card)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(C.bg)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = C.muted, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = C.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            right?.invoke()
        }
        HorizontalDivider(thickness = 1.dp, color = C.border)
    }
}

// ─── Bottom nav ───────────────────────────────────────────────────────────────

data class NavItem(val id: String, val label: String, val icon: String)

@Composable
fun NavBar(
    items: List<NavItem>,
    active: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().background(C.card)) {
        HorizontalDivider(thickness = 1.dp, color = C.border)
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
        items.forEach { item ->
            val isActive = active == item.id
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(item.id) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(item.icon, fontSize = 22.sp)
                Text(
                    item.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) C.primary else C.navIdle
                )
                if (isActive) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(C.primary)
                    )
                }
            }
        }
        }
    }
}

// ─── Bottom sheet ─────────────────────────────────────────────────────────────

@Composable
fun BottomSheet(
    open: Boolean,
    onClose: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!open) return
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * 0.85f).dp
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        TransparentDialogWindow()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x802C2825))
                .pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(C.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(C.border)
                )
                if (title != null) {
                    Text(title, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                content()
            }
        }
    }
}

// ─── Overlay modal ────────────────────────────────────────────────────────────

@Composable
fun Modal(
    open: Boolean,
    onClose: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!open) return
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        TransparentDialogWindow()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x802C2825))
                .pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (title != null) {
                    Text(title, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                content()
            }
        }
    }
}

@Composable
private fun TransparentDialogWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setDimAmount(0f)
    }
}

// ─── Form primitives ──────────────────────────────────────────────────────────

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(bottom = 6.dp),
        color = C.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun Input(
    value: String,
    onChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        if (label != null) FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = C.text, fontSize = 14.sp),
            cursorBrush = SolidColor(C.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(C.bg)
                .border(1.dp, C.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = C.muted, fontSize = 14.sp)
                }
                inner()
            }
        )
    }
}

@Composable
fun Textarea(
    value: String,
    onChange: (String) -> Unit,
    label: String? = null,
    placeholder: String? = null,
    rows: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        if (label != null) FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = C.text, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(C.primary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (rows * 22).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.bg)
                .border(1.dp, C.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = C.muted, fontSize = 14.sp)
                }
                inner()
            }
        )
    }
}

data class Option(val value: String, val label: String)

@Composable
fun Select(
    value: String,
    onChange: (String) -> Unit,
    options: List<Option>,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == value }?.label ?: value

    Column(modifier) {
        if (label != null) FieldLabel(label)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.bg)
                    .border(1.dp, C.border, RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedLabel, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("▾", color = C.muted, fontSize = 12.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 14.sp) },
                        onClick = {
                            onChange(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Toggle(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    sub: String? = null,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        label = "toggle"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (sub != null) {
                Text(
                    sub,
                    color = C.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) C.sage else C.border)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onChange(!checked) }
                .padding(2.dp)
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

// ─── Row item ─────────────────────────────────────────────────────────────────

@Composable
fun RowItem(
    icon: String,
    label: String,
    sub: String? = null,
    onClick: (() -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    danger: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, C.border, RoundedCornerShape(14.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (danger) C.dangerLt else C.bg),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = if (danger) C.danger else C.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (sub != null) {
                Text(sub, color = C.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (right != null) right() else Text("›", color = C.chevron, fontSize = 20.sp)
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = C.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        action?.invoke()
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: String, title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 44.sp)
        Text(title, color = C.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
        if (sub != null) {
            Text(sub, color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ─── Action buttons pair ──────────────────────────────────────────────────────

@Composable
fun ActionPair(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    cancelLabel: String = "Cancel",
    confirmDanger: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, C.border, RoundedCornerShape(12.dp))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Text(cancelLabel, color = C.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (confirmDanger) C.danger else C.primary)
                .clickable { onConfirm() },
            contentAlignment = Alignment.Center
        ) {
            Text(confirmLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Toast ────────────────────────────────────────────────────────────────────

@Composable
fun Toast(message: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(2500)
        onDone()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(C.text)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✓", color = Color.White, fontSize = 18.sp)
        Text(message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

// ─── Chip filter bar ──────────────────────────────────────────────────────────

@Composable
fun <T> ChipBar(
    options: List<Pair<String, T>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, optionValue) ->
            val selected = optionValue == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) C.primary else C.card)
                    .then(
                        if (selected) Modifier
                        else Modifier.border(1.dp, C.border, RoundedCornerShape(50))
                    )
                    .clickable { onChange(optionValue) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else C.muted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ─── Stat card ────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: Any,
    sub: String? = null,
    bg: Color? = null,
    textColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(bg ?: C.card, shape)
            .then(
                if (bg == null) Modifier.border(1.dp, C.border, shape) else Modifier
            )
            .padding(16.dp)
    ) {
        Text(
            label,
            color = (textColor ?: C.muted).copy(alpha = if (textColor != null) 0.8f else 1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            value.toString(),
            color = textColor ?: C.text,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (sub != null) {
            Text(
                sub,
                color = (textColor ?: C.muted).copy(alpha = if (textColor != null) 0.67f else 1f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ─── Layout helpers ───────────────────────────────────────────────────────────

@Composable
fun PCard(
    modifier: Modifier = Modifier,
    bg: Color = C.card,
    borderColor: Color? = C.border,
    radius: Int = 14,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, shape)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun ScrollBody(
    modifier: Modifier = Modifier,
    spacing: Int = 16,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content
    )
}

@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(C.bg)) {
        Header(title = title, onBack = onBack, right = right)
        ScrollBody(modifier = Modifier.weight(1f), content = content)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = C.primary,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    radius: Int = 14,
    height: Int = 52,
    fontSize: Int = 15
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(radius.dp))
            .background(bg.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = C.card,
    textColor: Color = C.muted,
    borderColor: Color? = C.border,
    radius: Int = 12,
    height: Int = 48,
    fontSize: Int = 14
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .height(height.dp)
            .background(bg, shape)
            .then(if (borderColor != null) Modifier.border(BorderStroke(1.dp, borderColor), shape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
