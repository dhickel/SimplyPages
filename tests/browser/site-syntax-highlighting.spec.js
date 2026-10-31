const {test, expect} = require('playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '../..');
const vendor = '/vendor/highlightjs/11.12.0/';
const fixture = path.join(root, 'site/target/site-syntax-highlighting-fixture.html');
const fixtureSources = [
    'site/src/test/java/io/mindspice/demo/SiteSyntaxHighlightingTest.java',
    'site/src/main/java/io/mindspice/demo/SiteShell.java',
    'site/src/main/java/io/mindspice/demo/DocumentationService.java',
    'simplypages/src/main/java/io/mindspice/simplypages/components/Code.java'
];
if (!fs.existsSync(fixture) || fixtureSources.some(source => fs.statSync(path.join(root, source)).mtimeMs > fs.statSync(fixture).mtimeMs)) {
    throw new Error('Run fresh Maven package with SiteSyntaxHighlightingTest before browser validation');
}

function observe(page) {
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => {
        if (['warning', 'error'].includes(message.type()) || /already highlighted/.test(message.text())) errors.push(message.text());
    });
    return errors;
}

async function fixtureRoute(page) {
    await page.route('**/syntax-fixture', route => route.fulfill({contentType:'text/html; charset=UTF-8', body:fs.readFileSync(fixture, 'utf8')}));
}

async function sourceText(page, route) {
    const response = await page.request.get(route);
    expect(response.status()).toBe(200);
    return page.evaluate(html => [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('.sp-site-main pre code')].map(code => code.textContent), await response.text());
}

async function assertSource(page, source) {
    expect(await page.locator('.sp-site-main pre code').allTextContents()).toEqual(source);
}

