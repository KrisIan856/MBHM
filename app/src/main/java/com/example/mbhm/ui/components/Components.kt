package com.example.mbhm.ui.components

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

// ─── Color tokens ─────────────────────────────────────────────────────────────

object C {
    val bg = Color(0xFFF8FAFC)
    val card = Color(0xFFFFFFFF)
    val primary = Color(0xFF4F46E5)
    val primaryLt = Color(0xFFEEF2FF)
    val sage = Color(0xFF10B981)
    val sageLt = Color(0xFFECFDF5)
    val text = Color(0xFF0F172A)
    val muted = Color(0xFF64748B)
    val border = Color(0xFFE2E8F0)
    val danger = Color(0xFFEF4444)
    val dangerLt = Color(0xFFFEF2F2)
    val warn = Color(0xFFF59E0B)
    val warnLt = Color(0xFFFFFBEB)
    val greenText = Color(0xFF047857)
    val blueText = Color(0xFF2563EB)
    val blueLt = Color(0xFFEFF6FF)
    val blueBar = Color(0xFF3B82F6)
    val chevron = Color(0xFF94A3B8)
    val navIdle = Color(0xFF94A3B8)
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

/** Picks a readable text color for any avatar background (light colors get dark text). */
fun readableTextOn(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.62f) Color(0xFF0F172A) else Color.White
}

@Composable
fun Avatar(initials: String, color: String, size: Int = 40, modifier: Modifier = Modifier) {
    val bg = hexColor(color)
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = readableTextOn(bg),
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
        PayStatus.PARTIAL -> ChipLabel("½ Partial", C.primaryLt, C.primary, modifier)
        PayStatus.PENDING -> ChipLabel("⏳ Pending", C.bg, C.muted, modifier)
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
        IncidentType.SOS -> ChipLabel("🆘 SOS", Color(0xFFFEF2F2), Color(0xFFDC2626), modifier)
        IncidentType.MISSED_WORSHIP -> ChipLabel("📅 Worship", C.warnLt, C.warn, modifier)
        IncidentType.MAINTENANCE -> ChipLabel("🔧 Maintenance", Color(0xFFF0F9FF), Color(0xFF0284C7), modifier)
        IncidentType.OTHER -> ChipLabel("•• Other", Color(0xFFF5F3FF), Color(0xFF7C3AED), modifier)
    }
}

@Composable
fun CurfewChip(status: CurfewStatus, modifier: Modifier = Modifier) {
    when (status) {
        CurfewStatus.COMPLIANT -> ChipLabel("✓ In", C.sageLt, C.greenText, modifier)
        CurfewStatus.LATE -> ChipLabel("⚠ Late", C.dangerLt, C.danger, modifier)
        CurfewStatus.ABSENT -> ChipLabel("✕ Absent", Color(0xFFFEF2F2), Color(0xFFDC2626), modifier)
        CurfewStatus.PENDING -> ChipLabel("⏳ Pending", Color(0xFFF1F5F9), Color(0xFF475569), modifier)
    }
}

// ─── QR Code display (real QR generated from content via ZXing) ──────────────

private fun buildQrCells(content: String): Array<BooleanArray> {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 4
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    return Array(matrix.height) { r -> BooleanArray(matrix.width) { c -> matrix.get(c, r) } }
}

