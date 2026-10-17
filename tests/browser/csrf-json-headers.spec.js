const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixtureRoot = path.join(root, 'simplypages/target/csrf-json-browser-fixtures');
const formClass = 'io/mindspice/simplypages/components/forms/Form.class';
const libraryJar = path.join(root, 'simplypages/target/simplypages-1.1.0a.jar');
const demoJar = path.join(root, 'demo/target/simplypages-demo.jar');
const sha256 = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const read = relative => fs.readFileSync(path.join(root, relative));
const unpack = (jar, entry) => execFileSync('unzip', ['-p', jar, entry], {maxBuffer: 32 * 1024 * 1024});
let server, html, oracle, assets;
let received = [];

// One owned test-only receiver, never a production/demo endpoint or a substituted HTMX client.
// A conflicting listener is an error, not permission to reuse or stop someone else's service.
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
        html = fs.readFileSync(path.join(fixtureRoot, 'csrf-json.html'), 'utf8');
        oracle = JSON.parse(fs.readFileSync(path.join(fixtureRoot, 'oracle.json'), 'utf8'));
        expect(sha256(read('simplypages/src/main/java/io/mindspice/simplypages/components/forms/Form.java')))
            .toBe(oracle.formSourceSha256);
        expect(sha256(read('simplypages/src/test/java/io/mindspice/simplypages/components/forms/CsrfJsonBrowserFixtureTest.java')))
            .toBe(oracle.fixtureSourceSha256);
        expect(sha256(read('simplypages/target/classes/' + formClass))).toBe(oracle.formClassSha256);
        expect(sha256(read('simplypages/target/test-classes/io/mindspice/simplypages/components/forms/CsrfJsonBrowserFixtureTest.class')))
            .toBe(oracle.fixtureClassSha256);
        expect(sha256(Buffer.from(html, 'ascii'))).toBe(oracle.htmlSha256);
        expect(sha256(unpack(libraryJar, formClass))).toBe(oracle.formClassSha256);
        expect(unpack(demoJar, 'BOOT-INF/lib/simplypages-1.1.0a.jar').equals(fs.readFileSync(libraryJar))).toBe(true);
        assets = {};
        for (const url of ['/css/framework.css', '/webjars/htmx.org/dist/htmx.min.js']) {
            const response = await request.get(url);
            expect(response.status()).toBe(200);
            assets[url] = await response.body();
        }
        expect(assets['/css/framework.css'].equals(read('simplypages/src/main/resources/static/css/framework.css'))).toBe(true);
        const htmxEntry = 'META-INF/resources/webjars/htmx.org/2.0.11/dist/htmx.min.js';
        const webjar = fs.readFileSync(path.join(process.env.HOME,
            '.m2/repository/org/webjars/npm/htmx.org/2.0.11/htmx.org-2.0.11.jar'));
        expect(unpack(demoJar, 'BOOT-INF/lib/htmx.org-2.0.11.jar').equals(webjar)).toBe(true);
        expect(assets['/webjars/htmx.org/dist/htmx.min.js'].equals(unpack(
            path.join(process.env.HOME, '.m2/repository/org/webjars/npm/htmx.org/2.0.11/htmx.org-2.0.11.jar'), htmxEntry))).toBe(true);
        expect(sha256(assets['/webjars/htmx.org/dist/htmx.min.js']))
            .toBe('d6fdc75f204e6bdefa99b69bf1e6d4ac69b8a364f77929f45c13476b4000f717');
        server = http.createServer((req, res) => {
            const url = new URL(req.url, 'http://127.0.0.1:18081');
            if (req.method === 'GET' && url.pathname === '/csrf-json-fixture') {
                res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'});
                res.end(html);
            } else if (req.method === 'GET' && assets[url.pathname]) {
                res.writeHead(200, {'Content-Type': url.pathname.endsWith('.css') ? 'text/css' : 'text/javascript'});
                res.end(assets[url.pathname]);
            } else if (req.method === 'POST' && url.pathname === '/csrf-json-receiver') {
                const chunks = [];
                req.on('data', chunk => chunks.push(chunk));
                req.on('end', () => {
                    const id = url.searchParams.get('id');
                    received.push({id, method: req.method, url: req.url, headers: req.headers,
                        rawHeaders: req.rawHeaders, body: Buffer.concat(chunks).toString('ascii')});
                    res.writeHead(200, {'Content-Type': 'text/plain'});
                    res.end('received ' + id);
                });
            } else {
                res.writeHead(404);
                res.end('not found');
            }
        });
        server.requestTimeout = 10000;
        server.headersTimeout = 10000;
        await new Promise((resolve, reject) => {
            server.once('error', reject);
            server.listen(18081, '127.0.0.1', resolve);
        });
    } catch (error) {
        await closeReceiver();
        throw error;
    }
});
test.afterAll(closeReceiver);

