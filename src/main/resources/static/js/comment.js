// 댓글 작성
async function submitComment() {
    const input = document.getElementById('commentInput');
    const content = input.value.trim();

    if (!content) {
        Alert.show('댓글 내용을 입력해주세요.');
        return;
    }

    if (content.length > 500) {
        Alert.show('댓글은 최대 500자까지 입력 가능합니다.');
        return;
    }

    const submitBtn = document.getElementById('submitComment');
    const reviewId = submitBtn.dataset.reviewId;

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = '작성 중...';

        await API.post('/api/comments', {
            reviewId: reviewId,
            content: content,
            parentId: null,
        });

        Alert.success('댓글이 작성되었습니다.');
        window.location.reload();
    } catch (error) {
        Alert.error(error.message || '댓글 작성에 실패했습니다.');
        submitBtn.disabled = false;
        submitBtn.textContent = '댓글 작성';
    }
}

// 답글 폼 토글
function toggleReplyForm(button) {
    const commentId = button.dataset.commentId;
    const replyForm = document.getElementById(`replyForm-${commentId}`);

    if (replyForm) {
        replyForm.classList.toggle('hidden');
        if (!replyForm.classList.contains('hidden')) {
            const textarea = replyForm.querySelector('textarea');
            textarea?.focus();
        }
    }
}

// 답글 작성
async function submitReply(button) {
    const parentId = button.dataset.commentId;
    const reviewId = button.dataset.reviewId;
    const textarea = document.getElementById(`replyInput-${parentId}`);
    const content = textarea.value.trim();

    if (!content) {
        Alert.show('답글 내용을 입력해주세요.');
        return;
    }

    if (content.length > 500) {
        Alert.show('답글은 최대 500자까지 입력 가능합니다.');
        return;
    }

    try {
        button.disabled = true;
        button.textContent = '작성 중...';

        await API.post('/api/comments', {
            reviewId: reviewId,
            content: content,
            parentId: parentId,
        });

        Alert.success('답글이 작성되었습니다.');
        window.location.reload();
    } catch (error) {
        Alert.error(error.message || '답글 작성에 실패했습니다.');
        button.disabled = false;
        button.textContent = '답글 작성';
    }
}

// 댓글 삭제
async function deleteComment(button) {
    if (!Alert.confirm('정말로 이 댓글을 삭제하시겠습니까?')) {
        return;
    }

    const commentId = button.dataset.commentId;

    try {
        button.disabled = true;

        await API.delete(`/api/comments/${commentId}`);

        Alert.success('댓글이 삭제되었습니다.');
        window.location.reload();
    } catch (error) {
        Alert.error(error.message || '댓글 삭제에 실패했습니다.');
        button.disabled = false;
    }
}

// 글자 수 카운터
document.addEventListener('DOMContentLoaded', () => {
    const commentInput = document.getElementById('commentInput');
    const charCount = document.getElementById('charCount');

    if (commentInput && charCount) {
        commentInput.addEventListener('input', () => {
            const length = commentInput.value.length;
            charCount.textContent = `${length} / 500`;

            if (length >= 500) {
                charCount.classList.add('text-red-500');
                charCount.classList.remove('text-stone-500');
            } else {
                charCount.classList.remove('text-red-500');
                charCount.classList.add('text-stone-500');
            }
        });
    }
});
