package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.BookshelfEntry
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.dto.MonthlyReadCount
import com.woocurlee.bookview.dto.YearlyReadCount
import com.woocurlee.bookview.repository.BookshelfRepository
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BookshelfServiceTest {
    @Mock lateinit var bookshelfRepository: BookshelfRepository

    private lateinit var bookshelfService: BookshelfService

    private val today = LocalDate.of(2026, 3, 15)

    @BeforeEach
    fun setUp() {
        bookshelfService = BookshelfService(bookshelfRepository)
    }

    @Test
    fun `이번 달과 올해 권수는 완독일 기준으로 집계한다`() {
        val stats =
            bookshelfService.calculateReadingStats(
                listOf(
                    entry("2026-03-01"),
                    entry("2026-03-31"),
                    entry("2026-02-28"),
                    entry("2025-03-15"),
                ),
                today,
            )

        assertThat(stats.thisMonthCount).isEqualTo(2)
        assertThat(stats.thisYearCount).isEqualTo(3)
        assertThat(stats.totalCount).isEqualTo(4)
    }

    @Test
    fun `월별 추이는 연도를 넘어 최근 12개월을 오래된 달부터 0을 채워 반환한다`() {
        val stats =
            bookshelfService.calculateReadingStats(
                listOf(entry("2025-04-30"), entry("2025-12-01"), entry("2026-03-02"), entry("2025-03-31")),
                today,
            )

        assertThat(stats.monthly).hasSize(12)
        assertThat(stats.monthly.first()).isEqualTo(MonthlyReadCount(2025, 4, 1))
        assertThat(stats.monthly).contains(MonthlyReadCount(2025, 12, 1), MonthlyReadCount(2026, 1, 0))
        assertThat(stats.monthly.last()).isEqualTo(MonthlyReadCount(2026, 3, 1))
    }

    @Test
    fun `연도별 합계는 기록이 있는 연도만 최신순으로 반환한다`() {
        val stats =
            bookshelfService.calculateReadingStats(
                listOf(entry("2023-05-01"), entry("2026-01-01"), entry("2026-02-01")),
                today,
            )

        assertThat(stats.yearly).containsExactly(YearlyReadCount(2026, 2), YearlyReadCount(2023, 1))
    }

    @Test
    fun `같은 책을 재독하면 완독 기록마다 1권으로 센다`() {
        val stats =
            bookshelfService.calculateReadingStats(
                listOf(entry("2026-03-01", isbn = "same"), entry("2026-03-10", isbn = "same")),
                today,
            )

        assertThat(stats.thisMonthCount).isEqualTo(2)
    }

    @Test
    fun `완독 기록이 없으면 12개월 모두 0이고 연도별은 비어 있다`() {
        val stats = bookshelfService.calculateReadingStats(emptyList(), today)

        assertThat(stats.totalCount).isZero()
        assertThat(stats.monthly).hasSize(12).allMatch { it.count == 0 }
        assertThat(stats.yearly).isEmpty()
    }

    @Test
    fun `유저 통계 조회는 읽는 중인 책을 제외한다`() {
        given(bookshelfRepository.findByUserIdAndStatusOrderByCreatedAtDesc("google-1", Status.ACTIVE))
            .willReturn(listOf(entry("2026-03-01"), entry(null)))

        val stats = bookshelfService.getReadingStats("google-1")

        assertThat(stats.totalCount).isEqualTo(1)
    }

    private fun entry(
        finishedAt: String?,
        isbn: String = "isbn",
    ) = BookshelfEntry(
        userId = "google-1",
        bookTitle = "책",
        bookAuthor = "저자",
        bookThumbnail = null,
        bookIsbn = isbn,
        startedAt = null,
        finishedAt = finishedAt?.let(LocalDate::parse),
    )
}
