const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');

const snapshots = path.resolve(__dirname, '../../simplypages/src/test/resources/snapshots');
const accordion = fs.readFileSync(path.join(snapshots, 'integration/accessibility/accordion-components.snap.html'), 'utf8');
const modal = fs.readFileSync(path.join(snapshots, 'display/modal/default.snap.html'), 'utf8');

async function fixture(page) {
    await page.route('**/accordion-focus-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <script defer src="/js/framework.js"></script></head><body>${accordion}
        <div style="height:6000px">Ordinary following page content</div><div id="modal-host"></div></body></html>`}));
    await page.goto('/accordion-focus-fixture');
}

async function geometry(header) {
    return header.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const bounds = {top: 0, bottom: innerHeight, left: 0, right: innerWidth};
        const dialog = element.closest('.modal-container');
        for (let ancestor = element.parentElement; dialog && dialog.contains(ancestor); ancestor = ancestor.parentElement) {
            const box = ancestor.getBoundingClientRect();
            const style = getComputedStyle(ancestor);
            if (style.overflowY !== 'visible' || ancestor === dialog) {
                bounds.top = Math.max(bounds.top, box.top + ancestor.clientTop);
                bounds.bottom = Math.min(bounds.bottom, box.top + ancestor.clientTop + ancestor.clientHeight);
            }
            if (style.overflowX !== 'visible' || ancestor === dialog) {
                bounds.left = Math.max(bounds.left, box.left + ancestor.clientLeft);
                bounds.right = Math.min(bounds.right, box.left + ancestor.clientLeft + ancestor.clientWidth);
            }
        }
        const hit = document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2);
        const backdrop = element.closest('.modal-backdrop');
        return {focused: document.activeElement === element, rect: rect.toJSON(), bounds,
            hit: !!hit && element.contains(hit), windowScroll: {x: scrollX, y: scrollY},
            backdropScroll: backdrop && {x: backdrop.scrollLeft, y: backdrop.scrollTop},
            dialogScroll: dialog && {x: dialog.scrollLeft, y: dialog.scrollTop},
            visible: rect.top >= bounds.top - 1 && rect.bottom <= bounds.bottom + 1
                && rect.left >= bounds.left - 1 && rect.right <= bounds.right + 1};
    });
}

async function jsonEvidence(testInfo, name, value) {
    const output = testInfo.outputPath(`${name}.json`);
    fs.writeFileSync(output, JSON.stringify(value, null, 2));
    await testInfo.attach(name, {path: output, contentType: 'application/json'});
}

async function expectRevealed(header) {
    await expect(header).toBeFocused();
    await expect.poll(async () => (await geometry(header)).visible).toBe(true);
    expect((await geometry(header)).hit).toBe(true);
}

async function watchRestoration(header, panel) {
    await header.evaluate((element, panelId) => {
        element.addEventListener('focus', () => {
            const panel = document.getElementById(panelId);
            element.dataset.restoredBeforeUnavailable = String(!panel.hidden && !panel.inert
                && panel.getAttribute('aria-hidden') === 'false');
        }, {once: true});
    }, await panel.getAttribute('id'));
}

async function closeFocusedBody(page, scope, inModal, testInfo, label, background) {
    const header = scope.locator('#accordion-single-item-0-header');
    const panel = scope.locator('#accordion-single-item-0-content');
    const final = panel.getByRole('button', {name: 'Final accordion control', exact: true});
    if (inModal) {
        // Use the actual modal Tab/reveal path, not native focus that could scroll its backdrop/page.
        await header.focus();
        await page.keyboard.press('Tab'); // Generated disclosure trigger; its menu is closed.
        await page.keyboard.press('Tab');
    } else await final.focus();
    await expect(final).toBeFocused();
    const before = await geometry(header);
    expect(before.rect.bottom).toBeLessThan(before.bounds.top);
    await watchRestoration(header, panel);
    await header.evaluate(element => element.click());
    await jsonEvidence(testInfo, `${label}-geometry`, {before, after: await geometry(header)});
    await expect(panel).toHaveJSProperty('hidden', true);
    await expect(panel).toHaveJSProperty('inert', true);
    await expect(header).toHaveAttribute('data-restored-before-unavailable', 'true');
    await expectRevealed(header);
    const after = await geometry(header);
    if (inModal) {
        expect(after.windowScroll).toEqual(background.windowScroll);
        expect(after.backdropScroll).toEqual(background.backdropScroll);
    }
    const screenshot = testInfo.outputPath(`${label}-visible-focus.png`);
    await page.screenshot({path: screenshot});
    await testInfo.attach(`${label}-visible-focus`, {path: screenshot, contentType: 'image/png'});

    // Native activation keeps the restored button usable and focus stable.
    await page.keyboard.press('Enter');
    await expect(panel).toHaveJSProperty('hidden', false);
    await expect(header).toBeFocused();
    await page.keyboard.press('Space');
    await expect(panel).toHaveJSProperty('inert', true);
    await expectRevealed(header);
    await page.keyboard.press('Enter');

    // Single-expand programmatic activation closes a different focused body, not nested owners.
    if (inModal) {
        await page.keyboard.press('Tab');
        await page.keyboard.press('Tab');
    } else await final.focus();
    await expect(final).toBeFocused();
    await scope.locator('#accordion-single-item-1-header').evaluate(element => element.click());
    await expectRevealed(header);
    await expect(panel).toHaveJSProperty('hidden', true);
    const secondPanel = scope.locator('#accordion-single-item-1-content');
    const secondHeader = scope.locator('#accordion-single-item-1-header');
    await expect(secondPanel).toHaveJSProperty('inert', false);

    // The opposite single-expand direction opens a tall preceding panel AFTER restoring focus.
    // Visibility must be measured after the whole transition, not only before closing descendants.
    if (inModal) {
        await secondHeader.focus();
        await page.keyboard.press('Tab');
    } else await secondPanel.locator('button').focus();
    await expect(secondPanel.locator('button')).toBeFocused();
    await watchRestoration(secondHeader, secondPanel);
    await header.evaluate(element => element.click());
    await expect(secondPanel).toHaveJSProperty('inert', true);
    await expect(secondHeader).toHaveAttribute('data-restored-before-unavailable', 'true');
    const reverseGeometry = await geometry(secondHeader);
    await jsonEvidence(testInfo, `${label}-reverse-single-expand`, reverseGeometry);
    await expectRevealed(secondHeader);
    if (inModal) {
        expect(reverseGeometry.windowScroll).toEqual(background.windowScroll);
        expect(reverseGeometry.backdropScroll).toEqual(background.backdropScroll);
    }

    // Move the same generated long accordion into the generated nested owner.
    await scope.locator('#accordion-single').evaluate(element => {
        document.getElementById('nested-accordion-item-1-content').querySelector('.accordion-body').append(element);
    });
    const innerHeader = scope.locator('#nested-accordion-item-1-header');
    await innerHeader.evaluate(element => element.click());
    if (inModal) {
        await header.focus();
        await page.keyboard.press('Tab');
        await page.keyboard.press('Tab');
    } else await final.focus();
    await expect(final).toBeFocused();
    const outerHeader = scope.locator('#accordion-nested-item-0-header');
    const outerPanel = scope.locator('#accordion-nested-item-0-content');
    await watchRestoration(outerHeader, outerPanel);
    await outerHeader.evaluate(element => element.click());
    await jsonEvidence(testInfo, `${label}-nested-ancestor-close`, await geometry(outerHeader));
    await expectRevealed(outerHeader);
    await expect(outerHeader).toHaveAttribute('data-restored-before-unavailable', 'true');
    await expect(panel).toHaveJSProperty('inert', false); // Nested expansion survives unavailable ancestor.
    await expect(innerHeader).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('Space');
    await expect(outerPanel).toHaveJSProperty('inert', false);
    await expect(panel).toHaveJSProperty('hidden', false);
    await expect(outerHeader).toBeFocused();
    if (inModal) {
        const finalGeometry = await geometry(outerHeader);
        expect(finalGeometry.windowScroll).toEqual(background.windowScroll);
        expect(finalGeometry.backdropScroll).toEqual(background.backdropScroll);
    }
}

async function insertModal(page, overflow) {
    await page.locator('#modal-host').evaluate((host, {html, overflow}) => {
        const generated = document.querySelector('body > div');
        host.innerHTML = html;
        const body = host.querySelector('.modal-body');
        if (overflow) {
            body.innerHTML = `<div id="internal-scroll" style="height:180px;overflow:${overflow};border:6px solid black"></div>`;
            body.firstElementChild.append(generated);
        } else body.replaceChildren(generated);
        body.insertAdjacentHTML('beforeend', '<div style="height:6000px">Following dialog content</div>');
    }, {html: modal, overflow});
    const dialog = page.getByRole('dialog');
    await expect.poll(() => dialog.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    return dialog;
}

for (const viewport of [{width: 390, height: 844}, {width: 1440, height: 900}]) {
    for (const reducedMotion of ['no-preference', 'reduce']) {
        test(`generated long-page accordion closure reveals focus at ${viewport.width}px (${reducedMotion})`, async ({page}, testInfo) => {
            await page.setViewportSize(viewport);
            await page.emulateMedia({reducedMotion});
            await fixture(page);
            await closeFocusedBody(page, page.locator('body'), false, testInfo, 'page');
        });

        test(`generated modal accordion closure reveals focus without background scroll at ${viewport.width}px (${reducedMotion})`, async ({page}, testInfo) => {
            await page.setViewportSize(viewport);
            await page.emulateMedia({reducedMotion});
            await fixture(page);
            await page.evaluate(() => window.scrollTo(0, 450));
            const background = await page.evaluate(() => ({windowScroll: {x: scrollX, y: scrollY}, backdropScroll: {x: 0, y: 0}}));
            expect(background.windowScroll.y).toBe(450);
            const dialog = await insertModal(page);
            await closeFocusedBody(page, dialog, true, testInfo, 'modal', background);
            await page.keyboard.press('Escape');
            await expect(dialog).toHaveCount(0);
        });
    }

    for (const overflow of ['auto', 'hidden']) {
        test(`generated accordion restoration uses nested modal ${overflow} boundaries at ${viewport.width}px`, async ({page}, testInfo) => {
            await page.setViewportSize(viewport);
            await fixture(page);
            await page.evaluate(() => window.scrollTo(0, 450));
            const background = await page.evaluate(() => ({windowScroll: {x: scrollX, y: scrollY}, backdropScroll: {x: 0, y: 0}}));
            const dialog = await insertModal(page, overflow);
            await closeFocusedBody(page, dialog, true, testInfo, `modal-${overflow}`, background);
            await page.keyboard.press('Escape');
            await expect(dialog).toHaveCount(0);
        });
    }
}

test('oversized generated accordion headers retain modal nearest alignment without background scroll', async ({page}, testInfo) => {
    await page.setViewportSize({width: 1440, height: 900});
    await fixture(page);
    await page.evaluate(() => window.scrollTo(0, 450));
    const dialog = await insertModal(page, 'hidden');
    const header = dialog.locator('#accordion-single-item-0-header');
    const panel = dialog.locator('#accordion-single-item-0-content');
    await header.evaluate(element => {element.style.height = '420px';});
    await header.focus({preventScroll: true});
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await expect(panel.getByRole('button', {name: 'Final accordion control', exact: true})).toBeFocused();
    const before = await geometry(header);
    expect(before.rect.bottom).toBeLessThan(before.bounds.top);
    await watchRestoration(header, panel);
    await header.evaluate(element => element.click());
    await expect(header).toBeFocused();
    await expect(header).toHaveAttribute('data-restored-before-unavailable', 'true');
    const after = await geometry(header);
    const internal = await page.locator('#internal-scroll').evaluate(element => {
        const box = element.getBoundingClientRect();
        return {top: box.top + element.clientTop,
            bottom: box.top + element.clientTop + element.clientHeight, height: element.clientHeight};
    });
    expect(after.rect.height).toBeGreaterThan(internal.height);
    expect(after.rect.top).toBeLessThan(internal.top);
    expect(after.rect.bottom).toBeCloseTo(internal.bottom, 1); // Native-like nearest end alignment.
    expect(after.windowScroll).toEqual(before.windowScroll);
    expect(after.backdropScroll).toEqual(before.backdropScroll);
    await jsonEvidence(testInfo, 'oversized-modal-header', {before, after, internal});
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
});

test('accordion closure does not redirect or reveal unrelated page focus', async ({page}) => {
    await page.setViewportSize({width: 390, height: 844});
    await fixture(page);
    const other = page.locator('#accordion-multiple-item-0-header');
    await other.focus();
    const before = await geometry(other);
    await page.locator('#accordion-nested-item-0-header').evaluate(element => element.click());
    await expect(other).toBeFocused();
    expect((await geometry(other)).windowScroll).toEqual(before.windowScroll);
});

test('single-expand ownership tolerates item wrappers and rejects same-owner cross-item targets', async ({page}) => {
    await page.setViewportSize({width: 1440, height: 900});
    await fixture(page);
    await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', `
        <div class="accordion" id="wrapped-accordion" data-single-expand="true">
            <div class="accordion-item"><div class="harmless-wrapper">
                <button class="accordion-header active" id="wrapped-header-one" type="button"
                    aria-expanded="true" aria-controls="wrapped-content-one">Wrapped one</button>
                <div class="accordion-content expanded" id="wrapped-content-one" role="region"
                    aria-labelledby="wrapped-header-one" aria-hidden="false">
                    <div class="accordion-body">One</div>
                </div>
            </div></div>
            <div class="accordion-item"><div class="harmless-wrapper">
                <button class="accordion-header" id="wrapped-header-two" type="button"
                    aria-expanded="false" aria-controls="wrapped-content-two">Wrapped two</button>
                <div class="accordion-content" id="wrapped-content-two" role="region"
                    aria-labelledby="wrapped-header-two" aria-hidden="true" hidden inert>
                    <div class="accordion-body">Two</div>
                </div>
            </div></div>
        </div>`));

    const firstHeader = page.locator('#wrapped-header-one');
    const firstPanel = page.locator('#wrapped-content-one');
    const secondHeader = page.locator('#wrapped-header-two');
    const secondPanel = page.locator('#wrapped-content-two');
    await secondHeader.click();
    await expect(firstHeader).not.toHaveClass(/active/);
    await expect(firstHeader).toHaveAttribute('aria-expanded', 'false');
    await expect(firstPanel).toBeHidden();
    await expect(firstPanel).toHaveAttribute('inert');
    await expect(secondHeader).toHaveClass(/active/);
    await expect(secondPanel).not.toBeHidden();
    await expect(secondPanel).not.toHaveAttribute('inert');

    // A same-owner panel is not a valid target for another item's header. No state
    // may change before this sibling/owner relationship has been rejected.
    await secondHeader.evaluate(element => element.setAttribute('aria-controls', 'wrapped-content-one'));
    await secondHeader.click();
    await expect(firstPanel).toBeHidden();
    await expect(firstPanel).toHaveAttribute('inert');
    await expect(secondPanel).not.toBeHidden();
    await expect(secondPanel).not.toHaveAttribute('inert');
    await expect(secondHeader).toHaveAttribute('aria-expanded', 'true');
});
