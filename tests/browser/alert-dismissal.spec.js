const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');

// Generated on every Java fixture test run; no handwritten component or prototype fallback.
const fixtureRoot = path.resolve(__dirname, '../../simplypages/target/alert-browser-fixtures');
const generated = Object.fromEntries([
    ['alert', 'alert.html'], ['alert2', 'alert2.html'], ['persistent', 'non-dismissible.html'],
    ['nested', 'nested.html'], ['severities', 'severities.html'], ['modal', 'modal.html'],
    ['modalAlert', 'modal-alert.html'], ['nestedModal', 'nested-modal.html']
].map(([key, file]) => [key, fs.readFileSync(path.join(fixtureRoot, file), 'utf8')]));
const base = (attrs = '', content = generated.alert, persistent = true) =>
    `<button id="before" type="button">Before</button><section id="parent" ${attrs}>`
    + (persistent ? '<p>Surviving content</p><button id="inside-before" type="button">Inside before</button>' : '')
    + content + (persistent ? '<button id="inside-after" type="button">Inside after</button>' : '')
    + '</section><button id="after" type="button">After</button>';
test.beforeEach(({page}) => {
    page.alertErrors = [];
    page.on('pageerror', error => page.alertErrors.push(error.message));
});
test.afterEach(({page}) => { expect(page.alertErrors).toEqual([]); });

const hosts = '<div id="modal-host"></div><div id="transport"></div>';
const modalVariant = name => generated.modalAlert.replaceAll('alert-containing-modal', `${name}-modal`)
    .replaceAll('modal-alert', `${name}-alert`).replaceAll('modal-after', `${name}-after`);

