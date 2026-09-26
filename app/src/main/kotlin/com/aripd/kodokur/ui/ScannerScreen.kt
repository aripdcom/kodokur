package com.aripd.kodokur.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aripd.kodokur.KodokurViewModel
import com.aripd.kodokur.R
import com.aripd.kodokur.core.Content
import com.aripd.kodokur.core.ContentParser
import com.aripd.kodokur.core.Scan
import com.aripd.kodokur.platform.GalleryReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Seri taramada iki okuma arası: aynı kod kadrajdan çıkmadan yenisi gelmesin. */
private const val BATCH_PAUSE_MS = 1200L

@Composable
fun ScannerScreen(
    vm: KodokurViewModel,
    onHistory: () -> Unit,
    onAbout: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    // İzin reddedildi ve sistem artık sormuyor: yalnızca ayarlardan verilebilir.
    var blocked by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var readingImage by remember { mutableStateOf(false) }
    val trigger = remember { ScanTrigger() }
    val zoom = remember { ZoomControl() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        // Reddedildikten sonra gerekçe istenmiyorsa sistem bir daha sormayacak demektir.
        // Sonuç geldiği anda hesaplanır; sonradan okunsa ekran yenilenmezdi.
        blocked = !it && context.findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
    }
    // Ayarlardan izin verip dönen kullanıcı için.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = context.hasCameraPermission() }
    LaunchedEffect(Unit) {
        if (hasCamera && !granted && !asked) {
            asked = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun confirm() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }

    fun onScan(scan: Scan) {
        confirm()
        if (!vm.batchMode) {
            // Ekran okuyucu yeni ekrana kendiliğinden geçmeyebilir: ne okunduğunu söyle.
            // Seri kipte bildirim zaten duyurulur.
            val content = ContentParser.parse(scan)
            @Suppress("DEPRECATION")
            view.announceForAccessibility("${context.getString(content.kindLabel())}: ${content.headline()}")
            vm.open(scan)
            return
        }
        val label = batchLabel(scan)
        val added = vm.addToBatch(scan)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            launch {
                snackbar.showSnackbar(
                    context.getString(if (added) R.string.batch_added else R.string.batch_duplicate, label),
                )
            }
            delay(BATCH_PAUSE_MS)
            trigger.armed = true
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readingImage = true
            val outcome = GalleryReader.read(context, uri)
            readingImage = false
            when (outcome) {
                is GalleryReader.Outcome.Found -> {
                    confirm()
                    vm.open(outcome.scan)
                }
                GalleryReader.Outcome.NotFound -> snackbar.showSnackbar(context.getString(R.string.gallery_none))
                GalleryReader.Outcome.Failed -> snackbar.showSnackbar(context.getString(R.string.gallery_error))
            }
        }
    }
    val openGallery = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val cameraReady = hasCamera && granted && !cameraFailed
        if (cameraReady) {
            CameraPreview(
                trigger = trigger,
                zoom = zoom,
                torch = torch,
                onTorchAvailable = { torchAvailable = it },
                onScan = ::onScan,
                onError = { cameraFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
            Viewfinder(Modifier.fillMaxSize())
        } else {
            NoCamera(
                message = when {
                    !hasCamera -> stringResource(R.string.no_camera)
                    cameraFailed -> stringResource(R.string.camera_error)
                    else -> null
                },
                blocked = blocked,
                onGrant = { asked = true; permissionLauncher.launch(Manifest.permission.CAMERA) },
                onSettings = { context.openAppSettings() },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Üst çubuk
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            IconButton(onClick = onHistory) {
                Icon(KodokurIcons.History, stringResource(R.string.action_history), tint = Color.White)
            }
            IconButton(onClick = onAbout) {
                Icon(Icons.Filled.Info, stringResource(R.string.action_about), tint = Color.White)
            }
        }

        // Alt çubuk
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnackbarHost(snackbar)
            if (cameraReady) {
                Text(
                    if (vm.batchMode) pluralStringResource(R.plurals.batch_count, vm.batchCount, vm.batchCount)
                    else stringResource(R.string.scan_hint),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(onClick = openGallery, enabled = !readingImage) {
                    if (readingImage) {
                        val reading = stringResource(R.string.gallery_reading)
                        CircularProgressIndicator(
                            Modifier.size(22.dp).semantics { contentDescription = reading },
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(KodokurIcons.Image, stringResource(R.string.action_gallery), tint = Color.White)
                    }
                }
                if (cameraReady && torchAvailable) {
                    RoundButton(onClick = { torch = !torch }) {
                        Icon(
                            if (torch) KodokurIcons.FlashOff else KodokurIcons.FlashOn,
                            stringResource(if (torch) R.string.action_torch_off else R.string.action_torch_on),
                            tint = if (torch) Amber else Color.White,
                        )
                    }
                }
                if (cameraReady && zoom.maxRatio > 1.05f) {
                    val label = String.format(java.util.Locale.ROOT, "%.1f×", zoom.ratio)
                    val description = stringResource(R.string.zoom_level, label)
                    RoundButton(
                        onClick = { zoom.cycle() },
                        modifier = Modifier.semantics { contentDescription = description },
                    ) {
                        Text(label, color = if (zoom.ratio > 1.05f) Amber else Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (cameraReady) {
                    FilterChip(
                        selected = vm.batchMode,
                        onClick = {
                            vm.toggleBatch()
                            trigger.armed = true
                            if (vm.batchMode) {
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    snackbar.showSnackbar(context.getString(R.string.batch_mode_on))
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.batch_mode)) },
                        leadingIcon = { Icon(KodokurIcons.Stack, null, Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = 0.45f),
                            labelColor = Color.White,
                            iconColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

/** Seri taramada bildirimde gösterilen kısa ad: kitapta tireli ISBN. */
private fun batchLabel(scan: Scan): String = when (val c = ContentParser.parse(scan)) {
    is Content.Book -> c.isbn.hyphenated13
    else -> scan.text.take(40)
}

@Composable
private fun RoundButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** Karartılmış çerçeve ve kehribar köşeler. Yalnız yol gösterir; çözücü tüm kareye bakar. */
@Composable
private fun Viewfinder(modifier: Modifier) {
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val w = size.width * 0.8f
        val h = minOf(w * 0.62f, size.height * 0.4f)
        val left = (size.width - w) / 2
        val top = (size.height - h) / 2 - size.height * 0.04f
        val radius = 20.dp.toPx()
        drawRect(Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            Color.Transparent, Offset(left, top), Size(w, h), CornerRadius(radius), blendMode = BlendMode.Clear,
        )
        val arm = 28.dp.toPx()
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        // Her köşede bir çeyrek yay ve iki kısa kol.
        val corners = listOf(
            Offset(left, top) to 180f,
            Offset(left + w - 2 * radius, top) to 270f,
            Offset(left + w - 2 * radius, top + h - 2 * radius) to 0f,
            Offset(left, top + h - 2 * radius) to 90f,
        )
        for ((origin, start) in corners) {
            drawArc(Amber, start, 90f, false, origin, Size(2 * radius, 2 * radius), style = stroke)
        }
        val s = stroke.width
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(Amber, Offset(x1, y1), Offset(x2, y2), s, StrokeCap.Round)
        line(left + radius, top, left + radius + arm, top)
        line(left, top + radius, left, top + radius + arm)
        line(left + w - radius, top, left + w - radius - arm, top)
        line(left + w, top + radius, left + w, top + radius + arm)
        line(left + w - radius, top + h, left + w - radius - arm, top + h)
        line(left + w, top + h - radius, left + w, top + h - radius - arm)
        line(left + radius, top + h, left + radius + arm, top + h)
        line(left, top + h - radius, left, top + h - radius - arm)
    }
}

/**
 * Kamera yoksa ya da izin verilmediyse. İzin ikinci kez de reddedildiyse sistem
 * bir daha sormaz; o zaman ayarlara yönlendirilir. Galeriden okuma her durumda açık.
 */
@Composable
private fun NoCamera(
    message: String?,
    blocked: Boolean,
    onGrant: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.widthIn(max = 420.dp).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(KodokurIcons.Barcode, null, tint = Amber, modifier = Modifier.size(56.dp))
        if (message != null) {
            Text(message, color = Color.White, textAlign = TextAlign.Center)
            return@Column
        }
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(stringResource(R.string.permission_body), color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        if (blocked) {
            Button(onClick = onSettings) { Text(stringResource(R.string.permission_settings)) }
        } else {
            Button(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
        }
        Text(
            stringResource(R.string.permission_gallery_hint),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
