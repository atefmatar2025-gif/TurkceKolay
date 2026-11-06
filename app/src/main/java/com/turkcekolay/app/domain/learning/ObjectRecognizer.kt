package com.turkcekolay.app.domain.learning

/**
 * Future feature «تعلم من حولك» (learn from your surroundings).
 *
 * A replaceable image-recognition provider (on-device ML Kit, a cloud vision API, …).
 * The UI must ask the learner to confirm a detection before saving it, and camera
 * permission must be requested only when the learner opens this feature.
 * Not wired into the app yet: the core learning experience comes first (see docs/ARCHITECTURE.md).
 */
interface ObjectRecognizer {
    suspend fun recognize(jpegBytes: ByteArray): List<RecognizedObject>
}

data class RecognizedObject(
    /** Label in English as returned by most providers, used to look up local vocabulary. */
    val label: String,
    val confidence: Float,
    /** Matching vocabulary id in the local catalog, when one exists. */
    val vocabularyId: String? = null,
)
