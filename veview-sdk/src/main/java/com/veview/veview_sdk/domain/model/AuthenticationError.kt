package com.veview.veview_sdk.domain.model

// WIP
sealed class AuthenticationError(override val message: String?) : RuntimeException(message) {

    data object InvalidAPIKey: AuthenticationError("Invalid API key") {
        private fun readResolve(): Any = InvalidAPIKey
    }
}