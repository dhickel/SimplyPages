const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');

// Use the deliberately maintained Java render snapshots, not an alternate modal implementation.
const snapshotRoot = path.resolve(__dirname, '../../simplypages/src/test/resources/snapshots/display/modal');
const snapshot = name => fs.readFileSync(path.join(snapshotRoot, `${name}.snap.html`), 'utf8');
const defaultModal = snapshot('default');
const formModal = defaultModal.replace('<div>Body content</div>', `
    <input id="first" aria-label="First"><input id="preferred" aria-label="Preferred" autofocus>
    <input disabled id="disabled"><input hidden id="hidden">
    <fieldset disabled><input id="fieldset-disabled"></fieldset>
    <div inert><input id="inert"></div><button id="negative" tabindex="-1">Not a tab stop</button>
    <button id="nested-launcher" type="button">Nested</button>`);

async function fixture(page, initial = '') {
    await page.route('**/modal-fixture', route => route.fulfill({
        contentType: 'text/html',
        body: `<!doctype html><html><head><link rel="stylesheet" href="/css/framework.css">
            <script defer src="/webjars/htmx.org/dist/htmx.min.js"></script>
            <script defer src="/js/framework.js"></script></head><body>
            <button id="launcher" hx-get="/modal-response" hx-target="#host">Open</button>
            <button id="outside">Outside</button><div id="host">${initial}</div></body></html>`
    }));
    await page.route('**/modal-response', route => route.fulfill({contentType: 'text/html', body: formModal}));
    await page.goto('/modal-fixture');
}

async function insert(page, markup, host = '#host') {
    await page.locator(host).evaluate((element, html) => element.insertAdjacentHTML('beforeend', html), markup);
}

async function htmxClick(page, launcher) {
    // Arm before clicking: autofocus processing belongs to HTMX's real settle phase.
    await page.evaluate(() => {
        window.modalSettled = new Promise(resolve => {
            const settled = event => {
                if (event.detail.target !== document.getElementById('host')) return;
                document.removeEventListener('htmx:afterSettle', settled);
                resolve();
            };
            document.addEventListener('htmx:afterSettle', settled);
        });
    });
    await launcher.click();
    await page.evaluate(() => window.modalSettled);
}

async function backdropClick(page, selector = '.modal-backdrop') {
    await page.locator(selector).click({position: {x: 2, y: 2}});
}

async function focusGeometry(control) {
    return control.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const bounds = {top: 0, bottom: innerHeight, left: 0, right: innerWidth};
        const ancestors = [];
        const dialog = element.closest('.modal-container');
        for (let ancestor = element.parentElement; dialog.contains(ancestor); ancestor = ancestor.parentElement) {
            const box = ancestor.getBoundingClientRect();
            const style = getComputedStyle(ancestor);
            const clip = {
                top: box.top + ancestor.clientTop, left: box.left + ancestor.clientLeft,
                bottom: box.top + ancestor.clientTop + ancestor.clientHeight,
                right: box.left + ancestor.clientLeft + ancestor.clientWidth
            };
            if (style.overflowY !== 'visible' || ancestor === dialog) {
                bounds.top = Math.max(bounds.top, clip.top);
                bounds.bottom = Math.min(bounds.bottom, clip.bottom);
            }
            if (style.overflowX !== 'visible' || ancestor === dialog) {
                bounds.left = Math.max(bounds.left, clip.left);
                bounds.right = Math.min(bounds.right, clip.right);
            }
            ancestors.push({
                id: ancestor.id, className: ancestor.className, clip,
                overflowX: style.overflowX, overflowY: style.overflowY,
                scrollTop: ancestor.scrollTop, scrollLeft: ancestor.scrollLeft
            });
        }
        return {
            focused: document.activeElement === element,
            text: element.textContent || element.getAttribute('aria-label'),
            control: {top: rect.top, bottom: rect.bottom, left: rect.left, right: rect.right, height: rect.height},
            bounds, ancestors, background: {x: scrollX, y: scrollY},
            fullyVisible: rect.top >= bounds.top - 1 && rect.bottom <= bounds.bottom + 1
                && rect.left >= bounds.left - 1 && rect.right <= bounds.right + 1
        };
    });
}

async function expectFocusVisible(control) {
    await expect(control).toBeFocused();
    await expect.poll(async () => (await focusGeometry(control)).fullyVisible).toBe(true);
}

