const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const {test, expect} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixtures = path.join(root, 'simplypages/target/phase21-client-browser-fixtures');
const library = path.join(root, 'simplypages/target/simplypages-1.1.1.jar');
const executable = path.join(root, 'demo/target/simplypages-demo.jar');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const unpack = (jar, entry) => execFileSync('unzip', ['-p', jar, entry], {maxBuffer: 32 * 1024 * 1024});
const names = ['X-CSRF-TOKEN', 'hasOwnProperty', 'constructor', 'toString', '__proto__', 'HASOWNPROPERTY',
    '__defineGetter__', '__defineSetter__', '__lookupGetter__', '__lookupSetter__', 'isPrototypeOf',
    'propertyIsEnumerable', 'valueOf', 'toLocaleString'];
const legacy = {selfRequestsOnly: false, methodsThatUseUrlParams: ['get'], scrollBehavior: 'smooth'};
const upstream = {selfRequestsOnly: true, methodsThatUseUrlParams: ['get', 'delete'], scrollBehavior: 'instant'};
let server, cross, base, crossPort, assets, oracle;
let received = [], crossReceived = [];
const documents = {};
const setup = `window.before=[];window.after=[];window.prototypeBefore=Object.getOwnPropertyDescriptors(Object.prototype);
window.headerProtos=new WeakMap();window.getterCalls=0;
document.addEventListener('htmx:configRequest',e=>{
 window.headerProtos.set(e.detail.headers,Object.getPrototypeOf(e.detail.headers));window.before.push(Object.keys(e.detail.headers));
},true);`;
const observe = `document.addEventListener('htmx:configRequest',e=>{
 const h=e.detail.headers;window.after.push({keys:Object.keys(h),protoSame:window.headerProtos.get(h)===Object.getPrototypeOf(h),
 ownProto:Object.getOwnPropertyDescriptor(h,'__proto__'),getterCalls:window.getterCalls});
});`;

function selected() {
    return {selfRequestsOnly: htmx.config.selfRequestsOnly, methodsThatUseUrlParams: htmx.config.methodsThatUseUrlParams,
        scrollBehavior: htmx.config.scrollBehavior};
}
const listen = receiver => new Promise((resolve, reject) => {
    receiver.once('error', reject);
    receiver.listen(0, '127.0.0.1', () => resolve(receiver.address().port));
});
async function closeReceivers() {
    for (const owned of [server, cross]) {
        if (!owned) continue;
        owned.closeAllConnections();
        if (owned.listening) await new Promise((resolve, reject) => owned.close(error => error ? reject(error) : resolve()));
    }
    server = cross = undefined;
}

