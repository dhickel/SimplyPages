const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const htmlPath = process.env.PHASE20_FIXTURE_HTML || path.join(root, 'simplypages/target/url-validation-browser-fixture.html');
const jsonPath = process.env.PHASE20_FIXTURE_JSON || path.join(root, 'simplypages/target/url-validation-browser-fixture.json');
const sources = [
    'simplypages/src/test/java/io/mindspice/simplypages/integration/UrlValidationBrowserFixtureTest.java',
    'simplypages/src/main/java/io/mindspice/simplypages/components/navigation/Link.java',
    'simplypages/src/main/java/io/mindspice/simplypages/components/Image.java'
];
for (const fixture of [htmlPath, jsonPath]) {
    if (!fs.existsSync(fixture) || sources.some(source => fs.statSync(fixture).mtimeMs < fs.statSync(path.join(root, source)).mtimeMs)) {
        throw new Error('Stale current-Java URL boundary fixture: ' + fixture);
    }
}
const generated = fs.readFileSync(htmlPath, 'utf8');
const oracle = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const units = text => text === null ? null : Array.from({length: text.length}, (_, index) => text.charCodeAt(index));
expect(oracle.localeRestored).toBe(true);
if (process.env.PHASE20_EXPECT_LIBRARY) {
    for (const location of Object.values(oracle.origins)) expect(location).toBe(process.env.PHASE20_EXPECT_LIBRARY);
}

// Independent representation oracle: the encoder replaces invalid HTML units with space;
// literal CR/CRLF then undergo HTML parser newline normalization. No URL decoding here.
function domUnits(input) {
    if (input === null) return [];
    const result = [];
    for (let index = 0; index < input.length; index++) {
        const unit = input[index];
        if (unit === 13) {
            result.push(10);
            if (input[index + 1] === 10) index++;
        } else if ((unit < 32 && ![9, 10].includes(unit)) ||
            (unit >= 0xd800 && unit <= 0xdfff &&
                !(unit <= 0xdbff ? input[index + 1] >= 0xdc00 && input[index + 1] <= 0xdfff : input[index - 1] >= 0xd800 && input[index - 1] <= 0xdbff))) {
            result.push(32);
        } else result.push(unit);
    }
    return result;
}

async function settle(page) {
    await page.evaluate(() => document.fonts.ready);
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    await expect.poll(() => page.evaluate(() => document.getAnimations()
        .filter(animation => animation.pending || animation.playState === 'running').length)).toBe(0);
}