@Composable
fun QRCodeDisplay(seed: String, size: Int = 200, modifier: Modifier = Modifier) {
    val cells = remember(seed) { buildQrCells(seed) }
    val grid = cells.size

    Canvas(
        modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        val totalPx = this.size.minDimension
        val cellPx = totalPx / grid
        val dark = Color(0xFF0F172A)

        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, on ->
                if (on) {
                    drawRect(
                        color = dark,
                        topLeft = Offset(c * cellPx, r * cellPx),
                        size = Size(cellPx + 0.5f, cellPx + 0.5f)
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
                .background(Color(0xFF0F172A))
                .border(
                    2.dp,
                    if (state == ScannerState.ACTIVE) scannerPrimary else C.border,
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
                    "🚫", "Camera access denied", Color(0xFFEF4444),
                    "Allow camera in settings, then retry"
                )
                ScannerState.ERROR -> ScannerMessage(
                    "⚠️", "Camera error", Color(0xFFF59E0B), errorMsg
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
    Column(
        modifier = modifier.fillMaxWidth().background(C.card)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(C.bg)
                        .border(1.dp, C.border, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = C.text,
                fontSize = 18.sp,
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
                .padding(top = 6.dp, bottom = 20.dp, start = 4.dp, end = 4.dp),
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
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isActive) C.primaryLt else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.icon, fontSize = 20.sp)
                }
                Text(
                    item.label,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) C.primary else C.navIdle
                )
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
                .background(Color(0x660F172A))
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            title,
                            color = C.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    CloseButton(onClose)
                }
                content()
            }
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(C.bg)
            .border(1.dp, C.border, CircleShape)
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = C.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        val configuration = LocalConfiguration.current
        val maxModalHeight = (configuration.screenHeightDp - 120).dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F172A))
                .pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxModalHeight)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.card)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title != null) {
                        Text(
                            title,
                            color = C.text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    CloseButton(onClose)
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
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = C.text, fontSize = 14.sp),
            cursorBrush = SolidColor(C.primary),
            singleLine = true,
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(C.card)
                .border(1.dp, if (focused) C.primary else C.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
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
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = C.text, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(C.primary),
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (rows * 22).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(C.card)
                .border(1.dp, if (focused) C.primary else C.border, RoundedCornerShape(12.dp))
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.card)
                    .border(1.dp, C.border, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
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
        modifier = modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onChange(!checked) },
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

// ─── Persistent settings toggle state ────────────────────────────────────────

@Composable
fun rememberPrefBoolean(key: String, default: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("mbhm_settings", Context.MODE_PRIVATE)
    }
    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }
    return remember(state) {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(value) {
                    state.value = value
                    prefs.edit().putBoolean(key, value).apply()
                }
            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
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
        Text(title.uppercase(), color = C.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        action?.invoke()
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: String, title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(C.primaryLt),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 34.sp)
        }
        Text(title, color = C.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
        if (sub != null) {
            Text(
                sub,
                color = C.muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
            )
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
        Text("✓", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    color = if (selected) Color.White else C.text.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
        )
        Text(
            value.toString(),
            color = textColor ?: C.text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
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
            .shadow(2.dp, shape)
            .background(bg, shape)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .padding(padding),
        content = content
    )
}

// ─── Data table ───────────────────────────────────────────────────────────────

/**
 * Column spec for [TableCard] / [TableRow].
 * [weight] is the relative width inside the table; [end] right-aligns the column.
 */
data class TableColumn(val title: String, val weight: Float, val end: Boolean = false)

/**
 * Card-styled table with a muted uppercase header and hairline dividers.
 * Use it when a list has to be read as numbers side by side (balances, history).
 */
@Composable
fun TableCard(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    PCard(modifier = modifier, padding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .background(C.bg)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            columns.forEach { col ->
                Text(
                    col.title.uppercase(),
                    color = C.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    textAlign = if (col.end) TextAlign.End else TextAlign.Start,
                    maxLines = 1,
                    modifier = Modifier.weight(col.weight)
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = C.border)
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * One table row. [cells] must match the order of [columns]; missing cells render blank.
 * Set [divider] = false for the first row so lines never double up.
 */
@Composable
fun TableRow(
    columns: List<TableColumn>,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    cells: List<@Composable () -> Unit>
) {
    Column(modifier.fillMaxWidth()) {
        if (divider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = C.border,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
        val rowModifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp)

        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            columns.forEachIndexed { index, col ->
                Box(
                    Modifier.weight(col.weight),
                    contentAlignment = if (col.end) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    val cell = cells.getOrNull(index)
                    if (cell != null) cell()
                }
            }
        }
    }
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
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .height(height.dp)
            .shadow(4.dp, shape)
            .clip(shape)
            .background(
                if (bg == C.primary && enabled) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(bg, Color(0xFF3730A3))
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(bg.copy(alpha = if (enabled) 1f else 0.4f),
                               bg.copy(alpha = if (enabled) 1f else 0.4f))
                    )
                }
            )
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
    textColor: Color = C.text,
    borderColor: Color? = C.border,
    radius: Int = 12,
    height: Int = 48,
    fontSize: Int = 14,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .height(height.dp)
            .shadow(1.dp, shape)
            .background(bg.copy(alpha = if (enabled) 1f else 0.4f), shape)
            .then(if (borderColor != null) Modifier.border(BorderStroke(1.dp, borderColor), shape) else Modifier)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// --- Date helpers + range filter ---------------------------------------------

private val MONTH_ABBRS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

fun isoToday(): String = java.time.LocalDate.now().toString()

fun parseIsoDate(value: String): Long? = runCatching {
    java.time.LocalDate.parse(value)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

fun prettyIso(value: String): String {
    val date = runCatching { java.time.LocalDate.parse(value) }.getOrNull() ?: return value
    return date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH))
}

private fun monthIndex(name: String): Int = MONTH_ABBRS.indexOf(name.lowercase().take(3)) + 1

private fun toEpoch(year: Int, month: Int, day: Int): Long? = runCatching {
    java.time.LocalDate.of(year, month, day)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

/**
 * Parses the display style dates used across the app, e.g.
 * "Oct 5, 2026 · 3:04 PM", "Oct 5 · 3:04 PM", "Just now", "Sep 15, 2026", "Oct 2026".
 */
fun parseDisplayDate(raw: String): Long? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("Just now", ignoreCase = true)) return System.currentTimeMillis()

    Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(text)?.let { m ->
        return toEpoch(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }
    Regex("""^([A-Za-z]{3,9})\s+(\d{1,2}),\s*(\d{4})""").find(text)?.let { m ->
        val month = monthIndex(m.groupValues[1])
        if (month > 0) return toEpoch(m.groupValues[3].toInt(), month, m.groupValues[2].toInt())
    }
    Regex("""^([A-Za-z]{3})\s+(\d{1,2})(?!\d)""").find(text)?.let { m ->
        val month = monthIndex(m.groupValues[1])
        if (month > 0) {
            val today = java.time.LocalDate.now()
            var year = today.year
            val candidate = toEpoch(year, month, m.groupValues[2].toInt()) ?: return null
            if (candidate > System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000) {
                year -= 1
                return toEpoch(year, month, m.groupValues[2].toInt())
            }
            return candidate
        }
    }
    Regex("""^([A-Za-z]{3})\s+(\d{4})$""").find(text)?.let { m ->
        val month = monthIndex(m.groupValues[1])
        if (month > 0) return toEpoch(m.groupValues[2].toInt(), month, 1)
    }
    return null
}

/** True when [raw] falls inside the inclusive From/To range (empty bounds are ignored). */
fun withinDateRange(raw: String, fromIso: String, toIso: String): Boolean {
    if (fromIso.isEmpty() && toIso.isEmpty()) return true
    val epoch = parseDisplayDate(raw) ?: return false
    val from = if (fromIso.isEmpty()) Long.MIN_VALUE else (parseIsoDate(fromIso) ?: Long.MIN_VALUE)
    val endOfDay = if (toIso.isEmpty()) {
        Long.MAX_VALUE
    } else {
        (parseIsoDate(toIso) ?: Long.MAX_VALUE) + 24L * 60 * 60 * 1000 - 1
    }
    return epoch in from..endOfDay
}

/**
 * Month pager used by both the guardian and boarder payment screens so the period a
 * user is looking at is identical on each side. [periods] must be chronological.
 */
@Composable
fun PayPeriodPager(
    periods: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    caption: String = "PAYMENT PERIOD"
) {
    if (periods.isEmpty()) return
    val index = periods.indexOf(selected).coerceIn(0, periods.lastIndex)
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.card)
            .border(1.dp, C.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .background(if (index > 0) C.bg else C.card)
                .clickable(enabled = index > 0) { onSelect(periods[index - 1]) },
            contentAlignment = Alignment.Center
        ) {
            Text("‹", color = if (index > 0) C.text else C.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(caption, color = C.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(periods[index], color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .background(if (index < periods.lastIndex) C.bg else C.card)
                .clickable(enabled = index < periods.lastIndex) { onSelect(periods[index + 1]) },
            contentAlignment = Alignment.Center
        ) {
            Text("›", color = if (index < periods.lastIndex) C.text else C.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Current wall-clock time, refreshed automatically every [intervalMs] milliseconds.
 * Use it for any UI that depends on the time of day so it updates without a restart.
 */
@Composable
fun rememberNow(intervalMs: Long = 15_000L): Long {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(intervalMs)
        }
    }
    return now.longValue
}

/**
 * Epoch millis for a calendar date plus a clock time, e.g. ("Oct 6, 2026", "6:30 PM").
 * Returns null when either part cannot be parsed.
 */
fun windowMillis(dateLabel: String, timeLabel: String): Long? =
    runCatching {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.ENGLISH)
            .parse("$dateLabel $timeLabel")?.time
    }.getOrNull()

/** True when [now] falls inside the [dateLabel] window that ends at [endLabel]. */
fun windowEnded(dateLabel: String, endLabel: String, now: Long): Boolean {
    val end = windowMillis(dateLabel, endLabel) ?: return false
    return now > end
}

@Composable
fun DateRangeFilter(
    from: String,
    to: String,
    onChange: (from: String, to: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateFilterButton("FROM", from, Modifier.weight(1f)) { onChange(it, to) }
        DateFilterButton("TO", to, Modifier.weight(1f)) { onChange(from, it) }
        if (from.isNotEmpty() || to.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(C.primaryLt)
                    .clickable { onChange("", "") }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("Clear", color = C.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DateFilterButton(
    label: String,
    value: String,
    modifier: Modifier,
    onPick: (String) -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(C.card)
            .border(1.dp, C.border, shape)
            .clickable {
                val calendar = java.util.Calendar.getInstance()
                if (value.isNotEmpty()) {
                    parseIsoDate(value)?.let { calendar.timeInMillis = it }
                }
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onPick(String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day))
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                ).show()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(label, color = C.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(
                if (value.isEmpty()) "Any date" else prettyIso(value),
                color = C.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
