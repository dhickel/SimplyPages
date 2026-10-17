const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixture = path.resolve(process.env.PHASE19_FIXTURE_HTML ||
    path.join(root, 'simplypages/target/phase19-forum-depth-browser-fixture.html'));
const inputs = [
    'simplypages/src/test/java/io/mindspice/simplypages/integration/Phase19ForumDepthBrowserFixtureTest.java',
    'simplypages/src/main/java/io/mindspice/simplypages/components/forum/comments/DefaultForumCommentComponent.java'
];
if (!fs.existsSync(fixture) || inputs.some(source => fs.statSync(fixture).mtimeMs < fs.statSync(path.join(root, source)).mtimeMs)) {
    throw new Error('Stale Phase19 Java fixture');
}
const generated = fs.readFileSync(fixture, 'utf8');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const depths = [0, 1, 2, 4, 5, 9, -2147483648, 2147483647];

async function load(page, width, hostWidth, direction) {
    console.log(JSON.stringify({trace: 'Phase19 load before navigation', width, hostWidth, direction}));
    await page.setViewportSize({width, height: 1000});
    const failures = [];
    page.on('pageerror', error => failures.push(error.message));
    page.on('console', message => {if (message.type() === 'error') failures.push(message.text());});
    await page.route('**/phase19-forum-depth-fixture', route => route.fulfill({contentType: 'text/html', body:
        '<!doctype html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">' +
        '<link rel="stylesheet" href="/css/framework.css"><script defer src="/js/framework.js"></script></head><body>' +
        generated + '</body></html>'}));
    const css = page.waitForResponse(response => new URL(response.url()).pathname === '/css/framework.css');
    await page.goto('/phase19-forum-depth-fixture');
    const response = await css;
    expect(response.status()).toBe(200);
    expect(sha(await response.body())).toBe(sha(fs.readFileSync(path.join(root,
        'simplypages/src/main/resources/static/css/framework.css'))));
    await page.locator('#phase19-forum-depth-fixture').evaluate((node, settings) => {
        node.style.width = `${settings.width}px`;
        node.dir = settings.direction;
    }, {width: hostWidth, direction});
    await page.evaluate(() => document.fonts.ready);
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    await expect.poll(() => page.evaluate(() => document.getAnimations()
        .filter(animation => animation.pending || animation.playState === 'running').length)).toBe(0);
    return failures;
}

