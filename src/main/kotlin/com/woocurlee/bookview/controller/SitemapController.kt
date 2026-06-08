package com.woocurlee.bookview.controller

import com.woocurlee.bookview.service.ReviewService
import com.woocurlee.bookview.service.UserService
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SitemapController(
    private val reviewService: ReviewService,
    private val userService: UserService,
    @Value("\${app.base-url}") private val baseUrl: String,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val pageSize = 1000

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun sitemapIndex(): ResponseEntity<String> {
        val reviewCount = reviewService.countActiveReviews()
        val userCount = userService.countActiveUsersWithNickname()
        val reviewPages = ceil(reviewCount.toDouble() / pageSize).toInt().coerceAtLeast(1)
        val userPages = ceil(userCount.toDouble() / pageSize).toInt().coerceAtLeast(1)

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""").append("\n")

        repeat(reviewPages) { page ->
            sb.append("  <sitemap><loc>$baseUrl/sitemap-reviews-$page.xml</loc></sitemap>\n")
        }
        repeat(userPages) { page ->
            sb.append("  <sitemap><loc>$baseUrl/sitemap-users-$page.xml</loc></sitemap>\n")
        }

        sb.append("</sitemapindex>")
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(sb.toString())
    }

    @GetMapping("/sitemap-reviews-{page}.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun reviewsSitemap(
        @PathVariable page: Int,
    ): ResponseEntity<String> {
        if (page < 0) return ResponseEntity.badRequest().build()
        val reviews = reviewService.getActiveReviewsForSitemap(page, pageSize)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""").append("\n")

        reviews.content.forEach { review ->
            if (review.reviewNo != null) {
                sb.appendUrl(
                    loc = "$baseUrl/r/${review.reviewNo}",
                    lastmod = review.updatedAt.format(dateFormatter),
                    changefreq = "weekly",
                    priority = "0.8",
                )
            }
        }

        sb.append("</urlset>")
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(sb.toString())
    }

    @GetMapping("/sitemap-users-{page}.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    fun usersSitemap(
        @PathVariable page: Int,
    ): ResponseEntity<String> {
        if (page < 0) return ResponseEntity.badRequest().build()
        val users = userService.getActiveUsersForSitemap(page, pageSize)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""").append("\n")

        users.content.forEach { user ->
            sb.appendUrl(
                loc = "$baseUrl/u/${user.nickname}",
                lastmod = user.lastLoginAt.format(dateFormatter),
                changefreq = "weekly",
                priority = "0.6",
            )
        }

        sb.append("</urlset>")
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(sb.toString())
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
