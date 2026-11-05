function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    const overlay = document.getElementById('mobileMenuOverlay');
    if (menu.classList.contains('-translate-x-full')) {
        menu.classList.remove('-translate-x-full');
        menu.classList.add('translate-x-0');
        overlay.classList.remove('hidden');
    } else {
        menu.classList.add('-translate-x-full');
        menu.classList.remove('translate-x-0');
        overlay.classList.add('hidden');
    }
}

function openLogoutModal() {
    document.getElementById('logoutModal').classList.remove('hidden');
}

function closeLogoutModal() {
    document.getElementById('logoutModal').classList.add('hidden');
}

document.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-action]');
    if (!trigger) return;

    switch (trigger.dataset.action) {
        case 'menu-toggle':
            toggleMobileMenu();
            break;
        case 'menu-logout-open':
            openLogoutModal();
            break;
        case 'menu-logout-close':
            closeLogoutModal();
            break;
    }
});