for (const width of [1440, 390]) {
    test(`URL classification preserves native meaning, literals and presentation at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height: 1000});
        const errors = [];
        let interceptedNetworkImages = 0;
        page.on('pageerror', error => errors.push(error.message));
        await page.route('**/*', async route => {
            if (route.request().resourceType() === 'image') {
                interceptedNetworkImages++;
                return route.abort();
            }
            return route.continue();
        });
        await page.route('**/url-validation-test-fixture', route => route.fulfill({contentType: 'text/html; charset=UTF-8', body:
            '<!doctype html><html><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">' +
            '<link rel="stylesheet" href="/css/framework.css"></head><body>' + generated + '</body></html>'}));
        const css = page.waitForResponse(response => new URL(response.url()).pathname === '/css/framework.css');
        await page.goto('/url-validation-test-fixture');
        const response = await css;
        expect(response.status()).toBe(200);
        expect(sha(await response.body())).toBe(sha(fs.readFileSync(path.join(root, 'simplypages/src/main/resources/static/css/framework.css'))));
        const nodes = await page.locator('.url-boundary-case').evaluateAll(wrappers => wrappers.map(wrapper => {
            const node = wrapper.firstElementChild;
            const name = node.tagName === 'A' ? 'href' : 'src';
            const value = node.getAttribute(name);
            const resolved = node[name];
            const numeric = text => Array.from({length: text.length}, (_, index) => text.charCodeAt(index));
            return {id: Number(wrapper.dataset.case), mode: wrapper.dataset.mode, name,
                attribute: numeric(value), resolved, protocol: new URL(resolved, document.baseURI).protocol,
                reparsed: new URL(value, document.baseURI).href};
        }));
        let accepted = 0;
        for (const row of oracle.rows) {
            const actual = nodes.filter(node => node.id === row.id);
            expect(actual.length, `Java policy ${row.id}`).toBe(row.accepted ? 2 : 0);
            if (!row.accepted) continue;
            accepted++;
            expect(actual.map(node => node.mode)).toEqual(['direct', 'compiled']);
            for (const node of actual) {
                expect(node.name).toBe(row.type === 'link' ? 'href' : 'src');
                expect(node.attribute, `literal DOM units ${row.id}`).toEqual(domUnits(row.input));
                expect(node.protocol, `independent native scheme ${row.id}`).toBe(row.protocol);
                expect(node.resolved, `native property/parser parity ${row.id}`).toBe(node.reparsed);
            }
            expect(actual[0].resolved).toBe(actual[1].resolved);
        }
        expect(nodes.length).toBe(accepted * 2);
        expect(oracle.rows.some(row => row.accepted && row.input?.includes(9) && row.protocol === 'data:')).toBe(true);
        expect(oracle.rows.some(row => row.accepted && row.input?.includes(9) && row.protocol === 'http:')).toBe(true);
        expect(oracle.rows.some(row => !row.accepted && row.input?.includes(9) && row.protocol === 'javascript:')).toBe(true);
        // A literal entity-looking Java argument is encoded once, not recursively decoded to TAB.
        const entity = oracle.rows.find(row => row.type === 'image' && row.path === 'constructor' &&
            JSON.stringify(row.input) === JSON.stringify(units('java&#x09;script:x')));
        expect(nodes.find(node => node.id === entity.id).attribute).toEqual(units('java&#x09;script:x'));
        await settle(page);
        const presentation = page.locator('#url-boundary-presentation');
        await expect(presentation.locator('a')).toHaveCount(2);
        await expect(presentation.locator('img')).toHaveCount(2);
        const geometry = await presentation.locator('[data-mode]').evaluateAll(groups => groups.map(group => {
            const link = group.querySelector('a'), image = group.querySelector('img');
            const box = node => {const r = node.getBoundingClientRect(); return {x: r.x, width: r.width, height: r.height};};
            return {link: box(link), image: box(image), visible: link.checkVisibility() && image.checkVisibility(),
                text: link.textContent, alt: image.alt};
        }));
        expect(geometry[0]).toEqual(geometry[1]);
        expect(geometry[0].visible).toBe(true);
        expect(geometry[0].image.width).toBe(40);
        expect(geometry[0].image.height).toBe(40);
        expect(geometry[0].text).toBe('Read the guide');
        const avatar = page.locator('#url-boundary-fallback .forum-comment-avatar-slot');
        await expect(avatar.locator('img')).toHaveCount(0);
        expect(await avatar.evaluate(node => ({width: node.getBoundingClientRect().width, height: node.getBoundingClientRect().height})))
            .toEqual({width: 150, height: 150});
        expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
        expect(errors).toEqual([]);
        await testInfo.attach('native-url-evidence', {body: JSON.stringify({javaCases: oracle.rows.length, accepted,
            nativeNodes: nodes.length, interceptedNetworkImages, nodes, geometry}, null, 2), contentType: 'application/json'});
        await page.screenshot({path: testInfo.outputPath(`url-boundaries-${width}.png`), fullPage: true});
        // Protocol recognition is not image execution, network-fetch coverage or loadability.
    });

    test(`rendered packaged security guidance remains readable at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height: 1000});
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const response = await page.goto('/docs/security/01-security-boundaries-and-safe-rendering');
        expect(response.status()).toBe(200);
        await expect(page.getByRole('heading', {name: 'URL classification is method-specific', exact: true})).toBeVisible();
        await expect(page.getByRole('heading', {name: 'CSS values need their own trust boundary', exact: true})).toHaveCount(1);
        await settle(page);
        expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
        expect(errors).toEqual([]);
        await page.screenshot({path: testInfo.outputPath(`url-security-guidance-${width}.png`), fullPage: true});
    });
}
