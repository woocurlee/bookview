package com.woocurlee.bookview.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

@ControllerAdvice
class GlobalModelAdvice(
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${app.ga-id:}") private val gaId: String,
) {
    @ModelAttribute("baseUrl")
    fun baseUrl(): String = baseUrl

    @ModelAttribute("gaId")
    fun gaId(): String? = gaId.ifEmpty { null }
}
