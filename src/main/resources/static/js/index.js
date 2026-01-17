let currentPage = 1;
let isLoading = false;
let hasMore = true;

function goToWriteReview() {
    window.location.href = '/write-review';
}

function searchBooks() {
    const query = document.getElementById('searchInput').value;
    if (query.trim()) {
        window.location.href = '/books?query=' + encodeURIComponent(query);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('searchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchBooks();
        }
    });
});

// 무한 스크롤
async function loadMoreReviews() {
    if (isLoading || !hasMore) return;

    isLoading = true;
    document.getElementById('loading').classList.remove('hidden');

    // 현재 스크롤 위치 저장
    const scrollHeight = document.documentElement.scrollHeight;

    try {
        const response = await fetch(`/api/reviews?page=${currentPage}&size=10`);
        const data = await response.json();

        if (data.reviews && data.reviews.length > 0) {
            const container = document.getElementById('reviewsContainer');
            data.reviews.forEach(review => {
                const reviewCard = createReviewCard(review);
                container.insertAdjacentHTML('beforeend', reviewCard);
            });
            currentPage++;
            hasMore = data.hasMore;

            // 스크롤 위치 조정
            const newScrollHeight = document.documentElement.scrollHeight;
            window.scrollTo(0, window.scrollY + (newScrollHeight - scrollHeight));
        } else {
            hasMore = false;
        }

        if (!hasMore) {
            document.getElementById('endMessage').classList.remove('hidden');
        }
    } catch (error) {
        console.error('리뷰 로딩 실패:', error);
    } finally {
        isLoading = false;
        document.getElementById('loading').classList.add('hidden');
    }
}

function createReviewCard(review) {
    const thumbnail = review.bookThumbnail
        ? `<img src="${review.bookThumbnail}" alt="책 표지" class="h-48 object-cover rounded-lg flex-shrink-0">`
        : '';

    const stars = '★'.repeat(review.rating) + '☆'.repeat(5 - review.rating);

    const quote = review.quote
        ? `<div class="review-quote">"${review.quote}"</div>`
        : '';

    return `
        <a href="/review/${review.reviewNo}" class="block no-underline">
            <div class="bg-gray-50 rounded-xl p-5 mb-5 transition-all hover:-translate-y-1 hover:shadow-lg flex gap-5 cursor-pointer">
                ${thumbnail}
                <div class="flex-1">
                    <div class="text-xl font-bold mb-2 text-gray-800">${review.title}</div>
                    <div class="text-sm text-gray-600 mb-2.5">
                        <span>${review.bookTitle}</span> -
                        <span>${review.bookAuthor}</span>
                    </div>
                    <div class="text-yellow-400 mb-2.5">${stars}</div>
                    ${quote}
                    <div class="text-gray-700 leading-relaxed mb-2.5 overflow-hidden line-clamp-3">${review.content}</div>
                    <div class="text-xs text-gray-400">${new Date(review.createdAt).toLocaleString('ko-KR')}</div>
                </div>
            </div>
        </a>
    `;
}

// 스크롤 이벤트 리스너
let scrollTimeout;
window.addEventListener('scroll', () => {
    if (scrollTimeout) {
        clearTimeout(scrollTimeout);
    }
    scrollTimeout = setTimeout(() => {
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 500) {
            loadMoreReviews();
        }
    }, 100);
});
