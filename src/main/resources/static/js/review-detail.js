// 리뷰 상세 페이지 스크립트

async function deleteReview(button) {
    if (!Alert.confirm('정말로 이 리뷰를 삭제하시겠습니까?')) {
        return;
    }

    try {
        await API.delete(`/api/reviews/${button.dataset.reviewId}`);
        Alert.success('리뷰가 삭제되었습니다.');
        window.location.href = '/';
    } catch (error) {
        console.log(error);
        Alert.error('리뷰 삭제에 실패했습니다.');
    }
}

function toggleMenu() {
    document.getElementById('dropdownMenu').classList.toggle('hidden');
}

document.addEventListener('click', (event) => {
    const menuButton = document.getElementById('reviewMenuButton');
    const menu = document.getElementById('dropdownMenu');

    if (menuButton && menuButton.contains(event.target)) {
        toggleMenu();
    } else if (menu && !menu.contains(event.target)) {
        menu.classList.add('hidden');
    }

    const trigger = event.target.closest('[data-action]');
    if (trigger && trigger.dataset.action === 'review-delete') {
        deleteReview(trigger);
    }
});