for (const width of [1440, 390]) {
    test(`real home, demo and documentation tokens retain text and layout at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height:900});
        const errors = observe(page);
        const external = [];
        page.on('request', request => { if (new URL(request.url()).origin !== 'http://127.0.0.1:18080') external.push(request.url()); });
        const routes = [
            ['home', '/', 'java'],
            ['demo', '/demos/components/text-structure', 'java'],
            ['docs-java', '/docs/tutorials/first-page.md', 'java'],
            ['docs-xml', '/docs/getting-started/01-installation-and-first-static-page.md', 'xml'],
            ['docs-bash', '/docs/how-to/build-test-and-document.md', 'bash'],
            ['docs-css', '/docs/core/05-css-defaults-overrides-and-structure.md', 'css'],
            ['docs-markdown', '/docs/how-to/build-a-markdown-blog.md', 'markdown']
        ];
        const contrasts = [];
        for (const [name, route, language] of routes) {
            const source = await sourceText(page, route);
            await page.goto(route);
            const code = page.locator(`.sp-site-main pre code.language-${language}.hljs`).filter({has:page.locator('span')}).first();
            await expect(code.locator('span').first()).toBeAttached();
            await assertSource(page, source);
            await expect(page.locator(`script[src='${vendor}highlight.min.js']`)).toHaveCount(1);
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), route).toBe(true);
            const samples = await page.locator('.sp-site-main pre code.hljs, .sp-site-main pre code.hljs span').evaluateAll(nodes => {
                function rgb(value) { return value.match(/[\d.]+/g).slice(0, 3).map(Number); }
                function luminance(color) {
                    return rgb(color).map(value => value / 255).map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
                        .reduce((sum, value, index) => sum + value * [.2126, .7152, .0722][index], 0);
                }
                return nodes.filter(node => node.textContent.trim()).map(node => {
                    let background = node;
                    while (background && getComputedStyle(background).backgroundColor === 'rgba(0, 0, 0, 0)') background = background.parentElement;
                    const color = getComputedStyle(node).color;
                    const bg = getComputedStyle(background).backgroundColor;
                    const a = luminance(color), b = luminance(bg);
                    return {token:node.className, color, background:bg, contrast:(Math.max(a, b) + .05) / (Math.min(a, b) + .05)};
                });
            });
            expect(new Set(samples.map(sample => sample.color)).size, route).toBeGreaterThan(1);
            for (const sample of samples) expect(sample.contrast, JSON.stringify(sample)).toBeGreaterThanOrEqual(4.5);
            contrasts.push({route, minimum:Math.min(...samples.map(sample => sample.contrast)), samples:[...new Map(samples.map(sample => [sample.color, sample])).values()]});
            await code.evaluate(node => {
                const pre = node.closest('pre');
                pre.scrollIntoView({block:'center', inline:'nearest'});
                pre.scrollLeft = 0;
            });
            await page.screenshot({path:testInfo.outputPath(`${name}-${width}.png`)});
        }
        const contrastPath = testInfo.outputPath('measured-token-contrast.json');
        fs.writeFileSync(contrastPath, JSON.stringify(contrasts, null, 2));
        await testInfo.attach('measured-token-contrast', {path:contrastPath, contentType:'application/json'});
        expect(errors).toEqual([]);
        expect(external).toEqual([]);
    });
}

test('real docs outerHTML navigation, repeat navigation and history preserve highlighted source', async ({page}) => {
    const errors = observe(page);
    await page.goto('/docs');
    await page.evaluate(() => window.sameDocument = true);
    for (const route of ['/docs/tutorials/first-page.md', '/docs/core/05-css-defaults-overrides-and-structure.md', '/docs/tutorials/first-page.md']) {
        const source = await sourceText(page, route);
        await page.locator(`.docs-nav a[href='${route}']`).click();
        await expect(page).toHaveURL(new RegExp(route.replaceAll('.', '\\.').replaceAll('/', '\\/') + '$'));
        await expect(page.locator('#docs-content pre code.hljs').first()).toHaveAttribute('data-highlighted', 'yes');
        await expect(page.locator('#docs-content')).toHaveCount(1);
        await assertSource(page, source);
        expect(await page.evaluate(() => window.sameDocument)).toBe(true);
        const before = await page.locator('#docs-content').innerHTML();
        await page.locator('#docs-content').evaluate(article => {
            for (let i = 0; i < 3; i++) htmx.trigger(article, 'htmx:load', {elt:article});
        });
        expect(await page.locator('#docs-content').innerHTML()).toBe(before);
    }
    await page.goBack();
    await expect(page).toHaveURL(/05-css-defaults-overrides-and-structure/);
    await expect(page.locator('#docs-content .language-css.hljs').first()).toHaveAttribute('data-highlighted', 'yes');
    await assertSource(page, await sourceText(page, '/docs/core/05-css-defaults-overrides-and-structure.md'));
    expect(errors).toEqual([]);
});

test('actual HTMX swaps highlight wrapper, pre, code roots and OOB nodes exactly once', async ({page}) => {
    const errors = observe(page);
    await page.goto('/');
    const source = '\n\t// <script>window.snippetExecuted=true</script> &amp;\nclass Demo { String value = "<>&\\\"\'"; }  \n\n';
    const escaped = source.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
    await page.evaluate(() => {
        const host = document.createElement('div');
        host.id = 'syntax-host';
        const pre = document.createElement('pre');
        pre.id = 'syntax-oob';
        document.querySelector('.sp-site-content').append(host, pre);
    });
    for (const [name, html, target] of [
        ['wrapper', `<div id="syntax-host"><pre><code class="language-java">${escaped}</code></pre></div>`, '#syntax-host'],
        ['pre', `<pre id="syntax-host"><code id="syntax-code" class="language-java">${escaped}</code></pre>`, '#syntax-host'],
        ['code', `<code id="syntax-code" class="language-java">${escaped}</code>`, '#syntax-code'],
        ['oob', `<code id="syntax-code" class="language-java">${escaped}</code><pre id="syntax-oob" hx-swap-oob="outerHTML"><code class="language-html">${escaped}</code></pre>`, '#syntax-code']
    ]) {
        await page.route(`**/syntax-fragment-${name}`, route => route.fulfill({contentType:'text/html', body:html}));
        await page.evaluate(({name, target}) => htmx.ajax('GET', '/syntax-fragment-' + name, {target, swap:'outerHTML'}), {name, target});
        const code = page.locator('#syntax-host code');
        await expect(code).toHaveAttribute('data-highlighted', 'yes');
        expect(await code.textContent()).toBe(source);
        const before = await code.innerHTML();
        await code.evaluate(node => {
            for (let i = 0; i < 3; i++) htmx.trigger(node, 'htmx:load', {elt:node});
        });
        expect(await code.innerHTML()).toBe(before);
        if (name === 'oob') {
            await expect(page.locator('#syntax-oob code')).toHaveAttribute('data-highlighted', 'yes');
            expect(await page.locator('#syntax-oob code').textContent()).toBe(source);
        }
    }
    expect(await page.evaluate(() => window.snippetExecuted)).toBeUndefined();
    await expect(page.locator('#syntax-host script, #syntax-host img')).toHaveCount(0);
    expect(errors).toEqual([]);
});

test('Java-rendered fences, aliases, special text, plaintext and nested Code preview stay safe', async ({page}) => {
    const errors = observe(page);
    await fixtureRoute(page);
    await page.goto('/syntax-fixture');
    const expected = await page.evaluate(html => [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('.sp-site-main pre code')].map(code => code.textContent), fs.readFileSync(fixture, 'utf8'));
    await assertSource(page, expected);
    for (const language of ['java', 'xml', 'html', 'bash', 'sh', 'css', 'markdown', 'md']) {
        const blocks = page.locator(`.sp-site-docs-article > pre:not(.nohighlight) code.language-${language}`);
        expect(await blocks.count()).toBeGreaterThan(0);
        for (const block of await blocks.all()) await expect(block).toHaveAttribute('data-highlighted', 'yes');
    }
    for (const selector of ['.language-unknown', '.language-text', '.language-mermaid', 'pre code:not([class])', 'pre.nohighlight code']) {
        await expect(page.locator(selector)).not.toHaveClass(/hljs/);
        await expect(page.locator(selector + ' span')).toHaveCount(0);
    }
    await expect(page.locator('.sp-site-preview pre code span').first()).toBeAttached();
    await expect(page.locator('#rendered-preview')).toHaveText('Actual preview');
    await expect(page.locator('.code-inline span, .code-title span, .sp-site-main pre script, .sp-site-main pre img')).toHaveCount(0);
    expect(await page.evaluate(() => window.snippetExecuted)).toBeUndefined();
    const original = await page.locator('.sp-site-main').innerHTML();
    await page.locator('.sp-site-main').evaluate(node => htmx.trigger(node, 'htmx:load', {elt:node}));
    expect(await page.locator('.sp-site-main').innerHTML()).toBe(original);
    expect(errors).toEqual([]);
});

for (const mode of ['no-javascript', 'failed-vendor']) {
    test(`${mode} leaves safe, source-identical readable code`, async ({browser}) => {
        const context = await browser.newContext({javaScriptEnabled:mode !== 'no-javascript'});
        const page = await context.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        if (mode === 'failed-vendor') await page.route('**/vendor/highlightjs/**', route => route.abort());
        await fixtureRoute(page);
        await page.goto('http://127.0.0.1:18080/syntax-fixture');
        const expected = await page.evaluate(html => [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('.sp-site-main pre code')].map(code => code.textContent), fs.readFileSync(fixture, 'utf8'));
        await assertSource(page, expected);
        await expect(page.locator('pre code.hljs, pre code span, pre code script, pre code img')).toHaveCount(0);
        await expect(page.locator('pre code').first()).toBeVisible();
        expect(await page.evaluate(() => window.snippetExecuted)).toBeUndefined();
        expect(errors).toEqual([]);
        await context.close();
    });
}

test('highlighting changes paint only and preserves native selection and horizontal overflow', async ({page}) => {
    async function selectedText() {
        return page.locator('pre code.language-java').first().evaluate(code => {
            const range = document.createRange();
            range.selectNodeContents(code);
            getSelection().removeAllRanges();
            getSelection().addRange(range);
            return getSelection().toString();
        });
    }
    async function metrics() {
        return page.locator('.sp-site-main pre').evaluateAll(nodes => nodes.map(node => {
            const style = getComputedStyle(node);
            const codeStyle = getComputedStyle(node.querySelector('code'));
            return {
                width:node.getBoundingClientRect().width, height:node.getBoundingClientRect().height,
                scrollWidth:node.scrollWidth, scrollHeight:node.scrollHeight,
                padding:style.padding, overflow:style.overflowX,
                font:codeStyle.font, lineHeight:codeStyle.lineHeight
            };
        }));
    }
    for (const width of [1440, 390]) {
        await page.setViewportSize({width, height:900});
        for (const route of ['/', '/docs/tutorials/first-page.md']) {
            await page.route('**/vendor/highlightjs/**', request => request.abort());
            await page.goto(route);
            const plain = await metrics();
            const plainSelection = await selectedText();
            await page.unroute('**/vendor/highlightjs/**');
            await page.reload();
            await expect(page.locator('pre code.hljs').first()).toHaveAttribute('data-highlighted', 'yes');
            expect(await metrics()).toEqual(plain);
            expect(await selectedText()).toBe(plainSelection);
            if (width === 390) {
                const scrolled = await page.locator('pre code.language-java.hljs').first().evaluate(code => {
                    const pre = code.closest('pre');
                    pre.scrollLeft = pre.scrollWidth;
                    return pre.scrollLeft;
                });
                expect(scrolled).toBeGreaterThan(0);
            }
        }
    }
});

test('packaged local assets match upstream bytes without any remote runtime requests', async ({page}) => {
    const external = [];
    await page.route('**/*', route => {
        if (new URL(route.request().url()).origin !== 'http://127.0.0.1:18080') {
            external.push(route.request().url());
            return route.abort();
        }
        return route.continue();
    });
    for (const file of ['highlight.min.js', 'LICENSE']) {
        const response = await page.request.get(vendor + file);
        expect(response.status()).toBe(200);
        const expected = fs.readFileSync(path.join(root, 'site/src/main/resources/static', vendor, file));
        expect(crypto.createHash('sha256').update(await response.body()).digest('hex')).toBe(crypto.createHash('sha256').update(expected).digest('hex'));
    }
    await page.goto('/docs/tutorials/first-page.md');
    await expect(page.locator('pre code.language-java.hljs').first()).toHaveAttribute('data-highlighted', 'yes');
    expect(external).toEqual([]);
});
