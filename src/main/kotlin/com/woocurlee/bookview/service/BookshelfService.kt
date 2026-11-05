package com.woocurlee.bookview.service

import com.woocurlee.bookview.domain.BookshelfEntry
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.dto.AddBookshelfEntryRequest
import com.woocurlee.bookview.dto.MonthlyReadCount
import com.woocurlee.bookview.dto.ReadingStats
import com.woocurlee.bookview.dto.UpdateBookshelfEntryRequest
import com.woocurlee.bookview.dto.YearlyReadCount
import com.woocurlee.bookview.repository.BookshelfRepository
import com.woocurlee.bookview.util.HtmlSanitizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.springframework.stereotype.Service

/** 통계의 '이번 달' 기준 시간대. 서버 시간대와 무관하게 한국 날짜로 판단한다. */
private val STATS_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

/** 월별 추이에 보여줄 개월 수 */
private const val TREND_MONTHS = 12L

/**
 * 유저 책장 데이터.
 * @property reading 완독일이 없는 "읽는 중" 책 (시작일 최신순)
 * @property finished 완독한 책 (완독일 최신순)
 */
data class BookshelfData(
    val reading: List<BookshelfEntry>,
    val finished: List<BookshelfEntry>,
)

@Service
class BookshelfService(
    private val bookshelfRepository: BookshelfRepository,
) {
    /** 유저의 책장을 읽는 중/완독으로 분리해 반환한다. */
    fun getBookshelf(userId: String): BookshelfData {
        val entries = bookshelfRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Status.ACTIVE)
        val (finished, reading) = entries.partition { it.finishedAt != null }
        return BookshelfData(
            reading = reading.sortedByDescending { it.startedAt ?: LocalDate.MIN },
            finished = finished.sortedByDescending { it.finishedAt },
        )
    }

    /** 유저의 독서 통계를 조회한다. */
    fun getReadingStats(userId: String): ReadingStats = calculateReadingStats(getBookshelf(userId).finished)

    /** 완독 항목을 완독일 기준으로 월별·연도별 집계한다. */
    fun calculateReadingStats(
        finished: List<BookshelfEntry>,
        today: LocalDate = LocalDate.now(STATS_ZONE),
    ): ReadingStats {
        val finishedDates = finished.mapNotNull { it.finishedAt }
        val countByMonth = finishedDates.groupingBy { YearMonth.from(it) }.eachCount()
        val countByYear = finishedDates.groupingBy { it.year }.eachCount()
        val thisMonth = YearMonth.from(today)

        return ReadingStats(
            thisMonthCount = countByMonth[thisMonth] ?: 0,
            thisYearCount = countByYear[today.year] ?: 0,
            totalCount = finishedDates.size,
            monthly =
                (TREND_MONTHS - 1 downTo 0).map { monthsAgo ->
                    val month = thisMonth.minusMonths(monthsAgo)
                    MonthlyReadCount(month.year, month.monthValue, countByMonth[month] ?: 0)
                },
            yearly =
                countByYear
                    .toSortedMap(reverseOrder())
                    .map { (year, count) -> YearlyReadCount(year, count) },
        )
    }

    /** 책장에 책을 추가한다. */
    fun addEntry(
        userId: String,
        request: AddBookshelfEntryRequest,
    ): BookshelfEntry {
        validateDates(request.startedAt, request.finishedAt)
        checkNoDuplicate(userId, request.bookIsbn, request.finishedAt, excludeId = null)
        val entry =
            BookshelfEntry(
                userId = userId,
                bookTitle = HtmlSanitizer.toPlainText(request.bookTitle),
                bookAuthor = HtmlSanitizer.toPlainText(request.bookAuthor),
                bookThumbnail = request.bookThumbnail,
                bookIsbn = request.bookIsbn,
                startedAt = request.startedAt,
                finishedAt = request.finishedAt,
            )
        return bookshelfRepository.save(entry)
    }

    /**
     * 책장 항목의 날짜를 수정한다.
     * @return 항목이 없거나 소유자가 아니면 null
     */
    fun updateEntry(
        id: String,
        userId: String,
        request: UpdateBookshelfEntryRequest,
    ): BookshelfEntry? {
        val entry = findOwnedActiveEntry(id, userId) ?: return null
        validateDates(request.startedAt, request.finishedAt)
        checkNoDuplicate(userId, entry.bookIsbn, request.finishedAt, excludeId = entry.id)
        val updated =
            entry.copy(
                startedAt = request.startedAt,
                finishedAt = request.finishedAt,
                updatedAt = LocalDateTime.now(),
            )
        return bookshelfRepository.save(updated)
    }

    /**
     * 책장 항목을 소프트 삭제한다.
     * @return 항목이 없거나 소유자가 아니면 false
     */
    fun deleteEntry(
        id: String,
        userId: String,
    ): Boolean {
        val entry = findOwnedActiveEntry(id, userId) ?: return false
        bookshelfRepository.save(entry.copy(status = Status.DELETED, updatedAt = LocalDateTime.now()))
        return true
    }

    private fun findOwnedActiveEntry(
        id: String,
        userId: String,
    ): BookshelfEntry? {
        val entry = bookshelfRepository.findById(id).orElse(null) ?: return null
        if (entry.status != Status.ACTIVE || entry.userId != userId) return null
        return entry
    }

    /** 완독일은 시작일보다 앞설 수 없다. */
    private fun validateDates(
        startedAt: LocalDate?,
        finishedAt: LocalDate?,
    ) {
        if (startedAt != null && finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw IllegalArgumentException("완독일은 시작일보다 앞설 수 없습니다.")
        }
    }

    /**
     * 같은 책의 중복 등록을 막는다.
     * - 완독(finishedAt 있음): 같은 완독일 중복만 차단, 다른 날짜는 N회차 재독으로 허용
     * - 읽는 중(finishedAt 없음): 같은 책의 '읽는 중' 항목이 이미 있으면 차단
     * isbn이 없으면 책을 식별할 수 없으므로 검사하지 않는다.
     */
    private fun checkNoDuplicate(
        userId: String,
        bookIsbn: String,
        finishedAt: LocalDate?,
        excludeId: String?,
    ) {
        if (bookIsbn.isBlank()) return
        val existing =
            if (finishedAt == null) {
                bookshelfRepository.findByUserIdAndBookIsbnAndFinishedAtIsNullAndStatus(userId, bookIsbn, Status.ACTIVE)
            } else {
                bookshelfRepository.findByUserIdAndBookIsbnAndFinishedAtAndStatus(
                    userId,
                    bookIsbn,
                    finishedAt,
                    Status.ACTIVE,
                )
            }
        if (existing.any { it.id != excludeId }) {
            val message =
                if (finishedAt == null) {
                    "이미 '읽는 중'으로 등록된 책입니다."
                } else {
                    "이미 같은 날짜에 완독한 책으로 등록되어 있습니다."
                }
            throw IllegalArgumentException(message)
        }
    }
}
