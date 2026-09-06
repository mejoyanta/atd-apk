package com.barabd.facekiosk.ml

import android.content.Context
import android.graphics.Bitmap
import com.barabd.facekiosk.data.EmbeddingCodec
import com.barabd.facekiosk.data.PersonEntity
import com.barabd.facekiosk.settings.KioskPreferences

sealed class PipelineResult {
    data class Matched(val person: PersonEntity, val similarity: Float, val liveness: Float) : PipelineResult()
    data class Rejected(val reason: RejectReason, val detail: String = "") : PipelineResult()
}

enum class RejectReason {
    NO_FACE,
    MULTI_FACE,
    QUALITY,
    SPOOF,
    UNKNOWN,
    MODELS_MISSING
}

class FaceMatcher {
    fun bestMatch(
        embedding: FloatArray,
        people: List<PersonEntity>,
        threshold: Float = KioskPreferences.IDENTIFY_THRESHOLD
    ): Pair<PersonEntity, Float>? {
        var best: PersonEntity? = null
        var bestScore = -1f
        for (person in people) {
            val stored = EmbeddingCodec.fromBytes(person.embedding)
            if (stored.size != embedding.size) continue
            val score = EmbeddingExtractor.cosineSimilarity(embedding, stored)
            if (score > bestScore) {
                bestScore = score
                best = person
            }
        }
        val person = best ?: return null
        if (bestScore < threshold) return null
        return person to bestScore
    }

    /** Average multiple enroll embeddings then L2-normalize. */
    fun averageEmbeddings(list: List<FloatArray>): FloatArray {
        require(list.isNotEmpty())
        val size = list.first().size
        val acc = FloatArray(size)
        for (e in list) {
            require(e.size == size)
            for (i in 0 until size) acc[i] += e[i]
        }
        val n = list.size.toFloat()
        for (i in 0 until size) acc[i] /= n
        var sum = 0f
        for (v in acc) sum += v * v
        val norm = kotlin.math.sqrt(sum)
        if (norm > 1e-6f) {
            for (i in 0 until size) acc[i] /= norm
        }
        return acc
    }
}

class FacePipeline(context: Context) : AutoCloseable {
    val detector = FaceDetectorHelper()
    val embeddingExtractor = EmbeddingExtractor(context)
    val livenessChecker = LivenessChecker(context)
    val matcher = FaceMatcher()

    val modelsReady: Boolean get() = embeddingExtractor.available

    suspend fun identify(
        bitmap: Bitmap,
        people: List<PersonEntity>,
        requireLiveness: Boolean = true
    ): PipelineResult {
        if (!embeddingExtractor.available) {
            return PipelineResult.Rejected(RejectReason.MODELS_MISSING)
        }
        val faces = detector.detect(bitmap)
        if (faces.isEmpty()) return PipelineResult.Rejected(RejectReason.NO_FACE)
        if (faces.size > 1) return PipelineResult.Rejected(RejectReason.MULTI_FACE, "One face only")

        val face = faces.first()
        val quality = QualityGate.evaluate(face, bitmap.width, bitmap.height)
        if (!quality.ok) return PipelineResult.Rejected(RejectReason.QUALITY, quality.reason)

        val box = QualityGate.expandBox(face.boundingBox, bitmap.width, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, box.left, box.top, box.width(), box.height())

        val liveScore = livenessChecker.score(cropped)
        if (requireLiveness && livenessChecker.available &&
            liveScore < KioskPreferences.LIVENESS_THRESHOLD
        ) {
            return PipelineResult.Rejected(RejectReason.SPOOF, "liveness=$liveScore")
        }

        val embedding = embeddingExtractor.extract(cropped)
        val match = matcher.bestMatch(embedding, people) ?: return PipelineResult.Rejected(RejectReason.UNKNOWN)
        return PipelineResult.Matched(match.first, match.second, liveScore)
    }

    suspend fun extractForEnroll(bitmap: Bitmap): Result<FloatArray> {
        if (!embeddingExtractor.available) {
            return Result.failure(IllegalStateException("models missing"))
        }
        val faces = detector.detect(bitmap)
        if (faces.size != 1) {
            return Result.failure(IllegalStateException(if (faces.isEmpty()) "No face" else "One face only"))
        }
        val face = faces.first()
        val quality = QualityGate.evaluate(face, bitmap.width, bitmap.height)
        if (!quality.ok) return Result.failure(IllegalStateException(quality.reason))

        val box = QualityGate.expandBox(face.boundingBox, bitmap.width, bitmap.height)
        val cropped = Bitmap.createBitmap(bitmap, box.left, box.top, box.width(), box.height())

        if (livenessChecker.available) {
            val live = livenessChecker.score(cropped)
            if (live < KioskPreferences.LIVENESS_THRESHOLD) {
                return Result.failure(IllegalStateException("Liveness failed"))
            }
        }
        return Result.success(embeddingExtractor.extract(cropped))
    }

    override fun close() {
        detector.close()
        embeddingExtractor.close()
        livenessChecker.close()
    }
}
