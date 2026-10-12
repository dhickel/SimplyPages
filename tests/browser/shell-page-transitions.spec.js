const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const generated = name => fs.readFileSync(path.join(root, 'simplypages/target/phase13-shell-page-fixtures', `${name}.html`), 'utf8');
const disclosure = (page, id = 'first-page') => page.locator(`#${id} details.sticky-sidebar-mobile-collapse`);
const summary = details => details.locator(':scope > summary');
async function settle(page) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
}
async function host(page, content = generated('page-details')) {
    await page.route('**/phase13-transitions', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <link rel="stylesheet" href="/css/framework.css"><script defer src="/webjars/htmx.org/dist/htmx.min.js"></script>
        <script defer src="/js/framework.js"></script></head><body><button id="application">Application</button>
        ${content}<div id="modal-host"></div></body></html>`}));
    await page.goto('/phase13-transitions');
    await settle(page);
}
async function resize(page, width) {
    await page.setViewportSize({width, height: width <= 768 ? 844 : 900});
    await settle(page);
}
async function modal(page, id = 'phase13-outer-modal', nested = false) {
    await page.locator('#modal-host').evaluate((host, {html, id, nested}) => {
        const element = new DOMParser().parseFromString(html, 'text/html').getElementById(id);
        if (!nested) element.querySelectorAll('.modal-backdrop').forEach(child => child.remove());
        host.append(element);
    }, {html: generated('modal-page'), id, nested});
    await expect.poll(() => page.locator(`#${id} > .modal-container`).evaluate(element => getComputedStyle(element).opacity)).toBe('1');
}
async function nativeVisibleHit(target) {
    await expect(target).toBeFocused();
    await expect.poll(() => target.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const dialog = element.closest('.modal-container');
        const bounds = {top: 0, bottom: innerHeight, left: 0, right: innerWidth};
        for (let ancestor = element.parentElement; ancestor && (!dialog || dialog.contains(ancestor)); ancestor = ancestor.parentElement) {
            const box = ancestor.getBoundingClientRect();
            const style = getComputedStyle(ancestor);
            const top = box.top + ancestor.clientTop;
            const left = box.left + ancestor.clientLeft;
            if (style.overflowY !== 'visible' || ancestor === dialog) {
                bounds.top = Math.max(bounds.top, top);
                bounds.bottom = Math.min(bounds.bottom, top + ancestor.clientHeight);
            }
            if (style.overflowX !== 'visible' || ancestor === dialog) {
                bounds.left = Math.max(bounds.left, left);
                bounds.right = Math.min(bounds.right, left + ancestor.clientWidth);
            }
        }
        return element.checkVisibility({checkVisibilityCSS: true}) && rect.top >= bounds.top - 1
            && rect.bottom <= bounds.bottom + 1
            && rect.left >= bounds.left - 1 && rect.right <= bounds.right + 1
            && element.contains(document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2));
    })).toBe(true);
}

test.beforeEach(({page}) => {
    page.phase13Errors = [];
    page.on('pageerror', error => page.phase13Errors.push(error.message));
});
test.afterEach(({page}) => expect(page.phase13Errors).toEqual([]));