test.beforeEach(async ({page}) => {
    received = [];
    page.csrfErrors = [];
    page.on('pageerror', error => page.csrfErrors.push(error.message));
    await page.goto('http://127.0.0.1:18081/csrf-json-fixture');
    expect(await page.evaluate(() => window.htmx.version)).toBe('2.0.11');
});
test.afterEach(({page}) => { expect(page.csrfErrors).toEqual([]); });

async function nativePairs(page, cases, exhaustive = false) {
    // The expected bytes are manually packed Java char units, not a copied JSON encoder or
    // a charset conversion that could replace isolated surrogates. No code-point iteration.
    return page.evaluate(({cases, exhaustive, chunkSize}) => {
        const decode = base64 => {
            const bytes = atob(base64);
            if (bytes.length % 2) throw new Error('Odd code-unit oracle length');
            const units = [];
            for (let index = 0; index < bytes.length; index += 2) {
                units.push(bytes.charCodeAt(index) | (bytes.charCodeAt(index + 1) << 8));
            }
            return units;
        };
        const sameUnits = (text, units, id, position) => {
            if (typeof text !== 'string' || text.length !== units.length) throw new Error(`${id}: ${position} length/type`);
            for (let index = 0; index < units.length; index++) {
                if (text.charCodeAt(index) !== units[index]) throw new Error(`${id}: ${position} unit ${index}`);
            }
        };
        const coverage = {};
        let comparisons = 0;
        for (const row of cases) {
            const form = document.getElementById(row.id);
            if (!form || form.tagName !== 'FORM') throw new Error(`${row.id}: missing Java form`);
            const parsed = JSON.parse(form.getAttribute('hx-headers'));
            const keys = Object.keys(parsed);
            if (keys.length !== 1 || Object.getOwnPropertyNames(parsed).length !== 1
                || Object.getOwnPropertySymbols(parsed).length !== 0
                || Object.getPrototypeOf(parsed) !== Object.prototype) throw new Error(`${row.id}: not exactly one own pair`);
            const name = keys[0];
            if (!Object.prototype.hasOwnProperty.call(parsed, name)) throw new Error(`${row.id}: inherited pair`);
            const value = Object.getOwnPropertyDescriptor(parsed, name).value;
            const nameUnits = decode(row.nameUnits), valueUnits = decode(row.valueUnits);
            sameUnits(name, nameUnits, row.id, 'key');
            sameUnits(value, valueUnits, row.id, 'value');
            comparisons += nameUnits.length + valueUnits.length;
            if (exhaustive) {
                const position = row.id.startsWith('matrix-key-') ? 'key' : 'value';
                const mode = row.compiled ? 'compiled' : 'direct';
                const units = position === 'key' ? nameUnits : valueUnits;
                if (units.length !== chunkSize * 3) throw new Error(`${row.id}: incomplete chunk`);
                const covered = coverage[position + '-' + mode] ||= new Uint8Array(65536);
                for (let index = 0; index < chunkSize; index++) {
                    const unit = row.start + index;
                    if (units[index * 3] !== 97 || units[index * 3 + 1] !== unit || units[index * 3 + 2] !== 122)
                        throw new Error(`${row.id}: numeric oracle does not cover unit ${unit}`);
                    covered[unit]++;
                }
            }
        }
        if (exhaustive) {
            for (const key of ['key-direct', 'key-compiled', 'value-direct', 'value-compiled']) {
                const units = coverage[key];
                if (!units || units.length !== 65536) throw new Error(`Missing matrix ${key}`);
                for (let unit = 0; unit < 65536; unit++) {
                    if (units[unit] !== 1) throw new Error(`${key}: coverage count ${units[unit]} at ${unit}`);
                }
            }
        }
        return {pairs: cases.length, comparisons, matrices: Object.keys(coverage).length};
    }, {cases, exhaustive, chunkSize: oracle.chunkSize});
}

test('fresh source/classes/library/demo and actual packaged assets/Javadoc agree', async ({page}) => {
    expect(require('playwright/package.json').version).toBe('1.62.0');
    const response = await page.request.get('http://127.0.0.1:18080/javadocs/io/mindspice/simplypages/components/forms/Form.html');
    expect(response.status()).toBe(200);
    expect((await response.body()).equals(read('simplypages/target/reports/apidocs/io/mindspice/simplypages/components/forms/Form.html'))).toBe(true);
    const javadoc = (await response.body()).toString('utf8');
    expect(javadoc).toContain('withHxPostCsrf');
    expect(javadoc).not.toContain('quotedJsonString');
    expect(javadoc).toContain('does not generate or validate');
});

test('native JSON.parse restores all representative/null/empty/reserved own string pairs in both modes', async ({page}) => {
    const cases = oracle.cases.filter(row => row.start < 0);
    const result = await nativePairs(page, cases);
    expect(result.pairs).toBe(40);
    expect(cases.filter(row => row.compiled)).toHaveLength(20);
});

