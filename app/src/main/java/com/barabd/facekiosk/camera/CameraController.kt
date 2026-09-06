package com.barabd.facekiosk.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private val analyzing = AtomicBoolean(false)

    var onFrame: ((Bitmap) -> Unit)? = null
    var analyzeEnabled: Boolean = true

    suspend fun start(useFront: Boolean = true) {
        val provider = awaitProvider()
        cameraProvider = provider
        provider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(analysisExecutor) { image ->
                    processImage(image)
                }
            }

        val selector = if (useFront) {
            androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        } catch (_: Exception) {
            provider.bindToLifecycle(
                lifecycleOwner,
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    fun shutdown() {
        stop()
        analysisExecutor.shutdown()
    }

    private fun processImage(image: ImageProxy) {
        if (!analyzeEnabled || !analyzing.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            val bitmap = imageProxyToBitmap(image) ?: return
            onFrame?.invoke(bitmap)
        } finally {
            analyzing.set(false)
            image.close()
        }
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }

    companion object {
        fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
            return try {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer
                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()
                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)
                val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
                val bytes = out.toByteArray()
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                val rotation = image.imageInfo.rotationDegrees
                if (rotation != 0) {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                // Mirror front camera for natural preview-aligned crops
                val mirror = Matrix().apply { preScale(-1f, 1f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, mirror, true)
            } catch (_: Exception) {
                null
            }
        }
    }
}
