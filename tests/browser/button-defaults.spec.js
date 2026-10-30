const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');
const fixture = path.resolve(__dirname, '../../simplypages/target/button-browser-fixture.html');

async function generatedButtons(page) {
    const html = fs.readFileSync(fixture, 'utf8');
    await page.route('**/button-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <style>.host-override {display:inline-block}</style></head><body>${html}
        <button id="native-until-found" hidden="until-found">Native until found</button></body></html>`}));
    await page.goto('/button-fixture');
}

async function tabTo(page, target) {
    for (let i = 0; i < 100; i++) {
        await page.keyboard.press('Tab');
        if (await target.evaluate(element => element === document.activeElement)) return;
    }
    throw new Error('Target was not reached through native sequential focus');
}

async function paints(button) {
    return button.evaluate(element => {
        const s = getComputedStyle(element);
        const rgb = value => [...value.matchAll(/rgba?\(([^)]+)\)/g)]
            .map(m => m[1].split(',').map(Number)).map(c => [...c.slice(0, 3), c[3] ?? 1]);
        const lum = c => c.slice(0, 3).map(x => x / 255)
            .map(x => x <= .04045 ? x / 12.92 : ((x + .055) / 1.055) ** 2.4)
            .reduce((v, x, i) => v + x * [0.2126, 0.7152, 0.0722][i], 0);
        const fg = rgb(s.color)[0], stops = rgb(s.backgroundImage);
        const contrast = c => (Math.max(lum(fg), lum(c)) + .05) / (Math.min(lum(fg), lum(c)) + .05);
        const rect = element.getBoundingClientRect();
        const hit = document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2);
        return {color: s.color, image: s.backgroundImage, foreground: fg, stops, opacity: s.opacity,
            fontSize: s.fontSize, fontWeight: s.fontWeight, outline: s.outlineStyle,
            outlineWidth: s.outlineWidth, outlineColor: s.outlineColor,
            focusVisible: element.matches(':focus-visible'), ratios: stops.map(contrast),
            minimum: Math.min(...Array.from({length: 1025}, (_, i) => contrast(stops[0].map((v, channel) =>
                v + (stops[1][channel] - v) * i / 1024)))),
            rect: {x: rect.x, y: rect.y, width: rect.width, height: rect.height},
            viewport: {width: innerWidth, height: innerHeight},
            hit: hit === element || element.contains(hit),
            unclipped: element.scrollWidth <= element.clientWidth && element.scrollHeight <= element.clientHeight};
    });
}

function readable(paint) {
    expect(paint.stops).toHaveLength(2);
    expect(paint.stops.every(c => c[3] === 1)).toBe(true);
    expect(paint.foreground[3]).toBe(1);
    expect(paint.opacity).toBe('1');
    for (const ratio of paint.ratios) expect(ratio, JSON.stringify(paint)).toBeGreaterThanOrEqual(4.5);
    expect(paint.minimum).toBeGreaterThanOrEqual(4.5);
}

for (const width of [390, 1440]) {
    test(`generated direct/compiled hidden states and native keyboard/host overrides at ${width}`, async ({page}) => {
        await page.setViewportSize({width, height: 900});
        await generatedButtons(page);
        for (const mode of ['direct', 'compiled']) {
            for (const state of ['empty', 'hidden', 'invalid', 'false']) {
                const button = page.locator(`#${mode}-${state}`);
                await expect(button).toHaveCSS('display', 'none');
                await expect(button).toBeHidden();
                expect(await button.evaluate(element => element.checkVisibility())).toBe(false);
                expect(await button.evaluate(element => element.getClientRects().length)).toBe(0);
            }
            for (const state of ['until-found', 'until-found-upper']) {
                const until = page.locator(`#${mode}-${state}`);
                await expect(until).toHaveCSS('display', 'inline-block');
                await expect(until).toHaveCSS('content-visibility', 'hidden');
                // Native until-found hides contents, not the button's own painted/focusable box.
                expect(await until.evaluate(element => element.checkVisibility())).toBe(
                    await page.locator('#native-until-found').evaluate(element => element.checkVisibility()));
                expect(await until.evaluate(element => element.getBoundingClientRect().width)).toBeGreaterThan(0);
            }
            for (const state of ['override', 'inline-override']) {
                await expect(page.locator(`#${mode}-${state}`)).toHaveCSS('display', 'inline-block');
            }
        }
        const order = ['direct-until-found', 'direct-until-found-upper', 'direct-visible',
            'direct-override', 'direct-inline-override', 'compiled-until-found', 'compiled-until-found-upper',
            'compiled-visible', 'compiled-override', 'compiled-inline-override', 'native-until-found'];
        for (const id of order) {
            await page.keyboard.press('Tab');
            await expect(page.locator(`#${id}`)).toBeFocused();
        }
        for (const id of order.slice(0, -1).reverse()) {
            await page.keyboard.press('Shift+Tab');
            await expect(page.locator(`#${id}`)).toBeFocused();
        }
        // Removing/restoring hidden changes native eligibility without framework JS.
        const direct = page.locator('#direct-empty');
        await direct.evaluate(element => element.removeAttribute('hidden'));
        await expect(direct).toHaveCSS('display', 'inline-block');
        await page.keyboard.press('Shift+Tab');
        await expect(direct).toBeFocused();
        await direct.evaluate(element => element.setAttribute('hidden', ''));
        await page.keyboard.press('Tab');
        await expect(page.locator('#direct-until-found')).toBeFocused();
    });

    test(`until-found native fragment reveal preserves generated direct/compiled semantics at ${width}`, async ({page}) => {
        await page.setViewportSize({width, height: 900});
        await generatedButtons(page);
        await page.evaluate(() => {
            window.revealed = [];
            document.addEventListener('beforematch', event => window.revealed.push(event.target.id));
        });
        for (const mode of ['direct', 'compiled']) {
            for (const state of ['until-found', 'until-found-upper']) {
                const id = `${mode}-${state}`;
                await page.evaluate(id => { location.hash = id; }, id);
                const button = page.locator(`#${id}`);
                await expect.poll(() => button.getAttribute('hidden')).toBe(null);
                await expect(button).toHaveCSS('content-visibility', 'visible');
                expect(await button.evaluate(element => element.checkVisibility())).toBe(true);
                expect(await page.evaluate(id => window.revealed.includes(id), id)).toBe(true);
            }
        }
    });

    test(`packaged secondary Add Module contrast, native focus and activation at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height: 900});
        await page.goto('/editing-demo');
        const button = page.locator('.add-module-section .btn-secondary').first();
        await expect(button).toBeEnabled();
        await page.mouse.move(0, 0);
        await tabTo(page, button);
        await expect(button).toBeFocused();
        const normal = await paints(button);
        readable(normal);
        expect(normal.focusVisible).toBe(true);
        expect(normal.outline).not.toBe('none');
        expect(parseFloat(normal.outlineWidth)).toBeGreaterThan(0);
        expect(normal.hit).toBe(true);
        expect(normal.unclipped).toBe(true);
        expect(normal.rect.x).toBeGreaterThanOrEqual(0);
        expect(normal.rect.x + normal.rect.width).toBeLessThanOrEqual(width);
        expect(normal.rect.y).toBeGreaterThanOrEqual(0);
        expect(normal.rect.y + normal.rect.height).toBeLessThanOrEqual(900);
        const screenshot = testInfo.outputPath(`button-focus-${width}.png`);
        await page.screenshot({path: screenshot});
        await testInfo.attach('native-focus', {path: screenshot, contentType: 'image/png'});
        await button.hover();
        await expect.poll(async () => (await paints(button)).stops[0].slice(0, 3)).toEqual([84, 106, 178]);
        const hover = await paints(button);
        readable(hover);
        const output = testInfo.outputPath(`button-paints-${width}.json`);
        fs.writeFileSync(output, JSON.stringify({normal, hover}, null, 2));
        await testInfo.attach('computed-paints', {path: output, contentType: 'application/json'});
        await page.mouse.move(0, 0);
        await page.keyboard.press(width === 390 ? 'Space' : 'Enter');
        const modal = page.locator('.modal-container[role="dialog"]:visible');
        await expect(modal).toBeVisible();
        await expect(modal).toContainText('Add Module');
        await page.keyboard.press('Escape');
        await expect(modal).toHaveCount(0);
        await expect(button).toBeFocused();
    });
}

test('secondary Add Module retains ordinary host class and token paint overrides', async ({page}) => {
    await page.goto('/editing-demo');
    const button = page.locator('.add-module-section .btn-secondary').first();
    await page.mouse.move(0, 0);
    await page.addStyleTag({content: ':root {--sp-button-secondary-bg: linear-gradient(rgb(20, 30, 40), rgb(30, 40, 50))}'});
    await expect(button).toHaveCSS('background-image', 'linear-gradient(rgb(20, 30, 40), rgb(30, 40, 50))');
    await page.addStyleTag({content: '.host-paint {color: rgb(200, 210, 220); background-image: linear-gradient(rgb(40, 50, 60), rgb(50, 60, 70))}'});
    await button.evaluate(element => element.classList.add('host-paint'));
    await expect(button).toHaveCSS('color', 'rgb(200, 210, 220)');
    await expect(button).toHaveCSS('background-image', 'linear-gradient(rgb(40, 50, 60), rgb(50, 60, 70))');
    await button.evaluate(element => { element.style.background = 'rgb(40, 50, 60)'; });
    await expect(button).toHaveCSS('background-color', 'rgb(40, 50, 60)');
    await expect(button).toHaveCSS('background-image', 'none');
});
