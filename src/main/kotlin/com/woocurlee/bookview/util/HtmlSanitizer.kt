package com.woocurlee.bookview.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

object HtmlSanitizer {
    private val safelist =
        Safelist
            .relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addAttributes("p", "class")
            .addAttributes("span", "class")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")

    /**
     * 사용자 입력 HTML을 안전하게 새니타이즈
     * - 위험한 태그/속성 제거 (script, onclick 등)
     * - 허용된 태그만 유지 (p, strong, em, ul, ol, li, a, h1-h6 등)
     */
    fun sanitize(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return Jsoup.clean(html, "", safelist, Document.OutputSettings().prettyPrint(false))
    }

    /**
     * 일반 텍스트로 변환 (모든 HTML 제거)
     */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return Jsoup.parse(html).text()
    }
}
