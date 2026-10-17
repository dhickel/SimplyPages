const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixture = path.resolve(process.env.PHASE18_FIXTURE_HTML ||
    path.join(root, 'simplypages/target/phase18-rendering-browser-fixture.html'));
const source = path.join(root, 'simplypages/src/test/java/io/mindspice/simplypages/integration/Phase18RenderingBrowserFixtureTest.java');
if (fs.statSync(fixture).mtimeMs < fs.statSync(source).mtimeMs) throw new Error('Stale Phase18 Java fixture');
const generated = fs.readFileSync(fixture, 'utf8');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const value = '<雪> & "request" 😀';

async function load(page, width) {
    await page.setViewportSize({width, height: 900});
    const failures = [];
    page.on('pageerror', error => failures.push(error.message));
    page.on('console', message => {if (message.type() === 'error') failures.push(message.text());});
    await page.route('**/phase18-rendering-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">` +
        `<link rel="stylesheet" href="/css/framework.css"><script defer src="/js/framework.js"></script></head>` +
        `<body>${generated}</body></html>`}));
    const css = page.waitForResponse(response => new URL(response.url()).pathname === '/css/framework.css');
    const js = page.waitForResponse(response => new URL(response.url()).pathname === '/js/framework.js');
    await page.goto('/phase18-rendering-fixture');
    for (const [response, relative] of [[await css, 'css/framework.css'], [await js, 'js/framework.js']]) {
        expect(response.status()).toBe(200);
        expect(sha(await response.body())).toBe(sha(fs.readFileSync(path.join(root,
            'simplypages/src/main/resources/static', relative))));
    }
    await page.evaluate(() => document.fonts.ready);
    await expect.poll(() => page.evaluate(() => document.getAnimations()
        .filter(animation => animation.pending || animation.playState === 'running').length)).toBe(0);
    return failures;
}

