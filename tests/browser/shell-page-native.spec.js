const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');

test.use({hasTouch: true, viewport: {width: 1440, height: 900}});

const repositoryRoot = path.resolve(__dirname, '..', '..');
const fixtureRoot = path.join(repositoryRoot, 'simplypages', 'target', 'phase13-shell-page-fixtures');
const snapshotRoot = path.join(repositoryRoot, 'simplypages', 'src', 'test', 'resources', 'snapshots', 'integration', 'full-page');
const phase13SnapshotRoot = path.join(repositoryRoot, 'simplypages', 'src', 'test', 'resources', 'snapshots', 'integration', 'phase13-shell-page');
const generated = Object.fromEntries([
    ['full', 'full-shell.html'], ['body', 'body-shell.html'], ['template', 'template-shell.html'],
    ['page', 'page-details.html'], ['modal', 'modal-page.html'], ['fragments', 'htmx-fragments.html']
].map(([key, file]) => [key, fs.readFileSync(path.join(fixtureRoot, file), 'utf8')]));
const snapshots = Object.fromEntries([
    ['no-sidebar', 'shell-no-sidebar.snap.html'], ['ordinary-sidebar', 'shell-sidebar-default.snap.html']
].map(([key, file]) => [key, fs.readFileSync(path.join(snapshotRoot, file), 'utf8')]));
const phase13Snapshots = Object.fromEntries([
    ['full', 'full-shell.snap.html'], ['body', 'body-shell.snap.html'], ['template', 'template-shell.snap.html'],
    ['page', 'page-details.snap.html'], ['modal', 'modal-page.snap.html'], ['fragments', 'htmx-fragments.snap.html']
].map(([key, file]) => [key, fs.readFileSync(path.join(phase13SnapshotRoot, file), 'utf8')]));
const frameworkJs = path.join(repositoryRoot, 'simplypages', 'src', 'main', 'resources', 'static', 'js', 'framework.js');
const frameworkCss = path.join(repositoryRoot, 'simplypages', 'src', 'main', 'resources', 'static', 'css', 'framework.css');

function isDocument(source) {
    return /^\s*(?:<!doctype\s+html\s*>\s*)?<html[\s>]/i.test(source);
}

function responseHtml(source, javascript = true) {
    if (isDocument(source)) {
        return javascript ? source : source.replace(/<script\b[\s\S]*?<\/script>/gi, '');
    }
    return `<!doctype html><html><head><meta charset="UTF-8">
        <link rel="stylesheet" href="/css/framework.css">
        ${javascript ? '<script defer src="/webjars/htmx.org/dist/htmx.min.js"></script><script defer src="/js/framework.js"></script>' : ''}
        </head><body>${source}</body></html>`;
}

async function configureStorage(page, storage) {
    await page.addInitScript(({storage}) => {
        if (storage === 'denied') {
            const methods = Storage.prototype;
            methods.getItem = () => { throw new DOMException('Storage denied', 'SecurityError'); };
            methods.setItem = () => { throw new DOMException('Storage denied', 'SecurityError'); };
            methods.removeItem = () => { throw new DOMException('Storage denied', 'SecurityError'); };
            return;
        }
        try {
            if (storage === 'true' || storage === 'false') localStorage.setItem('sidebarCollapsed', storage);
            else localStorage.removeItem('sidebarCollapsed');
        } catch (_) { /* The product owns its best-effort storage boundary. */ }
    }, {storage});
}

async function loadSource(page, source, {storage = 'absent', javascript = true, route = 'phase13-shell-page'} = {}) {
    await configureStorage(page, storage);
    await page.unroute(`**/${route}*`);
    await page.route(`**/${route}*`, request => request.fulfill({
        contentType: 'text/html', body: responseHtml(source, javascript)
    }));
    await page.goto(`/${route}?fixture=generated`, {waitUntil: 'load'});
    await settle(page);
}

async function loadGenerated(page, name, options = {}) {
    return loadSource(page, generated[name], options);
}