async function inspect(page, mode, width, direction, testInfo) {
    const view = page.locator(`.phase19-forum-view[data-mode="${mode}"]`);
    await expect(view.locator('.phase19-forum-comment')).toHaveCount(8);
    const result = await view.locator('.phase19-forum-comment').evaluateAll(wrappers => wrappers.map(wrapper => {
        const card = wrapper.querySelector(':scope > .forum-comment');
        const style = getComputedStyle(card);
        const box = node => {
            const rect = node.getBoundingClientRect();
            return {left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom, width: rect.width, height: rect.height};
        };
        const parent = getComputedStyle(wrapper);
        const layout = card.querySelector(':scope > .forum-comment-layout');
        const body = card.querySelector('.phase19-body');
        const range = document.createRange();
        range.selectNodeContents(body.querySelector('p'));
        return {
            raw: card.getAttribute('data-depth'), parent: card.getAttribute('data-parent-id'),
            id: card.getAttribute('data-comment-id'), classes: [...card.classList], inline: card.getAttribute('style'),
            marginStart: parseFloat(style.marginInlineStart), marginLeft: parseFloat(style.marginLeft),
            marginRight: parseFloat(style.marginRight), box: box(card), host: box(wrapper),
            basis: wrapper.clientWidth - parseFloat(parent.paddingLeft) - parseFloat(parent.paddingRight),
            contentWidth: card.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight),
            columns: getComputedStyle(layout).gridTemplateColumns.split(' ').map(Number.parseFloat),
            nestedColumns: getComputedStyle(card.querySelector('.phase19-unowned-layout')).gridTemplateColumns.split(' ').length,
            descendants: [...card.querySelectorAll('.forum-comment-identity, .forum-comment-main, .forum-comment-meta, ' +
                '.forum-comment-avatar-slot, .phase19-body, .forum-comment-actions, .forum-comment-footer, button')]
                .map(node => ({class: node.className, ...box(node), visible: node.checkVisibility()})),
            textBoxes: [...range.getClientRects()].map(rect => ({left: rect.left, right: rect.right})),
            avatar: box(card.querySelector('.forum-comment-avatar-slot')),
            body: body.textContent, likes: card.querySelector('.forum-comment-likes').textContent,
            replies: card.querySelector('.forum-comment-replies').textContent,
            buttonId: card.querySelector('button').id, buttonType: card.querySelector('button').type
        };
    }));
    await testInfo.attach(`${mode}-geometry`, {body: JSON.stringify({width, direction, result}, null, 2), contentType: 'application/json'});
    for (const [index, item] of result.entries()) {
        const label = `${mode}/${direction}/${width}/${index}`;
        const bucket = Math.max(0, Math.min(depths[index], 4));
        const expected = Math.min(bucket * (width <= 920 ? 8 : 16), item.basis * 0.08);
        expect(item.raw, `${label}/raw`).toBe(String(depths[index]));
        expect(item.classes.filter(name => name.startsWith('forum-comment-depth-')), `${label}/finite class`)
            .toEqual([`forum-comment-depth-${bucket}`]);
        expect(item.inline, `${label}/inline`).toBe(null);
        expect(item.parent, `${label}/parent independence`).toBe(index === 0 ? 'parent<&"' : null);
        expect(item.id).toBe(`phase19-${mode}-${index}`);
        expect(item.marginStart, `${label}/exact selected step and containing bound`).toBeCloseTo(expected, 1);
        expect(direction === 'rtl' ? item.marginRight : item.marginLeft).toBeCloseTo(expected, 1);
        expect(direction === 'rtl' ? item.marginLeft : item.marginRight).toBe(0);
        expect(item.box.width + expected, `${label}/width flow`).toBeCloseTo(item.basis, 1);
        expect(item.box.left).toBeGreaterThanOrEqual(item.host.left - 0.1);
        expect(item.box.right).toBeLessThanOrEqual(item.host.right + 0.1);
        expect(item.avatar.width).toBe(150);
        expect(item.avatar.height).toBe(150);
        expect(item.columns.length, `${label}/localized layout`).toBe(width <= 920 || item.contentWidth <= 370 ? 1 : 2);
        if (item.columns.length === 2) expect(item.columns[0], `${label}/desktop identity`).toBe(170);
        expect(item.nestedColumns, `${label}/unowned nested layout`).toBe(width <= 920 ? 1 : 2);
        for (const child of item.descendants) {
            expect(child.visible, `${label}/${child.class}/visible`).toBe(true);
            expect(child.left, `${label}/${child.class}/left containment`).toBeGreaterThanOrEqual(item.box.left - 0.1);
            expect(child.right, `${label}/${child.class}/right containment`).toBeLessThanOrEqual(item.box.right + 0.1);
            expect(child.width, `${label}/${child.class}/width`).toBeGreaterThan(0);
            expect(child.height, `${label}/${child.class}/height`).toBeGreaterThan(0);
        }
        for (const text of item.textBoxes) {
            expect(text.left, `${label}/body text left`).toBeGreaterThanOrEqual(item.box.left - 0.1);
            expect(text.right, `${label}/body text right`).toBeLessThanOrEqual(item.box.right + 0.1);
        }
        expect(item.body).toContain(`<depth> & "body" ${mode} ${depths[index]}`);
        expect(item.likes).toBe('2 likes');
        expect(item.replies).toBe('3 replies');
        expect(item.buttonId).toBe(`phase19-${mode}-${index}-action`);
        expect(item.buttonType).toBe('button');
    }
    return result;
}

