// Intersection Observer로 스크롤 애니메이션
const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
        }
    });
}, {
    threshold: 0.1
});

// 모든 fade-in 요소 관찰
document.querySelectorAll('.fade-in').forEach(el => {
    observer.observe(el);
});

// Hero 섹션은 즉시 표시
document.querySelectorAll('.bg-gradient-to-br .fade-in').forEach(el => {
    el.classList.add('visible');
});
