// 고화질 표지(비공식 fname URL) 로드 실패 시 원본 썸네일로 폴백.
// error 이벤트는 버블링되지 않으므로 캡처 단계에서 받는다.
// 이미지보다 먼저 리스너가 등록되도록 layout.html의 head에서 로드한다.
document.addEventListener(
    'error',
    (event) => {
        const img = event.target;
        if (!(img instanceof HTMLImageElement)) return;

        const fallback = img.dataset.fallback;
        if (!fallback) return;

        delete img.dataset.fallback;
        img.src = fallback;
    },
    true,
);
