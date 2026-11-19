// MyBlog モダン Web 標準インタラクション
// - 確認ダイアログ (<dialog> + トップレイヤー終了アニメーション / closedby フォールバック)
// - テーマ切替 (<meta name="color-scheme"> ベース 2 状態トグル + タブ間同期)
// - ヘッダーのスクロール状態・現在ページ (aria-current="page")
// - 読書進捗バーの非対応ブラウザ向けフォールバック
// - 記事内コードブロックのコピーボタン (Invoker Commands + Async Clipboard API) & テーブル横スクロール
// - パスワード表示切替 & :user-invalid の ARIA 同期

// ----------------------------------------------------------------------
// 1. トップレイヤー終了アニメーション補助 (Firefox / Safari 向けフォールバック)
//    modern-web-guidance: animate-to-from-top-layer 準拠
// ----------------------------------------------------------------------
let supportsDisplayTransition;

function canTransitionDisplay() {
    if (supportsDisplayTransition !== undefined) return supportsDisplayTransition;
    if (!window.CSS?.supports?.('transition-behavior', 'allow-discrete') || !document.body) {
        return false;
    }
    const probe = document.createElement('div');
    probe.style.cssText = 'transition: display 1s allow-discrete !important; display: block;';
    document.body.appendChild(probe);
    getComputedStyle(probe).display;
    probe.style.display = 'none';
    supportsDisplayTransition = getComputedStyle(probe).display === 'block';
    probe.remove();
    return supportsDisplayTransition;
}

function supportsTopLayerExit() {
    return Boolean(window.CSS?.supports?.('overlay', 'auto')) && canTransitionDisplay();
}

async function closeTopLayerDialog(dialog, returnValue = '') {
    if (!dialog.open) return;
    if (!supportsTopLayerExit()) {
        dialog.setAttribute('data-closing', '');
        const animations = dialog.getAnimations({ subtree: true });
        if (animations.length > 0) {
            await Promise.race([
                Promise.allSettled(animations.map((a) => a.finished)),
                new Promise((r) => setTimeout(r, 1000)),
            ]);
        }
        if (!dialog.hasAttribute('data-closing')) return;
        dialog.removeAttribute('data-closing');
    }
    dialog.close(returnValue);
}

// ----------------------------------------------------------------------
// 2. 確認ダイアログ: window.confirm() の代わりに使う
//    レイアウトの <dialog id="confirmDialog"> をモーダルで開き、Promise<boolean> を返す
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
                // Esc で閉じたときは returnValue が設定されないので、前回の値が残らないよう必ず初期化する
                dialog.returnValue = '';
                resolve(confirmed);
            },
            { once: true }
        );
        dialog.showModal();
    });
}

// ----------------------------------------------------------------------
// 3. コードブロックのコピー機能 (Invoker Commands + Async Clipboard API)
//    modern-web-guidance: copy-button 準拠
// ----------------------------------------------------------------------
const copyTimers = new WeakMap();
let lastCopyAnnouncement = '';

function reportCopyStatus(button, ok) {
    const tip = button.nextElementSibling;
    if (!tip) return;
    clearTimeout(copyTimers.get(button));
    button.removeAttribute('data-copied');
    button.removeAttribute('data-failed');

    let message = ok ? 'コピーしました' : 'コピーに失敗しました';
    // VoiceOver は直前と同じ文字列を読み飛ばすため、連続時は \u00A0 で差分を作る
    if (message === lastCopyAnnouncement) {
        message += '\u00A0';
    }
    lastCopyAnnouncement = message;

    tip.textContent = '';
    void button.offsetWidth;
    button.setAttribute(ok ? 'data-copied' : 'data-failed', '');
    requestAnimationFrame(() => {
        tip.textContent = message;
    });

    copyTimers.set(
        button,
        setTimeout(() => {
            button.removeAttribute('data-copied');
            button.removeAttribute('data-failed');
            tip.textContent = '';
        }, 2000)
    );
}

