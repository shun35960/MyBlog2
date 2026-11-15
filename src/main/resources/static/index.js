// Material Design 3 のインタラクション (テーマ切り替え / App Bar / リップル / 確認ダイアログ)

// ----------------------------------------------------------------------
// 確認ダイアログ: window.confirm() の代わりに使う
// レイアウトの <dialog id="confirmDialog"> をモーダルで開き、確定したかを Promise<boolean> で返す。
// Esc キーやモバイルの戻る操作で閉じた場合はキャンセル扱いになる
// ----------------------------------------------------------------------
function confirmDialog(title, { detail = '', confirmLabel = '削除' } = {}) {
    const dialog = document.getElementById('confirmDialog');
    if (!dialog) {
        return Promise.resolve(window.confirm(detail ? `${title}\n${detail}` : title));
    }

    dialog.querySelector('#confirmDialogTitle').textContent = title;
    const detailElement = dialog.querySelector('#confirmDialogDetail');
    detailElement.textContent = detail;
    detailElement.hidden = !detail;
    dialog.querySelector('button[value="confirm"]').textContent = confirmLabel;

    return new Promise((resolve) => {
        dialog.addEventListener(
            'close',
            () => {
                const confirmed = dialog.returnValue === 'confirm';
                // Esc で閉じたときは returnValue が設定されないので、前回の値が残らないよう戻す
                dialog.returnValue = '';
                resolve(confirmed);
            },
            { once: true }
        );
        dialog.showModal();
    });
}

document.addEventListener('DOMContentLoaded', function () {
    // ------------------------------------------------------------------
    // data-confirm 付きフォーム: 送信前に確認ダイアログを出す
    //   data-confirm="見出し" data-confirm-detail="補足 (任意)"
    // ------------------------------------------------------------------
    document.addEventListener('submit', async (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.dataset.confirm) {
            return;
        }
        if (form.dataset.confirmed === 'true') {
            delete form.dataset.confirmed;
            return;
        }

        event.preventDefault();
        const submitter = event.submitter;
        const confirmed = await confirmDialog(form.dataset.confirm, { detail: form.dataset.confirmDetail || '' });
        if (confirmed) {
            form.dataset.confirmed = 'true';
            form.requestSubmit(submitter);
        }
    });

    // ------------------------------------------------------------------
    // テーマ (ライト / ダーク)
    // 配色は <meta name="color-scheme"> の content で決まる。
    //   "light dark" = システム設定に従う (localStorage に値なし)
    //   "light" / "dark" = ユーザーによる上書き (localStorage に保存)
    // トグルは「システム設定」と「その反対」の 2 状態を行き来する
    // ------------------------------------------------------------------
    const THEME_KEY = 'theme';
    const colorSchemeMeta = document.querySelector('meta[name="color-scheme"]');
    const systemDark = window.matchMedia('(prefers-color-scheme: dark)');

    const applyStoredScheme = (stored) => {
        colorSchemeMeta.content = stored === 'light' || stored === 'dark' ? stored : 'light dark';
    };

    const darkModeToggle = document.getElementById('darkModeToggle');
    if (colorSchemeMeta && darkModeToggle) {
        darkModeToggle.addEventListener('click', function () {
            const systemScheme = systemDark.matches ? 'dark' : 'light';
            const renderedScheme = colorSchemeMeta.content === 'light dark' ? systemScheme : colorSchemeMeta.content;
            const targetScheme = renderedScheme === 'dark' ? 'light' : 'dark';

            // システム設定と同じ配色に戻すときは上書きを解除する (値を保存すると以後のシステム変更に追従しなくなる)
            const stored = targetScheme === systemScheme ? null : targetScheme;
            applyStoredScheme(stored);
            try {
                if (stored === null) {
                    localStorage.removeItem(THEME_KEY);
                } else {
                    localStorage.setItem(THEME_KEY, stored);
                }
            } catch (e) {
                /* localStorage が使えない環境でも、このページの表示には反映する */
            }
        });

        // 別タブで切り替えた場合も揃える
        window.addEventListener('storage', (event) => {
            if (event.key === THEME_KEY || event.key === null) {
                applyStoredScheme(event.newValue);
            }
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
});