for (const storage of ['absent', 'true', 'false', 'denied']) {
    test(`late body-only real HTMX replacement retains ${storage} desktop preference and one host asset`, async ({page}) => {
        await page.addInitScript(storage => {
            if (storage === 'denied') Object.defineProperty(window, 'localStorage', {get() {throw new DOMException('Denied', 'SecurityError');}});
            else if (storage !== 'absent') localStorage.setItem('sidebarCollapsed', storage);
        }, storage);
        await resize(page, 1440);
        await host(page, '<div class="main-container" id="decoy"></div><div id="late-shell"></div>');
        await page.route('**/phase13-body-response', route => route.fulfill({contentType: 'text/html', body: generated('body-shell')}));
        for (let round = 0; round < 3; round++) {
            await page.evaluate(() => htmx.ajax('GET', '/phase13-body-response', {target: '#late-shell', swap: 'innerHTML'}));
            await settle(page);
            const expected = round === 0 ? storage === 'true' : storage !== 'true';
            await expect(page.locator('#main-sidebar')).toHaveClass(expected ? /collapsed/ : /^(?!.*\bcollapsed\b)/);
            await expect(page.locator('#decoy')).not.toHaveClass(/sidebar-collapsed/);
            await expect(page.locator('script[src="/js/framework.js"]')).toHaveCount(1);
            await expect(page.locator('#late-shell script, #late-shell link, #late-shell header')).toHaveCount(0);
            if (round === 0) await page.locator('.sidebar-toggle').click();
            else {
                await page.evaluate(() => document.body.append(document.createElement('span')));
                await settle(page);
                await expect(page.locator('#main-sidebar')).toHaveClass(expected ? /collapsed/ : /^(?!.*\bcollapsed\b)/);
            }
        }
    });
}

test('real HTMX OOB shell replacement finishes new-identity preference initialization at settle', async ({page}) => {
    await resize(page, 1440);
    await host(page, '<div id="late-shell"></div><div id="transport"></div>');
    const response = await page.evaluate(html => {
        const shell = new DOMParser().parseFromString(html, 'text/html').body.firstElementChild;
        shell.id = 'late-shell';
        shell.setAttribute('hx-swap-oob', 'true');
        return shell.outerHTML;
    }, generated('body-shell'));
    await page.route('**/phase13-oob-shell', route => route.fulfill({contentType: 'text/html', body: response}));
    await page.evaluate(() => htmx.ajax('GET', '/phase13-oob-shell', {target: '#transport'}));
    await settle(page);
    await page.locator('.sidebar-toggle').click();
    await expect(page.locator('#main-sidebar')).toHaveClass(/collapsed/);
    for (let round = 0; round < 3; round++) {
        await page.evaluate(() => htmx.ajax('GET', '/phase13-oob-shell', {target: '#transport'}));
        await settle(page);
        await expect(page.locator('#main-sidebar')).toHaveClass(/collapsed/);
        await expect(page.locator('#late-shell > .main-container')).toHaveClass(/sidebar-collapsed/);
        await expect(page.locator('#main-sidebar')).toHaveCount(1);
        await expect(page.locator('script[src="/js/framework.js"]')).toHaveCount(1);
    }
});

test('legacy desktop global is harmless for missing/noncollapsible sidebars and blocked behind a modal', async ({page}) => {
    await resize(page, 1440);
    await host(page, '<div id="shell-host"></div>');
    await page.evaluate(() => toggleSidebar());
    await page.locator('#shell-host').evaluate((element, html) => element.innerHTML = html, generated('body-shell'));
    await settle(page);
    await modal(page);
    await page.evaluate(() => { toggleSidebar(); document.querySelector('.sidebar-toggle').click(); });
    await expect(page.locator('#main-sidebar')).not.toHaveClass(/collapsed/);
    await page.keyboard.press('Escape');
    await page.locator('.sidebar-toggle').click();
    await expect(page.locator('#main-sidebar')).toHaveClass(/collapsed/);
    await page.locator('#main-sidebar').evaluate(element => element.classList.remove('collapsible'));
    await page.evaluate(() => toggleSidebar());
    await expect(page.locator('#main-sidebar')).toHaveClass(/collapsed/);
});

for (const width of [1440, 769]) {
    test(`observable application close on ${width}px desktop reopens natively but remembers mobile false`, async ({page}) => {
        await resize(page, width);
        await host(page);
        const details = disclosure(page);
        await details.evaluate(element => { element.open = false; });
        await expect.poll(() => details.evaluate(element => element.open)).toBe(true);
        expect(await page.locator('#first-page-link').evaluate(element => element.checkVisibility({checkVisibilityCSS: true}))).toBe(true);
        await resize(page, 768);
        await expect(details).toHaveJSProperty('open', false);
        await resize(page, width);
        await expect(details).toHaveJSProperty('open', true);
        // A same-value *observable attribute write* can update preference; no setter interception/no-op promise.
        await details.evaluate(element => element.setAttribute('open', element.getAttribute('open')));
        await settle(page);
        await resize(page, 390);
        await expect(details).toHaveJSProperty('open', true);
    });
}

