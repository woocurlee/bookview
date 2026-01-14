// Quill Editor 초기화
const quill = new Quill('#editor', {
    theme: 'snow',
    placeholder: '책을 읽으며 느낀 점을 자유롭게 작성해보세요...',
    modules: {
        toolbar: [
            [{ 'header': [1, 2, 3, false] }],
            ['bold', 'italic', 'underline', 'strike'],
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            ['blockquote', 'code-block'],
            ['link'],
            ['clean']
        ]
    }
});

let selectedRating = 0;
let selectedBook = null;
let currentBookPage = 1;
let currentBookQuery = '';
let isLoadingBooks = false;
let hasMoreBooks = true;

document.addEventListener('DOMContentLoaded', function() {
    // 명언 글자수 카운터
    document.getElementById('quote').addEventListener('input', function() {
        document.getElementById('quoteLength').textContent = this.value.length;
    });

    // 엔터키로 검색
    document.getElementById('bookSearch').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchBooks();
        }
    });

    // 모달 스크롤 이벤트 - 무한 스크롤
    const bookModal = document.getElementById('bookModal');
    const modalContent = bookModal?.querySelector('.overflow-y-auto');

    if (modalContent) {
        modalContent.addEventListener('scroll', function() {
            if (this.scrollHeight - this.scrollTop - this.clientHeight < 100) {
                searchBooks(true);
            }
        });
    }
});

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
    document.getElementById('bookModal').classList.remove('hidden');
    currentBookPage = 1;
    currentBookQuery = '';
    hasMoreBooks = true;
    document.getElementById('bookList').innerHTML = '';
}

// 모달 닫기
function closeBookModal() {
    document.getElementById('bookModal').classList.add('hidden');
    // 검색어 및 결과 초기화
    document.getElementById('bookSearch').value = '';
    document.getElementById('bookList').innerHTML = '';
    currentBookPage = 1;
    currentBookQuery = '';
    hasMoreBooks = true;
    isLoadingBooks = false;
}

// 모달 바깥 클릭 시 닫기
function handleModalClick(event) {
    if (event.target.id === 'bookModal') {
        closeBookModal();
    }
}

// 책 검색 (무한 스크롤 지원)
async function searchBooks(loadMore = false) {
    const query = document.getElementById('bookSearch').value;
    if (!query.trim()) {
        alert('검색어를 입력하세요');
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
        const response = await fetch(`/api/external/books/search?query=${encodeURIComponent(currentBookQuery)}&page=${currentBookPage}&size=10`);
        const data = await response.json();

        loading.classList.add('hidden');
        isLoadingBooks = false;

        if (data.documents && data.documents.length > 0) {
            data.documents.forEach(book => {
                const bookItem = document.createElement('div');
                bookItem.className = 'flex flex-col p-4 border-2 border-gray-200 rounded-lg cursor-pointer transition-all hover:border-green-500 hover:bg-gray-50';
                bookItem.onclick = () => selectBook(book);
                bookItem.innerHTML = `
                    <img src="${book.thumbnail}" alt="${book.title}" class="w-full h-48 object-contain rounded mb-3">
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
        console.error('검색 오류:', error);
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

// 리뷰 제출
async function submitReview() {
    const title = document.getElementById('reviewTitle').value;
    const quote = document.getElementById('quote').value;
    const content = quill.root.innerHTML;

    if (!title.trim()) {
        alert('리뷰 제목을 입력하세요');
        return;
    }

    if (!selectedBook) {
        alert('책을 선택하세요');
        return;
    }

    if (selectedRating === 0) {
        alert('별점을 선택하세요');
        return;
    }

    if (!quote.trim()) {
        alert('명언을 입력하세요');
        return;
    }

    if (quote.trim().length < 5 || quote.trim().length > 100) {
        alert('명언은 5~100자로 입력하세요');
        return;
    }

    if (!content.trim() || content === '<p><br></p>') {
        alert('리뷰 내용을 입력하세요');
        return;
    }

    try {
        const response = await fetch('/api/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                title: title,
                bookTitle: selectedBook.title,
                bookAuthor: selectedBook.author,
                bookIsbn: selectedBook.isbn,
                bookThumbnail: selectedBook.thumbnail,
                rating: selectedRating,
                quote: quote,
                content: content
            })
        });

        if (response.ok) {
            alert('리뷰가 등록되었습니다!');
            window.location.href = '/';
        } else {
            alert('리뷰 등록에 실패했습니다.');
        }
    } catch (error) {
        console.error('제출 오류:', error);
        alert('리뷰 등록에 실패했습니다.');
    }
}
