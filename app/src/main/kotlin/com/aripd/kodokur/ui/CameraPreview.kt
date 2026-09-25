package com.aripd.kodokur.ui

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aripd.kodokur.core.BarcodeDecoder
import com.aripd.kodokur.core.LumaImage
import com.aripd.kodokur.core.Scan
import java.util.concurrent.Executors

/**
 * Çözümleyicinin tetiği. Bir kod bulununca çözümleyici kendini kapatır (aynı kod
 * saniyede on kez gelmesin); ekran sonucu işleyince yeniden kurar.
 */
class ScanTrigger {
    @Volatile
    var armed: Boolean = true
}

/**
 * Arka kamera önizlemesi ve kare çözümleme. Önizleme ekranı doldurur; çözümleyici
 * karenin tamamına bakar, vizör yalnızca kullanıcıya yol gösterir.
 */
@Composable
fun CameraPreview(
    trigger: ScanTrigger,
    torch: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onScan: (Scan) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScan by rememberUpdatedState(onScan)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
    var camera by remember { mutableStateOf<Camera?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setTapToFocus { camera }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val analyzer = ScanAnalyzer(trigger) { scan ->
            main.execute { currentOnScan(scan) }
        }
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get().also { provider = it }
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    // 720p: kitap barkodu uzaktan da okunur, kare yine hızlı çözülür.
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, analyzer)
                p.unbindAll()
                val bound = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                camera = bound
                currentOnTorchAvailable(bound.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                Log.w("Kodokur", "Kamera başlatılamadı", e)
                currentOnError()
            }
        }, main)
        onDispose {
            disposed = true
            provider?.unbindAll()
            camera = null
            executor.shutdown()
        }
    }

    LaunchedEffect(camera, torch) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(torch) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Dokunulan noktaya odaklanır ve pozlamayı oraya göre ayarlar. */
@SuppressLint("ClickableViewAccessibility")
private fun PreviewView.setTapToFocus(camera: () -> Camera?) {
    setOnTouchListener { view, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            val point = meteringPointFactory.createPoint(event.x, event.y)
            camera()?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
            view.performClick()
        }
        true
    }
}

/**
 * Her kareden Y (parlaklık) düzlemini alır, dik konuma çevirir ve çözer. Çizgili
 * barkodlar için yan çevrilmiş deneme her iki karede bir yapılır: dik tutulan
 * kitap da okunur, kare süresi de ikiye katlanmaz.
 */
private class ScanAnalyzer(
    private val trigger: ScanTrigger,
    private val onHit: (Scan) -> Unit,
) : ImageAnalysis.Analyzer {

    private val decoder = BarcodeDecoder()
    private var buffer = ByteArray(0)
    private var frame = 0

    override fun analyze(image: ImageProxy) {
        image.use {
            if (!trigger.armed) return
            val plane = image.planes[0]
            val data = plane.buffer
            data.rewind()
            val size = data.remaining()
            if (buffer.size < size) buffer = ByteArray(size)
            data.get(buffer, 0, size)
            val luma = LumaImage.fromPlane(buffer, image.width, image.height, plane.rowStride)
                .rotated(image.imageInfo.rotationDegrees)
            frame++
            val scan = decoder.decode(luma, alsoSideways = frame % 2 == 0) ?: return
            if (!trigger.armed) return
            trigger.armed = false
            onHit(scan)
        }
    }
}