async function copyCodeTarget(button, target) {
    const text = target.matches('input, textarea') ? target.value : target.innerText.trim();
    if (!text) return;
    try {
        await navigator.clipboard.writeText(text);
        reportCopyStatus(button, true);
    } catch {
        reportCopyStatus(button, false);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    // ------------------------------------------------------------------
    // ダイアログの拡張 (closedby="any" 非対応ブラウザ向けライトディスミス + 終了アニメ)
    // ------------------------------------------------------------------
    const confirmDialogEl = document.getElementById('confirmDialog');
    if (confirmDialogEl) {
        const dialogForm = confirmDialogEl.querySelector('form[method="dialog"]');
        if (dialogForm) {
            dialogForm.addEventListener('submit', (event) => {
                if (!supportsTopLayerExit()) {
                    event.preventDefault();
                    const value = event.submitter?.value || '';
                    closeTopLayerDialog(confirmDialogEl, value);
                }
            });
        }

        confirmDialogEl.addEventListener('cancel', (event) => {
            if (!supportsTopLayerExit()) {
                event.preventDefault();
                closeTopLayerDialog(confirmDialogEl, '');
            }
        });

        if (!('closedBy' in HTMLDialogElement.prototype)) {
            const isOutside = (e, dialog) => {
                const r = dialog.getBoundingClientRect();
                return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
            };
            let pressedOutside = false;
            confirmDialogEl.addEventListener('pointerdown', (e) => {
                pressedOutside = isOutside(e, confirmDialogEl);
            });
            confirmDialogEl.addEventListener('pointerup', (e) => {
                if (pressedOutside && isOutside(e, confirmDialogEl)) {
                    closeTopLayerDialog(confirmDialogEl, '');
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // data-confirm 付きフォーム: 送信前に確認ダイアログを出す
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
        const confirmed = await confirmDialog(form.dataset.confirm, {
            detail: form.dataset.confirmDetail || '',
            confirmLabel: form.dataset.confirmLabel || '削除',
        });
        if (confirmed) {
            form.dataset.confirmed = 'true';
            form.requestSubmit(submitter);
        }
    });

    // ------------------------------------------------------------------
    // テーマ (ライト / ダーク): <meta name="color-scheme"> を 2 状態で切り替え
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

            // システム設定と同じ配色に戻すときは上書きを解除する
            const stored = targetScheme === systemScheme ? null : targetScheme;
            applyStoredScheme(stored);
            try {
                if (stored === null) {
                    localStorage.removeItem(THEME_KEY);
                } else {
                    localStorage.setItem(THEME_KEY, stored);
                }
            } catch (e) {
                /* localStorage が使えない環境でも表示は反映する */
            }
        });

        window.addEventListener('storage', (event) => {
            if (event.key === THEME_KEY || event.key === null) {
                applyStoredScheme(event.newValue);
            }
        });
    }

    // ------------------------------------------------------------------
    // 現在ページのナビゲーションリンクへ aria-current="page" を付与
    // ------------------------------------------------------------------
    const pathname = window.location.pathname;
    document.querySelectorAll('.site-nav-links .nav-link').forEach((link) => {
        const match = link.dataset.navMatch;
        const isCurrent =
            (match === 'home' && (pathname === '/' || pathname.startsWith('/ViewDescription'))) ||
            (match === 'series' && pathname.startsWith('/Series')) ||
            (match === 'about' && pathname === '/about');
        if (isCurrent) {
            link.setAttribute('aria-current', 'page');
        }
    });

    // ------------------------------------------------------------------
    // ヘッダー: スクロール時に境界・背景を強調
    // ------------------------------------------------------------------
    const appBar = document.getElementById('appBar');
    if (appBar) {
        const updateAppBar = () => {
            appBar.classList.toggle('is-scrolled', window.scrollY > 8);
        };
        updateAppBar();
        window.addEventListener('scroll', updateAppBar, { passive: true });
    }

    // ------------------------------------------------------------------
    // 読書進捗バー: scroll-driven animations 非対応ブラウザ向けフォールバック
    // ------------------------------------------------------------------
    const progressEl = document.querySelector('.reading-progress');
    if (
        progressEl &&
        !CSS.supports('animation-timeline', 'scroll()') &&
        !window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
        const updateProgress = () => {
            const scrollable = document.documentElement.scrollHeight - window.innerHeight;
            const ratio = scrollable > 0 ? Math.min(Math.max(window.scrollY / scrollable, 0), 1) : 0;
            progressEl.style.transform = `scaleX(${ratio})`;
        };
        updateProgress();
        window.addEventListener('scroll', updateProgress, { passive: true });
        window.addEventListener('resize', updateProgress, { passive: true });
    }

    // ------------------------------------------------------------------
    // 記事本文のエンハンス: コードブロックに言語ラベル & コピーボタンを追加
    // ------------------------------------------------------------------
    let codeBlockIndex = 0;
    document.querySelectorAll('.article-content pre').forEach((pre) => {
        const code = pre.querySelector(':scope > code');
        if (!code || pre.closest('.code-block')) return;

        codeBlockIndex += 1;
        if (!code.id) {
            code.id = `code-snippet-${codeBlockIndex}`;
        }

        const langMatch = `${code.className} ${pre.className}`.match(/language-([a-zA-Z0-9_+-]+)/);
        const lang = code.dataset.language || pre.dataset.language || (langMatch ? langMatch[1] : '');

        const wrapper = document.createElement('div');
        wrapper.className = 'code-block copyable';

        const header = document.createElement('div');
        header.className = 'code-block-header';

        const langBadge = document.createElement('span');
        langBadge.className = 'code-lang';
        langBadge.textContent = lang ? lang.toUpperCase() : 'CODE';
        header.appendChild(langBadge);

        const copyWrap = document.createElement('span');
        copyWrap.className = 'copy';

        const copyBtn = document.createElement('button');
        copyBtn.type = 'button';
        copyBtn.className = 'copy-button';
        copyBtn.setAttribute('command', '--copy');
        copyBtn.setAttribute('commandfor', code.id);
        copyBtn.setAttribute('aria-label', lang ? `${lang}コードをコピー` : 'コードをコピー');
        copyBtn.innerHTML =
            '<svg class="icon-copy" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
            '<rect width="14" height="14" x="8" y="8" rx="2" ry="2"></rect>' +
            '<path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"></path></svg>' +
            '<svg class="icon-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
            '<path d="M20 6 9 17l-5-5"></path></svg>';

        const tip = document.createElement('span');
        tip.className = 'copy-tip';
        tip.setAttribute('role', 'status');

        copyWrap.appendChild(copyBtn);
        copyWrap.appendChild(tip);
        header.appendChild(copyWrap);

        pre.setAttribute('tabindex', '0');
        pre.parentNode.insertBefore(wrapper, pre);
        wrapper.appendChild(header);
        wrapper.appendChild(pre);
    });

    // Invoker Commands ネイティブ対応 or フォールバックのどちらか一方のみ登録
    if ('CommandEvent' in window) {
        document.addEventListener(
            'command',
            (event) => {
                if (event.command === '--copy' && event.source && event.target) {
                    copyCodeTarget(event.source, event.target);
                }
            },
            true
        );
    } else {
        document.addEventListener('click', (event) => {
            const button = event.target.closest('button[command="--copy"]');
            if (!button) return;
            const target = document.getElementById(button.getAttribute('commandfor'));
            if (target) {
                copyCodeTarget(button, target);
            }
        });
    }

    // ------------------------------------------------------------------
    // 記事本文のテーブルを横スクロール可能にラップ
    // ------------------------------------------------------------------
    document.querySelectorAll('.article-content table').forEach((table) => {
        if (table.parentElement?.classList.contains('table-scroll')) return;
        const wrap = document.createElement('div');
        wrap.className = 'table-scroll';
        wrap.setAttribute('tabindex', '0');
        table.parentNode.insertBefore(wrap, table);
        wrap.appendChild(table);
    });

    // ------------------------------------------------------------------
    // パスワード表示切替トグル ([data-toggle-password])
    // ------------------------------------------------------------------
    document.querySelectorAll('[data-toggle-password]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const input = document.getElementById(btn.dataset.togglePassword);
            if (!input) return;
            const willShow = input.type === 'password';
            input.type = willShow ? 'text' : 'password';
            btn.setAttribute('aria-pressed', String(willShow));
            btn.setAttribute('aria-label', willShow ? 'パスワードを隠す' : 'パスワードを表示');
            const label = btn.querySelector('[data-toggle-label]');
            if (label) {
                label.textContent = willShow ? '隠す' : '表示';
            }
        });
    });

    // ------------------------------------------------------------------
    // フォームバリデーションの ARIA 同期 (validate-input-after-interaction 準拠)
    // ------------------------------------------------------------------
    const formFields = document.querySelectorAll('form input[data-error], form textarea[data-error], form select[data-error]');
    if (formFields.length > 0 && CSS.supports('selector(:user-invalid)')) {
        const announcer = document.createElement('div');
        announcer.setAttribute('aria-live', 'polite');
        announcer.className = 'visually-hidden';
        document.body.append(announcer);

        let announceTimer;
        let announcedField;
        const flagged = new WeakSet();
        const announced = new WeakSet();

        const linkError = (el, shown) => {
            const errorId = el.dataset.error;
            if (!errorId) return;
            const ids = (el.getAttribute('aria-describedby') || '')
                .split(/\s+/)
                .filter((id) => id && id !== errorId);
            if (shown) ids.push(errorId);
            if (ids.length) el.setAttribute('aria-describedby', ids.join(' '));
            else el.removeAttribute('aria-describedby');
        };

        const announceError = (el) => {
            const errorEl = document.getElementById(el.dataset.error);
            if (!errorEl) return;
            const labelText = el.labels?.[0] ? `${el.labels[0].textContent.trim()}: ` : '';
            clearTimeout(announceTimer);
            announcer.textContent = '';
            announcedField = el;
            announceTimer = setTimeout(() => {
                announcer.textContent = labelText + errorEl.textContent.replace(/\s+/g, ' ').trim();
            }, 500);
        };

        const syncAria = (el, announce = false) => {
            if (!el.matches?.('input[data-error], select[data-error], textarea[data-error]')) return;
            const invalid = el.matches(':user-invalid');
            if (invalid) {
                if (announce && !announced.has(el)) {
                    announceError(el);
                    announced.add(el);
                }
                el.setAttribute('aria-invalid', 'true');
                linkError(el, true);
                flagged.add(el);
            } else if (flagged.has(el) || el.getAttribute('aria-invalid') !== 'true') {
                el.setAttribute('aria-invalid', 'false');
                linkError(el, false);
                flagged.delete(el);
                announced.delete(el);
                if (announcedField === el) {
                    clearTimeout(announceTimer);
                    announcer.textContent = '';
                }
            }
        };

        formFields.forEach((el) => syncAria(el));
        document.addEventListener('blur', (e) => syncAria(e.target, true), true);
        document.addEventListener('input', (e) => {
            if (e.target.hasAttribute?.('aria-invalid')) syncAria(e.target);
        });
        document.addEventListener('change', (e) => syncAria(e.target, true), true);
        document.addEventListener(
            'invalid',
            (e) => {
                announced.add(e.target);
                syncAria(e.target);
            },
            true
        );
    }

    // ------------------------------------------------------------------
    // カードの登場アニメーションを少しずつずらす
    // ------------------------------------------------------------------
    document.querySelectorAll('.card-grid .card').forEach((card, index) => {
        card.style.setProperty('--card-index', String(Math.min(index, 10)));
    });
});
