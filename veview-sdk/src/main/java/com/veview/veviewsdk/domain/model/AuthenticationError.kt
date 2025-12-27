package com.veview.veviewsdk.domain.model

// WIP
sealed class AuthenticationError {
    data object InvalidAPIKey : AuthenticationError()
}