async function scrolledFixture(page) {
    await fixture(page);
    await page.evaluate(() => {
        document.body.style.minHeight = '2400px';
        const launcher = document.getElementById('launcher');
        launcher.style.marginTop = '600px';
        window.scrollTo(0, 450);
        launcher.focus({preventScroll: true});
    });
    const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
    expect(background.y).toBe(450);
    return background;
}

for (const {name, viewport, autofocus} of [
    {name: 'desktop', viewport: {width: 1440, height: 900}, autofocus: ''},
    {name: 'mobile', viewport: {width: 390, height: 844}, autofocus: ' autofocus'}
]) {
    test(`tall ${name} dialogs reveal offscreen initial focus and both keyboard wrap targets without background scroll`, async ({page}) => {
        await page.setViewportSize(viewport);
        const background = await scrolledFixture(page);
        const tall = defaultModal.replace('<div>Body content</div>', `
            <div style="height:1200px">Long dialog content</div>
            <input id="offscreen" aria-label="Offscreen control"${autofocus}>`);
        await insert(page, tall);
        const dialog = page.getByRole('dialog');
        const initial = dialog.locator('#offscreen');
        const close = dialog.locator('.modal-close');
        const save = dialog.locator('.modal-footer button');
        await expectFocusVisible(initial);
        expect(await dialog.evaluate(element => element.scrollTop)).toBeGreaterThan(0);
        await page.keyboard.press('Shift+Tab');
        await expectFocusVisible(close);
        await page.keyboard.press('Shift+Tab');
        await expectFocusVisible(save); // Reverse wrap must reveal the footer, not merely focus it.
        await page.keyboard.press('Tab');
        await expectFocusVisible(close); // Forward wrap must reveal the header again.
        await page.keyboard.press('Tab');
        await expectFocusVisible(initial);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
        await page.keyboard.press('Escape');
        await expect(page.locator('#launcher')).toBeFocused();
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
    });
}

for (const overflow of ['auto', 'hidden']) {
    test(`offscreen autofocus is revealed through nested ${overflow} clipping boundaries only inside the dialog`, async ({page}) => {
        await page.setViewportSize({width: 1440, height: 900});
        const background = await scrolledFixture(page);
        const nestedScroll = defaultModal.replace('<div>Body content</div>', `
            <div style="height:900px">Long dialog content</div>
            <div id="internal-scroll" style="height:180px;width:180px;overflow:${overflow};border:6px solid black">
                <div style="width:900px;height:1000px">
                    <div style="height:720px"></div>
                    <button id="offscreen" style="margin-left:700px" autofocus>Inside</button>
                </div>
            </div>`);
        await insert(page, nestedScroll);
        const control = page.locator('#offscreen');
        await expectFocusVisible(control);
        const geometry = await focusGeometry(control);
        const internal = geometry.ancestors.find(ancestor => ancestor.id === 'internal-scroll');
        expect(internal.scrollTop).toBeGreaterThan(0);
        expect(internal.scrollLeft).toBeGreaterThan(0);
        expect(await page.getByRole('dialog').evaluate(element => element.scrollTop)).toBeGreaterThan(0);
        await page.keyboard.press('Shift+Tab');
        await expectFocusVisible(page.locator('.modal-close'));
        await page.keyboard.press('Tab');
        await expectFocusVisible(control);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
        await page.keyboard.press('Escape');
        await expect(page.locator('#launcher')).toBeFocused();
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
    });
}

test('oversized focused controls use nearest scrolling without moving the page or backdrop', async ({page}) => {
    await page.setViewportSize({width: 1440, height: 900});
    const background = await scrolledFixture(page);
    const oversized = defaultModal.replace('<div>Body content</div>', `
        <div id="internal-scroll" style="height:180px;width:240px;overflow:hidden;border:8px solid black">
            <button id="oversized" style="display:block;width:220px;height:420px" autofocus>Oversized</button>
        </div>`);
    await insert(page, oversized);
    const control = page.locator('#oversized');
    await expect(control).toBeFocused();
    const geometry = await focusGeometry(control);
    const internal = geometry.ancestors.find(ancestor => ancestor.id === 'internal-scroll');
    expect(internal.scrollTop).toBe(0);
    expect(internal.scrollLeft).toBe(0);
    expect(geometry.control.top).toBeGreaterThanOrEqual(internal.clip.top - 1);
    expect(geometry.control.bottom).toBeGreaterThan(internal.clip.bottom);
    expect(await page.locator('.modal-backdrop').evaluate(element => ({
        top: element.scrollTop, left: element.scrollLeft
    }))).toEqual({top: 0, left: 0});
    expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
    await page.keyboard.press('Escape');
    await expect(page.locator('#launcher')).toBeFocused();
    expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
});