test('native toggle coalescing and rapid 768/769 resize do not consume application preference as an owned write', async ({page}) => {
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await details.evaluate(element => {
        window.nativeToggles = [];
        element.addEventListener('toggle', () => nativeToggles.push(element.open));
        element.open = false; element.open = true; element.open = false;
    });
    for (let round = 0; round < 5; round++) {
        for (const width of [769, 768, 1440, 390]) {
            await page.setViewportSize({width, height: 900});
            await expect.poll(() => details.evaluate(element => element.open)).toBe(width >= 769);
        }
    }
    await details.evaluate(element => { element.open = true; element.open = false; element.open = true; });
    await resize(page, 1440);
    await resize(page, 390);
    await expect(details).toHaveJSProperty('open', true);
    expect(await page.evaluate(() => nativeToggles.length)).toBeGreaterThan(0);
});

test('application open changes pending at a responsive media callback are drained before owned desktop writes', async ({page}) => {
    await page.addInitScript(() => {
        matchMedia('(max-width: 768px)').addEventListener('change', event => {
            const details = document.querySelector('#first-page details.sticky-sidebar-mobile-collapse');
            if (!event.matches && details) details.open = false;
        });
    });
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await resize(page, 1440);
    await expect(details).toHaveJSProperty('open', true);
    await resize(page, 390);
    await expect(details).toHaveJSProperty('open', false);
});

for (const target of ['first-page', 'empty-page']) {
    test(`desktop removal of focused ${target} summary transfers to content/root with native geometry`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page, target);
        await summary(details).focus();
        await summary(details).press('Space');
        await resize(page, 1440);
        const focus = target === 'empty-page' ? details : page.locator('#first-page-link');
        if (target === 'empty-page') {
            await expect(focus).toBeFocused();
            expect(await focus.evaluate(element => element.checkVisibility({checkVisibilityCSS: true}))).toBe(true);
            expect(await focus.evaluate(element => element.getBoundingClientRect().width)).toBeGreaterThan(0);
            const ring = await focus.evaluate(element => ({style: getComputedStyle(element).outlineStyle, width: getComputedStyle(element).outlineWidth}));
            expect(ring.style).not.toBe('none');
            expect(parseFloat(ring.width)).toBeGreaterThan(0);
        } else await nativeVisibleHit(focus);
        await expect(summary(details)).toBeHidden();
        await expect(details).toHaveAttribute('tabindex', '-1');
        await page.keyboard.press('Tab');
        await expect(details).not.toBeFocused();
    });
}

for (const mutation of ['positive', 'remove', 'same-value', 'invalid']) {
    test(`later caller ${mutation} tabindex survives empty-root responsive focus fallback`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page, 'empty-page');
        await details.evaluate((element, mutation) => {
            if (mutation === 'remove') element.removeAttribute('tabindex');
            else element.setAttribute('tabindex', mutation === 'positive' ? '4' : mutation === 'invalid' ? 'invalid' : '-1');
        }, mutation);
        const original = await details.getAttribute('tabindex');
        await summary(details).focus();
        await resize(page, 1440);
        expect(await details.getAttribute('tabindex')).toBe(original);
        if (mutation === 'positive' || mutation === 'same-value') await expect(details).toBeFocused();
        else await expect(summary(details)).toBeFocused(); // Failed native fallback keeps recoverable summary; no borrowing.
        await resize(page, 390);
        expect(await details.getAttribute('tabindex')).toBe(original);
    });
}

