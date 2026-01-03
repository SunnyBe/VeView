package com.veview.veviewsdk.domain.model

/**
 * Provides context about the product or service being reviewed.
 *
 * @param reviewId A unique identifier for the review.
 * @param contextLabel An identifier for what is being reviewed (e.g., product SKU, Restaurant).
 * @param boughtOrUsedService Indicates whether the user has purchased or used the item/service.
 */
data class ReviewContext(
    val reviewId: String,
    val contextLabel: String? = "General",
    val boughtOrUsedService: Boolean? = false
)