test('initial markup enters a named dialog and contains focus across both tab boundaries', async ({page}) => {
    await fixture(page, formModal);
    const dialog = page.getByRole('dialog', {name: 'Edit Item'});
    await expect(dialog).toHaveAttribute('aria-modal', 'true');
    await expect(page.locator('#preferred')).toBeFocused();
    await dialog.locator('.modal-footer button').focus();
    await page.keyboard.press('Tab');
    await expect(dialog.locator('.modal-close')).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(dialog.locator('.modal-footer button')).toBeFocused();
    await page.locator('#first').focus();
    await page.keyboard.press('Tab');
    await expect(page.locator('#preferred')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('#nested-launcher')).toBeFocused();
    await page.locator('#outside').evaluate(element => element.focus());
    await expect(page.locator('#preferred')).toBeFocused();
});

test('direct insertion prefers a body control then restores the connected launcher on Escape', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    const noUsableAutofocus = formModal.replace(' autofocus', '')
        .replace('<input id="first"', '<div autofocus>Not focusable</div><input autofocus hidden><input id="first"');
    await insert(page, noUsableAutofocus);
    await expect(page.locator('#first')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
});

test('child keyboard handlers can cancel Escape and own Tab without leaving the dialog', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, formModal);
    await page.locator('#first').evaluate(first => {
        first.addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                event.preventDefault();
                first.dataset.escapeHandled = 'true';
            }
            if (event.key === 'Tab') {
                event.preventDefault();
                document.getElementById('preferred').focus();
            }
        });
    });
    await page.locator('#first').focus();
    await page.keyboard.press('Escape');
    await expect(page.locator('#first')).toHaveAttribute('data-escape-handled', 'true');
    await expect(page.getByRole('dialog')).toHaveCount(1);
    await page.keyboard.press('Tab');
    await expect(page.locator('#preferred')).toBeFocused();
});

test('inside clicks stay open; backdrop and descendant close-button clicks restore focus', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, defaultModal);
    await page.locator('.modal-body').click();
    await expect(page.getByRole('dialog')).toHaveCount(1);
    await backdropClick(page);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
    await insert(page, defaultModal);
    await page.locator('.modal-close').evaluate(button => { button.innerHTML = '<span>Close</span>'; });
    await page.locator('.modal-close span').click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
});

test('disabled Escape/backdrop and hidden close button leave a content-only focus fallback', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    const content = snapshot('content-only')
        .replace('data-close-on-backdrop="true"', 'data-close-on-backdrop="false"')
        .replace('data-close-on-escape="true"', 'data-close-on-escape="false"')
        .replace(/<div class="modal-footer">.*?<\/div>/, '');
    await insert(page, content);
    const dialog = page.getByRole('dialog', {name: 'Dialog'});
    await expect(dialog).toBeFocused();
    await expect(dialog.locator('.modal-header, .modal-close')).toHaveCount(0);
    await page.keyboard.press('Tab');
    await expect(dialog).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(dialog).toBeFocused();
    await page.keyboard.press('Escape');
    await backdropClick(page);
    await expect(dialog).toHaveCount(1);
    await page.locator('#host').evaluate(host => { host.innerHTML = ''; });
    await expect(dialog).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
});

test('Escape-disabled dialogs still allow the default close button and backdrop', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, snapshot('escape-disabled'));
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(1);
    await page.locator('.modal-close').click();
    await expect(page.locator('#launcher')).toBeFocused();
    await insert(page, snapshot('escape-disabled'));
    await backdropClick(page);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
});