async function loadSnapshot(page, name, options = {}) {
    // Full-page snapshots are normalized body fragments that also retain head resources.
    // The host supplies those resources once, rather than executing duplicate script copies.
    const body = snapshots[name].replace(/<(?:meta|link)\b[^>]*>|<title>[\s\S]*?<\/title>|<script\b[^>]*\bsrc=[^>]*>[\s\S]*?<\/script>/gi, '');
    return loadSource(page, body, options);
}

async function settle(page) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
}

async function shellState(page) {
    return page.evaluate(() => ({
        sidebar: document.querySelector('#main-sidebar')?.classList.contains('collapsed') ?? null,
        container: document.querySelector('.main-container')?.classList.contains('sidebar-collapsed') ?? null,
        mobileOpen: document.querySelector('#main-sidebar')?.classList.contains('mobile-open') ?? null,
        storage: (() => { try { return localStorage.getItem('sidebarCollapsed'); } catch (_) { return 'denied'; } })()
    }));
}

async function details(page) {
    return page.locator('details.sticky-sidebar-mobile-collapse');
}

async function waitForOpen(locator, expected) {
    await expect.poll(() => locator.evaluate(element => element.open)).toBe(expected);
}

async function pageMarkup(page, index = 0) {
    return page.locator('.page-content.with-sticky-sidebar').nth(index).evaluate(element => element.outerHTML);
}

async function insertModalFixture(page) {
    const modalHtml = await page.evaluate(source => new DOMParser().parseFromString(source, 'text/html').body.innerHTML, generated.modal);
    await page.locator('body').evaluate((body, html) => body.insertAdjacentHTML('beforeend', html), modalHtml);
    await settle(page);
}

async function realHtmx(page, url, target, source) {
    await page.evaluate(({url, target, source}) => new Promise((resolve, reject) => {
        const targetElement = document.querySelector(target);
        const done = event => {
            if (event.detail?.target !== targetElement) return;
            document.removeEventListener('htmx:afterSettle', done);
            resolve();
        };
        document.addEventListener('htmx:afterSettle', done);
        window.htmx.ajax('GET', url, {target: targetElement, source: document.querySelector(source)})
            .catch(error => { document.removeEventListener('htmx:afterSettle', done); reject(error); });
    }), {url, target, source});
    await settle(page);
}

test.beforeEach(({page}) => {
    page.phase13Errors = [];
    page.on('pageerror', error => page.phase13Errors.push(error.message));
});
test.afterEach(({page}) => expect(page.phase13Errors).toEqual([]));

