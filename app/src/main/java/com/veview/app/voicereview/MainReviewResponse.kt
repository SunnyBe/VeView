package com.veview.app.voicereview

import com.veview.veviewsdk.annotations.FieldDescription
import com.veview.veviewsdk.annotations.JsonType

@Suppress("MaximumLineLength", "MaxLineLength")
data class MainReviewResponse(
    @FieldDescription(
        text = "A concise, one-sentence summary of the entire review.",
        type = JsonType.STRING
    )
    val summary: String,

    @FieldDescription(
        text = "An integer rating from 1 to 5, where 1 is very negative and 5 is very positive, based on the user's sentiment.",
        type = JsonType.INTEGER
    )
    val rating: Int,

    @FieldDescription(
        text = "A list of specific positive points. If none are found, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val pros: List<String>?,

    @FieldDescription(
        text = "A list of specific negative points. If none are found, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val cons: List<String>?,

    @FieldDescription(
        text = "A short, polite, and professional reply suggestion. If no reply is appropriate, this field can be omitted.",
        type = JsonType.STRING
    )
    val suggestedReply: String?,

    @FieldDescription(
        text = "A list of keywords that categorize the review (e.g., 'service', 'price'). If no specific keywords can be extracted, return an empty list [].",
        type = JsonType.LIST_OF_STRINGS
    )
    val keywords: List<String>?,

    @FieldDescription(
        text = "Extract one or two exact, impactful quotes from the review. If no standout quotes are found, this field can be omitted.",
        type = JsonType.LIST_OF_STRINGS
    )
    val quotedHighlights: List<String>?
)