test('actual HTMX repeated insertion, replacement and OOB removal preserve the original launcher', async ({page}) => {
    await fixture(page);
    for (let i = 0; i < 3; i++) {
        await htmxClick(page, page.locator('#launcher'));
        await expect(page.locator('#preferred')).toBeFocused();
        // Repeated settle/init notifications must not reset focus or duplicate close handlers.
        await page.locator('#first').focus();
        await page.evaluate(() => {
            for (const name of ['htmx:afterSwap', 'htmx:afterSettle']) {
                document.body.dispatchEvent(new CustomEvent(name, {
                    bubbles: true, detail: {target: document.getElementById('host')}
                }));
            }
        });
        await expect(page.locator('#first')).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(page.getByRole('dialog')).toHaveCount(0);
        await expect(page.locator('#launcher')).toBeFocused();
    }
    await htmxClick(page, page.locator('#launcher'));
    await page.locator('#nested-launcher').evaluate(button => {
        button.setAttribute('hx-get', '/modal-response');
        button.setAttribute('hx-target', '#host');
        htmx.process(button);
    });
    await htmxClick(page, page.locator('#nested-launcher'));
    await expect(page.locator('#preferred')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.locator('#launcher')).toBeFocused();

    await htmxClick(page, page.locator('#launcher'));
    await page.route('**/modal-clear', route => route.fulfill({
        contentType: 'text/html', body: '<div id="host" hx-swap-oob="true"></div>'
    }));
    await page.evaluate(() => htmx.ajax('GET', '/modal-clear', {target: '#host', swap: 'none'}));
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#launcher')).toBeFocused();
});

test('nested dialogs trap and close only the top, returning first to the inner launcher', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, formModal);
    await page.locator('#nested-launcher').focus();
    const nested = defaultModal.replaceAll('test-modal', 'inner-modal')
        .replace('<div>Body content</div>', '<input id="inner-input" aria-label="Inner input">');
    await insert(page, nested, '.modal-body');
    const inner = page.locator('#inner-modal');
    await expect(page.locator('#inner-input')).toBeFocused();
    await expect(page.locator('#test-modal > .modal-container')).toHaveAttribute('aria-modal', 'false');
    await inner.locator('.modal-close').focus();
    await page.keyboard.press('Shift+Tab');
    await expect(inner.locator('.modal-footer button')).toBeFocused();
    await page.locator('#first').evaluate(element => element.focus());
    await expect(page.locator('#inner-input')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(inner).toHaveCount(0);
    await expect(page.locator('#nested-launcher')).toBeFocused();
    await expect(page.locator('#test-modal > .modal-container')).toHaveAttribute('aria-modal', 'true');
    await page.keyboard.press('Escape');
    await expect(page.locator('#launcher')).toBeFocused();
});

test('stacked siblings consume disabled Escape and survive removal of a lower dialog', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, formModal);
    await page.locator('#nested-launcher').focus();
    await insert(page, snapshot('escape-disabled').replaceAll('test-modal', 'upper-modal'));
    await page.keyboard.press('Escape');
    await expect(page.locator('.modal-backdrop')).toHaveCount(2);
    // A lower close action must not close either dialog while an upper dialog is active.
    await page.locator('#test-modal .modal-close').evaluate(element => element.click());
    await expect(page.locator('.modal-backdrop')).toHaveCount(2);
    await page.locator('#test-modal').evaluate(element => element.remove());
    await expect(page.locator('#upper-modal .modal-close')).toBeFocused();
    await page.locator('#upper-modal .modal-close').click();
    await expect(page.locator('#launcher')).toBeFocused();
});

test('removing a launcher and dialog together leaves no stale trap on later openings', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, defaultModal);
    await page.evaluate(() => {
        document.getElementById('launcher').remove();
        document.getElementById('host').innerHTML = '';
    });
    await page.locator('#outside').focus();
    await expect(page.locator('#outside')).toBeFocused();
    await insert(page, defaultModal);
    await page.keyboard.press('Escape');
    await expect(page.locator('#outside')).toBeFocused();
});

test('external removal of the top or entire nested stack follows the connected opener chain', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').focus();
    await insert(page, formModal);
    await page.locator('#nested-launcher').focus();
    const nested = defaultModal.replaceAll('test-modal', 'inner-modal');
    await insert(page, nested, '#test-modal .modal-body');
    await expect(page.locator('#inner-modal .modal-close')).toBeFocused();
    await page.locator('#inner-modal').evaluate(element => element.remove());
    await expect(page.locator('#nested-launcher')).toBeFocused();
    await insert(page, nested, '#test-modal .modal-body');
    await expect(page.locator('#inner-modal .modal-close')).toBeFocused();
    await page.locator('#host').evaluate(host => { host.innerHTML = ''; });
    await expect(page.locator('#launcher')).toBeFocused();
    await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('tab containment follows positive tabindex and the checked radio stop', async ({page}) => {
    await fixture(page);
    const controls = defaultModal.replace('<div>Body content</div>', `
        <button id="second-positive" tabindex="2">Second</button>
        <button id="first-positive" tabindex="1">First</button>
        <input type="radio" name="choice" id="unchecked"><input type="radio" name="choice" id="checked" checked>`);
    await insert(page, controls);
    await expect(page.locator('#first-positive')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('#second-positive')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('.modal-close')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('#checked')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('.modal-footer button')).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.locator('#first-positive')).toBeFocused();
});

