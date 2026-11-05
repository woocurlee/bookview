// 리뷰 작성 페이지 스크립트
// 에디터(TipTap)는 write-review.html의 모듈 스크립트에서 초기화되며 window.reviewEditor로 접근한다.

// 편집 모드 데이터 (작성 모드에서는 #reviewEditData가 렌더링되지 않음)
const editData = document.getElementById('reviewEditData');
const isEditMode = editData !== null;

let selectedRating = 0;
let selectedBook = null;
let currentBookPage = 1;
let currentBookQuery = '';
let isLoadingBooks = false;
let hasMoreBooks = true;

// 명언 글자수 카운터 설정
function setupQuoteCounter() {
    const quoteInput = document.getElementById('quote');
    const quoteLength = document.getElementById('quoteLength');

    quoteInput.addEventListener('input', function() {
        quoteLength.textContent = this.value.length;
    });
}

// 모달 스크롤 이벤트 설정 (무한 스크롤)
function setupInfiniteScroll() {
    const bookModal = document.getElementById('bookModal');
    const modalContent = bookModal?.querySelector('.overflow-y-auto');

    if (modalContent) {
        modalContent.addEventListener('scroll', function() {
            if (this.scrollHeight - this.scrollTop - this.clientHeight < 100) {
                searchBooks(true);
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', function() {
    setupQuoteCounter();
    setupRatingStars();
    setupActionButtons();

    // 편집 모드인 경우
    if (isEditMode) {
        // 별점 초기화
        selectedRating = Number(editData.dataset.rating);
        updateStars(selectedRating);

        // 에디터 초기 컨텐츠 설정 (TipTap, 모듈 스크립트에서 window.reviewEditor 준비됨)
        const initialContent = editData.dataset.content;
        if (initialContent && window.reviewEditor) {
            window.reviewEditor.setContent(initialContent);
        }

        // 명언 글자수 초기화
        const quoteInput = document.getElementById('quote');
        if (quoteInput) {
            document.getElementById('quoteLength').textContent = quoteInput.value.length;
        }
    } else {
        // 작성 모드인 경우
        setupInfiniteScroll();

        // 엔터키로 검색
        const bookSearchInput = document.getElementById('bookSearch');
        if (bookSearchInput) {
            bookSearchInput.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') {
                    searchBooks();
                }
            });
        }

        // 모달 바깥 클릭 설정
        Modal.setupOutsideClick('bookModal');
    }
});

// 별점 이벤트 (mouseenter는 버블링되지 않아 컨테이너에서 mouseover로 위임)
function setupRatingStars() {
    const ratingEl = document.getElementById('rating');

    ratingEl.addEventListener('click', (event) => {
        const star = event.target.closest('.star');
        if (star) setRating(Number(star.dataset.rating));
    });

    ratingEl.addEventListener('mouseover', (event) => {
        const star = event.target.closest('.star');
        if (star) hoverRating(Number(star.dataset.rating));
    });

    ratingEl.addEventListener('mouseleave', resetHover);
}

function setupActionButtons() {
    document.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-action]');
        if (!trigger) return;

        switch (trigger.dataset.action) {
            case 'go-back':
                history.back();
                break;
            case 'review-submit':
                submitReview();
                break;
            case 'book-modal-open':
                openBookModal();
                break;
            case 'book-modal-close':
                closeBookModal();
                break;
            case 'book-search':
                searchBooks();
                break;
        }
    });
}

// 별점 설정
function setRating(rating) {
    selectedRating = rating;
    updateStars(rating);
}

// 별점 호버
function hoverRating(rating) {
    updateStars(rating);
}

// 호버 해제시 원래 선택된 별점으로 복원
function resetHover() {
    updateStars(selectedRating);
}

// 별 업데이트 공통 함수
function updateStars(rating) {
    const stars = document.querySelectorAll('.star');
    stars.forEach((star, index) => {
        if (index < rating) {
            star.classList.add('active');
            star.classList.remove('text-gray-300');
        } else {
            star.classList.remove('active');
            star.classList.add('text-gray-300');
        }
    });
}

// 모달 열기
function openBookModal() {
    Modal.open('bookModal');
    currentBookPage = 1;
    currentBookQuery = '';
    hasMoreBooks = true;
    document.getElementById('bookList').innerHTML = '';
}

// 모달 닫기
function closeBookModal() {
    Modal.close('bookModal');
    // 검색어 및 결과 초기화
    document.getElementById('bookSearch').value = '';
    document.getElementById('bookList').innerHTML = '';
    currentBookPage = 1;
    currentBookQuery = '';
    hasMoreBooks = true;
    isLoadingBooks = false;
}

