const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');
const generated = fs.readFileSync(path.resolve(__dirname,
    '../../simplypages/src/test/resources/snapshots/integration/accessibility/default-components.snap.html'), 'utf8');

async function fixture(page, width) {
    await page.setViewportSize({width, height: width <= 768 ? 844 : 900});
    await page.route('**/responsive-tabs-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <script defer src="/js/framework.js"></script></head><body>${generated}<div id="dynamic-tabs-host"></div></body></html>`}));
    await page.goto('/responsive-tabs-fixture');
    await page.evaluate(() => {
        window.tabKeyEvents = [];
        window.addEventListener('keydown', event => window.tabKeyEvents.push({key: event.key,
            prevented: event.defaultPrevented, focused: document.activeElement.id}));
    });
}

function nav(page, id) {
    return page.locator(`#${id}-tab-0`).locator('xpath=ancestor::ul[1]');
}

async function axis(page, width) {
    for (const id of ['outer-tabs', 'inner-tabs']) {
        await expect(nav(page, id)).toHaveCSS('flex-direction', width <= 768 ? 'column' : 'row');
        await expect(nav(page, id)).toHaveAttribute('aria-orientation', width <= 768 ? 'vertical' : 'horizontal');
    }
}

async function selected(page, id, index, focused = true) {
    for (let i = 0; i < (id === 'outer-tabs' ? 3 : 2); i++) {
        const tab = page.locator(`#${id}-tab-${i}`);
        await expect(tab).toHaveAttribute('aria-selected', String(i === index));
        await expect(tab).toHaveAttribute('tabindex', i === index ? '0' : '-1');
        const panel = page.locator(`#${id}-panel-${i}`);
        if (i === index) await expect(panel).not.toHaveAttribute('hidden', '');
        else await expect(panel).toHaveAttribute('hidden', '');
    }
    if (focused) await expect(page.locator(`#${id}-tab-${index}`)).toBeFocused();
}

async function evidence(page, testInfo, name) {
    const value = await page.locator('.tab-nav').evaluateAll(elements => elements.map(element => ({
        orientation: element.getAttribute('aria-orientation'), direction: getComputedStyle(element).flexDirection,
        focused: document.activeElement.id, tabs: [...element.querySelectorAll('.tab-button')].map(tab => ({
            id: tab.id, selected: tab.getAttribute('aria-selected'), tabindex: tab.tabIndex,
            panelHidden: document.getElementById(tab.getAttribute('aria-controls')).hidden}))})));
    const output = testInfo.outputPath(`${name}.json`);
    fs.writeFileSync(output, JSON.stringify({tablists: value,
        keys: await page.evaluate(() => window.tabKeyEvents)}, null, 2));
    await testInfo.attach('computed-axis-state', {path: output, contentType: 'application/json'});
    await expect.poll(() => page.locator('.tab-panel.active:visible').evaluateAll(elements =>
        elements.every(element => getComputedStyle(element).opacity === '1'))).toBe(true);
    const screenshot = testInfo.outputPath(`${name}.png`);
    await page.locator('#outer-tabs').screenshot({path: screenshot});
    await testInfo.attach('responsive-tabs', {path: screenshot, contentType: 'image/png'});
}

for (const width of [1440, 390]) {
    test(`generated responsive tabs axis, native keys and nested activation at ${width}px`, async ({page}, testInfo) => {
        await fixture(page, width);
        await axis(page, width); // Initial ARIA matches CSS before any focus/keyboard interaction.
        const next = width <= 768 ? 'ArrowDown' : 'ArrowRight';
        const previous = width <= 768 ? 'ArrowUp' : 'ArrowLeft';
        const perpendicular = width <= 768 ? ['ArrowLeft', 'ArrowRight'] : ['ArrowUp', 'ArrowDown'];
        await page.locator('#outer-tabs-tab-0').focus();
        for (const key of [...perpendicular, ...['Control', 'Alt', 'Meta', 'Shift'].flatMap(modifier =>
            [next, previous, 'Home', 'End'].map(key => `${modifier}+${key}`))]) {
            await page.keyboard.press(key);
            await expect(page.locator('#outer-tabs-tab-0')).toBeFocused();
            await selected(page, 'outer-tabs', 0);
            expect((await page.evaluate(() => window.tabKeyEvents.at(-1))).prevented, key).toBe(false);
        }
        await page.keyboard.press(next);
        await selected(page, 'outer-tabs', 1);
        expect((await page.evaluate(() => window.tabKeyEvents.at(-1))).prevented).toBe(true);
        await page.keyboard.press('Tab');
        await expect(page.getByRole('textbox', {name: 'Second control'})).toBeFocused();
        await page.locator('#outer-tabs-tab-1').focus();
        await page.keyboard.press('End');
        await selected(page, 'outer-tabs', 2);
        await page.keyboard.press(next);
        await selected(page, 'outer-tabs', 0);
        await page.keyboard.press(previous);
        await selected(page, 'outer-tabs', 2);
        await page.keyboard.press('Home');
        await selected(page, 'outer-tabs', 0);
        await page.locator('#inner-tabs-tab-0').focus();
        await page.keyboard.press(next);
        await selected(page, 'inner-tabs', 1);
        await selected(page, 'outer-tabs', 0, false);
        await page.locator('#outer-tabs-tab-1').click();
        await page.locator('#outer-tabs-tab-0').click();
        await selected(page, 'inner-tabs', 1, false);
        await selected(page, 'outer-tabs', 0);
        await evidence(page, testInfo, `generated-tabs-${width}`);
        // Invalid/cross-owner targets remain ineligible on either keyboard axis.
        await page.locator('#outer-tabs-tab-1').evaluate(element => element.setAttribute('aria-controls', 'inner-tabs-panel-0'));
        await page.keyboard.press(next);
        await selected(page, 'outer-tabs', 2);
        await selected(page, 'inner-tabs', 1, false);
    });
}

test('responsive resize changes only tab orientation, preserving focus, selection and hidden nested state', async ({page}, testInfo) => {
    await fixture(page, 1440);
    await page.locator('#inner-tabs-tab-0').focus();
    await page.keyboard.press('ArrowRight');
    await page.locator('#outer-tabs-tab-1').click();
    const state = () => page.locator('.tab-button, .tab-panel').evaluateAll(elements => elements.map(element => ({
        id: element.id, classes: element.className, selected: element.getAttribute('aria-selected'),
        tabindex: element.getAttribute('tabindex'), hidden: element.hidden})));
    const before = await state();
    for (const width of [768, 769, 390, 1440, 768]) {
        await page.setViewportSize({width, height: 900});
        await axis(page, width);
        await expect(page.locator('#outer-tabs-tab-1')).toBeFocused();
        expect(await state()).toEqual(before);
    }
    await page.keyboard.press('ArrowDown');
    await selected(page, 'outer-tabs', 2);
    await page.setViewportSize({width: 769, height: 900});
    await axis(page, 769);
    await page.keyboard.press('ArrowLeft');
    await selected(page, 'outer-tabs', 1);
    await page.keyboard.press('Home');
    await selected(page, 'outer-tabs', 0);
    await selected(page, 'inner-tabs', 1, false);
    await page.locator('#inner-tabs-tab-1').focus();
    await page.setViewportSize({width: 390, height: 844});
    await axis(page, 390);
    await expect(page.locator('#inner-tabs-tab-1')).toBeFocused();
    await page.keyboard.press('ArrowUp');
    await selected(page, 'inner-tabs', 0);
    await selected(page, 'outer-tabs', 0, false);
    await evidence(page, testInfo, 'resized-tabs-mobile');
});

for (const width of [1440, 390]) {
    test(`dynamic generated tabs synchronize axis and handle immediate keyboard at ${width}px`, async ({page}) => {
        await fixture(page, width);
        const markup = await page.locator('#outer-tabs').evaluate(element => {
            const html = element.outerHTML;
            element.remove();
            return html;
        });
        for (let attempt = 0; attempt < 3; attempt++) {
            await page.locator('#dynamic-tabs-host').evaluate((host, html) => {host.innerHTML = html;}, markup);
            await axis(page, width); // Observer synchronizes semantics even without interaction.
            await page.locator('#inner-tabs-tab-0').focus();
            await page.keyboard.press(width <= 768 ? 'ArrowDown' : 'ArrowRight');
            await selected(page, 'inner-tabs', 1);
            await selected(page, 'outer-tabs', 0, false);
        }
        // A synchronous key before the insertion observer runs must also use the computed axis.
        const result = await page.locator('#dynamic-tabs-host').evaluate((host, {html, key}) => {
            host.innerHTML = html;
            const tab = host.querySelector('#outer-tabs-tab-0');
            tab.focus();
            const event = new KeyboardEvent('keydown', {key, bubbles: true, cancelable: true});
            tab.dispatchEvent(event);
            return {prevented: event.defaultPrevented, focused: document.activeElement.id,
                orientation: tab.closest('.tab-nav').getAttribute('aria-orientation')};
        }, {html: markup, key: width <= 768 ? 'ArrowDown' : 'ArrowRight'});
        expect(result).toEqual({prevented: true, focused: 'outer-tabs-tab-1',
            orientation: width <= 768 ? 'vertical' : 'horizontal'});
        await selected(page, 'outer-tabs', 1);
    });
}