test('HTMX uses its actual request source even when a different control was focused', async ({page}) => {
    await fixture(page);
    await page.locator('#outside').focus();
    await page.locator('#launcher').evaluate(element => element.click());
    await expect(page.locator('#preferred')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(page.locator('#launcher')).toBeFocused();
});

test('outerHTML HTMX insertion retains a non-focused request launcher after target replacement', async ({page}) => {
    await fixture(page);
    await page.locator('#launcher').evaluate(element => element.setAttribute('hx-swap', 'outerHTML'));
    await page.locator('#outside').focus();
    await page.locator('#launcher').evaluate(element => element.click());
    await expect(page.locator('#preferred')).toBeFocused();
    await expect(page.locator('#host')).toHaveCount(0);
    await page.keyboard.press('Escape');
    await expect(page.locator('#launcher')).toBeFocused();
});

for (const {name, viewport} of [
    {name: 'desktop', viewport: {width: 1440, height: 900}},
    {name: 'mobile', viewport: {width: 390, height: 844}}
]) {
    test(`packaged /editing-demo ${name} HTMX reveals keyboard targets, closes and restores on all local close paths`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        const errors = [];
        const checks = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto('/editing-demo');
        const launcher = page.locator('button.module-edit-btn[hx-get*="/editing-demo/edit/module-1"]');
        for (const closePath of ['escape', 'header', 'footer', 'external']) {
            await launcher.scrollIntoViewIfNeeded();
            const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
            await launcher.click();
            const dialog = page.getByRole('dialog', {name: 'Edit Module'});
            await expect(dialog).toHaveAttribute('aria-modal', 'true');
            await dialog.evaluate(element => Promise.all(element.getAnimations().map(animation => animation.finished)));
            await expectFocusVisible(dialog.locator('input[name="title"]'));
            const initial = await focusGeometry(dialog.locator('input[name="title"]'));
            expect(initial.background).toEqual(background);
            const titleId = await dialog.getAttribute('aria-labelledby');
            await expect(page.locator(`[id="${titleId}"]`)).toHaveText('Edit Module');
            // Move from the initial title control to Close using actual keyboard navigation.
            await page.keyboard.press('Shift+Tab');
            await expectFocusVisible(dialog.locator('.modal-close'));
            await page.keyboard.press('Shift+Tab');
            const save = dialog.locator('button[hx-post]');
            await expectFocusVisible(save);
            const geometry = await focusGeometry(save);
            expect(geometry.background).toEqual(background);
            if (closePath === 'escape') {
                const screenshot = testInfo.outputPath(`${name}-footer-visible.png`);
                await page.screenshot({path: screenshot});
                await testInfo.attach(`${name}-footer-visible`, {path: screenshot, contentType: 'image/png'});
            }
            await page.keyboard.press('Tab');
            await expectFocusVisible(dialog.locator('.modal-close'));
            const forwardWrap = await focusGeometry(dialog.locator('.modal-close'));
            expect(forwardWrap.background).toEqual(background);
            await backdropClick(page);
            await expect(dialog).toHaveCount(1); // EditModalBuilder deliberately disables backdrop close.
            if (closePath === 'escape') await page.keyboard.press('Escape');
            if (closePath === 'header') await dialog.locator('.modal-close').click();
            if (closePath === 'footer') await dialog.locator('.modal-footer button[data-modal-id]').click();
            if (closePath === 'external') await page.locator('#edit-modal-container').evaluate(host => { host.innerHTML = ''; });
            await expect(dialog).toHaveCount(0);
            await expect(launcher).toBeFocused();
            const restored = await launcher.evaluate(element => ({
                focused: document.activeElement === element, background: {x: scrollX, y: scrollY}
            }));
            expect(restored.background).toEqual(background);
            checks.push({closePath, background, initial, reverseWrap: geometry, forwardWrap, restored});
        }
        expect(errors).toEqual([]);
        const evidence = testInfo.outputPath(`${name}-focus-geometry.json`);
        fs.writeFileSync(evidence, JSON.stringify({viewport, checks, errors}, null, 2));
        await testInfo.attach(`${name}-focus-geometry`, {path: evidence, contentType: 'application/json'});
    });
}
