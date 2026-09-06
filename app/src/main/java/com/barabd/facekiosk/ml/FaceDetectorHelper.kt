package com.barabd.facekiosk.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FaceDetectorHelper {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
    )

    suspend fun detect(bitmap: Bitmap): List<Face> = suspendCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun close() = detector.close()
}

object QualityGate {
    data class Result(val ok: Boolean, val reason: String)

    fun evaluate(face: Face, imageWidth: Int, imageHeight: Int): Result {
        val box = face.boundingBox
        val minSide = min(imageWidth, imageHeight)
        val faceSize = max(box.width(), box.height()).toFloat() / minSide
        if (faceSize < 0.18f) return Result(false, "Move closer")
        if (faceSize > 0.85f) return Result(false, "Move back")

        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        if (abs(yaw) > 20f || abs(pitch) > 20f) {
            return Result(false, "Face the camera")
        }

        // Keep box inside frame with small margin
        if (box.left < 4 || box.top < 4 ||
            box.right > imageWidth - 4 || box.bottom > imageHeight - 4
        ) {
            return Result(false, "Center your face")
        }

        return Result(true, "OK")
    }

    fun expandBox(box: Rect, imageWidth: Int, imageHeight: Int, scale: Float = 1.3f): Rect {
        val cx = box.centerX()
        val cy = box.centerY()
        val halfW = (box.width() * scale / 2f).toInt()
        val halfH = (box.height() * scale / 2f).toInt()
        return Rect(
            max(0, cx - halfW),
            max(0, cy - halfH),
            min(imageWidth, cx + halfW),
            min(imageHeight, cy + halfH)
        )
    }
}
