package com.veview.veview_sdk.domain.model

/**
 * Provides context about the product or service being reviewed.
 *
 * @param field An identifier for what is being reviewed (e.g., product SKU, service ID).
 * @param boughtOrUsedService Indicates whether the user has purchased or used the item/service.
 */
data class ReviewContext(
    val reviewId: String,
    val field: String,
    val boughtOrUsedService: Boolean? = false
)