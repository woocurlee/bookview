package com.woocurlee.bookview.dto

data class UpdateProfileRequest(
    val nickname: String,
    val agreedToTerms: Boolean? = null,
)
