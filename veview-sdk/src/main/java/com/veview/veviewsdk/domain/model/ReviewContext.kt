package com.veview.veviewsdk.domain.model

/**
 * Provides essential context about the review being recorded. This includes details about
 * what is being reviewed, locale information, and any custom metadata.
 *
 * @param referenceId A unique identifier for the review session, provided by the client.
 *   This is useful for tracking and associating the review on the client's backend.
 * @param subject An object containing details about the subject of the review.
 * @param locale An object containing language and regional information for the review.
 * @param metadata A map for passing any custom key-value pairs associated with the review.
 */
data class ReviewContext(
    val referenceId: String,
    val subject: ReviewSubject,
    val locale: LocaleContext? = null,
    val metadata: Map<String, String> = emptyMap()
) {

    /**
     * Describes the item or service being reviewed.
     *
     * @param name The name of the product, service, or place (e.g., "Grand Hotel").
     * @param type The category of the subject being reviewed.
     */
    data class ReviewSubject(
        val name: String?,
        val type: SubjectType = SubjectType.OTHER
    )

    /**
     * Specifies the locale for the review, which can influence the analysis.
     *
     * @param languageTag The IETF BCP 47 language tag (e.g., "en-US", "fr-FR").
     * @param countryCode The ISO 3166-1 alpha-2 country code (e.g., "US", "FR").
     */
    data class LocaleContext(
        val languageTag: String? = null,
        val countryCode: String? = null
    )
}

/**
 * Defines the general category of the item being reviewed.
 */
enum class SubjectType {
    RESTAURANT,
    SHOP,
    SERVICE,
    PRODUCT,
    OTHER
}
