// Material Design 3 のインタラクション (テーマ切り替え / App Bar / リップル)
document.addEventListener('DOMContentLoaded', function () {
    // ------------------------------------------------------------------
    // テーマ (ライト / ダーク)
    // head のインラインスクリプトが html.theme-dark-pending を付けているので、
    // ここで body.dark-mode に引き継いで一元管理する
    // ------------------------------------------------------------------
    const root = document.documentElement;
    const pending = root.classList.contains('theme-dark-pending');
    root.classList.remove('theme-dark-pending');

    if (pending) {
        document.body.classList.add('dark-mode');
    }

    const darkModeToggle = document.getElementById('darkModeToggle');
    if (darkModeToggle) {
        const syncToggleLabel = () => {
            const isDark = document.body.classList.contains('dark-mode');
            darkModeToggle.setAttribute('aria-pressed', String(isDark));
            darkModeToggle.setAttribute('aria-label', isDark ? 'ライトモードに切り替え' : 'ダークモードに切り替え');
        };

        syncToggleLabel();

        darkModeToggle.addEventListener('click', function () {
            const isDark = document.body.classList.toggle('dark-mode');
            try {
                localStorage.setItem('theme', isDark ? 'dark' : 'light');
            } catch (e) {
                /* localStorage が使えない環境では保存をスキップする */
            }
            syncToggleLabel();
        });
    }

    // ------------------------------------------------------------------
    // Top app bar: スクロール時にエレベーションを付与する
    // ------------------------------------------------------------------
    const appBar = document.getElementById('appBar');
    if (appBar) {
        const updateAppBarElevation = () => {
            appBar.classList.toggle('is-scrolled', window.scrollY > 0);
        };
        updateAppBarElevation();
        window.addEventListener('scroll', updateAppBarElevation, { passive: true });
    }

    // ------------------------------------------------------------------
    // リップル (タッチフィードバック)
    // ------------------------------------------------------------------
    const RIPPLE_SELECTOR = '.btn, .card-link, .md-icon-button, .navbar-brand, .nav-link, .md-chip, .badge';

    document.addEventListener('pointerdown', function (event) {
        const target = event.target.closest(RIPPLE_SELECTOR);
        if (!target || target.hasAttribute('disabled')) {
            return;
        }

        const rect = target.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height);
        const ripple = document.createElement('span');
        ripple.className = 'md-ripple';
        ripple.style.width = size + 'px';
        ripple.style.height = size + 'px';
        ripple.style.left = event.clientX - rect.left - size / 2 + 'px';
        ripple.style.top = event.clientY - rect.top - size / 2 + 'px';
        ripple.addEventListener('animationend', () => ripple.remove());

        target.appendChild(ripple);
    });

    // ------------------------------------------------------------------
    // カードの登場アニメーションを少しずつずらす
    // ------------------------------------------------------------------
    document.querySelectorAll('.row .card').forEach((card, index) => {
        card.style.animationDelay = Math.min(index * 40, 320) + 'ms';
    });

    // Passkey functionality (jQuery)
    if (typeof $ !== 'undefined') {
        $('#createPasskey').on('click', () => createPasskey());
        $('#authenticatePasskey').on('click', () => signInWithPasskey());
    }
});