// 책 검색 (무한 스크롤 지원)
async function searchBooks(loadMore = false) {
    const query = document.getElementById('bookSearch').value.trim();
    if (!query) {
        Alert.error('검색어를 입력하세요');
        return;
    }

    // 새로운 검색이면 초기화
    if (!loadMore || query !== currentBookQuery) {
        currentBookQuery = query;
        currentBookPage = 1;
        hasMoreBooks = true;
        document.getElementById('bookList').innerHTML = '';
    }

    if (isLoadingBooks || !hasMoreBooks) return;

    const bookList = document.getElementById('bookList');
    const loading = document.getElementById('bookLoading');

    isLoadingBooks = true;
    loading.classList.remove('hidden');

    try {
        const data = await API.get(`/api/external/books/search?query=${encodeURIComponent(currentBookQuery)}&page=${currentBookPage}&size=10`);

        loading.classList.add('hidden');
        isLoadingBooks = false;

        if (data.documents && data.documents.length > 0) {
            data.documents.forEach(book => {
                const bookItem = document.createElement('div');
                bookItem.className = 'flex flex-col p-4 border-2 border-stone-200 rounded-lg cursor-pointer transition-all hover:border-amber-500 hover:bg-amber-50';
                bookItem.onclick = () => selectBook(book);
                bookItem.innerHTML = `
                    ${coverImgTag(book.thumbnail, 'w-full h-48 object-contain rounded mb-3', book.title)}
                    <div class="flex-1">
                        <div class="font-semibold mb-1 line-clamp-2">${book.title}</div>
                        <div class="text-gray-600 text-sm line-clamp-1">${book.authors.join(', ')}</div>
                    </div>
                `;
                bookList.appendChild(bookItem);
            });

            currentBookPage++;

            if (data.meta && data.meta.is_end) {
                hasMoreBooks = false;
                const endMessage = document.createElement('p');
                endMessage.className = 'text-center text-gray-500 py-4 col-span-full';
                endMessage.textContent = '모든 검색 결과를 불러왔습니다.';
                bookList.appendChild(endMessage);
            }
        } else {
            if (currentBookPage === 1) {
                bookList.innerHTML = '<p class="text-gray-500 col-span-full text-center">검색 결과가 없습니다.</p>';
            }
            hasMoreBooks = false;
        }
    } catch (error) {
        loading.classList.add('hidden');
        isLoadingBooks = false;
        if (currentBookPage === 1) {
            bookList.innerHTML = '<p class="text-red-500">검색에 실패했습니다. 다시 시도해주세요.</p>';
        }
    }
}

// 책 선택
function selectBook(book) {
    selectedBook = {
        title: book.title,
        author: book.authors.join(', '),
        isbn: book.isbn,
        thumbnail: book.thumbnail
    };

    document.getElementById('selectedBookTitle').textContent = selectedBook.title;
    document.getElementById('selectedBookAuthor').textContent = selectedBook.author;
    document.getElementById('selectedBookThumbnail').src = selectedBook.thumbnail;
    document.getElementById('selectedBook').classList.remove('hidden');
    document.getElementById('selectedBook').classList.add('flex');

    closeBookModal();
}

// 리뷰 제출 (작성 및 수정 통합)
async function submitReview() {
    const title = document.getElementById('reviewTitle').value.trim();
    const quote = document.getElementById('quote').value.trim();
    const content = window.reviewEditor.getHTML();

    // 유효성 검사
    if (!title) {
        Alert.error('리뷰 제목을 입력하세요');
        return;
    }

    // 작성 모드: 책 선택 필수
    if (!isEditMode && !selectedBook) {
        Alert.error('책을 선택하세요');
        return;
    }

    if (!Validator.rating(selectedRating)) {
        Alert.error('별점을 선택하세요');
        return;
    }

    if (!quote) {
        Alert.error('인상 깊은 문장을 입력하세요');
        return;
    }

    if (!Validator.textLength(quote, 5, 250)) {
        Alert.error('인상 깊은 문장은 5~250자로 입력하세요');
        return;
    }

    // 빈 내용 검증: 실제 텍스트 기준으로 판단
    if (window.reviewEditor.getText().trim().length === 0) {
        Alert.error('리뷰 내용을 입력하세요');
        return;
    }

    try {
        if (isEditMode) {
            // 편집 모드: PUT 요청
            await API.put(`/api/reviews/${editData.dataset.reviewId}`, {
                title: title,
                rating: selectedRating,
                quote: quote,
                content: content
            });

            Alert.success('리뷰가 수정되었습니다!');
            window.location.replace(`/r/${editData.dataset.reviewNo}`);
        } else {
            // 작성 모드: POST 요청
            await API.post('/api/reviews', {
                title: title,
                bookTitle: selectedBook.title,
                bookAuthor: selectedBook.author,
                bookIsbn: selectedBook.isbn,
                bookThumbnail: selectedBook.thumbnail,
                rating: selectedRating,
                quote: quote,
                content: content
            });

            Alert.success('리뷰가 등록되었습니다!');
            window.location.replace('/');
        }
    } catch (error) {
        Alert.error(isEditMode ? '리뷰 수정에 실패했습니다.' : '리뷰 등록에 실패했습니다.');
    }
}
