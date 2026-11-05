package com.woocurlee.bookview.dto

/** 특정 월의 완독 권수 */
data class MonthlyReadCount(
    val year: Int,
    val month: Int, // 1-12
    val count: Int,
)

/** 특정 연도의 완독 권수 */
data class YearlyReadCount(
    val year: Int,
    val count: Int,
)

/**
 * 독서 통계. 재독은 완독 기록마다 1권으로 센다.
 * @property monthly 최근 12개월 추이 (오래된 달 → 이번 달, 완독이 없는 달은 0)
 * @property yearly 완독 기록이 있는 연도별 합계 (최신 연도순)
 */
data class ReadingStats(
    val thisMonthCount: Int,
    val thisYearCount: Int,
    val totalCount: Int,
    val monthly: List<MonthlyReadCount>,
    val yearly: List<YearlyReadCount>,
)
