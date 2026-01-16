package com.veview.app.voicereview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class ReviewInstructionItem(
    val id: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val detailRes: Int,
    @DrawableRes val iconRes: Int? = null
)