for (const direction of ['ltr', 'rtl']) {
    for (const [width, hostWidth] of [[1440, 1440], [921, 921], [920, 920], [390, 390], [320, 320],
        [1440, 480], [1440, 438], [1440, 436], [1440, 400], [1440, 390], [1440, 320], [1440, 240]]) {
        test(`Phase19 exact native depth and owned containment ${width}/${hostWidth}/${direction}`, async ({page}, testInfo) => {
            const failures = await load(page, width, hostWidth, direction);
            const direct = await inspect(page, 'direct', width, direction, testInfo);
            const compiled = await inspect(page, 'compiled', width, direction, testInfo);
            expect(compiled.map(item => [item.raw, item.classes, item.marginStart, item.box.width, item.columns]))
                .toEqual(direct.map(item => [item.raw, item.classes, item.marginStart, item.box.width, item.columns]));
            const ids = await page.locator('#phase19-forum-depth-fixture [id]').evaluateAll(nodes => nodes.map(node => node.id));
            expect(new Set(ids).size).toBe(ids.length);
            expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
            expect(failures).toEqual([]);
            const screenshot = testInfo.outputPath(`phase19-${width}-${hostWidth}-${direction}.png`);
            await page.screenshot({path: screenshot, fullPage: true});
            await testInfo.attach('settled-layout', {path: screenshot, contentType: 'image/png'});
        });
    }
}

for (const direction of ['ltr', 'rtl']) {
    for (const [width, hostWidth] of [[390, 390], [1440, 240]]) {
        test(`Phase19 real keyboard Tab and native hitboxes ${width}/${hostWidth}/${direction}`, async ({page}, testInfo) => {
            const failures = await load(page, width, hostWidth, direction);
            const buttons = page.locator('.phase19-forum-comment button.phase19-action');
            await expect(buttons).toHaveCount(16);
            const hits = [];
            for (let index = 0; index < 16; index++) {
                console.log(JSON.stringify({trace: 'Phase19 before keyboard Tab', index, width, hostWidth, direction}));
                await page.keyboard.press('Tab');
                const button = buttons.nth(index);
                await expect(button).toBeFocused();
                const geometry = await button.evaluate(node => {
                    const rect = node.getBoundingClientRect();
                    const host = document.querySelector('#phase19-forum-depth-fixture').getBoundingClientRect();
                    const hit = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
                    return {id: node.id, left: rect.left, right: rect.right, top: rect.top, bottom: rect.bottom,
                        hostLeft: host.left, hostRight: host.right, visible: node.checkVisibility(),
                        hit: hit === node || node.contains(hit)};
                });
                hits.push(geometry);
                console.log(JSON.stringify({trace: 'Phase19 keyboard geometry', geometry}));
                expect(geometry.visible).toBe(true);
                expect(geometry.left).toBeGreaterThanOrEqual(geometry.hostLeft);
                expect(geometry.right).toBeLessThanOrEqual(geometry.hostRight);
                expect(geometry.top).toBeGreaterThanOrEqual(0);
                // Native scroll offsets round to whole pixels while card geometry can be fractional.
                expect(geometry.bottom).toBeLessThanOrEqual(1001);
                expect(geometry.hit).toBe(true);
            }
            await testInfo.attach('native-keyboard-hitboxes', {body: JSON.stringify(hits, null, 2), contentType: 'application/json'});
            expect(failures).toEqual([]);
        });
    }
}

test('Phase19 metadata-only custom cards stay outside new CSS and ordinary host overrides win', async ({page}) => {
    const failures = await load(page, 1440, 320, 'ltr');
    for (const id of ['phase19-custom', 'phase19-legacy']) {
        const state = await page.locator(`#${id}`).evaluate(node => ({
            container: getComputedStyle(node).containerType, start: getComputedStyle(node).marginInlineStart,
            columns: getComputedStyle(node.firstElementChild).gridTemplateColumns.split(' ').length,
            depth: node.getAttribute('data-depth'), classes: node.className
        }));
        expect(state.container).toBe('normal');
        expect(state.start).toBe('0px');
        expect(state.columns).toBe(2);
        expect(state.classes).toBe('forum-comment');
        expect(state.depth).toBe(id === 'phase19-custom' ? '2147483647' : null);
    }
    await page.addStyleTag({content: '.forum-comment { margin-inline-start: 7px; container-type: normal; }'});
    for (const mode of ['direct', 'compiled']) {
        const state = await page.locator(`.phase19-forum-view[data-mode="${mode}"] .phase19-forum-comment`).nth(7)
            .locator(':scope > .forum-comment').evaluate(node => ({
                margin: getComputedStyle(node).marginInlineStart, container: getComputedStyle(node).containerType
            }));
        expect(state).toEqual({margin: '7px', container: 'normal'});
    }
    expect(failures).toEqual([]);
});
