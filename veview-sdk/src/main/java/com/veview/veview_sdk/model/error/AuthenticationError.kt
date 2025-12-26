package com.veview.veview_sdk.model.error

// WIP
sealed class AuthenticationError(override val message: String?) : RuntimeException(message) {

    data object InvalidAPIKey: AuthenticationError("Invalid API key") {
        private fun readResolve(): Any = InvalidAPIKey
    }
}