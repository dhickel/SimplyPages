const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixtureRoot = path.join(root, 'simplypages/target/textarea-browser-fixtures');
const libraryJar = path.join(root, 'simplypages/target/simplypages-1.2.jar');
const demoJar = path.join(root, 'demo/target/simplypages-demo.jar');
const unpack = (jar, entry) => execFileSync('unzip', ['-p', jar, entry], {maxBuffer: 32 * 1024 * 1024});
const sha256 = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const units = text => Array.from({length: text.length}, (_, index) => text.charCodeAt(index));
let server, baseURL, oracle;
let received = [];

test.describe.configure({mode: 'serial'});

async function closeReceiver() {
    if (!server) return;
    const owned = server;
    server = undefined;
    owned.closeAllConnections();
    if (owned.listening) await new Promise((resolve, reject) => owned.close(error => error ? reject(error) : resolve()));
}

test.beforeAll(async ({request}) => {
    try {
        const html = fs.readFileSync(path.join(fixtureRoot, 'textarea.html'));
        oracle = JSON.parse(fs.readFileSync(path.join(fixtureRoot, 'oracle.json'), 'utf8'));
        expect(sha256(html)).toBe(oracle.htmlSha256);
        for (const [relative, hash] of Object.entries(oracle.hashes)) {
            expect(sha256(fs.readFileSync(path.join(root, 'simplypages', relative))), relative).toBe(hash);
            if (relative.startsWith('target/classes/')) {
                expect(sha256(unpack(libraryJar, relative.slice('target/classes/'.length))), relative).toBe(hash);
            }
        }
        expect(unpack(demoJar, 'BOOT-INF/lib/simplypages-1.2.jar').equals(fs.readFileSync(libraryJar))).toBe(true);
        const assets = {};
        for (const url of ['/css/framework.css', '/webjars/htmx.org/dist/htmx.min.js']) {
            const response = await request.get(url);
            expect(response.status()).toBe(200);
            assets[url] = await response.body();
        }
        expect(assets['/css/framework.css'].equals(fs.readFileSync(path.join(root,
            'simplypages/src/main/resources/static/css/framework.css')))).toBe(true);
        expect(sha256(assets['/webjars/htmx.org/dist/htmx.min.js']))
            .toBe('d6fdc75f204e6bdefa99b69bf1e6d4ac69b8a364f77929f45c13476b4000f717');
        server = http.createServer((req, res) => {
            const url = new URL(req.url, 'http://127.0.0.1');
            if (req.method === 'GET' && url.pathname === '/') {
                res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'});
                res.end(html);
            } else if (req.method === 'GET' && assets[url.pathname]) {
                res.writeHead(200, {'Content-Type': url.pathname.endsWith('.css') ? 'text/css' : 'text/javascript'});
                res.end(assets[url.pathname]);
            } else if (req.method === 'POST' && url.pathname === '/submit') {
                const chunks = [];
                req.on('data', chunk => chunks.push(chunk));
                req.on('end', () => {
                    received.push({body: Buffer.concat(chunks).toString('utf8'), headers: req.headers});
                    res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'});
                    res.end('<!doctype html><title>Received</title><p>Received</p>');
                });
            } else {
                res.writeHead(404);
                res.end();
            }
        });
        server.requestTimeout = 10000;
        server.headersTimeout = 10000;
        await new Promise((resolve, reject) => {
            server.once('error', reject);
            server.listen(0, '127.0.0.1', resolve);
        });
        baseURL = `http://127.0.0.1:${server.address().port}`;
    } catch (error) {
        await closeReceiver();
        throw error;
    }
});
test.afterAll(closeReceiver);

async function assertNativeValues(page) {
    const actual = await page.evaluate(() => {
        const units = text => Array.from({length: text.length}, (_, index) => text.charCodeAt(index));
        const form = document.getElementById('values');
        const data = new FormData(form);
        return Array.from(form.querySelectorAll('textarea'), area => ({
            id: area.id, value: units(area.value), defaultValue: units(area.defaultValue),
            submitted: units(data.get(area.name)), rows: area.rows
        }));
    });
    expect(actual).toHaveLength(oracle.cases.length);
    for (let index = 0; index < actual.length; index++) {
        const row = oracle.cases[index];
        const expected = units(Buffer.from(row.expected, 'base64').toString('utf16le'));
        expect(actual[index], row.id).toMatchObject({id: row.id, value: expected, defaultValue: expected, submitted: expected});
    }
    expect(await page.evaluate(() => window.injected)).toBeUndefined();
    await expect(page.locator('script:not([src]), b')).toHaveCount(0);
    await expect(page.locator('textarea')).toHaveCount(181);
    await expect(page.locator('form')).toHaveCount(1);
}

for (const width of [1440, 390]) {
    test.describe(`native textarea ${width}`, () => {
        test.beforeEach(async ({page}) => {
            received = [];
            await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
            page.textareaErrors = [];
            page.on('pageerror', error => page.textareaErrors.push(error.message));
            await page.goto(baseURL);
            await page.waitForFunction(() => window.htmx?.version === '2.0.11');
        });
        test.afterEach(({page}) => { expect(page.textareaErrors).toEqual([]); });

        test('direct/compiled/static/slot/default/cached values preserve logical newlines and escaping', async ({page}, testInfo) => {
            await assertNativeValues(page);
            const first = page.locator('textarea').first();
            await first.focus();
            expect(await first.evaluate(area => {
                const rect = area.getBoundingClientRect();
                const style = getComputedStyle(area);
                return {withinViewport: rect.left >= 0 && rect.right <= innerWidth,
                    visible: rect.top >= 0 && rect.bottom <= innerHeight,
                    rows: area.rows, whiteSpace: style.whiteSpace,
                    noHorizontalOverflow: document.documentElement.scrollWidth <= innerWidth};
            })).toEqual({withinViewport: true, visible: true, rows: 3, whiteSpace: 'pre-wrap', noHorizontalOverflow: true});
            await page.screenshot({path: testInfo.outputPath(`textarea-${width}.png`)});
        });

        test('native reset restores the preserved default without adding or dropping a line', async ({page}) => {
            const first = page.locator('textarea').first();
            await first.fill('changed\nvalue');
            expect(await first.evaluate(area => area.defaultValue)).toBe('\nFirst\nLine');
            await page.locator('#reset').click();
            await assertNativeValues(page);
        });

        for (const htmx of [false, true]) {
            test(`${htmx ? 'bundled HTMX LF' : 'native HTML CRLF'} submission retains all native control values`, async ({page}) => {
                await assertNativeValues(page);
                const response = page.waitForResponse(response => response.url() === baseURL + '/submit' && response.request().method() === 'POST');
                await page.locator(htmx ? '#htmx-submit' : '#native-submit').click();
                expect((await response).status()).toBe(200);
                expect(received).toHaveLength(1);
                expect(received[0].headers['content-type']).toContain('application/x-www-form-urlencoded');
                expect(received[0].headers['hx-request']).toBe(htmx ? 'true' : undefined);
                const data = new URLSearchParams(received[0].body);
                expect([...data.keys()]).toHaveLength(oracle.cases.length);
                for (const row of oracle.cases) {
                    let expected = Buffer.from(row.expected, 'base64').toString('utf16le');
                    if (!htmx) expected = expected.replaceAll('\n', '\r\n');
                    expect(units(data.get(row.id)), row.id).toEqual(units(expected));
                }
                if (htmx) await assertNativeValues(page);
            });
        }
    });
}