test.describe('generated ShellBuilder modes', () => {
    for (const mode of ['full', 'body', 'template']) {
        test(`${mode} emits one native non-submit sidebar control and actual Page content`, async ({page}) => {
            await loadGenerated(page, mode);
            const toggle = page.locator('.sidebar-toggle');
            await expect(toggle).toHaveCount(1);
            await expect(toggle).toHaveAttribute('type', 'button');
            await expect(toggle).toHaveAttribute('onclick', 'toggleSidebar()');
            await expect(toggle).toHaveAttribute('aria-label', 'Toggle sidebar');
            const mobileToggle = page.locator('.mobile-sidebar-toggle');
            await expect(mobileToggle).toHaveAttribute('type', 'button');
            await expect(mobileToggle).toHaveAttribute('onclick', 'toggleMobileSidebar()');
            await expect(mobileToggle).toHaveAttribute('aria-label', 'Toggle navigation');
            await expect(page.locator('.main-sidebar')).toHaveCount(1);
            await expect(page.locator('.page-content')).toHaveCount(1);
            await expect(page.locator('#fixture-page')).toHaveCount(1);
            await expect(page.locator('#fixture-page-link')).toHaveText('Fixture page navigation');
            await expect(page.locator('#content-area')).toHaveCount(1);
            await expect(page.locator('#content-area')).not.toHaveAttribute('hx-get');
            await expect(page.locator('#content-area')).not.toHaveAttribute('hx-trigger');
            await expect(page.locator('script:not([src])').filter({hasText: 'toggleSidebar'})).toHaveCount(0);
            const shape = await page.evaluate(() => ({
                document: document.documentElement.outerHTML.includes('<head>'),
                body: document.body.innerHTML,
                frameworkScripts: document.querySelectorAll('script[src="/js/framework.js"]').length,
                inlineCollapse: [...document.scripts].filter(script => !script.src && script.textContent.includes('toggleSidebar')).length
            }));
            if (mode === 'body') {
                expect(generated.body).not.toMatch(/<(?:html|head|script|link)\b/i);
                expect(shape.document).toBe(true); // The route wrapper is ours; the generated fragment is not a document.
                expect(shape.frameworkScripts).toBe(1);
                expect(shape.body).not.toContain('<html');
                expect(shape.body).not.toContain('<head');
            } else {
                expect(shape.frameworkScripts).toBe(1);
                expect(shape.inlineCollapse).toBe(0);
                expect(generated[mode]).toContain('id="fixture-page"');
                expect(generated[mode]).toContain('id="fixture-page-link"');
                expect(generated[mode]).not.toContain('hx-trigger="load"');
            }
        });

        for (const activation of ['click', 'Enter', 'Space']) {
            test(`${mode} native ${activation} toggles without submitting a form`, async ({page}) => {
                await loadGenerated(page, mode);
                await page.locator('body').evaluate(body => {
                    const form = document.createElement('form');
                    form.id = 'phase13-form';
                    let submits = 0;
                    form.addEventListener('submit', event => { event.preventDefault(); submits++; });
                    body.prepend(form);
                    window.phase13Submits = () => submits;
                    document.querySelector('.sidebar-toggle').setAttribute('form', form.id);
                });
                const toggle = page.locator('.sidebar-toggle');
                await expect.poll(() => shellState(page)).toMatchObject({sidebar: false, container: false});
                if (activation === 'click') await toggle.click();
                else {
                    await toggle.focus();
                    await page.keyboard.press(activation);
                }
                await expect.poll(() => shellState(page)).toMatchObject({sidebar: true, container: true});
                expect(await page.evaluate(() => window.phase13Submits())).toBe(0);
            });
        }
    }

    for (const storage of ['true', 'false', 'absent']) {
        test(`desktop preference ${storage} is applied, toggled and persisted`, async ({page}) => {
            await page.setViewportSize({width: 1440, height: 900});
            await loadGenerated(page, 'full', {storage});
            const expected = storage === 'true';
            await expect.poll(() => shellState(page)).toMatchObject({sidebar: expected, container: expected});
            await page.locator('.sidebar-toggle').click();
            await expect.poll(() => shellState(page)).toMatchObject({sidebar: !expected, container: !expected});
            expect((await shellState(page)).storage).toBe(String(!expected));
        });
    }

    test('denied storage does not disable document-local collapse behavior', async ({page}) => {
        await page.setViewportSize({width: 1440, height: 900});
        await loadGenerated(page, 'full', {storage: 'denied'});
        await page.locator('.sidebar-toggle').click();
        await expect.poll(() => shellState(page)).toMatchObject({sidebar: true, container: true, storage: 'denied'});
        await page.locator('.sidebar-toggle').click();
        await expect.poll(() => shellState(page)).toMatchObject({sidebar: false, container: false});
    });

    test('fresh Java browser fixtures and maintained snapshots expose the same generated contracts', async ({page}) => {
        for (const mode of Object.keys(generated)) {
            expect(generated[mode]).toContain('sticky-sidebar-mobile-collapse');
            expect(phase13Snapshots[mode]).toContain('sticky-sidebar-mobile-collapse');
        }
        expect(generated.full).toContain('id="fixture-page"');
        expect(phase13Snapshots.full).toContain('id="fixture-page"');
        expect(generated.page).toContain('id="first-page"');
        expect(phase13Snapshots.page).toContain('id="first-page"');
        expect(generated.modal).toContain('id="phase13-modal-alert"');
        expect(phase13Snapshots.modal).toContain('id="phase13-modal-alert"');
        await loadGenerated(page, 'full');
        await expect(page.locator('#fixture-page-link')).toHaveText('Fixture page navigation');
    });

    test('actual packaged assets retain order, HTMX active-nav lifecycle and no duplicate collapse bootstrap', async ({page}) => {
        await loadGenerated(page, 'full');
        const scripts = await page.locator('head script').evaluateAll(elements => elements.map(element => element.getAttribute('src')));
        expect(scripts.filter(src => src === '/js/framework.js')).toHaveLength(1);
        expect(scripts.indexOf('/js/framework.js')).toBeGreaterThanOrEqual(0);
        const inline = await page.locator('script:not([src])').allTextContents();
        expect(inline.filter(text => text.includes('toggleSidebar'))).toHaveLength(0);
        expect(inline.some(text => text.includes('htmx:afterSettle'))).toBe(true);
        const [js, css] = await Promise.all([page.request.get('/js/framework.js'), page.request.get('/css/framework.css')]);
        expect(js.status()).toBe(200);
        expect(css.status()).toBe(200);
        expect((await js.body()).equals(fs.readFileSync(frameworkJs))).toBe(true);
        expect((await css.body()).equals(fs.readFileSync(frameworkCss))).toBe(true);
    });

    test('no-sidebar and non-collapsible generated snapshots have no sidebar behavior', async ({page}) => {
        await loadSnapshot(page, 'no-sidebar');
        await expect(page.locator('#main-sidebar, .sidebar-toggle, .mobile-sidebar-toggle')).toHaveCount(0);
        await expect(page.locator('.main-container')).toHaveCount(1);
        await loadSnapshot(page, 'ordinary-sidebar');
        await expect(page.locator('.main-sidebar')).toHaveCount(1);
        await expect(page.locator('.sidebar-toggle')).toHaveCount(0);
        await expect(page.locator('.mobile-sidebar-toggle')).toHaveCount(1);
    });

    test('late direct generated sidebar insertion is initialized once and uses its owning container', async ({page}) => {
        await loadGenerated(page, 'full');
        const sidebar = await page.locator('#main-sidebar').evaluate(element => element.outerHTML);
        await page.locator('#main-sidebar').evaluate(element => element.remove());
        await page.locator('.main-container').evaluate((container, html) => {
            container.insertAdjacentHTML('afterbegin', html);
        }, sidebar);
        await settle(page);
        const toggle = page.locator('#main-sidebar .sidebar-toggle');
        await toggle.click();
        await expect.poll(() => shellState(page)).toMatchObject({sidebar: true, container: true});
        await toggle.click();
        await expect.poll(() => shellState(page)).toMatchObject({sidebar: false, container: false});
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', '<div id="phase13-orphan-host"></div>'));
        const orphan = await page.locator('#main-sidebar').evaluate(element => element.outerHTML);
        await page.locator('#main-sidebar').evaluate(element => element.remove());
        await page.locator('#phase13-orphan-host').evaluate((host, html) => host.innerHTML = html, orphan);
        await page.locator('#phase13-orphan-host .sidebar-toggle').click();
        await expect.poll(() => shellState(page)).toMatchObject({sidebar: false, container: false});
    });

    test('real HTMX shell/Page and OOB fragment insertion is idempotent', async ({page}) => {
        await loadGenerated(page, 'full');
        await page.route('**/phase13-fragments', request => request.fulfill({contentType: 'text/html', body: generated.fragments}));
        await page.locator('#content-area').evaluate(host => host.insertAdjacentHTML('beforeend',
            '<button id="phase13-fragment-source" hx-get="/phase13-fragments" hx-target="#content-area" hx-swap="beforeend">Load</button>'));
        await page.evaluate(() => htmx.process(document.getElementById('phase13-fragment-source')));
        for (let attempt = 0; attempt < 3; attempt++) {
            await realHtmx(page, '/phase13-fragments', '#content-area', '#phase13-fragment-source');
            expect(await page.locator('.page-content').count()).toBeGreaterThan(0);
            expect(await page.locator('.sticky-sidebar-mobile-collapse').count()).toBeGreaterThan(0);
            await expect.poll(() => page.locator('.sidebar-toggle').count()).toBe(1);
        }
    });
});

