// 마이페이지 스크립트

function openEditModal() {
    Modal.open('editModal');
}

function closeEditModal() {
    Modal.close('editModal');
}

async function saveProfile() {
    const nickname = document.getElementById('editNickname').value.trim();

    // 닉네임 검증
    const validation = Validator.nickname(nickname);
    if (!validation.valid) {
        Alert.error(validation.errors[0]);
        return;
    }

    try {
        await API.put('/api/users/profile', { nickname });
        Alert.success('프로필이 수정되었습니다!');
        window.location.href = '/u/' + nickname;
    } catch (error) {
        Alert.error(error.message || '프로필 수정에 실패했습니다.');
    }
}

// 모달 바깥 클릭 이벤트 설정
document.addEventListener('DOMContentLoaded', () => {
    Modal.setupOutsideClick('editModal');
});