for (const close of ['property', 'attribute']) {
    test(`mobile ${close} closure transfers contained focus to a native usable summary`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page);
        await page.locator('#first-page-link').focus();
        await details.evaluate((element, close) => close === 'property' ? element.open = false : element.removeAttribute('open'), close);
        await nativeVisibleHit(summary(details));
        expect(await page.locator('#first-page-link').evaluate(element => element.checkVisibility({checkVisibilityCSS: true}))).toBe(false);
        const outline = await summary(details).evaluate(element => ({style: getComputedStyle(element).outlineStyle, width: getComputedStyle(element).outlineWidth}));
        expect(outline.style).not.toBe('none');
        expect(parseFloat(outline.width)).toBeGreaterThan(0);
    });
}

test('application-selected focus and deliberate blur during desktop transition never revive a stale summary', async ({page}) => {
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await summary(details).focus();
    await summary(details).evaluate(element => element.blur());
    await resize(page, 1440);
    await expect(page.locator('#first-page-link')).not.toBeFocused();
    await resize(page, 390);
    await summary(details).focus();
    await page.locator('#first-page-link').evaluate(element => element.addEventListener('focus', () => {
        document.getElementById('application').focus({preventScroll: true});
        window.applicationScroll = {x: scrollX, y: scrollY};
    }, {once: true}));
    await resize(page, 1440);
    await expect(page.locator('#application')).toBeFocused();
    expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(await page.evaluate(() => applicationScroll));
});

test('a deliberate content blur after an observable native close is not mistaken for transition-native focus loss', async ({page}) => {
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await page.locator('#first-page-link').focus();
    await details.evaluate(element => {
        const link = element.querySelector('a');
        element.open = false;
        link.blur();
    });
    await settle(page);
    await expect(summary(details)).not.toBeFocused();
    await expect(details).toHaveJSProperty('open', false);
});

for (const selection of ['blur', 'summary']) {
    test(`desktop content focus callback deliberately selects ${selection} without a later fallback stealing focus`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page);
        await summary(details).focus();
        await page.locator('#first-page-link').evaluate((element, selection) => element.addEventListener('focus', () => {
            if (selection === 'blur') element.blur();
            else {
                const summary = element.closest('details').querySelector(':scope > summary');
                summary.style.display = 'block';
                summary.focus({preventScroll: true});
                summary.style.removeProperty('display');
            }
        }, {once: true}), selection);
        await resize(page, 1440);
        if (selection === 'summary') await expect(summary(details)).toBeFocused();
        else expect(await page.evaluate(() => document.activeElement === document.body)).toBe(true);
        await expect(page.locator('#first-page-button')).not.toBeFocused();
        await expect(details).not.toBeFocused();
    });
}

for (const callback of ['remove', 'reparent', 'close', 'cover']) {
    test(`Page focus callback ${callback} is rechecked before revealing stale content`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page);
        await summary(details).focus();
        await page.locator('#first-page-link').evaluate((element, {callback, modalHtml}) => {
            element.addEventListener('focus', () => {
                const details = element.closest('details');
                if (callback === 'remove') details.closest('.page-content').remove();
                if (callback === 'reparent') document.querySelector('#second-page .sticky-sidebar-aside').append(details);
                if (callback === 'close') details.open = false;
                if (callback === 'cover') {
                    const modal = new DOMParser().parseFromString(modalHtml, 'text/html').getElementById('phase13-stacked-modal');
                    document.getElementById('modal-host').append(modal);
                }
                document.getElementById('application').focus({preventScroll: true});
                window.callbackScroll = {x: scrollX, y: scrollY};
            }, {once: true});
        }, {callback, modalHtml: generated('modal-page')});
        await resize(page, 1440);
        if (callback === 'cover') await expect.poll(() => page.locator('#phase13-stacked-modal').evaluate(element => element.contains(document.activeElement))).toBe(true);
        else await expect(page.locator('#application')).toBeFocused();
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(await page.evaluate(() => callbackScroll));
    });
}