for (const width of [1440, 390]) {
    test(`Phase18 actual direct and fresh compiled reserved styling, escaping and layout ${width}`, async ({page}, testInfo) => {
        const failures = await load(page, width);
        await expect(page.locator('.phase18-sample')).toHaveCount(2);
        for (const mode of ['direct', 'compiled']) {
            const sample = page.locator(`.phase18-sample[data-mode="${mode}"]`);
            const paragraph = page.locator(`#${mode}-paragraph`);
            await expect(paragraph).toHaveClass('caller aligned-right align-center');
            await expect(paragraph).toHaveCSS('text-align', 'center');
            await expect(paragraph).toHaveAttribute('data-escaped', value);
            expect(await paragraph.textContent()).toBe(value + ' Child');
            await expect(paragraph.locator('雪, request, script')).toHaveCount(0);
            for (const kind of ['plain', 'text', 'empty', 'vertical']) {
                const divider = page.locator(`#${mode}-${kind}`);
                const actual = await divider.evaluate(node => {
                    const style = getComputedStyle(node), rect = node.getBoundingClientRect();
                    return {inline: Object.fromEntries(node.getAttribute('style').split(';').filter(value => value.includes(':'))
                        .map(value => {const colon = value.indexOf(':'); return [value.slice(0, colon).trim(), value.slice(colon + 1).trim()];})),
                        classes: [...node.classList], borderTop: style.borderTopWidth, borderLeft: style.borderLeftWidth,
                        colorTop: style.borderTopColor, colorLeft: style.borderLeftColor,
                        topStyle: style.borderTopStyle, leftStyle: style.borderLeftStyle,
                        display: style.display, position: style.position, height: rect.height, width: rect.width};
                });
                expect(actual.inline['--phase18-owned']).toBe('retained');
                expect(actual.classes).toContain('divider-with-textish');
                if (kind === 'vertical') {
                    expect(actual.borderLeft).toBe('4px');
                    expect(actual.colorLeft).toBe('rgb(12, 34, 56)');
                    expect(actual.leftStyle).toBe('dashed');
                    expect(actual.inline.display).toBe('inline-block');
                    expect(actual.height).toBeCloseTo(48, 0);
                    await expect(divider.locator('.divider-text')).toHaveCount(0);
                } else {
                    const active = kind === 'plain' ? ['border-top', 'width', 'display', 'line-height']
                        : ['position', 'text-align', 'margin'];
                    const inactive = kind === 'plain' ? ['position', 'text-align', 'margin']
                        : ['border-top', 'width', 'display', 'line-height'];
                    for (const name of active) expect(actual.inline[name], `${mode}/${kind}/${name}`).toBeTruthy();
                    for (const name of inactive) expect(Object.hasOwn(actual.inline, name), `${mode}/${kind}/${name}`).toBe(false);
                    expect(actual.classes.includes('divider-with-text')).toBe(kind !== 'plain');
                    expect(actual.width).toBeGreaterThan(100);
                    if (kind === 'plain') {
                        expect(actual.borderTop).toBe('4px');
                        expect(actual.colorTop).toBe('rgb(12, 34, 56)');
                        expect(actual.topStyle).toBe('dashed');
                        expect(actual.display).toBe('block');
                    } else {
                        expect(actual.position).toBe('relative');
                        expect(actual.display).toBe('flex');
                        await expect(divider.locator('.divider-text')).toHaveText(kind === 'empty' ? '' : value);
                        if (kind === 'text') {
                            expect(await divider.evaluate(node => [...node.children].map(child => child.className)))
                                .toEqual(['divider-text', 'divider-child']);
                            await expect(divider.locator('.divider-child')).toHaveText(value);
                        }
                    }
                }
            }
            const steps = await sample.locator('[data-transition]').evaluateAll(elements => elements.map(element => {
                const divider = element.firstElementChild;
                return {step: element.getAttribute('data-transition'), style: divider.getAttribute('style'),
                    classes: divider.className, text: divider.textContent};
            }));
            expect(steps.map(step => step.step)).toEqual(['plain', 'text', 'empty', 'again']);
            expect(steps[0].style).toBe(steps[3].style);
            expect(steps[0].classes).toBe(steps[3].classes);
            expect(steps[1].text).toBe(value);
            expect(steps[2].text).toBe('');
            expect(steps[1].style).toBe(steps[2].style);
            await expect(sample.locator('.ignored-wrapper-child')).toHaveCount(0);
            const geometry = await sample.evaluate(node => {
                const rect = node.getBoundingClientRect();
                return {left: rect.left, right: rect.right, scroll: document.documentElement.scrollWidth,
                    viewport: innerWidth, childOverflow: [...node.querySelectorAll('.editable-row-wrapper, .row, .col')]
                        .some(child => child.scrollWidth > child.clientWidth + 1)};
            });
            expect(geometry.left).toBeGreaterThanOrEqual(-1);
            expect(geometry.right).toBeLessThanOrEqual(width + 1);
            expect(geometry.scroll).toBeLessThanOrEqual(width + 1);
            expect(geometry.childOverflow).toBe(false);
        }
        const screenshot = testInfo.outputPath(`phase18-styling-${width}.png`);
        await page.screenshot({path: screenshot, fullPage: true});
        await testInfo.attach(`generated-layout-${width}`, {path: screenshot, contentType: 'image/png'});
        expect(failures).toEqual([]);
        console.log(JSON.stringify({proof: 'Phase18 Java/current assets/layout', width,
            fixtureSHA: sha(Buffer.from(generated)), samples: 2, failures}));
    });

    test(`Phase18 wrapper IDs, control semantics and native keyboard preservation ${width}`, async ({page}) => {
        const failures = await load(page, width);
        const ids = await page.locator('[id]').evaluateAll(elements => elements.map(element => element.id));
        expect(new Set(ids).size).toBe(ids.length);
        for (const mode of ['direct', 'compiled']) {
            const sample = page.locator(`.phase18-sample[data-mode="${mode}"]`);
            for (let index = 0; index < 2; index++) {
                const wrapper = sample.locator(`[id="row-${mode}-r${index}"]`);
                const order = await wrapper.evaluate(node => [...node.children].map(child => child.id || child.className));
                expect(order[0]).toBe(`${mode}-r${index}-source`);
                expect(order[1]).toBe('row');
                expect(order.length).toBe(index === 0 ? 3 : 2);
                if (index === 0) expect(order[2]).toBe('add-module-section');
                await expect(wrapper.locator(':scope > .row:nth-child(2) > .col-12')).toHaveCount(1);
                expect(await wrapper.locator(':scope > .row:nth-child(1) > .col').evaluateAll(nodes => nodes.map(node => node.className)))
                    .toEqual(index === 0 ? ['col col-5', 'col col-7'] : ['col col-12']);
                const edit = wrapper.locator('.module-edit-btn'), remove = wrapper.locator('.module-delete-btn');
                await expect(edit).toHaveAttribute('hx-get', `/api/pages/${mode}%2Fpage/modules/${mode}-r${index}-list/edit?editMode=USER_EDIT`);
                await expect(edit).toHaveAttribute('hx-target', '#edit-modal-container');
                await expect(edit).toHaveAttribute('hx-swap', 'innerHTML');
                await expect(remove).toHaveAttribute('hx-delete', `/api/pages/${mode}%2Fpage/modules/${mode}-r${index}-list/delete?editMode=USER_EDIT`);
                const target = await remove.getAttribute('hx-target');
                expect(await remove.evaluate((node, target) => document.querySelectorAll(target).length === 1
                    && node.closest('.editable-module-wrapper') === document.querySelector(target), target)).toBe(true);
                await expect(remove).toHaveAttribute('hx-swap', 'outerHTML');
            }
            const positions = await sample.locator('.insert-row-section [hx-post]').evaluateAll(elements => elements.map(element =>
                new URL(element.getAttribute('hx-post'), location.href).searchParams.get('position')));
            expect(positions).toEqual(['1', '2', '0']);
            for (const control of await sample.locator('.insert-row-section [hx-post]').all()) {
                await expect(control).toHaveAttribute('hx-target', 'closest .insert-row-section');
                await expect(control).toHaveAttribute('hx-swap', 'beforebegin');
            }
            const source = sample.locator('[data-source-control="0"]');
            await source.focus();
            await expect(source).toBeFocused();
            await page.keyboard.press('Tab');
            await expect(sample.locator('.module-edit-btn').first()).toBeFocused();
            const focused = await page.evaluate(() => {
                const node = document.activeElement, rect = node.getBoundingClientRect();
                return {visible: node.checkVisibility(), left: rect.left, right: rect.right, top: rect.top,
                    bottom: rect.bottom, hit: node.contains(document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2))};
            });
            expect(focused.visible).toBe(true);
            expect(focused.left).toBeGreaterThanOrEqual(0);
            expect(focused.right).toBeLessThanOrEqual(width);
            expect(focused.top).toBeGreaterThanOrEqual(0);
            expect(focused.bottom).toBeLessThanOrEqual(900);
            expect(focused.hit).toBe(true);
            const account = page.locator(`#${mode}-account`), greeting = account.locator('[aria-controls]');
            await greeting.focus();
            await page.keyboard.press('Enter');
            await expect(greeting).toHaveAttribute('aria-expanded', 'true');
            await page.keyboard.press('Tab');
            await expect(account.getByRole('link', {name: 'Profile', exact: true})).toBeFocused();
            await page.keyboard.press('Escape');
            await expect(greeting).toBeFocused();
            await expect(greeting).toHaveAttribute('aria-expanded', 'false');
            await expect(sample.locator('.page-content')).toHaveText('Standard Page preserved');
        }
        expect(failures).toEqual([]);
    });
}
