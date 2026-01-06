package com.veview.veviewsdk.domain.model

import com.squareup.moshi.JsonClass
import java.io.File

/**
 * Represents the structured data extracted from a successfully processed voice review.
 * All properties are immutable (`val`) to ensure thread safety and predictability.
 *
 * This class is annotated with [JsonClass] to enable Moshi's codegen, which avoids
 * runtime reflection and makes it compatible with ProGuard.
 *
 * @param transcript The raw text transcribed from the user's audio.
 * @param summary A concise summary of the review.
 * @param estimatedRating A rating from 1-5, estimated from the review content.
 * @param pros A list of positive points mentioned in the review.
 * @param cons A list of negative points mentioned in the review.
 * @param audioFile The local file containing the recorded audio.
 */
@JsonClass(generateAdapter = true)
data class VoiceReview(
    val transcript: String,
    val summary: String,
    val estimatedRating: Int,
    val pros: List<String>,
    val cons: List<String>,
    val audioFile: File?
)
