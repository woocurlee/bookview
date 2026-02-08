// 리뷰 수정 페이지 스크립트

// Quill 에디터 초기화
const quill = new Quill('#editor', {
    theme: 'snow',
    placeholder: '책을 읽고 느낀 점을 자유롭게 작성해주세요',
    formats: ['header', 'bold', 'italic', 'underline', 'strike', 'align', 'blockquote', 'code-block'],
    modules: {
        toolbar: [
            [{ 'header': [1, 2, 3, false] }],
            ['bold', 'italic', 'underline', 'strike'],

            [{ 'align': [] }],
            ['blockquote', 'code-block'],
            ['clean']
        ]
    }
});

// 별점 관리
let selectedRating = 0;

function setRating(button) {
    selectedRating = parseInt(button.dataset.rating);
    document.getElementById('rating').value = selectedRating;
    updateStarDisplay(selectedRating);
}

function updateStarDisplay(rating) {
    const stars = document.querySelectorAll('.star-btn');
    stars.forEach((star, index) => {
        if (index < rating) {
            star.classList.remove('text-gray-300');
            star.classList.add('text-yellow-400');
        } else {
            star.classList.remove('text-yellow-400');
            star.classList.add('text-gray-300');
        }
    });
}

// 명언 글자수 카운터
function setupQuoteCounter() {
    const quoteTextarea = document.getElementById('quote');
    const quoteCount = document.getElementById('quoteCount');

    function updateQuoteCount() {
        quoteCount.textContent = quoteTextarea.value.length;
    }

    updateQuoteCount();
    quoteTextarea.addEventListener('input', updateQuoteCount);
}

// 폼 제출
async function submitEditForm(reviewId, reviewNo) {
    const title = document.getElementById('title').value.trim();
    const rating = parseInt(document.getElementById('rating').value);
    const quote = document.getElementById('quote').value.trim();
    const content = quill.root.innerHTML.trim();

    // 유효성 검사
    if (!title || !quote) {
        Alert.error('모든 필수 항목을 입력해주세요.');
        return;
    }

    if (!Validator.quillContent(content)) {
        Alert.error('리뷰 내용을 입력해주세요.');
        return;
    }

    if (!Validator.rating(rating)) {
        Alert.error('별점을 선택해주세요.');
        return;
    }

    if (!Validator.textLength(quote, 5, 100)) {
        Alert.error('인상 깊은 문장은 5~100자 사이로 입력해주세요.');
        return;
    }

    try {
        await API.put(`/api/reviews/${reviewId}`, {
            title,
            rating,
            quote,
            content
        });

        Alert.success('리뷰가 수정되었습니다!');
        window.location.href = `/r/${reviewNo}`;
    } catch (error) {
        Alert.error('리뷰 수정에 실패했습니다.');
    }
}

// 페이지 초기화
function initEditReviewPage(reviewId, reviewNo, initialRating, initialContent) {
    // 별점 초기화
    selectedRating = initialRating;
    updateStarDisplay(initialRating);

    // 에디터 초기 컨텐츠 설정
    if (initialContent) {
        quill.root.innerHTML = initialContent;
    }

    // 명언 카운터 설정
    setupQuoteCounter();

    // 폼 제출 이벤트
    document.getElementById('editReviewForm').addEventListener('submit', (e) => {
        e.preventDefault();
        submitEditForm(reviewId, reviewNo);
    });
}