test.beforeAll(async ({request}) => {
    try {
        oracle = JSON.parse(fs.readFileSync(path.join(fixtures, 'oracle.json'), 'utf8'));
        for (const [name, hash] of Object.entries(oracle.files)) {
            const bytes = fs.readFileSync(path.join(fixtures, name));
            expect(sha(bytes), name).toBe(hash);
            documents[name] = bytes.toString('utf8');
        }
        for (const [name, hash] of Object.entries(oracle.inputs)) {
            const relative = name.endsWith('.class') ? 'simplypages/target/'
                + (name.includes('Phase21ClientCompatibilityBrowserFixtureTest') ? 'test-classes/' : 'classes/') + name : name;
            expect(sha(fs.readFileSync(path.join(root, relative))), name).toBe(hash);
            if (name.endsWith('.class') && !name.includes('Phase21ClientCompatibilityBrowserFixtureTest')) {
                expect(sha(unpack(library, name)), name).toBe(hash);
            }
        }
        expect(unpack(executable, 'BOOT-INF/lib/simplypages-1.1.1.jar').equals(fs.readFileSync(library))).toBe(true);
        const webjar = path.join(process.env.HOME, '.m2/repository/org/webjars/npm/htmx.org/2.0.11/htmx.org-2.0.11.jar');
        expect(unpack(executable, 'BOOT-INF/lib/htmx.org-2.0.11.jar').equals(fs.readFileSync(webjar))).toBe(true);
        assets = {};
        for (const [url, jar, entry] of [
            ['/webjars/htmx.org/dist/htmx.min.js', webjar, 'META-INF/resources/webjars/htmx.org/2.0.11/dist/htmx.min.js'],
            ['/js/framework.js', library, 'static/js/framework.js'],
            ['/css/framework.css', library, 'static/css/framework.css']
        ]) {
            const response = await request.get(url);
            expect(response.status(), url).toBe(200);
            expect(response.headers()['content-type']).toMatch(url.endsWith('.css') ? /text\/css/ : /(?:text|application)\/javascript/);
            assets[url] = await response.body();
            expect(assets[url].equals(unpack(jar, entry)), url).toBe(true);
            if (url.startsWith('/js/') || url.startsWith('/css/')) {
                expect(assets[url].equals(fs.readFileSync(path.join(root, 'simplypages/src/main/resources/static', url))), url).toBe(true);
            }
        }
        expect(sha(assets['/webjars/htmx.org/dist/htmx.min.js']))
            .toBe('d6fdc75f204e6bdefa99b69bf1e6d4ac69b8a364f77929f45c13476b4000f717');
        expect(assets['/webjars/htmx.org/dist/htmx.min.js']).toHaveLength(52182);
        cross = http.createServer((req, res) => {
            res.setHeader('Access-Control-Allow-Origin', '*');
            res.setHeader('Access-Control-Allow-Methods', 'GET,POST,DELETE,OPTIONS');
            res.setHeader('Access-Control-Allow-Headers', req.headers['access-control-request-headers'] || '*');
            if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }
            crossReceived.push({method: req.method, url: req.url});
            res.writeHead(200, {'Content-Type': 'text/html'}); res.end('cross-ack');
        });
        crossPort = await listen(cross);
        server = http.createServer((req, res) => {
            const url = new URL(req.url, 'http://localhost');
            if (assets[url.pathname]) {
                res.writeHead(200, {'Content-Type': url.pathname.endsWith('.css') ? 'text/css' : 'text/javascript'});
                return res.end(assets[url.pathname]);
            }
            if (url.pathname === '/observe.js') {
                res.writeHead(200, {'Content-Type': 'text/javascript'}); return res.end(observe);
            }
            if (url.pathname === '/custom.js') {
                res.writeHead(200, {'Content-Type': 'text/javascript'});
                return res.end(`window.beforeCustom=(${selected.toString()})();
if(new URL(location.href).searchParams.get('override')==='immediate')Object.assign(htmx.config,${JSON.stringify(upstream)});
if(new URL(location.href).searchParams.get('override')==='ready')document.addEventListener('DOMContentLoaded',()=>Object.assign(htmx.config,${JSON.stringify(upstream)}));`);
            }
            if (url.pathname === '/forms') {
                const html = documents['forms.html'].replace('<head>', '<head><script>' + setup + '</script>')
                    .replace('</head>', '<script defer src="/observe.js"></script></head>');
                res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'}); return res.end(html);
            }
            if (url.pathname === '/shell') {
                const mode = url.searchParams.get('mode');
                let html = documents[(url.searchParams.get('kind') || 'shell') + '-' + (mode !== 'manual') + '.html'];
                if (mode === 'manual') html = html.replace('<head>', '<head><script defer src="/webjars/htmx.org/dist/htmx.min.js"></script>');
                if (mode === 'plain-meta') html = html.replace(/<meta name="simplypages-htmx-compatibility" content="1"\s*\/?>/,
                    '<meta name="htmx-config" content=\'' + JSON.stringify(legacy) + '\'>');
                if (mode === 'host-meta') html = html.replace('<head>', '<head><meta name="htmx-config" content=\'' + JSON.stringify(upstream) + '\'>');
                res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'}); return res.end(html);
            }
            if (url.pathname === '/manual') {
                const meta = url.searchParams.has('legacy') ? '<meta name="htmx-config" content=\'' + JSON.stringify(legacy) + '\'>' : '';
                res.writeHead(200, {'Content-Type': 'text/html; charset=UTF-8'});
                return res.end('<!doctype html><head>' + meta + '<script defer src="/webjars/htmx.org/dist/htmx.min.js"></script>'
                    + '<script defer src="/js/framework.js"></script></head><body>Manual host</body>');
            }
            if (url.pathname === '/receive' || url.pathname === '/wire') {
                const chunks = [];
                req.on('data', chunk => chunks.push(chunk));
                req.on('end', () => {
                    // req.headers loses an own __proto__ in Node. Capture raw pairs as safe own data properties.
                    received.push({id: url.searchParams.get('id'), method: req.method, url: req.url,
                        headers: Object.fromEntries(req.rawHeaders.flatMap((value, index, all) => index % 2 ? [] : [[value.toLowerCase(), all[index + 1]]])),
                        rawHeaders: req.rawHeaders, body: Buffer.concat(chunks).toString('utf8')});
                    res.writeHead(200, {'Content-Type': 'text/html'}); res.end('ack');
                });
                return;
            }
            if (url.pathname === '/fragment') { res.writeHead(200, {'Content-Type': 'text/html'}); return res.end('scroll-ack'); }
            res.writeHead(204); res.end();
        });
        server.requestTimeout = server.headersTimeout = 10000;
        base = 'http://127.0.0.1:' + await listen(server);
    } catch (error) { await closeReceivers(); throw error; }
});
test.afterAll(closeReceivers);
test.beforeEach(async ({page}) => {
    received = []; crossReceived = [];
    page.compatibilityErrors = [];
    page.on('pageerror', error => page.compatibilityErrors.push(error.message));
});
test.afterEach(async ({page}, info) => {
    expect(page.compatibilityErrors).toEqual([]);
    await info.attach('native-wire', {body: Buffer.from(JSON.stringify({received, crossReceived})), contentType: 'application/json'});
});

