// GA4 초기화. 측정 ID는 스크립트 태그의 data-ga-id로 전달된다.
(function () {
    const gaId = document.currentScript.dataset.gaId;

    window.dataLayer = window.dataLayer || [];
    window.gtag = function () {
        window.dataLayer.push(arguments);
    };

    window.gtag('js', new Date());
    window.gtag('config', gaId);
})();
