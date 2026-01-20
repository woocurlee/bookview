// 리뷰 상세 페이지 스크립트

async function deleteReview() {
    if (!Alert.confirm('정말로 이 리뷰를 삭제하시겠습니까?')) {
        return;
    }

    try {
        await API.delete(`/api/reviews/${reviewId}`);
        Alert.success('리뷰가 삭제되었습니다.');
        window.location.href = '/';
    } catch (error) {
        console.log(error);
        Alert.error('리뷰 삭제에 실패했습니다.');
    }
}

function editReview(reviewNo) {
    window.location.href = `/r/${reviewNo}/edit`;
}