async function openForms(page) {
    await page.goto(base + '/forms');
    expect(await page.evaluate(() => htmx.version)).toBe('2.0.11');
}
async function prototypeSafe(page) {
    expect(await page.evaluate(() => {
        const before = window.prototypeBefore, after = Object.getOwnPropertyDescriptors(Object.prototype);
        return Reflect.ownKeys(before).length === Reflect.ownKeys(after).length
            && Reflect.ownKeys(before).every(key => Reflect.ownKeys(before[key]).every(field => before[key][field] === after[key][field]));
    })).toBe(true);
}
function header(name, value, record = received[0]) {
    const key = name.toLowerCase();
    expect(Object.getOwnPropertyDescriptor(record.headers, key)?.value).toBe(value);
    expect(record.rawHeaders.filter((entry, index) => index % 2 === 0 && entry.toLowerCase() === key)).toHaveLength(value === undefined ? 0 : 1);
    for (const extra of ['x-injected', 'x-unintended', 'onload']) expect(Object.hasOwn(record.headers, extra)).toBe(false);
}
async function submitted(page, id, mode = 'native', compiled = false) {
    if (mode === 'ajax') {
        await page.evaluate(async id => {
            const form = document.getElementById(id);
            const pending = htmx.ajax('POST', form.getAttribute('hx-post'), {source: form, swap: 'none'});
            if (!pending || typeof pending.then !== 'function') throw new Error('ajax is not thenable');
            await pending;
        }, id);
    } else if (compiled) await page.locator('#' + id + ' input').press('Enter');
    else await page.locator('#' + id + '-submit').click();
    await expect.poll(() => received.length).toBe(1);
    await page.waitForTimeout(60);
    expect(received).toHaveLength(1);
}