test.describe('native Page details lifecycle', () => {
    for (const width of [1440, 390, 768, 769]) {
        test(`native Page is usable at the ${width}px responsive boundary`, async ({page}) => {
            await page.setViewportSize({width, height: width <= 768 ? 844 : 900});
            await loadGenerated(page, 'page');
            const all = await details(page);
            expect(await all.count()).toBeGreaterThanOrEqual(3);
            const first = all.first();
            const summary = first.locator(':scope > summary');
            const content = first.locator(':scope > .sticky-sidebar-content');
            await expect(first).toHaveAttribute('tabindex', '-1');
            if (width <= 768) await expect(summary).toBeVisible();
            else await expect(summary).toBeHidden();
            await expect(summary).toHaveJSProperty('tabIndex', 0);
            await expect(content).toBeVisible();
            const firstLink = content.locator('a[href], button, input').first();
            await firstLink.scrollIntoViewIfNeeded();
            const box = await firstLink.boundingBox();
            expect(box).not.toBeNull();
            expect(await firstLink.evaluate(element => {
                const box = element.getBoundingClientRect();
                return element.contains(document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2))
                    && element.checkVisibility({checkVisibilityCSS: true});
            })).toBe(true);
            if (width <= 768) await summary.focus();
            else await firstLink.focus();
            if (width <= 768) await page.keyboard.press('Tab');
            await expect(firstLink).toBeFocused();
            if (width <= 768) {
                await page.keyboard.press('Shift+Tab');
                await expect(summary).toBeFocused();
            }
        });
    }

    test('desktop opens every generated Page while each mobile preference round-trips independently', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const all = await details(page);
        expect(await all.count()).toBeGreaterThanOrEqual(3);
        const first = all.nth(0);
        const second = all.nth(1);
        await first.locator(':scope > summary').click();
        await expect(first).toHaveJSProperty('open', false);
        await expect(second).toHaveJSProperty('open', true);
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(first, true);
        await waitForOpen(second, true);
        await page.setViewportSize({width: 390, height: 844});
        await waitForOpen(first, false);
        await waitForOpen(second, true);
        await second.locator(':scope > summary').click();
        await page.setViewportSize({width: 769, height: 900});
        await waitForOpen(first, true);
        await waitForOpen(second, true);
        await page.setViewportSize({width: 768, height: 844});
        await waitForOpen(first, false);
        await waitForOpen(second, false);
    });

    for (const key of ['Enter', 'Space']) {
        test(`native Page summary ${key} uses native disclosure activation`, async ({page}) => {
            await page.setViewportSize({width: 390, height: 844});
            await loadGenerated(page, 'page');
            const first = (await details(page)).first();
            const summary = first.locator(':scope > summary');
            await summary.click();
            await expect(first).toHaveJSProperty('open', false);
            await summary.focus();
            await page.keyboard.press(key);
            await expect(first).toHaveJSProperty('open', true);
            await summary.press(key);
            await expect(first).toHaveJSProperty('open', false);
        });
    }

    test('caller tabindex and application focus remain caller-owned through responsive writes', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const first = (await details(page)).first();
        await first.evaluate(element => element.setAttribute('tabindex', '3'));
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(first, true);
        await expect(first).toHaveAttribute('tabindex', '3');
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('afterbegin', '<button id="unrelated-focus">Unrelated</button>'));
        const other = page.locator('#unrelated-focus');
        await other.focus();
        await first.evaluate(element => { element.open = false; });
        await settle(page);
        await expect(other).toBeFocused();
        await page.setViewportSize({width: 390, height: 844});
        await expect(other).toBeFocused();
        await first.locator(':scope > summary').focus();
        await first.locator(':scope > summary').evaluate(element => element.blur());
        await expect(first.locator(':scope > summary')).not.toBeFocused();
        await page.setViewportSize({width: 769, height: 900});
        await expect(first.locator(':scope > summary')).not.toBeFocused();
        await expect(first).toHaveAttribute('tabindex', '3');
    });

    test('rapid native mutations coalesce to the last mobile preference without stale desktop closure', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const first = (await details(page)).first();
        await first.evaluate(element => {
            element.open = false;
            element.open = true;
            element.open = false;
        });
        await settle(page);
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(first, true);
        await page.setViewportSize({width: 390, height: 844});
        await waitForOpen(first, false);
    });

    test('new and replaced generated Page nodes initialize independently', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const markup = await pageMarkup(page, 0);
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', '<div id="phase13-page-host"></div>'));
        const host = page.locator('#phase13-page-host');
        await host.evaluate((element, html) => element.insertAdjacentHTML('beforeend', html), markup);
        const added = host.locator('.page-content.with-sticky-sidebar details').first();
        await expect(added).toHaveJSProperty('open', true);
        await added.locator(':scope > summary').click();
        await expect(added).toHaveJSProperty('open', false);
        await host.evaluate((element, html) => element.innerHTML = html, markup);
        const replacement = host.locator('.page-content.with-sticky-sidebar details').first();
        await expect(replacement).toHaveJSProperty('open', true);
        await replacement.locator(':scope > summary').click();
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(replacement, true);
        await page.setViewportSize({width: 390, height: 844});
        await waitForOpen(replacement, false);
    });

    test('reinserted and reparented Page nodes retain preference; detached nodes get no responsive focus', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const first = page.locator('#first-page details.sticky-sidebar-mobile-collapse');
        await first.locator(':scope > summary').click();
        await expect(first).toHaveJSProperty('open', false);
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', '<div id="phase13-reparent-host"></div>'));
        await page.locator('#phase13-reparent-host').evaluate((host, element) => host.append(element), await page.locator('#first-page').elementHandle());
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(first, true);
        await page.setViewportSize({width: 390, height: 844});
        await waitForOpen(first, false);
        const detached = await first.elementHandle();
        await page.evaluate(element => {
            window.phase13Detached = element;
            element.remove();
            document.body.focus();
        }, detached);
        const before = await page.evaluate(() => ({open: window.phase13Detached.open, active: document.activeElement === window.phase13Detached}));
        await page.setViewportSize({width: 1440, height: 900});
        await page.setViewportSize({width: 390, height: 844});
        const after = await page.evaluate(() => ({open: window.phase13Detached.open, active: document.activeElement === window.phase13Detached}));
        expect(after).toEqual(before);
    });

    test('ordinary nested details preserve native state across Page responsive lifecycle', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const nested = page.locator('.sticky-sidebar-content details:not(.sticky-sidebar-mobile-collapse)').first();
        await expect(nested).toHaveCount(1);
        await nested.evaluate(element => element.open = true);
        const outer = nested.locator('xpath=ancestor::details[contains(@class,"sticky-sidebar-mobile-collapse")]').first();
        await outer.locator(':scope > summary').click();
        await page.setViewportSize({width: 1440, height: 900});
        await waitForOpen(outer, true);
        await expect(nested).toHaveJSProperty('open', true);
        await page.setViewportSize({width: 390, height: 844});
        await waitForOpen(outer, false);
        await expect(nested).toHaveJSProperty('open', true);
    });

    test('empty Page fallback and programmatic details root are usable without creating a sequential stop', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const emptyRoot = page.locator('#empty-page');
        await expect(emptyRoot).toHaveCount(1);
        const empty = emptyRoot.locator('.sticky-sidebar-mobile-collapse');
        await expect(empty).toHaveAttribute('tabindex', '-1');
        await empty.locator('summary').click();
        await expect(empty).toHaveJSProperty('open', false);
        await empty.locator('summary').focus();
        await page.keyboard.press('Enter');
        await expect(empty).toHaveJSProperty('open', true);
        await empty.evaluate(element => element.focus({preventScroll: true}));
        await expect(empty).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(empty).not.toBeFocused();
    });

    test('native Page works with no JavaScript and focused summary recovers closed content', async ({page}) => {
        for (const width of [390, 1440]) {
            await page.setViewportSize({width, height: width === 390 ? 844 : 900});
            await loadGenerated(page, 'page', {javascript: false});
            const first = (await details(page)).first();
            const summary = first.locator(':scope > summary');
            if (width > 768) await first.evaluate(element => { element.open = false; });
            await expect(summary).toBeVisible();
            await summary.focus();
            if (width > 768) {
                await page.keyboard.press('Enter');
                await expect(first).toHaveJSProperty('open', true);
                await expect(summary).toBeVisible(); // Focused open summary retains native recovery keys.
            }
            await page.keyboard.press('Enter');
            await expect(first).toHaveJSProperty('open', false);
            await expect(first.locator('.sticky-sidebar-content')).toBeHidden();
            await page.keyboard.press('Space');
            await expect(first).toHaveJSProperty('open', true);
            await expect(first.locator('.sticky-sidebar-content')).toBeVisible();
        }
    });
});