test('target-only open observation is bounded, idempotent and preserves detached identity without responsive writes', async ({page}) => {
    await page.addInitScript(() => {
        const Native = MutationObserver;
        window.openWatches = [];
        window.MutationObserver = class extends Native {
            observe(target, options) {
                if (options.attributeFilter?.includes('open')) openWatches.push({target, options});
                super.observe(target, options);
            }
        };
    });
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await summary(details).click();
    await details.evaluate(element => {
        window.retainedPage = element.closest('.page-content');
        window.retainedDetails = element;
        window.detachedOpenWrites = 0;
        new MutationObserver(records => { detachedOpenWrites += records.length; }).observe(element, {attributes: true, attributeFilter: ['open']});
        retainedPage.remove();
    });
    await settle(page);
    await resize(page, 1440);
    await resize(page, 390);
    expect(await page.evaluate(() => retainedDetails.open)).toBe(false);
    expect(await page.evaluate(() => detachedOpenWrites)).toBe(0);
    await page.evaluate(() => document.body.append(retainedPage));
    for (let round = 0; round < 3; round++) {
        await resize(page, 1440); await resize(page, 390);
        await page.evaluate(() => document.body.append(document.createElement('span')));
    }
    await expect(details).toHaveJSProperty('open', false);
    const watches = await page.evaluate(() => openWatches.filter(watch => watch.target === retainedDetails).map(watch => ({
        subtree: watch.options.subtree || false, filter: watch.options.attributeFilter, oldValue: watch.options.attributeOldValue || false
    })));
    expect(watches.filter(watch => watch.oldValue)).toEqual([{subtree: false, filter: ['open'], oldValue: true}]);
});

test('same detached disclosure retains observable caller preference on reinsert, without detached responsive writes', async ({page}) => {
    await resize(page, 390);
    await host(page);
    const details = disclosure(page);
    await summary(details).click();
    await details.evaluate(element => {
        window.detachedPage = element.closest('.page-content');
        window.detachedDisclosure = element;
        detachedPage.remove();
        element.open = true;
    });
    await resize(page, 1440);
    expect(await page.evaluate(() => detachedDisclosure.open)).toBe(true);
    await page.evaluate(() => { detachedDisclosure.open = false; });
    await resize(page, 390);
    expect(await page.evaluate(() => detachedDisclosure.open)).toBe(false);
    await page.evaluate(() => document.body.append(detachedPage));
    await expect(details).toHaveJSProperty('open', false);
    await resize(page, 1440);
    await expect(details).toHaveJSProperty('open', true);
    await resize(page, 390);
    await expect(details).toHaveJSProperty('open', false);
});

for (const placement of ['sibling', 'nested']) {
    test(`${placement} top Modal responsive Page focus remains inside with covered/background native updates only`, async ({page}, testInfo) => {
        await resize(page, 390);
        await host(page);
        await page.evaluate(() => { document.body.style.minHeight = '3000px'; scrollTo(0, 280); });
        const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
        await modal(page, 'phase13-outer-modal', placement === 'nested');
        if (placement === 'sibling') await modal(page, 'phase13-stacked-modal');
        const topId = placement === 'nested' ? 'phase13-nested-modal' : 'phase13-stacked-modal';
        const pageId = placement === 'nested' ? 'nested-modal-page' : 'stacked-page';
        const top = page.locator(`#${topId} > .modal-container`);
        const details = disclosure(page, pageId);
        await summary(details).focus();
        await page.evaluate(() => {
            document.querySelector('#first-page details.sticky-sidebar-mobile-collapse').open = false;
            document.querySelector('#modal-page details.sticky-sidebar-mobile-collapse').open = false;
        });
        await resize(page, 1440);
        await nativeVisibleHit(page.locator(`#${pageId}-link`));
        await expect(disclosure(page)).toHaveJSProperty('open', true);
        await expect(disclosure(page, 'modal-page')).toHaveJSProperty('open', true);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
        await page.screenshot({path: testInfo.outputPath(`phase13-${placement}-modal-desktop.png`)});
        await resize(page, 390);
        await expect(top).toHaveAttribute('aria-modal', 'true');
        await expect.poll(() => top.evaluate(element => element.contains(document.activeElement))).toBe(true);
        await nativeVisibleHit(page.locator(`#${pageId}-link`));
        await details.evaluate(element => { element.open = false; });
        await nativeVisibleHit(summary(details));
        await expect(disclosure(page)).toHaveJSProperty('open', false);
        await expect(disclosure(page, 'modal-page')).toHaveJSProperty('open', false);
        expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
        await page.screenshot({path: testInfo.outputPath(`phase13-${placement}-modal.png`)});
    });
}

