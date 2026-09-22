// 약관 전체보기
function toggleTerms(type) {
    const modal = document.getElementById('termsModal');
    const title = document.getElementById('modalTitle');
    const content = document.getElementById('modalContent');

    if (type === 'terms') {
        title.textContent = '이용약관';
        content.innerHTML = getTermsContent();
    } else {
        title.textContent = '개인정보 처리방침';
        content.innerHTML = getPrivacyContent();
    }

    modal.classList.remove('hidden');
}

function closeModal() {
    document.getElementById('termsModal').classList.add('hidden');
}

// 이용약관 전체 내용
function getTermsContent() {
    return `
        <h3 class="font-bold text-lg mb-4">BookView 이용약관</h3>

        <h4 class="font-semibold mt-4 mb-2">제1조 (목적)</h4>
        <p class="mb-3">이 약관은 BookView(이하 "서비스")의 이용과 관련하여 회사와 이용자의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.</p>

        <h4 class="font-semibold mt-4 mb-2">제2조 (정의)</h4>
        <p class="mb-2">1. "서비스"란 BookView에서 제공하는 책 리뷰 작성 및 공유 서비스를 말합니다.</p>
        <p class="mb-2">2. "회원"이란 서비스에 접속하여 이 약관에 따라 서비스를 이용하는 자를 말합니다.</p>
        <p class="mb-3">3. "콘텐츠"란 회원이 서비스에 게시한 리뷰, 텍스트, 이미지 등을 말합니다.</p>

        <h4 class="font-semibold mt-4 mb-2">제3조 (약관의 효력 및 변경)</h4>
        <p class="mb-2">1. 이 약관은 서비스를 이용하고자 하는 모든 회원에 대하여 그 효력을 발생합니다.</p>
        <p class="mb-3">2. 회사는 필요한 경우 관련 법령을 위배하지 않는 범위에서 이 약관을 변경할 수 있으며, 변경된 약관은 서비스 내 공지사항을 통해 공지합니다.</p>

        <h4 class="font-semibold mt-4 mb-2">제4조 (회원가입)</h4>
        <p class="mb-2">1. 회원가입은 Google 계정을 통한 OAuth 인증으로 이루어집니다.</p>
        <p class="mb-2">2. 회원은 닉네임 설정 시 이 약관 및 개인정보 처리방침에 동의해야 합니다.</p>
        <p class="mb-3">3. 회원은 등록한 정보에 변경사항이 있을 경우 즉시 수정해야 합니다.</p>

        <h4 class="font-semibold mt-4 mb-2">제5조 (회원 탈퇴 및 자격 상실)</h4>
        <p class="mb-2">1. 회원은 언제든지 서비스 탈퇴를 요청할 수 있습니다.</p>
        <p class="mb-3">2. 회사는 회원이 다음 각 호의 사유에 해당하는 경우 회원 자격을 제한 또는 정지시킬 수 있습니다.</p>
        <p class="mb-2 ml-4">- 타인의 정보를 도용한 경우</p>
        <p class="mb-2 ml-4">- 서비스 운영을 고의로 방해한 경우</p>
        <p class="mb-3 ml-4">- 기타 관련 법령이나 회사가 정한 이용조건에 위배되는 경우</p>

        <p class="mt-6 text-right text-sm text-gray-500">시행일: 2026년 2월 1일</p>
    `;
}

// 개인정보 처리방침 전체 내용
function getPrivacyContent() {
    return `
        <h3 class="font-bold text-lg mb-4">개인정보 처리방침</h3>

        <p class="mb-4">BookView(이하 "회사")는 「개인정보 보호법」에 따라 이용자의 개인정보 보호 및 권익을 보호하고 개인정보와 관련한 이용자의 고충을 원활하게 처리할 수 있도록 다음과 같은 처리방침을 두고 있습니다.</p>

        <h4 class="font-semibold mt-4 mb-2">1. 개인정보의 수집 및 이용 목적</h4>
        <p class="mb-2">회사는 다음의 목적을 위하여 개인정보를 처리합니다.</p>
        <p class="mb-2 ml-4"><strong>가. 회원 가입 및 관리</strong></p>
        <p class="mb-2 ml-4">회원 가입의사 확인, 회원제 서비스 제공에 따른 본인 식별·인증</p>
        <p class="mb-3 ml-4"><strong>나. 서비스 제공</strong></p>
        <p class="mb-3 ml-4">책 리뷰 작성 및 조회, 맞춤형 서비스 제공</p>

        <h4 class="font-semibold mt-4 mb-2">2. 수집하는 개인정보 항목</h4>
        <p class="mb-2 ml-4">- Google OAuth: Google ID, 이메일 주소</p>
        <p class="mb-3 ml-4">- 사용자 직접 입력: 닉네임, 리뷰 내용</p>

        <h4 class="font-semibold mt-4 mb-2">3. 개인정보의 보유 기간</h4>
        <p class="mb-3 ml-4">회원 탈퇴 시까지</p>

        <p class="mt-6 text-right text-sm text-gray-500">시행일: 2026년 2월 1일</p>
    `;
}

// 폼 제출
async function submitNickname(event) {
    event.preventDefault();

    const nickname = document.getElementById('nickname').value.trim();
    const agreeTerms = document.getElementById('agreeTerms').checked;

    if (!agreeTerms) {
        alert('약관에 동의해주세요.');
        return;
    }

    try {
        const response = await fetch('/api/users/profile', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                nickname: nickname,
                agreedToTerms: true
            })
        });

        if (response.ok) {
            window.location.href = '/';
        } else {
            const error = await response.json();
            alert(error.message || '닉네임 설정에 실패했습니다.');
        }
    } catch (error) {
        alert('오류가 발생했습니다. 다시 시도해주세요.');
    }
}

// 모달 외부 클릭 시 닫기
document.getElementById('termsModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeModal();
    }
});

document.getElementById('nicknameForm').addEventListener('submit', submitNickname);

document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    switch (trigger.dataset.action) {
        case 'terms-show':
            toggleTerms(trigger.dataset.termsType);
            break;
        case 'terms-close':
            closeModal();
            break;
    }
});