for (const mode of ['native', 'ajax']) for (const compiled of [false, true]) for (let index = 0; index < names.length; index++) {
    test(`Form ${mode} ${compiled ? 'compiled' : 'direct'} exact own ${names[index]}`, async ({page}) => {
        await openForms(page);
        const id = `pair-${index}-${compiled}`;
        expect(await page.locator('#' + id).getAttribute('class')).toBe('caller-replaced');
        expect(await page.evaluate(id => {
            const pair = JSON.parse(document.getElementById(id).getAttribute('hx-headers'));
            return {keys: Object.keys(pair), value: Object.getOwnPropertyDescriptor(pair, Object.keys(pair)[0]).value};
        }, id)).toEqual({keys: [names[index]], value: 'synthetic-token'});
        await submitted(page, id, mode, compiled);
        header(names[index], 'synthetic-token');
        expect(received[0].body).toBe('field=native-value');
        expect(received[0].method).toBe('POST');
        expect(received[0].headers['hx-request']).toBe('true');
        for (const name of names.filter(name => name.toLowerCase() !== names[index].toLowerCase())) header(name, undefined);
        expect(await page.evaluate(() => window.after.length)).toBe(1);
        expect(await page.evaluate(() => window.after.every(row => row.protoSame))).toBe(true);
        await prototypeSafe(page);
    });
}
for (const id of ['evaluated', 'multi', 'duplicate', 'nonstring', 'inherited', 'nonform']) {
    test(`hook excludes ${id} without generalized parsing`, async ({page}) => {
        await openForms(page);
        await page.locator('#' + (id === 'nonform' ? id : id + '-submit')).click();
        await expect.poll(() => received.length).toBe(1);
        await page.waitForTimeout(60);
        expect(received).toHaveLength(1);
        header('toString', undefined);
        expect(await page.evaluate(() => window.before[0])).toEqual(await page.evaluate(() => window.after[0].keys));
        await prototypeSafe(page);
    });
}
test('late NEW Java form insertion and repeated processing activate exactly once per native submit', async ({page}) => {
    await openForms(page);
    await page.evaluate(() => {
        const form = document.getElementById('pair-4-true'), markup = form.outerHTML;
        form.remove(); document.getElementById('late').insertAdjacentHTML('beforeend', markup);
        htmx.process(document.body); htmx.process(document.body); htmx.trigger(document.body, 'htmx:load');
    });
    for (let index = 0; index < 3; index++) {
        await page.locator('#pair-4-true-submit').click();
        await expect.poll(() => received.length).toBe(index + 1);
        await page.waitForTimeout(60);
    }
    expect(received).toHaveLength(3);
    for (const row of received) header('__proto__', 'synthetic-token', row);
    expect(await page.evaluate(() => window.after.length)).toBe(3);
    await prototypeSafe(page);
});
for (const policy of ['own-before', 'accessor-before', 'own-null-before', 'later-override', 'cancel-before', 'cancel-after', 'ajax-own']) {
    test(`existing event-stage application policy: ${policy}`, async ({page}) => {
        await openForms(page);
        await page.evaluate(policy => {
            const form = document.getElementById('pair-4-false');
            if (policy !== 'ajax-own') form.addEventListener('htmx:configRequest', event => {
                if (policy === 'cancel-before') event.preventDefault();
                if (policy === 'own-before') Object.defineProperty(event.detail.headers, '__proto__', {value: 'app-before', enumerable: true, writable: true, configurable: true});
                if (policy === 'accessor-before') Object.defineProperty(event.detail.headers, '__proto__', {get: () => {window.getterCalls++; return 'app-accessor';}, enumerable: true, configurable: true});
                if (policy === 'own-null-before') Object.defineProperty(event.detail.headers, '__proto__', {value: null, enumerable: true, writable: true, configurable: true});
            });
            if (policy === 'later-override' || policy === 'cancel-after') document.addEventListener('htmx:configRequest', event => {
                if (policy === 'cancel-after') event.preventDefault();
                else Object.defineProperty(event.detail.headers, '__proto__', {value: 'app-after', enumerable: true, writable: true, configurable: true});
            });
        }, policy);
        if (policy === 'ajax-own') await page.evaluate(async () => {
            await htmx.ajax('POST', '/receive?id=ajax-own', {source: document.getElementById('pair-2-false'),
                headers: JSON.parse('{"constructor":"app-ajax"}'), swap: 'none'});
        });
        else await page.locator('#pair-4-false-submit').click();
        await page.waitForTimeout(150);
        expect(received).toHaveLength(policy.startsWith('cancel') ? 0 : 1);
        if (!policy.startsWith('cancel')) header(policy === 'ajax-own' ? 'constructor' : '__proto__',
            {'own-before': 'app-before', 'accessor-before': 'app-accessor', 'own-null-before': undefined,
                'later-override': 'app-after', 'ajax-own': 'app-ajax'}[policy]);
        if (policy === 'accessor-before') expect(await page.evaluate(() => window.after[0].getterCalls)).toBe(0);
        await prototypeSafe(page);
    });
}
for (const policy of ['case-alias', 'frozen-missing', 'null-prototype', 'ordinary-deleted', 'nonenumerable-own']) {
    test(`missing-entry ownership: ${policy}`, async ({page}) => {
        await openForms(page);
        const id = policy === 'ordinary-deleted' ? 'pair-0-false' : 'pair-2-false';
        await page.evaluate(({id, policy}) => document.getElementById(id).addEventListener('htmx:configRequest', event => {
            if (policy === 'case-alias') Object.defineProperty(event.detail.headers, 'Constructor', {value: 'case-override', enumerable: true});
            if (policy === 'frozen-missing') Object.freeze(event.detail.headers);
            if (policy === 'null-prototype') event.detail.headers = Object.create(null);
            if (policy === 'ordinary-deleted') delete event.detail.headers['X-CSRF-TOKEN'];
            if (policy === 'nonenumerable-own') Object.defineProperty(event.detail.headers, 'constructor', {value: 'do-not-emit', enumerable: false});
        }), {id, policy});
        await submitted(page, id);
        header(policy === 'ordinary-deleted' ? 'X-CSRF-TOKEN' : 'constructor',
            policy === 'case-alias' ? 'case-override' : policy === 'null-prototype' ? 'synthetic-token' : undefined);
        await prototypeSafe(page);
    });
}