test('a Page focus callback creating a pending covering Modal does not reveal the now-covered dialog', async ({page}) => {
    await resize(page, 390);
    await host(page);
    await modal(page, 'phase13-outer-modal');
    const details = disclosure(page, 'modal-page');
    await summary(details).focus();
    await page.locator('#modal-page-link').evaluate((element, html) => element.addEventListener('focus', () => {
        const oldDialog = element.closest('.modal-container');
        const cover = new DOMParser().parseFromString(html, 'text/html').getElementById('phase13-stacked-modal');
        document.getElementById('modal-host').append(cover);
        oldDialog.style.height = '180px';
        oldDialog.scrollTop = 100000;
        window.coveredScroll = oldDialog.scrollTop;
    }, {once: true}), generated('modal-page'));
    await resize(page, 1440);
    await expect.poll(() => page.locator('#phase13-stacked-modal').evaluate(element => element.contains(document.activeElement))).toBe(true);
    expect(await page.evaluate(() => coveredScroll)).toBeGreaterThan(0);
    expect(await page.locator('#phase13-outer-modal > .modal-container').evaluate(element => element.scrollTop))
        .toBe(await page.evaluate(() => coveredScroll));
});

test('empty generated Page in the top Modal uses its fixed root fallback and keeps Tab inside without background scrolling', async ({page}) => {
    await resize(page, 390);
    await host(page);
    await page.evaluate(() => { document.body.style.minHeight = '3000px'; scrollTo(0, 250); });
    const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
    await modal(page, 'phase13-stacked-modal');
    await page.locator('#phase13-stacked-modal .modal-body').evaluate((body, html) => {
        const empty = new DOMParser().parseFromString(html, 'text/html').getElementById('empty-page');
        empty.id = 'modal-empty-page';
        body.replaceChildren(empty);
    }, generated('page-details'));
    const details = disclosure(page, 'modal-empty-page');
    await summary(details).focus();
    await resize(page, 1440);
    await expect(details).toBeFocused();
    await expect(details).toHaveAttribute('tabindex', '-1');
    expect(await details.evaluate(element => element.checkVisibility({checkVisibilityCSS: true}))).toBe(true);
    await page.keyboard.press('Tab');
    await expect(page.locator('#phase13-stacked-modal .modal-close')).toBeFocused();
    expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
});

test('a freshly inserted closed desktop Page initializes native open before Modal first-focus enumeration', async ({page}) => {
    await resize(page, 1440);
    await host(page);
    await page.locator('#modal-host').evaluate((host, html) => {
        const modal = new DOMParser().parseFromString(html, 'text/html').getElementById('phase13-stacked-modal');
        modal.querySelector('details.sticky-sidebar-mobile-collapse').open = false;
        host.append(modal);
    }, generated('modal-page'));
    const details = disclosure(page, 'stacked-page');
    await expect.poll(() => details.evaluate(element => element.open)).toBe(true);
    await nativeVisibleHit(page.locator('#stacked-page-link'));
    await page.keyboard.press('Tab');
    await expect(page.locator('#stacked-page-button')).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await nativeVisibleHit(page.locator('#stacked-page-link'));
});

