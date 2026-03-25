package com.woocurlee.bookview.service

import com.woocurlee.bookview.common.SequenceNames
import com.woocurlee.bookview.domain.Comment
import com.woocurlee.bookview.domain.Status
import com.woocurlee.bookview.dto.CommentResponse
import com.woocurlee.bookview.dto.CreateCommentRequest
import com.woocurlee.bookview.repository.CommentRepository
import com.woocurlee.bookview.util.HtmlSanitizer
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val sequenceService: SequenceService,
    private val reviewService: ReviewService,
    private val userService: UserService,
) {
    fun createComment(
        request: CreateCommentRequest,
        userId: String,
    ): CommentResponse {
        // 1. 리뷰 존재 여부 확인
        val review =
            reviewService.getReviewByIdInternal(request.reviewId)
                ?: throw IllegalArgumentException("존재하지 않는 리뷰입니다.")

        // 2. 내용 길이 검증
        val trimmedContent = request.content.trim()
        if (trimmedContent.isEmpty()) {
            throw IllegalArgumentException("댓글 내용을 입력해주세요.")
        }
        if (trimmedContent.length > 500) {
            throw IllegalArgumentException("댓글은 최대 500자까지 입력 가능합니다.")
        }

        // 3. 대댓글인 경우 검증
        if (request.parentId != null) {
            val parentComment =
                commentRepository.findById(request.parentId).orElse(null)
                    ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

            // 삭제된 댓글인지 확인
            if (parentComment.status == Status.DELETED) {
                throw IllegalArgumentException("삭제된 댓글에는 답글을 달 수 없습니다.")
            }

            // 1-depth만 허용 (대댓글의 대댓글 방지)
            if (parentComment.parentId != null) {
                throw IllegalArgumentException("답글에는 답글을 달 수 없습니다.")
            }

            // 부모가 같은 리뷰에 속하는지 확인
            if (parentComment.reviewId != request.reviewId) {
                throw IllegalArgumentException("잘못된 요청입니다.")
            }
        }

        // 4. 시퀀스 ID 생성
        val commentNo = sequenceService.getNextSequence(SequenceNames.COMMENT_SEQ)

        // 5. XSS 방지: HTML 제거
        val sanitizedContent = HtmlSanitizer.toPlainText(trimmedContent)

        // 6. 댓글 저장
        val comment =
            Comment(
                commentNo = commentNo,
                reviewId = request.reviewId,
                userId = userId,
                content = sanitizedContent,
                parentId = request.parentId,
                status = Status.ACTIVE,
            )
        val savedComment = commentRepository.save(comment)

        return toCommentResponse(savedComment)
    }

    fun getCommentsByReviewId(reviewId: String): List<CommentResponse> {
        // 모든 댓글 조회 (삭제된 댓글 포함)
        val allComments =
            commentRepository.findByReviewIdAndStatus(reviewId, Status.ACTIVE) +
                commentRepository.findByReviewIdAndStatus(reviewId, Status.DELETED)

        // 대댓글 맵 생성
        val repliesMap = allComments.filter { it.parentId != null }.groupBy { it.parentId }

        // 삭제된 최상위 댓글은 대댓글이 있는 경우에만 포함
        val filteredComments =
            allComments.filter { comment ->
                if (comment.status == Status.DELETED && comment.parentId == null) {
                    // 삭제된 최상위 댓글은 대댓글이 있을 때만 표시
                    repliesMap[comment.id]?.isNotEmpty() == true
                } else {
                    true
                }
            }

        return filteredComments
            .sortedBy { it.createdAt }
            .map { toCommentResponse(it) }
    }

    fun deleteComment(
        commentId: String,
        userId: String,
    ) {
        val comment =
            commentRepository.findById(commentId).orElse(null)
                ?: throw IllegalArgumentException("존재하지 않는 댓글입니다.")

        if (comment.status == Status.DELETED) {
            throw IllegalArgumentException("이미 삭제된 댓글입니다.")
        }

        if (comment.userId != userId) {
            throw IllegalArgumentException("본인의 댓글만 삭제할 수 있습니다.")
        }

        val deleted =
            comment.copy(
                status = Status.DELETED,
                updatedAt = LocalDateTime.now(),
            )
        commentRepository.save(deleted)
    }

    private fun toCommentResponse(comment: Comment): CommentResponse {
        val user = userService.findByGoogleId(comment.userId)
        return CommentResponse(
            id = comment.id!!,
            reviewId = comment.reviewId,
            userId = comment.userId,
            userNickname = user?.nickname ?: "알 수 없음",
            content = comment.content,
            parentId = comment.parentId,
            status = comment.status,
            createdAt = comment.createdAt,
        )
    }
}