for (const kind of ['shell', 'template']) for (const [mode, override, expected] of [
    ['owned', 'none', legacy], ['owned', 'immediate', upstream], ['owned', 'ready', upstream],
    ['host-meta', 'none', upstream], ['manual', 'none', upstream], ['manual', 'immediate', upstream],
    ['plain-meta', 'immediate', legacy]
]) {
    test(`${kind} ${mode}/${override}: defaults, DELETE wire, real CORS and native scroll`, async ({page}) => {
        await page.goto(base + `/shell?kind=${kind}&mode=${mode}&override=${override}`);
        expect(await page.evaluate(selected)).toEqual(expected);
        expect(await page.evaluate(() => window.beforeCustom)).toEqual(['owned', 'host-meta'].includes(mode) ? legacy : upstream);
        await expect(page.locator('meta[name="simplypages-htmx-compatibility"]')).toHaveCount(['owned', 'host-meta'].includes(mode) ? 1 : 0);
        await page.evaluate(async () => { await htmx.ajax('DELETE', '/wire', {values: {field: 'wire-value'}, swap: 'none'}); });
        expect(received).toHaveLength(1);
        const query = expected.methodsThatUseUrlParams.includes('delete');
        expect(received[0]).toMatchObject({method: 'DELETE', url: query ? '/wire?field=wire-value' : '/wire', body: query ? '' : 'field=wire-value'});
        const outcome = await page.evaluate(async port => {
            try { await htmx.ajax('GET', 'http://127.0.0.1:' + port + '/cross', {swap: 'none'}); return 'resolved'; }
            catch (_) { return 'rejected'; }
        }, crossPort);
        expect(crossReceived).toHaveLength(expected.selfRequestsOnly ? 0 : 1);
        expect(outcome).toBe(expected.selfRequestsOnly ? 'rejected' : 'resolved');
        const scroll = await page.evaluate(async () => {
            const target = document.getElementById('scroll-target'), native = target.scrollIntoView, calls = [];
            target.scrollIntoView = function(options) { calls.push(options); return native.call(this, options); };
            await htmx.ajax('GET', '/fragment', {target: '#scroll-target', swap: 'innerHTML show:top'});
            await new Promise(resolve => setTimeout(resolve, 200));
            return {calls, y: scrollY};
        });
        expect(scroll.calls).toHaveLength(1);
        expect(scroll.calls[0].behavior).toBe(expected.scrollBehavior);
        expect(scroll.y).toBeGreaterThan(0);
        // The plain-meta diagnostic intentionally proves why upstream compatibility meta was rejected.
        if (mode === 'plain-meta') expect(await page.evaluate(selected)).not.toEqual(upstream);
    });
}
for (const included of [true, false]) {
    test(`body HTMX=${included} remains marker/script/asset free`, async () => {
        expect(documents[`body-${included}.html`]).not.toMatch(/<script|<meta|<link/);
        expect(documents[`body-${included}.html`]).not.toContain('simplypages-htmx-compatibility');
    });
}
for (const configured of [false, true]) {
    test(`unmarked manual host ${configured ? 'explicit legacy config' : 'upstream defaults'} stays host owned`, async ({page}) => {
        await page.goto(base + '/manual' + (configured ? '?legacy' : ''));
        expect(await page.evaluate(selected)).toEqual(configured ? legacy : upstream);
        await expect(page.locator('meta[name="simplypages-htmx-compatibility"]')).toHaveCount(0);
    });
}
test('owned defaults are never reapplied during insertion, processing or settle', async ({page}) => {
    await page.goto(base + '/shell?kind=template&mode=owned&override=none');
    expect(await page.evaluate(selected)).toEqual(legacy);
    await page.evaluate(config => {
        Object.assign(htmx.config, config);
        document.getElementById('shell-content').insertAdjacentHTML('beforeend', '<div id="inserted">Later content</div>');
        htmx.process(document.body); htmx.trigger(document.body, 'htmx:load');
    }, upstream);
    await page.evaluate(async () => { await htmx.ajax('GET', '/fragment', {target: '#inserted', swap: 'innerHTML'}); });
    await expect(page.locator('.htmx-settling, .htmx-added, .htmx-request')).toHaveCount(0);
    expect(await page.evaluate(selected)).toEqual(upstream);
});
