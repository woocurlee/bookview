// 공통 유틸리티 함수들

/**
 * 별점 HTML 생성
 */
function createStarRating(rating, maxRating = 5) {
    const filled = '★'.repeat(rating);
    const empty = '☆'.repeat(maxRating - rating);
    return filled + empty;
}

/**
 * 날짜 포맷팅
 */
function formatDate(dateString) {
    return new Date(dateString).toLocaleString('ko-KR');
}

/**
 * 모달 열기/닫기 유틸리티
 */
const Modal = {
    open: (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove('hidden');
            // body 스크롤 방지
            document.body.style.overflow = 'hidden';
        }
    },

    close: (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add('hidden');
            // body 스크롤 복원
            document.body.style.overflow = '';
        }
    },

    // 모달 바깥 클릭 시 닫기 처리
    setupOutsideClick: (modalId) => {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target.id === modalId) {
                    Modal.close(modalId);
                }
            });
        }
    }
};

/**
 * API 호출 헬퍼
 */
const API = {
    async request(url, options = {}) {
        try {
            const response = await fetch(url, {
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                },
                ...options
            });

            if (!response.ok) {
                // 에러 응답도 JSON이 아닐 수 있음
                try {
                    const error = await response.json();
                    throw new Error(error.message || '요청에 실패했습니다.');
                } catch {
                    throw new Error('요청에 실패했습니다.');
                }
            }

            // DELETE 요청이나 204 No Content 응답은 본문이 없음
            if (options.method === 'DELETE' || response.status === 204) {
                return null;
            }

            return await response.json();
        } catch (error) {
            console.error('API 요청 오류:', error);
            throw error;
        }
    },

    get(url) {
        return this.request(url, { method: 'GET' });
    },

    post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    delete(url) {
        return this.request(url, { method: 'DELETE' });
    }
};

/**
 * 폼 검증 헬퍼
 */
const Validator = {
    // 닉네임 검증
    nickname(nickname) {
        const errors = [];

        if (!nickname || !nickname.trim()) {
            errors.push('닉네임을 입력하세요');
        } else if (nickname.length < 1 || nickname.length > 30) {
            errors.push('닉네임은 1~30자 사이로 입력해주세요');
        } else if (!/^[a-z0-9_.]+$/.test(nickname)) {
            errors.push('닉네임은 영어 소문자, 숫자, 밑줄(_), 마침표(.)만 사용할 수 있습니다');
        } else if (nickname.startsWith('.') || nickname.endsWith('.')) {
            errors.push('닉네임은 마침표(.)로 시작하거나 끝날 수 없습니다');
        } else if (nickname.includes('..')) {
            errors.push('마침표(..)를 연속으로 사용할 수 없습니다');
        }

        return {
            valid: errors.length === 0,
            errors
        };
    },

    // 별점 검증
    rating(rating) {
        return rating >= 1 && rating <= 5;
    },

    // 텍스트 길이 검증
    textLength(text, min, max) {
        const length = text?.trim().length || 0;
        return length >= min && length <= max;
    },

    // 리뷰 내용 검증 (Quill 에디터용)
    quillContent(content) {
        return content && content.trim() !== '' && content !== '<p><br></p>';
    }
};

/**
 * Alert 헬퍼 (추후 커스텀 알림으로 확장 가능)
 */
const Alert = {
    show(message) {
        alert(message);
    },

    error(message) {
        alert('오류: ' + message);
    },

    success(message) {
        alert(message);
    },

    confirm(message) {
        return confirm(message);
    }
};
