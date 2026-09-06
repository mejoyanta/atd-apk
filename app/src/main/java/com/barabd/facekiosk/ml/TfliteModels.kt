package com.barabd.facekiosk.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class EmbeddingExtractor(context: Context) : AutoCloseable {
    private val interpreter: Interpreter?
    val available: Boolean
    val embeddingSize: Int
    private val inputSize: Int

    init {
        val model = loadModel(context, MODEL_PATH)
        if (model != null) {
            val interp = Interpreter(model)
            interpreter = interp
            available = true
            val outShape = interp.getOutputTensor(0).shape()
            embeddingSize = outShape.last()
            val inShape = interp.getInputTensor(0).shape()
            inputSize = if (inShape.size >= 3) inShape[1] else 112
        } else {
            interpreter = null
            available = false
            embeddingSize = 192
            inputSize = 112
        }
    }

    fun extract(faceBitmap: Bitmap): FloatArray {
        val interp = interpreter ?: error("MobileFaceNet model missing — see docs/MODELS.md")
        val input = preprocess(faceBitmap, inputSize)
        val output = Array(1) { FloatArray(embeddingSize) }
        interp.run(input, output)
        return l2Normalize(output[0])
    }

    override fun close() {
        interpreter?.close()
    }

    companion object {
        const val MODEL_PATH = "models/mobile_face_net.tflite"

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            require(a.size == b.size)
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot // already L2-normalized
        }

        private fun l2Normalize(v: FloatArray): FloatArray {
            var sum = 0f
            for (x in v) sum += x * x
            val norm = sqrt(sum)
            if (norm < 1e-6f) return v
            return FloatArray(v.size) { v[it] / norm }
        }

        private fun preprocess(bitmap: Bitmap, size: Int): ByteBuffer {
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val buffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4).order(ByteOrder.nativeOrder())
            val pixels = IntArray(size * size)
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)
            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                buffer.putFloat(r * 2f - 1f)
                buffer.putFloat(g * 2f - 1f)
                buffer.putFloat(b * 2f - 1f)
            }
            buffer.rewind()
            return buffer
        }

        fun loadModel(context: Context, assetPath: String): MappedByteBuffer? {
            return try {
                context.assets.openFd(assetPath).use { fd ->
                    FileInputStream(fd.fileDescriptor).channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}

class LivenessChecker(context: Context) : AutoCloseable {
    private val interpreter: Interpreter?
    val available: Boolean
    private val inputSize: Int
    private val outputLen: Int
    /** When true, model output is attack/spoof score (higher = spoof). */
    private val attackScoreSemantics: Boolean

    init {
        val model = EmbeddingExtractor.loadModel(context, MODEL_PATH)
        if (model != null) {
            val interp = Interpreter(model)
            interpreter = interp
            available = true
            val inShape = interp.getInputTensor(0).shape()
            inputSize = if (inShape.size >= 3) inShape[1] else 256
            val outShape = interp.getOutputTensor(0).shape()
            outputLen = outShape.last()
            // FaceAntiSpoofing.tflite demos emit a single attack score; MiniFASNet often emits 2-class.
            attackScoreSemantics = outputLen == 1
        } else {
            interpreter = null
            available = false
            inputSize = 256
            outputLen = 1
            attackScoreSemantics = true
        }
    }

    /**
     * Returns score in [0,1] where higher means more likely a live face.
     * If model missing, returns 1f (no gate) — Identify still checks [available].
     */
    fun score(faceBitmap: Bitmap): Float {
        val interp = interpreter ?: return 1f
        val input = preprocess(faceBitmap, inputSize)
        val output = Array(1) { FloatArray(outputLen.coerceAtLeast(1)) }
        interp.run(input, output)
        val live = if (attackScoreSemantics) {
            1f - output[0][0]
        } else if (output[0].size >= 2) {
            output[0][1]
        } else {
            output[0][0]
        }
        return live.coerceIn(0f, 1f)
    }

    override fun close() {
        interpreter?.close()
    }

    companion object {
        const val MODEL_PATH = "models/minifasnet_anti_spoof.tflite"

        private fun preprocess(bitmap: Bitmap, size: Int): ByteBuffer {
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val buffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4).order(ByteOrder.nativeOrder())
            val pixels = IntArray(size * size)
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                buffer.putFloat((pixel and 0xFF) / 255f)
            }
            buffer.rewind()
            return buffer
        }
    }
}