test.describe('Page, Shell and Modal ownership boundaries', () => {
    test('dynamic Page application replacement, focus blur and repeated insertion do not steal focus', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'page');
        const markup = await pageMarkup(page, 1);
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', '<button id="phase13-application-focus">Application focus</button><div id="phase13-mutations"></div>'));
        const applicationFocus = page.locator('#phase13-application-focus');
        await applicationFocus.focus();
        const host = page.locator('#phase13-mutations');
        for (let attempt = 0; attempt < 3; attempt++) {
            await host.evaluate((element, html) => element.innerHTML = html, markup);
            await expect(applicationFocus).toBeFocused();
            const replacement = host.locator('.page-content.with-sticky-sidebar details').first();
            await replacement.locator('summary').click();
            await expect(applicationFocus).not.toBeFocused();
            await applicationFocus.focus();
            await host.evaluate(element => element.replaceChildren());
            await expect(applicationFocus).toBeFocused();
        }
    });

    test('top modal Page owns focus, covered siblings and background scroll while Alert remains independent', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'full');
        await insertModalFixture(page);
        const backdrops = page.locator('.modal-backdrop');
        const dialogs = page.locator('.modal-container[role="dialog"]');
        await expect(backdrops).toHaveCount(3);
        await expect(dialogs).toHaveCount(3);
        await expect(dialogs.last()).toHaveAttribute('aria-modal', 'true');
        await expect(dialogs.nth(0)).toHaveAttribute('aria-modal', 'false');
        await expect(dialogs.nth(1)).toHaveAttribute('aria-modal', 'false');
        await expect.poll(() => dialogs.last().evaluate(element => element.contains(document.activeElement))).toBe(true);
        await page.evaluate(() => { document.body.style.minHeight = '2400px'; window.scrollTo(0, 320); });
        const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
        const top = dialogs.last();
        const topPage = top.locator('.sticky-sidebar-mobile-collapse').first();
        await expect(topPage).toHaveCount(1);
        await topPage.locator('summary').focus();
        await page.keyboard.press('Tab');
        await expect.poll(() => top.evaluate(element => element.contains(document.activeElement))).toBe(true);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
        await page.keyboard.press('Escape');
        await expect(backdrops).toHaveCount(2);
        await expect.poll(() => dialogs.nth(1).evaluate(element => element.contains(document.activeElement))).toBe(true);
        await page.keyboard.press('Escape');
        await expect(backdrops).toHaveCount(1);
        const outer = dialogs.first();
        const alertClose = outer.locator('#phase13-modal-alert .alert-close');
        await expect(alertClose).toHaveCount(1);
        await alertClose.click();
        await expect(outer.locator('#phase13-modal-alert')).toHaveJSProperty('hidden', true);
        await expect(outer).toHaveCount(1);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
    });

    test('mobile modal controls guard the Shell drawer and real hit testing wins', async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        await loadGenerated(page, 'full');
        const toggle = page.locator('.mobile-sidebar-toggle');
        const toggleBox = await toggle.boundingBox();
        await insertModalFixture(page);
        const top = page.locator('.modal-container[role="dialog"]').last();
        await expect(toggle).toHaveAttribute('inert', '');
        await expect(toggle).toBeHidden();
        expect(await page.evaluate(box => document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2)
            ?.closest('.modal-backdrop') !== null, toggleBox)).toBe(true);
        const summary = top.locator('.sticky-sidebar-mobile-summary').first();
        await expect(summary).toBeVisible();
        await summary.focus();
        const box = await summary.boundingBox();
        expect(await page.evaluate(({x, y}) => document.elementFromPoint(x, y)?.closest('.sticky-sidebar-mobile-summary') !== null,
            {x: box.x + box.width / 2, y: box.y + box.height / 2})).toBe(true);
    });
});
