package com.woocurlee.bookview.controller

import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.repository.ReviewRepository
import com.woocurlee.bookview.repository.UserRepository
import java.time.format.DateTimeFormatter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SitemapController(
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    @Value("\${app.base-url}") private val baseUrl: String,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemap(): ResponseEntity<String> {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("\n")
        sb.append("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")
        sb.append("\n")

        // 홈
        sb.appendUrl("$baseUrl/", changefreq = "daily", priority = "1.0")

        // 리뷰 상세 페이지
        val reviews = reviewRepository.findAllByStatus(Status.ACTIVE)
        for (review in reviews) {
            if (review.reviewNo != null) {
                sb.appendUrl(
                    loc = "$baseUrl/r/${review.reviewNo}",
                    lastmod = review.updatedAt.format(dateFormatter),
                    changefreq = "weekly",
                    priority = "0.8",
                )
            }
        }

        // 유저 프로필 페이지
        val users = userRepository.findAllByIsNicknameSetAndStatus(true, Status.ACTIVE)
        for (user in users) {
            sb.appendUrl(
                loc = "$baseUrl/u/${user.nickname}",
                lastmod = user.lastLoginAt.format(dateFormatter),
                changefreq = "weekly",
                priority = "0.6",
            )
        }

        sb.append("</urlset>")
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(sb.toString())
    }

    private fun StringBuilder.appendUrl(
        loc: String,
        lastmod: String? = null,
        changefreq: String,
        priority: String,
    ) {
        append("  <url>\n")
        append("    <loc>$loc</loc>\n")
        if (lastmod != null) append("    <lastmod>$lastmod</lastmod>\n")
        append("    <changefreq>$changefreq</changefreq>\n")
        append("    <priority>$priority</priority>\n")
        append("  </url>\n")
    }
}
