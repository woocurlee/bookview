async function toggleLike(buttonElement) {
    const reviewNo = buttonElement.dataset.reviewNo;
    const isLoggedIn = buttonElement.dataset.loggedIn === 'true';
    const isAuthor = buttonElement.dataset.isAuthor === 'true';

    if (!isLoggedIn) {
        Alert.show('로그인이 필요합니다.');
        return;
    }

    if (isAuthor) {
        Alert.show('본인의 리뷰에는 좋아요를 누를 수 없습니다.');
        return;
    }

    try {
        const data = await API.post(`/api/reviews/${reviewNo}/like`);

        const liked = data.liked;
        const svg = buttonElement.querySelector('svg');
        const countEl = buttonElement.querySelector('#likeCount') || buttonElement.querySelector('.like-count');

        buttonElement.dataset.liked = liked;

        if (liked) {
            buttonElement.classList.remove('text-gray-400');
            buttonElement.classList.add('text-red-500');
            svg.classList.add('fill-current');
        } else {
            buttonElement.classList.remove('text-red-500');
            buttonElement.classList.add('text-gray-400');
            svg.classList.remove('fill-current');
        }

        if (countEl) {
            countEl.textContent = data.likeCount;
        }
    } catch (error) {
        console.error('좋아요 처리 실패:', error);
    }
}