test('real HTMX OOB Page replacement initializes new native state and does not steal application focus', async ({page}) => {
    await resize(page, 390);
    await host(page, '<div id="phase13-fragment-host"></div><div id="phase13-oob-page"></div>');
    const response = await page.evaluate(html => new DOMParser().parseFromString(html, 'text/html')
        .getElementById('phase13-htmx-payload').innerHTML, generated('htmx-fragments'));
    await page.route('**/phase13-oob-response', route => route.fulfill({contentType: 'text/html', body: response}));
    await page.locator('#application').focus();
    for (let round = 0; round < 3; round++) {
        await page.evaluate(() => htmx.ajax('GET', '/phase13-oob-response', {target: '#phase13-fragment-host', swap: 'outerHTML'}));
        await settle(page);
        await expect(page.locator('#oob-page')).toHaveCount(1);
        await expect(disclosure(page, 'oob-page')).toHaveJSProperty('open', true);
        await expect(page.locator('#application')).toBeFocused();
        await disclosure(page, 'oob-page').evaluate(element => { element.open = false; });
        await resize(page, 1440);
        await resize(page, 390);
        await expect(disclosure(page, 'oob-page')).toHaveJSProperty('open', false);
        await expect(page.locator('#application')).toBeFocused();
    }
});

test('disabled-JavaScript native Page close on mobile recovers on desktop with Enter/Space and visible focused summary', async ({browser}) => {
    const context = await browser.newContext({javaScriptEnabled: false, hasTouch: true,
        baseURL: 'http://127.0.0.1:18080', viewport: {width: 390, height: 844}});
    try {
        const page = await context.newPage();
        await page.route('**/phase13-nojs', route => route.fulfill({contentType: 'text/html', body:
            '<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="stylesheet" href="/css/framework.css"></head><body>'
            + generated('page-details') + '</body></html>'}));
        await page.goto('/phase13-nojs');
        const details = disclosure(page);
        await summary(details).tap();
        await expect(details).toHaveJSProperty('open', false);
        await page.setViewportSize({width: 1440, height: 900});
        await expect(summary(details)).toBeVisible();
        await summary(details).focus();
        await summary(details).press('Enter');
        await expect(details).toHaveJSProperty('open', true);
        await expect(summary(details)).toBeFocused();
        await expect(summary(details)).toBeVisible();
        await summary(details).press('Space');
        await expect(details).toHaveJSProperty('open', false);
        await summary(details).press('Space');
        await expect(details).toHaveJSProperty('open', true);
        await page.keyboard.press('Tab');
        await expect(page.locator('#first-page-link')).toBeFocused();
        await expect(summary(details)).toBeHidden();
        expect(await page.locator('#first-page-link').evaluate(element => element.checkVisibility({checkVisibilityCSS: true}))).toBe(true);
    } finally { await context.close(); }
});

for (const callerWrite of [false, true]) {
    test(`Page native fallback coexists with Alert borrowed opener lifecycle (caller write ${callerWrite})`, async ({page}) => {
        await resize(page, 390);
        await host(page);
        const details = disclosure(page);
        const content = details.locator(':scope > .sticky-sidebar-content');
        await content.evaluate((content, html) => {
            const alert = new DOMParser().parseFromString(html, 'text/html').getElementById('phase13-modal-alert');
            content.replaceChildren(alert);
        }, generated('modal-page'));
        const close = content.locator('.alert-close');
        await close.focus();
        await close.click();
        await expect(content).toBeFocused();
        await expect(content).toHaveAttribute('tabindex', '-1');
        await expect(details).toHaveAttribute('tabindex', '-1');
        await modal(page, 'phase13-stacked-modal');
        await expect(content).not.toHaveAttribute('tabindex');
        if (callerWrite) await content.evaluate(element => element.setAttribute('tabindex', '-1'));
        await page.keyboard.press('Escape');
        await expect(content).toBeFocused();
        await expect(content).toHaveAttribute('tabindex', '-1');
        await page.locator('#application').focus();
        if (callerWrite) await expect(content).toHaveAttribute('tabindex', '-1');
        else await expect(content).not.toHaveAttribute('tabindex');
        await expect(details).toHaveAttribute('tabindex', '-1');
    });
}
