(function () {
    function ensureRoot() {
        let root = document.getElementById('appDialogRoot');
        if (!root) {
            root = document.createElement('div');
            root.id = 'appDialogRoot';
            document.body.appendChild(root);
        }
        return root;
    }

    function open(options) {
        return new Promise(resolve => {
            const root = ensureRoot();
            const isPrompt = options.type === 'prompt';
            const isConfirm = options.type === 'confirm' || isPrompt;
            root.innerHTML = `
                <div class="app-dialog-backdrop">
                    <section class="app-dialog" role="dialog" aria-modal="true" aria-labelledby="appDialogTitle">
                        <button class="app-dialog-close" type="button" aria-label="关闭">×</button>
                        <div class="app-dialog-icon ${options.type || 'info'}">${isConfirm ? '?' : 'i'}</div>
                        <div class="app-dialog-body">
                            <h3 id="appDialogTitle">${options.title || '温馨提示'}</h3>
                            <p>${String(options.message || '').replace(/</g, '&lt;').replace(/\n/g, '<br>')}</p>
                            ${isPrompt ? `<input class="app-dialog-input" value="${String(options.defaultValue || '').replace(/"/g, '&quot;')}" autocomplete="off">` : ''}
                        </div>
                        <div class="app-dialog-actions">
                            ${isConfirm ? '<button class="btn btn-secondary app-dialog-cancel" type="button">取消</button>' : ''}
                            <button class="btn btn-primary app-dialog-ok" type="button">${options.okText || '确定'}</button>
                        </div>
                    </section>
                </div>`;

            const backdrop = root.firstElementChild;
            const input = root.querySelector('.app-dialog-input');
            const finish = value => {
                document.removeEventListener('keydown', onKeydown);
                root.innerHTML = '';
                resolve(value);
            };
            const onKeydown = event => {
                if (event.key === 'Escape') finish(isConfirm ? null : true);
                if (event.key === 'Enter') finish(isPrompt ? input.value : true);
            };
            root.querySelector('.app-dialog-ok').addEventListener('click', () => finish(isPrompt ? input.value : true));
            root.querySelector('.app-dialog-close').addEventListener('click', () => finish(isConfirm ? null : true));
            root.querySelector('.app-dialog-cancel')?.addEventListener('click', () => finish(null));
            backdrop.addEventListener('click', event => {
                if (event.target === backdrop) finish(isConfirm ? null : true);
            });
            document.addEventListener('keydown', onKeydown);
            requestAnimationFrame(() => {
                backdrop.classList.add('show');
                (input || root.querySelector('.app-dialog-ok')).focus();
                if (input) input.select();
            });
        });
    }

    window.AppDialog = {
        alert(message, title) { return open({ type: 'info', message, title }); },
        confirm(message, title) { return open({ type: 'confirm', message, title: title || '请确认' }); },
        prompt(message, defaultValue, title) { return open({ type: 'prompt', message, defaultValue, title: title || '请输入信息' }); }
    };

    window.alert = function (message) {
        return window.AppDialog.alert(message);
    };
})();