async function fixture(page, body) {
    await page.unroute('**/alert-fixture');
    await page.route('**/alert-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <script defer src="/webjars/htmx.org/dist/htmx.min.js"></script>
        <script defer src="/js/framework.js"></script></head><body>${body}</body></html>`}));
    await page.goto('/alert-fixture');
}

async function settle(page) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
}

async function state(page) {
    return page.evaluate(() => ({
        active: document.activeElement.id || document.activeElement.tagName,
        parent: document.getElementById('parent')?.getAttribute('tabindex') ?? null,
        body: document.body.getAttribute('tabindex'),
        hidden: document.getElementById('alert')?.hidden,
        bodyFocused: document.body.matches(':focus'),
        scroll: {x: scrollX, y: scrollY}
    }));
}

async function dismiss(page, selector = '#alert > .alert-close') {
    // A real native button activation, with focus and click kept in the same task for callbacks.
    await page.locator(selector).evaluate(button => { button.focus({preventScroll: true}); button.click(); });
    await settle(page);
}

async function insert(page, html = generated.modal, selector = '#modal-host') {
    await page.locator(selector).evaluate((host, html) => host.insertAdjacentHTML('beforeend', html), html);
    await settle(page);
}

async function routeResponse(page, url, html) {
    await page.unroute(`**${url}`);
    await page.route(`**${url}`, route => route.fulfill({contentType: 'text/html', body: html}));
}

async function htmx(page, url, target, source, swap = 'innerHTML') {
    // Real HTMX transport and swap/settle processing, not synthetic lifecycle events.
    await page.evaluate(({url, target, source, swap}) => new Promise((resolve, reject) => {
        const done = () => { document.removeEventListener('htmx:afterSettle', done); resolve(); };
        document.addEventListener('htmx:afterSettle', done);
        window.htmx.ajax('GET', url, {target: document.querySelector(target),
            source: document.querySelector(source), swap}).catch(error => {
            document.removeEventListener('htmx:afterSettle', done); reject(error);
        });
    }), {url, target, source, swap});
    await settle(page);
}

async function attribute(locator, value) {
    if (value === null) await expect(locator).not.toHaveAttribute('tabindex');
    else await expect(locator).toHaveAttribute('tabindex', value);
}

async function evidence(page, testInfo, name, value, screenshot = false) {
    const output = testInfo.outputPath(`${name}.json`);
    fs.writeFileSync(output, JSON.stringify(value, null, 2));
    await testInfo.attach(name, {path: output, contentType: 'application/json'});
    if (screenshot) {
        const image = testInfo.outputPath(`${name}.png`);
        await page.screenshot({path: image, fullPage: true});
        await testInfo.attach(name + '-image', {path: image, contentType: 'image/png'});
    }
}

test('fresh Java markup, packaged asset bytes and caller semantics are preserved', async ({page, browser}, testInfo) => {
    await fixture(page, base('', generated.alert + generated.persistent));
    const button = page.getByRole('button', {name: 'Dismiss alert', exact: true});
    await expect(button).toHaveCount(1);
    await expect(button).toHaveAttribute('type', 'button');
    await expect(button).toHaveText('×');
    await expect(page.locator('#non-dismissible .alert-close')).toHaveCount(0);
    const assets = {};
    for (const [url, relative] of [['/js/framework.js', 'js/framework.js'], ['/css/framework.css', 'css/framework.css']]) {
        const response = await page.request.get(url);
        expect(response.status()).toBe(200);
        const bytes = await response.body();
        expect(bytes.equals(fs.readFileSync(path.resolve(__dirname, '../../simplypages/src/main/resources/static', relative)))).toBe(true);
        assets[url] = {status: response.status(), bytes: bytes.length};
    }
    const original = await page.locator('#non-dismissible').evaluate(element => element.outerHTML);
    await page.locator('#alert').evaluate(element => {
        element.setAttribute('role', 'status'); element.setAttribute('aria-live', 'polite');
        element.setAttribute('data-owner', 'caller'); element.style.display = 'flex';
    });
    await button.click();
    await expect(page.locator('#alert')).toHaveJSProperty('hidden', true);
    expect(await page.locator('#alert').evaluate(element => ({display: getComputedStyle(element).display,
        connected: element.isConnected, role: element.getAttribute('role'), live: element.getAttribute('aria-live'),
        owner: element.getAttribute('data-owner')}))).toEqual({display: 'none', connected: true,
        role: 'status', live: 'polite', owner: 'caller'});
    expect(await page.locator('#non-dismissible').evaluate(element => element.outerHTML)).toBe(original);
    const javadocPath = 'io/mindspice/simplypages/components/display/Alert.html';
    const javadoc = await page.request.get('/javadocs/' + javadocPath);
    expect(javadoc.status()).toBe(200);
    expect((await javadoc.body()).equals(fs.readFileSync(path.resolve(__dirname,
        '../../simplypages/target/reports/apidocs', javadocPath)))).toBe(true);
    await page.goto('/docs/reference/components-and-modules-catalog.md');
    await expect(page.locator('#alert-dismissal')).toHaveText('Alert dismissal');
    await expect(page.getByText(/including same-value writes/).first()).toBeVisible();
    await evidence(page, testInfo, 'packaged-alert-assets', {assets, javadocStatus: javadoc.status(),
        catalogUrl: page.url(), browser: browser.version(), playwright: require('playwright/package.json').version});
});

for (const key of ['Enter', 'Space']) {
    test(`native ${key} hides one Alert without submitting its form`, async ({page}) => {
        await fixture(page, `<form id="form">${base()}</form>`);
        await page.evaluate(() => { window.submits = 0; document.getElementById('form').addEventListener('submit', e => {
            e.preventDefault(); window.submits++;
        }); });
        await page.locator('#alert > .alert-close').focus();
        await page.keyboard.press(key);
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', true);
        await expect(page.locator('#parent')).toBeFocused();
        expect(await page.evaluate(() => window.submits)).toBe(0);
    });
}

test('descendant click, nested/sibling ownership and repeated activation retain independent DOM', async ({page}) => {
    await fixture(page, base('', generated.nested + generated.alert2));
    await page.locator('#inner-alert > .alert-close').evaluate(button => {
        const span = document.createElement('span'); span.append(...button.childNodes); button.append(span);
    });
    await page.locator('#inner-alert > .alert-close span').click();
    await expect(page.locator('#inner-alert')).toHaveJSProperty('hidden', true);
    await expect(page.locator('#outer-alert')).toHaveJSProperty('hidden', false);
    await expect(page.locator('#alert2')).toHaveJSProperty('hidden', false);
    await page.locator('#alert2 > .alert-close').click();
    await expect(page.locator('#alert2')).toHaveJSProperty('hidden', true);
    await page.locator('#outer-alert > .alert-close').click();
    await expect(page.locator('#outer-alert')).toHaveJSProperty('hidden', true);
    await page.locator('#after').focus();
    await page.locator('#inner-alert > .alert-close').evaluate(button => button.click());
    await page.locator('#outer-alert > .alert-close').evaluate(button => button.click());
    await expect(page.locator('#after')).toBeFocused();
    await expect(page.locator('#outer-alert, #inner-alert, #alert2')).toHaveCount(3);
});

for (const kind of ['cancelled', 'disabled', 'fieldset-disabled', 'type-submit', 'type-reset', 'type-invalid',
    'nonbutton', 'missing-owner', 'invalid-nearest', 'nearest-nondismissible']) {
    test(`invalid activation ${kind} cannot dismiss an outer owner`, async ({page}) => {
        await fixture(page, base('', generated.alert + generated.persistent));
        await page.evaluate(kind => {
            const alert = document.getElementById('alert');
            let button = alert.querySelector('.alert-close');
            if (kind === 'cancelled') button.addEventListener('click', e => e.preventDefault());
            if (kind === 'disabled') button.disabled = true;
            if (kind === 'fieldset-disabled') {
                const fieldset = document.createElement('fieldset'); fieldset.disabled = true;
                button.replaceWith(fieldset); fieldset.append(button);
            }
            if (kind.startsWith('type-')) button.setAttribute('type', kind.slice(5));
            if (kind === 'nonbutton') {
                const span = document.createElement('span'); span.className = button.className;
                span.textContent = button.textContent; button.replaceWith(span); button = span;
            }
            if (kind === 'missing-owner') alert.classList.remove('alert');
            if (kind === 'invalid-nearest') alert.classList.remove('alert-dismissible');
            if (kind === 'nearest-nondismissible') {
                const nearest = document.getElementById('non-dismissible'); alert.append(nearest); nearest.append(button);
            }
            button.id = 'invalid-close';
        }, kind);
        await page.locator('#after').focus();
        await page.locator('#invalid-close').dispatchEvent('click', {bubbles: true, cancelable: true});
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await expect(page.locator('#after')).toBeFocused();
        await attribute(page.locator('#parent'), null);
    });
}

for (const unavailable of ['hidden', 'inert', 'style="display:none"']) {
    test(`unavailable ${unavailable} ancestry rejects synthetic activation`, async ({page}) => {
        await fixture(page, base(unavailable));
        await page.locator('#after').focus();
        await page.locator('#alert > .alert-close').evaluate(button => button.click());
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await expect(page.locator('#after')).toBeFocused();
        await attribute(page.locator('#parent'), null);
    });
}

for (const visibilityAPI of [true, false]) {
    test(`closed details ancestry rejects activation with visibility API ${visibilityAPI}`, async ({page}) => {
        await fixture(page, `<details id="parent"><summary>Closed</summary><div style="display:block">${generated.alert}</div></details><button id="after">After</button>`);
        if (!visibilityAPI) await page.evaluate(() => { Element.prototype.checkVisibility = undefined; });
        await page.locator('#after').focus();
        await page.locator('#alert > .alert-close').evaluate(button => button.click());
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await expect(page.locator('#after')).toBeFocused();
    });
}

// Native eight-variant isolation is exercised after actual production dismissal.
for (const persistent of [false, true]) for (const callerRemoves of [false, true]) for (const key of ['Tab', 'Shift+Tab']) {
    test(`borrowed focus native baseline: persistent=${persistent}, callerRemoves=${callerRemoves}, ${key}`, async ({page}, testInfo) => {
        await fixture(page, base('', generated.alert, persistent));
        await dismiss(page);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), '-1');
        const retained = await state(page);
        if (callerRemoves) {
            await page.locator('#parent').evaluate(parent => parent.removeAttribute('tabindex'));
            await settle(page);
            expect((await state(page)).active).toBe('BODY');
            expect((await state(page)).bodyFocused).toBe(false);
        }
        await page.keyboard.press(key);
        await expect(page.locator(key === 'Tab' ? (persistent ? '#inside-before' : '#after') : '#before')).toBeFocused();
        await attribute(page.locator('#parent'), null);
        await page.keyboard.press(key);
        await expect(page.locator('#parent')).not.toBeFocused();
        await evidence(page, testInfo, 'native-borrow-lifetime', {retained, after: await state(page)});
    });
}

test('native focus precedes borrowing and target-only observation ends with each focus lifetime', async ({page}) => {
    await page.addInitScript(() => {
        const NativeObserver = window.MutationObserver;
        window.alertObservations = [];
        window.MutationObserver = class extends NativeObserver {
            observe(target, options) {
                if (options.attributeFilter?.includes('tabindex')) {
                    this.record = {target: target.id, options, disconnected: false};
                    window.alertObservations.push(this.record);
                }
                return super.observe(target, options);
            }
            disconnect() { if (this.record) this.record.disconnected = true; return super.disconnect(); }
        };
    });
    await fixture(page, base());
    await page.locator('#parent').evaluate(parent => {
        const nativeFocus = parent.focus.bind(parent);
        window.nativeFocusAttempts = [];
        parent.focus = options => { window.nativeFocusAttempts.push(parent.getAttribute('tabindex')); nativeFocus(options); };
    });
    for (let cycle = 0; cycle < 3; cycle++) {
        await page.locator('#alert').evaluate(alert => { alert.hidden = false; });
        await dismiss(page);
        const observations = await page.evaluate(() => window.alertObservations);
        expect(observations).toHaveLength(cycle + 1);
        expect(observations[cycle]).toEqual({target: 'parent', disconnected: false,
            options: {attributes: true, attributeFilter: ['tabindex'], attributeOldValue: true}});
        await page.locator('#after').focus();
        expect(await page.evaluate(() => window.alertObservations.every(record => record.disconnected))).toBe(true);
        await attribute(page.locator('#parent'), null);
    }
    expect(await page.evaluate(() => window.nativeFocusAttempts)).toEqual([null, '-1', null, '-1', null, '-1']);
});

for (const value of ['', 'foo', '-01', '+0', '-1', '0', '2']) {
    test(`existing caller tabindex ${JSON.stringify(value)} is never changed`, async ({page}) => {
        await fixture(page, base(`tabindex="${value}"`));
        await dismiss(page);
        await attribute(page.locator('#parent'), value);
        expect((await state(page)).active).toBe(['', 'foo'].includes(value) ? 'BODY' : 'parent');
        await page.locator('#after').focus();
        await attribute(page.locator('#parent'), value);
    });
}

for (const value of ['-1', '0', '2']) for (const key of ['Tab', 'Shift+Tab']) {
    test(`caller tabindex ${value} ${key} matches unmodified native DOM order`, async ({page}) => {
        await fixture(page, base(`tabindex="${value}"`));
        await page.evaluate(() => { document.getElementById('alert').hidden = true; document.getElementById('parent').focus(); });
        await page.keyboard.press(key);
        const native = await state(page);
        await fixture(page, base(`tabindex="${value}"`));
        await dismiss(page);
        await page.keyboard.press(key);
        expect((await state(page)).active).toBe(native.active);
        await attribute(page.locator('#parent'), value);
    });
}

for (const native of ['summary', 'contenteditable']) {
    test(`native ${native} container is focused without borrowing an attribute`, async ({page}) => {
        await fixture(page, native === 'summary'
            ? `<button id="before">Before</button><details><summary id="parent">Summary${generated.alert}</summary><p>Details</p></details><button id="after">After</button>`
            : base('contenteditable="true"'));
        await dismiss(page);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), null);
        if (native === 'summary') { await page.keyboard.press('Tab'); await expect(page.locator('#after')).toBeFocused(); }
    });
}

test('a no-href native anchor tabIndex value is not mistaken for successful native focus', async ({page}) => {
    await fixture(page, `<a id="parent">Persistent${generated.alert}</a><button id="after">After</button>`);
    expect(await page.locator('#parent').evaluate(parent => parent.tabIndex)).toBe(0);
    await dismiss(page);
    await expect(page.locator('#parent')).toBeFocused();
    await attribute(page.locator('#parent'), '-1');
    await page.locator('#after').focus(); await attribute(page.locator('#parent'), null);
});

test('empty zero-height containing element retains actual borrowed focus', async ({page}) => {
    await fixture(page, base('', generated.alert, false));
    await dismiss(page);
    await expect(page.locator('#parent')).toBeFocused();
    await attribute(page.locator('#parent'), '-1');
    expect(await page.locator('#parent').evaluate(element => element.getBoundingClientRect().height)).toBe(0);
});

for (const style of ['display:contents', 'visibility:hidden']) {
    test(`unusable container ${style} skips to a surviving outer container`, async ({page}) => {
        await fixture(page, `<main id="outer"><p>Outer</p><section id="parent" style="${style}">${generated.alert}</section></main><button id="after">After</button>`);
        await page.locator('#alert').evaluate(element => { element.style.visibility = 'visible'; });
        await dismiss(page);
        await expect(page.locator('#outer')).toBeFocused();
        await attribute(page.locator('#parent'), null);
        await attribute(page.locator('#outer'), '-1');
    });
}

for (const value of [null, '', '-1', '0', 'foo']) {
    test(`body fallback preserves existing caller tabindex ${JSON.stringify(value)}`, async ({page}) => {
        await fixture(page, `<div id="parent" style="display:contents">${generated.alert}</div><button id="after">After</button>`);
        if (value !== null) await page.locator('body').evaluate((body, value) => body.setAttribute('tabindex', value), value);
        await dismiss(page);
        const current = await state(page);
        expect(current.active).toBe('BODY');
        expect(current.bodyFocused).toBe(!['', 'foo'].includes(value));
        await attribute(page.locator('body'), value === null ? '-1' : value);
        await page.keyboard.press('Tab');
        await expect(page.locator('#after')).toBeFocused();
        await attribute(page.locator('body'), value);
        await attribute(page.locator('#parent'), null);
        await attribute(page.locator('html'), null);
    });
}

for (const synchronousBlur of [true, false]) for (const value of ['0', '3', '-1', null]) {
    test(`caller writes ${JSON.stringify(value)} during borrowed focus, synchronous blur=${synchronousBlur}`, async ({page}) => {
        await fixture(page, base());
        await dismiss(page);
        await page.evaluate(({value, synchronousBlur}) => {
            const parent = document.getElementById('parent');
            if (value === null) parent.removeAttribute('tabindex'); else parent.setAttribute('tabindex', value);
            if (synchronousBlur) document.getElementById('after').focus();
        }, {value, synchronousBlur});
        await settle(page);
        if (!synchronousBlur) await page.locator('#after').focus();
        await attribute(page.locator('#parent'), value);
        await expect(page.locator('#after')).toBeFocused();
    });
}

for (const value of ['-1', '0', null]) {
    test(`asynchronous caller tabindex write ${JSON.stringify(value)} remains caller-owned after blur`, async ({page}) => {
        await fixture(page, base());
        await dismiss(page);
        await page.evaluate(value => new Promise(resolve => setTimeout(() => {
            const parent = document.getElementById('parent');
            if (value === null) parent.removeAttribute('tabindex'); else parent.tabIndex = Number(value);
            resolve();
        }, 0)), value);
        await settle(page);
        await page.locator('#after').focus();
        await attribute(page.locator('#parent'), value);
    });
}

test('same-value write in a focus callback transfers attribute ownership to the caller', async ({page}) => {
    await fixture(page, base());
    await page.locator('#parent').evaluate(parent => parent.addEventListener('focus', () => parent.setAttribute('tabindex', '-1')));
    await dismiss(page);
    await page.locator('#after').focus();
    await attribute(page.locator('#parent'), '-1');
});

for (const value of ['-1', '0']) {
    test(`custom-element initial attribute callback preserves caller value ${value}`, async ({page}) => {
        await fixture(page, `<custom-parent id="parent" style="display:block">Persistent${generated.alert}</custom-parent><button id="after">After</button>`);
        await page.evaluate(value => customElements.define('custom-parent', class extends HTMLElement {
            static observedAttributes = ['tabindex'];
            attributeChangedCallback() { if (!this.changed) { this.changed = true; this.setAttribute('tabindex', value); } }
        }), value);
        await dismiss(page);
        await page.locator('#after').focus();
        await attribute(page.locator('#parent'), value);
    });
}

for (const operation of ['remove', 'reparent', 'hidden', 'inert', 'style-hidden', 'blur']) {
    test(`borrowed containing element ${operation} releases only library-owned state`, async ({page}) => {
        await fixture(page, base() + '<div id="new-host"></div>');
        await dismiss(page);
        await page.evaluate(operation => {
            const parent = window.retainedParent = document.getElementById('parent');
            if (operation === 'remove') parent.remove();
            if (operation === 'reparent') document.getElementById('new-host').append(parent);
            if (operation === 'hidden') parent.hidden = true;
            if (operation === 'inert') parent.inert = true;
            if (operation === 'style-hidden') parent.style.display = 'none';
            if (operation === 'blur') parent.blur();
        }, operation);
        await settle(page);
        expect(await page.evaluate(() => window.retainedParent.getAttribute('tabindex'))).toBe(null);
    });
}

test('disconnection preserves a caller same-value write even before observer delivery', async ({page}) => {
    await fixture(page, base());
    await dismiss(page);
    await page.evaluate(() => {
        window.retainedParent = document.getElementById('parent');
        window.retainedParent.setAttribute('tabindex', '-1'); window.retainedParent.remove();
    });
    await settle(page);
    expect(await page.evaluate(() => window.retainedParent.getAttribute('tabindex'))).toBe('-1');
});

test('repeated sibling dismissals use one bounded borrowed focus lifetime', async ({page}) => {
    await fixture(page, base('', generated.alert + generated.alert2));
    await dismiss(page);
    await dismiss(page, '#alert2 > .alert-close');
    await expect(page.locator('#parent')).toBeFocused();
    await attribute(page.locator('#parent'), '-1');
    await expect(page.locator('#alert2')).toHaveJSProperty('hidden', true);
    await page.locator('#after').focus();
    await attribute(page.locator('#parent'), null);
});

test('unrelated focus and repeated hidden activation never steal body or root focus', async ({page}) => {
    await fixture(page, base());
    await page.locator('#after').focus();
    await page.locator('#alert > .alert-close').evaluate(button => { button.click(); button.click(); });
    await settle(page);
    await expect(page.locator('#after')).toBeFocused();
    await attribute(page.locator('#parent'), null);
    await attribute(page.locator('body'), null);
    await attribute(page.locator('html'), null);
});

for (const callback of ['redismiss', 'sibling', 'divert', 'hide-divert', 'remove', 'reparent-remove']) {
    test(`focus callback ${callback} respects hide-first ordering and captured ancestors`, async ({page}) => {
        await fixture(page, `<main id="outer"><p>Outer</p>${base('', generated.alert + generated.alert2)}</main><div id="new-host"></div>`);
        await page.evaluate(callback => {
            window.focusCalls = 0;
            const parent = document.getElementById('parent');
            if (callback === 'hide-divert') {
                document.querySelector('#alert > .alert-close').addEventListener('blur', () => document.getElementById('after').focus());
                return;
            }
            parent.addEventListener('focus', () => {
                window.focusCalls++;
                if (window.focusCalls > 1) return;
                if (callback === 'redismiss') document.querySelector('#alert > .alert-close').click();
                if (callback === 'sibling') {
                    const button = document.querySelector('#alert2 > .alert-close'); button.focus(); button.click();
                }
                if (callback === 'divert') document.getElementById('after').focus();
                if (callback === 'remove' || callback === 'reparent-remove') {
                    window.removedParent = parent;
                    if (callback === 'reparent-remove') document.getElementById('new-host').append(parent);
                    parent.remove();
                }
            });
        }, callback);
        await dismiss(page);
        await expect(page.locator('#alert')).toHaveCount(['remove', 'reparent-remove'].includes(callback) ? 0 : 1);
        if (['divert', 'hide-divert'].includes(callback)) {
            await expect(page.locator('#after')).toBeFocused(); await attribute(page.locator('#parent'), null);
        } else if (['remove', 'reparent-remove'].includes(callback)) {
            await expect(page.locator('#outer')).toBeFocused();
            expect(await page.evaluate(() => window.removedParent.getAttribute('tabindex'))).toBe(null);
            await attribute(page.locator('#new-host'), null);
        } else {
            await expect(page.locator('#parent')).toBeFocused(); await attribute(page.locator('#parent'), '-1');
            if (callback === 'redismiss') expect(await page.evaluate(() => window.focusCalls)).toBe(1);
            if (callback === 'sibling') await expect(page.locator('#alert2')).toHaveJSProperty('hidden', true);
            await page.locator('#after').focus(); await attribute(page.locator('#parent'), null);
        }
    });
}

test('failed borrowed focus cleans up before trying the surviving captured outer container', async ({page}) => {
    await fixture(page, `<main id="outer"><p>Outer</p>${base()}</main>`);
    await page.locator('#parent').evaluate(parent => {
        const native = parent.focus.bind(parent);
        parent.focus = options => { if (!parent.hasAttribute('tabindex')) native(options); };
    });
    await dismiss(page);
    await expect(page.locator('#outer')).toBeFocused();
    await attribute(page.locator('#parent'), null);
});

test('page dismissal preserves non-clamped background scroll', async ({page}) => {
    await fixture(page, '<div style="height:900px"></div>' + base() + '<div style="height:2000px"></div>');
    await page.evaluate(() => window.scrollTo(0, 700));
    await dismiss(page);
    await expect(page.locator('#parent')).toBeFocused();
    expect((await state(page)).scroll).toEqual({x: 0, y: 700});
});

test('direct insertion and real HTMX insertion/outerHTML replacement remain delegated', async ({page}) => {
    await fixture(page, '<button id="load" type="button" hx-get="/alert-response" hx-target="#host">Load</button><div id="host"></div>');
    await routeResponse(page, '/alert-response', generated.alert);
    await insert(page, generated.alert2, '#host');
    await dismiss(page, '#alert2 > .alert-close');
    await expect(page.locator('#alert2')).toHaveJSProperty('hidden', true);
    // Native click drives a real hx-get before subsequent ajax-based adversarial transport.
    await page.locator('#load').click();
    await expect(page.locator('#host #alert')).toBeVisible();
    await settle(page);
    await dismiss(page);
    await expect(page.locator('#alert')).toHaveJSProperty('hidden', true);
    for (let cycle = 0; cycle < 2; cycle++) {
        await htmx(page, '/alert-response', '#alert', '#load', 'outerHTML');
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await dismiss(page);
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', true);
        await expect(page.locator('#alert > .alert-close')).toHaveCount(1);
    }
});

test('real HTMX OOB replacement inserts actual Java Alerts across repeated dismissal cycles', async ({page}) => {
    await fixture(page, base() + '<div id="transport"></div>');
    const oob = generated.alert.replace('id="alert"', 'id="alert" hx-swap-oob="true"');
    await routeResponse(page, '/alert-oob', oob);
    await dismiss(page);
    for (let cycle = 0; cycle < 2; cycle++) {
        await htmx(page, '/alert-oob', '#transport', '#after', 'none');
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await dismiss(page);
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', true);
        await expect(page.locator('#alert > .alert-close')).toHaveCount(1);
    }
});

test('packaged display demo keeps its existing non-dismissible Alerts unchanged', async ({page}, testInfo) => {
    // This baseline has no demo call to Alert.dismissible(); do not invent or retrofit one.
    await page.goto('/demos/display-data');
    await expect(page.locator('.alert')).toHaveCount(4);
    await expect(page.locator('.alert-close')).toHaveCount(0);
    expect(await page.locator('.alert').evaluateAll(alerts => alerts.map(alert => ({
        role: alert.getAttribute('role'), live: alert.getAttribute('aria-live'), hidden: alert.hidden
    })))).toEqual(Array(4).fill({role: null, live: null, hidden: false}));
    await evidence(page, testInfo, 'packaged-display-demo', {url: page.url(), dismissible: 0}, true);
});

for (const viewport of [{width: 1440, height: 900}, {width: 390, height: 844}]) {
    test(`top-dialog Alert focus, native Tab/ShiftTab and scroll boundaries at ${viewport.width}`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        await fixture(page, base() + hosts + '<div style="height:3000px"></div>');
        await page.evaluate(() => { window.scrollTo(0, 500); document.getElementById('before').focus({preventScroll: true}); });
        await insert(page, generated.modalAlert);
        // Measure the settled default layout, not the existing modalSlideIn transform.
        await expect.poll(() => page.locator('#alert-containing-modal > .modal-container')
            .evaluate(dialog => getComputedStyle(dialog).opacity)).toBe('1');
        await page.locator('.modal-body').evaluate(body => {
            const spacer = document.createElement('div'); spacer.style.height = '1200px'; body.prepend(spacer);
        });
        await dismiss(page, '#modal-alert > .alert-close');
        const dialog = page.locator('#alert-containing-modal > .modal-container');
        await expect(dialog).toBeFocused();
        await expect(page.locator('#modal-alert')).toHaveJSProperty('hidden', true);
        await attribute(page.locator('#modal-parent'), null);
        expect((await state(page)).scroll).toEqual({x: 0, y: 500});
        expect(await page.locator('.modal-backdrop').evaluate(backdrop => ({x: backdrop.scrollLeft, y: backdrop.scrollTop}))).toEqual({x: 0, y: 0});
        const traversal = [];
        for (const key of ['Tab', 'Shift+Tab', 'Shift+Tab']) {
            await page.keyboard.press(key);
            const geometry = await page.evaluate(() => {
                const active = document.activeElement, dialog = document.querySelector('.modal-container');
                const rect = active.getBoundingClientRect(), box = dialog.getBoundingClientRect();
                return {inside: dialog.contains(active), top: rect.top, bottom: rect.bottom, left: rect.left,
                    right: rect.right, bounds: {top: Math.max(0, box.top + dialog.clientTop),
                        bottom: Math.min(innerHeight, box.top + dialog.clientTop + dialog.clientHeight)},
                    page: {x: scrollX, y: scrollY}, backdrop: document.querySelector('.modal-backdrop').scrollTop};
            });
            expect(geometry.inside).toBe(true);
            expect(geometry.top).toBeGreaterThanOrEqual(geometry.bounds.top - 1);
            expect(geometry.bottom).toBeLessThanOrEqual(geometry.bounds.bottom + 1);
            expect(geometry.page).toEqual({x: 0, y: 500}); expect(geometry.backdrop).toBe(0);
            traversal.push(geometry);
        }
        await page.locator('#alert > .alert-close').evaluate(button => button.click());
        await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
        await evidence(page, testInfo, `dialog-alert-${viewport.width}`, {traversal, after: await state(page)}, true);
        await page.keyboard.press('Escape');
        await expect(page.locator('#before')).toBeFocused();
    });
}

test('stacked dialogs reject lower/background Alerts and dismiss only the top owner', async ({page}) => {
    await fixture(page, base() + hosts);
    await insert(page, modalVariant('lower'));
    await insert(page, modalVariant('top'));
    await page.locator('#alert > .alert-close').evaluate(button => button.click());
    await page.locator('#lower-alert > .alert-close').evaluate(button => button.click());
    await expect(page.locator('#alert')).toHaveJSProperty('hidden', false);
    await expect(page.locator('#lower-alert')).toHaveJSProperty('hidden', false);
    await dismiss(page, '#top-alert > .alert-close');
    await expect(page.locator('#top-alert')).toHaveJSProperty('hidden', true);
    await expect(page.locator('#top-modal > .modal-container')).toBeFocused();
    await expect(page.locator('.modal-container')).toHaveCount(2);
});

test('top-dialog programmatic dismissal preserves unrelated/application-selected in-dialog focus', async ({page}) => {
    await fixture(page, hosts);
    await insert(page, generated.modalAlert);
    await page.locator('#modal-after').focus();
    await page.locator('#modal-alert > .alert-close').evaluate(button => button.click());
    await expect(page.locator('#modal-after')).toBeFocused();
    await page.locator('#modal-alert').evaluate(alert => { alert.hidden = false; });
    await page.locator('#modal-alert > .alert-close').evaluate(button => {
        button.addEventListener('blur', () => document.getElementById('modal-after').focus());
    });
    await dismiss(page, '#modal-alert > .alert-close');
    await expect(page.locator('#modal-after')).toBeFocused();
    await expect(page.locator('.modal-container')).toHaveCount(1);
});

for (const repeat of [1, 2]) {
    test(`borrowed opener restores across direct close, external removal and blur cleanup ${repeat}`, async ({page}) => {
        await fixture(page, base() + hosts);
        await dismiss(page);
        for (const remove of [false, true]) {
            await insert(page);
            await attribute(page.locator('#parent'), null);
            if (remove) await page.locator('#modal-host').evaluate(host => host.replaceChildren());
            else await page.keyboard.press('Escape');
            await settle(page);
            await expect(page.locator('#parent')).toBeFocused();
            await attribute(page.locator('#parent'), '-1');
        }
        await page.locator('#after').focus();
        await attribute(page.locator('#parent'), null);
        await insert(page);
        await page.keyboard.press('Escape');
        await expect(page.locator('#after')).toBeFocused();
        await attribute(page.locator('#parent'), null);
    });

    test(`borrowed opener restores after real HTMX insertion, replacement and OOB removal ${repeat}`, async ({page}) => {
        await fixture(page, base() + hosts);
        await routeResponse(page, '/alert-modal-response', generated.modal);
        await routeResponse(page, '/alert-modal-clear', '<div id="modal-host" hx-swap-oob="true"></div>');
        await dismiss(page);
        await htmx(page, '/alert-modal-response', '#modal-host', '#parent');
        await attribute(page.locator('#parent'), null);
        await htmx(page, '/alert-modal-response', '#modal-host', '.modal-close');
        await expect(page.locator('.modal-container')).toHaveCount(1);
        await page.keyboard.press('Escape');
        await settle(page);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), '-1');
        await htmx(page, '/alert-modal-response', '#modal-host', '#parent');
        await htmx(page, '/alert-modal-clear', '#transport', '.modal-close', 'none');
        await expect(page.locator('.modal-container')).toHaveCount(0);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), '-1');
        await page.locator('#after').focus(); await attribute(page.locator('#parent'), null);
    });

    test(`borrowed opener survives nested Modal close and simultaneous external removal ${repeat}`, async ({page}) => {
        await fixture(page, base() + hosts);
        await dismiss(page);
        await insert(page, generated.nestedModal);
        await page.locator('#nested-launcher').focus();
        await insert(page, generated.modal, '#nested-host');
        await page.keyboard.press('Escape');
        await expect(page.locator('#nested-launcher')).toBeFocused();
        await insert(page, generated.modal, '#nested-host');
        await page.locator('#modal-host').evaluate(host => host.replaceChildren());
        await settle(page);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), '-1');
        // Repeat the removed nested opener-chain path using real HTMX OOB transport.
        await insert(page, generated.nestedModal);
        await page.locator('#nested-launcher').focus();
        await insert(page, generated.modal, '#nested-host');
        await routeResponse(page, '/nested-alert-modal-clear', '<div id="modal-host" hx-swap-oob="true"></div>');
        await htmx(page, '/nested-alert-modal-clear', '#transport', '#modal-after', 'none');
        await expect(page.locator('.modal-container')).toHaveCount(0);
        await expect(page.locator('#parent')).toBeFocused();
        await attribute(page.locator('#parent'), '-1');
    });
}

for (const mutation of ['remove', 'foo', '', '3', '-1', 'same-value-before-open', 'divert']) {
    test(`borrowed modal opener respects caller ${JSON.stringify(mutation)}`, async ({page}) => {
        await fixture(page, base() + hosts);
        await dismiss(page);
        if (mutation === 'same-value-before-open') await page.locator('#parent').evaluate(parent => parent.setAttribute('tabindex', '-1'));
        await insert(page);
        await page.evaluate(mutation => {
            const parent = window.retainedParent = document.getElementById('parent');
            if (mutation === 'remove') parent.remove();
            else if (mutation === 'divert') parent.addEventListener('focus', () => document.getElementById('after').focus());
            else if (mutation !== 'same-value-before-open') parent.setAttribute('tabindex', mutation);
        }, mutation);
        await page.keyboard.press('Escape'); await settle(page);
        if (['remove', 'foo', ''].includes(mutation)) expect((await state(page)).active).not.toBe('parent');
        else if (mutation === 'divert') await expect(page.locator('#after')).toBeFocused();
        else await expect(page.locator('#parent')).toBeFocused();
        await page.locator('#after').focus();
        expect(await page.evaluate(() => window.retainedParent.getAttribute('tabindex')))
            .toBe(['remove', 'divert'].includes(mutation) ? null : mutation === 'same-value-before-open' ? '-1' : mutation);
        if (mutation === 'remove') {
            await fixture(page, base() + hosts);
            await dismiss(page);
            await insert(page);
            await page.locator('#parent').evaluate(parent => {
                parent.tabIndex = -1;
                parent.removeAttribute('tabindex');
            });
            await page.keyboard.press('Escape'); await settle(page);
            await expect(page.locator('#parent')).not.toBeFocused();
            await attribute(page.locator('#parent'), null);
        }
    });
}

function colors(source) {
    return [...source.matchAll(/rgba?\(([^)]+)\)/g)].map(match => match[1].split(',').map(Number));
}
function luminance(rgb) {
    return rgb.slice(0, 3).map(value => value / 255).map(value => value <= .04045
        ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
        .reduce((sum, value, index) => sum + value * [.2126, .7152, .0722][index], 0);
}
function contrast(a, b) {
    return (Math.max(luminance(a), luminance(b)) + .05) / (Math.min(luminance(a), luminance(b)) + .05);
}

for (const viewport of [{width: 1440, height: 900}, {width: 390, height: 844}]) {
    test(`all severity long-message geometry and actual normal/hover/focus paint at ${viewport.width}`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        await fixture(page, generated.severities);
        const report = [];
        for (const severity of ['info', 'success', 'warning', 'danger']) {
            const button = page.locator(`#severity-${severity} > .alert-close`);
            const paints = {};
            for (const mode of ['normal', 'hover', 'focus']) {
                await page.mouse.move(1, 1);
                if (mode === 'hover') await button.hover();
                if (mode === 'focus') { await page.keyboard.press('Tab'); await button.focus(); }
                const paint = await button.evaluate(button => {
                    const alert = button.closest('.alert'), style = getComputedStyle(button), root = getComputedStyle(alert);
                    return {foreground: style.color, background: style.backgroundColor, image: style.backgroundImage,
                        alertImage: root.backgroundImage, alertBackground: root.backgroundColor,
                        outline: style.outlineColor, outlineStyle: style.outlineStyle, outlineWidth: style.outlineWidth,
                        outlineOffset: style.outlineOffset, focusVisible: button.matches(':focus-visible')};
                });
                const foreground = colors(paint.foreground)[0], control = colors(paint.background)[0];
                expect(control[3] ?? 1).toBe(1);
                expect(contrast(foreground, control)).toBeGreaterThanOrEqual(4.5);
                const endpoints = colors(paint.alertImage);
                expect(endpoints).toHaveLength(2);
                const gradient = Array.from({length: 101}, (_, step) => endpoints[0].slice(0, 3)
                    .map((value, channel) => value + (endpoints[1][channel] - value) * step / 100));
                expect(Math.min(...gradient.map(color => contrast(foreground, color)))).toBeGreaterThanOrEqual(4.5);
                if (mode === 'focus') {
                    expect(paint.focusVisible).toBe(true); expect(paint.outlineStyle).toBe('solid');
                    expect(parseFloat(paint.outlineWidth)).toBeGreaterThanOrEqual(2);
                    const ring = colors(paint.outline)[0];
                    expect(Math.min(...gradient.map(color => contrast(ring, color)))).toBeGreaterThanOrEqual(3);
                    expect(contrast(ring, control)).toBeGreaterThanOrEqual(3);
                }
                paints[mode] = paint;
            }
            expect(paints.hover.background).not.toBe(paints.normal.background);
            const geometry = await button.evaluate(button => {
                const alert = button.closest('.alert'), root = alert.getBoundingClientRect(), control = button.getBoundingClientRect();
                const text = [...alert.childNodes].filter(node => node.nodeType === Node.TEXT_NODE);
                const ranges = text.flatMap(node => { const range = document.createRange(); range.selectNodeContents(node);
                    return [...range.getClientRects()].map(rect => rect.toJSON()); });
                return {root: root.toJSON(), control: control.toJSON(), ranges,
                    overlap: ranges.some(rect => rect.left < control.right && rect.right > control.left
                        && rect.top < control.bottom && rect.bottom > control.top),
                    textOverflow: ranges.some(rect => rect.left < root.left - 1 || rect.right > root.right + 1),
                    controlOverflow: control.left < root.left || control.right > root.right || control.top < root.top || control.bottom > root.bottom,
                    documentOverflow: document.documentElement.scrollWidth > innerWidth};
            });
            expect(geometry.ranges.length).toBeGreaterThan(1);
            expect(geometry.overlap).toBe(false); expect(geometry.textOverflow).toBe(false);
            expect(geometry.controlOverflow).toBe(false); expect(geometry.documentOverflow).toBe(false);
            report.push({severity, paints, geometry});
            await evidence(page, testInfo, `alert-${severity}-focus-${viewport.width}`, {paints, geometry}, true);
        }
        await evidence(page, testInfo, `alert-severity-report-${viewport.width}`, {viewport, report});
    });
}
