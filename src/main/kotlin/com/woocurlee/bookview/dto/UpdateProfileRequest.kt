package com.woocurlee.bookview.dto

data class UpdateProfileRequest(
    val nickname: String,
    val agreedToTerms: Boolean = false,
    val termsVersion: String = "1.0",
)