test('native JSON.parse preserves every UTF-16 unit in keys and values, direct and fresh compiled', async ({page}) => {
    const cases = oracle.cases.filter(row => row.start >= 0);
    const result = await nativePairs(page, cases, true);
    expect(result.pairs).toBe(256);
    expect(result.matrices).toBe(4);
    expect(result.comparisons).toBe(4 * 65536 * 3 + 128 * ('fixed-name'.length + 'fixed-value'.length));
});

test('raw Java markup and parsed DOM contain no injected tags/attributes or added JSON keys', async ({page}) => {
    expect([...html.matchAll(/<form\b/g)]).toHaveLength(oracle.cases.length);
    expect([...html.matchAll(/ hx-headers="/g)]).toHaveLength(oracle.cases.length);
    expect([...html.matchAll(/<script\b/g)]).toHaveLength(1);
    expect([...html.matchAll(/ onload="/g)]).toHaveLength(0);
    expect([...html].every(character => character.charCodeAt(0) < 128)).toBe(true);
    const structure = await page.evaluate(rows => {
        for (const row of rows) {
            const form = document.getElementById(row.id);
            const expected = ['class', 'id', 'hx-post', 'hx-headers', ...(row.request ? ['hx-target'] : [])].sort();
            const actual = [...form.attributes].map(attribute => attribute.name).sort();
            if (JSON.stringify(actual) !== JSON.stringify(expected)) throw new Error(`${row.id}: unexpected attributes`);
            if (form.children.length !== (row.request ? 1 : 0)) throw new Error(`${row.id}: unexpected children`);
            if (row.request) {
                const button = form.firstElementChild;
                if (button.tagName !== 'BUTTON' || button.type !== 'submit' || button.id !== row.id + '-submit'
                    || JSON.stringify([...button.attributes].map(attribute => attribute.name).sort()) !== '["class","id","type"]')
                    throw new Error(`${row.id}: submit markup was altered`);
            }
        }
        return {forms: document.querySelectorAll('form').length, buttons: document.querySelectorAll('button').length,
            scripts: document.querySelectorAll('script').length, result: document.querySelectorAll('#csrf-result').length,
            bodyTags: [...document.body.querySelectorAll('*')].map(element => element.tagName).sort(),
            unexpected: document.querySelectorAll('[onload], [onclick], tag, script:not([src])').length};
    }, oracle.cases);
    expect(structure.forms).toBe(oracle.cases.length);
    expect(structure.buttons).toBe(4);
    expect(structure.scripts).toBe(1);
    expect(structure.result).toBe(1);
    expect(structure.unexpected).toBe(0);
    expect(structure.bodyTags).toEqual(['MAIN', 'DIV', ...Array(oracle.cases.length).fill('FORM'), ...Array(4).fill('BUTTON')].sort());
});

for (const kind of ['default', 'custom']) {
    for (const mode of ['direct', 'compiled']) {
        test(`actual bundled HTMX POST sends only configured ${kind} header from ${mode} Java Form`, async ({page}) => {
            const id = `request-${kind}-${mode}`;
            const row = oracle.cases.find(row => row.id === id);
            await nativePairs(page, [row]);
            const nameBytes = Buffer.from(row.nameUnits, 'base64'), valueBytes = Buffer.from(row.valueUnits, 'base64');
            const ascii = bytes => {
                let text = '';
                for (let index = 0; index < bytes.length; index += 2) {
                    const unit = bytes.readUInt16LE(index);
                    expect(unit).toBeGreaterThanOrEqual(32);
                    expect(unit).toBeLessThan(127);
                    text += String.fromCharCode(unit);
                }
                return text;
            };
            const name = ascii(nameBytes), value = ascii(valueBytes);
            const response = page.waitForResponse(response => response.url().endsWith('/csrf-json-receiver?id=' + id));
            const button = page.locator('#' + id + '-submit');
            if (mode === 'compiled') {
                await button.focus();
                await page.keyboard.press('Enter');
            } else {
                await button.click();
            }
            expect((await response).status()).toBe(200);
            await expect(page.locator('#csrf-result')).toHaveText('received ' + id);
            expect(received).toHaveLength(1);
            const actual = received[0];
            expect(actual.id).toBe(id);
            expect(actual.method).toBe('POST');
            expect(actual.body).toBe('');
            expect(actual.headers[name.toLowerCase()]).toBe(value);
            expect(actual.rawHeaders.filter((header, index) => index % 2 === 0 && header.toLowerCase() === name.toLowerCase())).toHaveLength(1);
            for (const unintended of ['x-injected', 'x-unintended', 'onload']) expect(actual.headers[unintended]).toBeUndefined();
            expect(actual.headers['hx-request']).toBe('true');
            await expect(page.locator('form')).toHaveCount(oracle.cases.length);
            await nativePairs(page, [row]);
        });
    }
}
