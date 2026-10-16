const fs = require('node:fs');
const {test, expect} = require('./phase17-editing-fixture');

// Each case owns a packaged-production backend. Only the real module-9 seed is arranged,
// before listener startup; all markup, routes and assets remain production-generated.
async function setup(editingFixture, options = {}) {
    return editingFixture(options);
}

async function cleanup(fixture) {
    await fixture.stop(); // Lifecycle cleanup, never privileged deletes or ignored HTTP failures.
}

async function enter(page, host, baseURL) {
    await page.goto(baseURL + (host === 'direct' ? '/editing-demo' : '/demos/htmx-editing'));
    if (host === 'fragment') {
        await page.getByRole('button', {name: 'Load editing demo fragment', exact: true}).click();
        await expect(page.locator('#editing-fragment #page-content')).toHaveCount(1);
    }
    await expect(page.locator('script[src="/js/editing-demo.js"]')).toHaveCount(1);
    expect(await page.evaluate(() => htmx.version)).toBe('2.0.11');
}

async function listenerCount(page) {
    const session = await page.context().newCDPSession(page);
    try {
        const result = await session.send('Runtime.evaluate', {
            expression: '(getEventListeners(document)["htmx:beforeSwap"] || []).filter(entry => entry.listener.toString().includes("X-Editing-Demo-Modal")).length',
            includeCommandLineAPI: true, returnByValue: true
        });
        return result.result.value;
    } finally { await session.detach(); }
}

function opener(page, id) { return page.locator(`[hx-get^="/editing-demo/edit/${id}?"]`); }
function childEdit(page, id, index = 0) { return page.locator(`[hx-get^="/editing-demo/edit-child/${id}/item-${index}?"]`); }
function childDelete(page, id, index = 0) { return page.locator(`[hx-delete^="/editing-demo/delete-child/${id}/item-${index}?"]`); }

async function clickResponse(page, control, suffix, status = 200) {
    // HTMX 2.0.11 initializes inserted controls at settle, not when they first become visible.
    await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
    const response = page.waitForResponse(response => response.url().includes(suffix));
    await control.click();
    expect((await response).status()).toBe(status);
}

async function pendingCards(page, request) {
    const response = await request.get('/editing-demo/pending-edits');
    expect(response.status()).toBe(200);
    return page.evaluate(html => [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('.card')]
        .map(card => card.innerHTML), await response.text());
}

async function uniqueHosts(page) {
    await expect(page.locator('#page-content')).toHaveCount(1);
    await expect(page.locator('#edit-modal-container')).toHaveCount(1);
    await expect(page.getByRole('dialog')).toHaveCount(1);
    expect(await listenerCount(page)).toBe(1);
}

async function settleDialog(page) {
    await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
    await expect.poll(() => page.getByRole('dialog').evaluate(dialog =>
        dialog.getAnimations({subtree: true}).filter(animation => animation.pending || animation.playState === 'running').length
    )).toBe(0);
    await page.getByRole('dialog').evaluate(() =>
        new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))
    );
    await expect(page.getByRole('dialog')).toHaveCSS('opacity', '1');
}

async function focusedVisible(control) {
    await expect(control).toBeFocused();
    await expect.poll(() => control.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const dialog = element.closest('[role=dialog]').getBoundingClientRect();
        return rect.top >= Math.max(0, dialog.top) - 1 && rect.bottom <= Math.min(innerHeight, dialog.bottom) + 1
            && rect.left >= Math.max(0, dialog.left) - 1 && rect.right <= Math.min(innerWidth, dialog.right) + 1;
    })).toBe(true);
}

async function geometry(page) {
    return page.getByRole('dialog').evaluate(dialog => {
        const rect = dialog.getBoundingClientRect();
        const input = dialog.querySelector('input, textarea');
        const box = input && input.getBoundingClientRect();
        return {
            withinViewport: rect.left >= -1 && rect.right <= innerWidth + 1 && rect.top >= -1 && rect.bottom <= innerHeight + 1,
            noOverflow: dialog.scrollWidth <= dialog.clientWidth + 1 && document.documentElement.scrollWidth <= innerWidth + 1,
            inputVisible: !box || (box.left >= rect.left && box.right <= rect.right && box.top >= rect.top && box.bottom <= rect.bottom)
        };
    });
}

for (const width of [1440, 390]) for (const host of ['direct', 'fragment']) {
    test(`phase16 native ${host} ${width}: draft errors, keyboard, repeated replacement and owner OOB`, async ({page, editingFixture}) => {
        test.setTimeout(90000);
        await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
        const fixture = await setup(editingFixture, {canDelete: false});
        const {id, request, baseURL} = fixture;
        const pageErrors = [];
        const consoleErrors = [];
        page.on('pageerror', error => pageErrors.push(error.message));
        page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()); });
        try {
            await enter(page, host, baseURL);
            if (host === 'fragment') {
                // Real repeated launcher replacement does not reload the asset or own another listener.
                await page.getByRole('button', {name: 'Load editing demo fragment', exact: true}).click();
                await expect(page.locator('#editing-fragment #page-content')).toHaveCount(1);
            }
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await uniqueHosts(page);
            await expect(page.getByRole('button', {name: 'Delete Module', exact: true})).toHaveCount(0);
            await expect(childDelete(page, id)).toHaveCount(1); // canDelete is parent-only.
            await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
            await expect(page.getByLabel('Text', {exact: true})).toHaveValue('Alpha');
            await focusedVisible(page.getByLabel('Text', {exact: true}));
            await uniqueHosts(page);
            const untouched = await page.locator('#page-content').innerHTML();
            const draft = '<script>alert("not code")</script> & 雪,' + 'L'.repeat(600);
            for (let attempt = 0; attempt < 2; attempt++) {
                await page.getByLabel('Text', {exact: true}).fill(draft);
                await clickResponse(page, page.getByRole('button', {name: 'Save Item', exact: true}), `/save-child/${id}/item-0`, 400);
                await expect(page.getByLabel('Text', {exact: true})).toHaveValue(draft);
                await expect(page.locator('#editing-child-error')).toBeVisible();
                await focusedVisible(page.getByLabel('Text', {exact: true}));
                expect(await page.locator('#page-content').innerHTML()).toBe(untouched);
                await uniqueHosts(page);
            }
            await settleDialog(page);
            expect(await geometry(page)).toEqual({withinViewport: true, noOverflow: true, inputVisible: true});
            // Native tab containment, reverse wrapping, Escape and a still-connected page opener.
            const background = await page.evaluate(() => ({x: scrollX, y: scrollY}));
            await page.keyboard.press('Shift+Tab');
            await focusedVisible(page.getByRole('button', {name: 'Close', exact: true}));
            await page.keyboard.press('Shift+Tab');
            await focusedVisible(page.getByRole('button', {name: 'Cancel', exact: true}));
            await page.keyboard.press('Tab');
            await focusedVisible(page.getByRole('button', {name: 'Close', exact: true}));
            await page.keyboard.press('Tab');
            await focusedVisible(page.getByLabel('Text', {exact: true}));
            expect(await page.evaluate(() => ({x: scrollX, y: scrollY}))).toEqual(background);
            await page.screenshot({path: `/tmp/phase17-senior117-${host}-${width}-error-${Date.now()}.png`});
            await page.keyboard.press('Escape');
            await expect(page.getByRole('dialog')).toHaveCount(0);
            await expect(opener(page, id)).toBeFocused();
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
            await page.getByRole('button', {name: 'Cancel', exact: true}).click();
            await expect(opener(page, id)).toBeFocused();
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
            const value = '  <b>雪 & "quoted" 😀</b>  ';
            await page.getByLabel('Text', {exact: true}).fill(value);
            await clickResponse(page, page.getByRole('button', {name: 'Save Item', exact: true}), `/save-child/${id}/item-0`);
            await expect(page.getByRole('dialog')).toHaveCount(0);
            await expect(page.locator(`#${id} li`).first()).toHaveText(value.trim());
            await expect(page.locator(`#${id} b, #${id} script`)).toHaveCount(0);
            await expect(page.locator('#page-content')).toHaveCount(1);
            await expect(page.locator('#edit-modal-container')).toHaveCount(1);
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
            await expect(page.getByLabel('Text', {exact: true})).toHaveValue(value.trim());
            await settleDialog(page);
            await page.screenshot({path: `/tmp/phase17-senior117-${host}-${width}-settled-${Date.now()}.png`});
            expect(await geometry(page)).toEqual({withinViewport: true, noOverflow: true, inputVisible: true});
            expect(pageErrors).toEqual([]);
            // Chromium logs the deliberate HTTP 400, not an unforced runtime/transport failure.
            expect(consoleErrors).toHaveLength(2);
            expect(consoleErrors.every(text => /^Failed to load resource: the server responded with a status of 400 \((?:Bad Request)?\)$/.test(text)), JSON.stringify(consoleErrors)).toBe(true);
        } finally { await cleanup(fixture); }
    });

    test(`phase16 native ${host} ${width}: allowed line breaks survive untouched save and invalid draft edges`, async ({page, editingFixture}) => {
        test.setTimeout(90000);
        await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
        const fixture = await setup(editingFixture, {content: '\t First\nLine \t,Item 2,Item 3', canDelete: false});
        const {id, request, baseURL} = fixture;
        const pageErrors = [];
        page.on('pageerror', error => pageErrors.push(error.message));
        // Compare code units too: rendered text matchers can normalize whitespace and hide data loss.
        const units = value => Array.from({length: value.length}, (_, i) => value.charCodeAt(i));
        const field = page.getByLabel('Text', {exact: true});
        const open = async () => {
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
        };
        const save = async status => {
            const sent = page.waitForRequest(request => request.method() === 'POST' && request.url().includes(`/save-child/${id}/item-0`));
            await clickResponse(page, page.getByRole('button', {name: 'Save Item', exact: true}), `/save-child/${id}/item-0`, status);
            return new URLSearchParams((await sent).postData()).get('text');
        };
        const assertValue = async expected => {
            await expect(field).toHaveValue(expected);
            expect(await field.evaluate(element => ({
                tag: element.tagName,
                value: Array.from({length: element.value.length}, (_, i) => element.value.charCodeAt(i)),
                defaultValue: Array.from({length: element.defaultValue.length}, (_, i) => element.defaultValue.charCodeAt(i)),
                wrap: element.wrap,
                limited: element.hasAttribute('maxlength') || element.hasAttribute('pattern')
            }))).toEqual({tag: 'TEXTAREA', value: units(expected), defaultValue: units(expected), wrap: 'soft', limited: false});
            await focusedVisible(field);
        };
        try {
            await enter(page, host, baseURL);
            await open();
            await assertValue('First\nLine');
            // No fill: the generated GET and untouched native form must retain the existing label.
            expect(await save(200)).toBe('First\nLine');
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(await page.locator(`#${id} li`).first().textContent()).toBe('First\nLine');
            await open();
            await assertValue('First\nLine');

            // Existing stored CRLF/CR are parsed through real GET markup, not only assigned by fill().
            await page.getByRole('button', {name: 'Cancel', exact: true}).click();
            expect((await request.post(`/editing-demo/save/${id}?editMode=OWNER_EDIT`, {form: {
                content: 'First\r\nLine\tCR\rLast,Item 2,Item 3', useMarkdown: 'true'
            }})).status()).toBe(200);
            await open();
            await assertValue('First\nLine\tCR\nLast');
            expect(await save(200)).toBe('First\nLine\tCR\nLast');
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(await page.locator(`#${id} li`).first().textContent()).toBe('First\nLine\tCR\nLast');
            await open();
            await assertValue('First\nLine\tCR\nLast');

            const entered = ' \t<b>雪 & "quoted" 😀</b>\tline\nnext\r\nCRLF\rtail \t';
            const native = ' \t<b>雪 & "quoted" 😀</b>\tline\nnext\nCRLF\ntail \t';
            const stored = '<b>雪 & "quoted" 😀</b>\tline\nnext\nCRLF\ntail';
            await field.fill(entered);
            await expect(field).toHaveValue(native);
            expect(await save(200)).toBe(native); // Bundled HTMX reads .value and URL-encodes LF, not CRLF.
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(await page.locator(`#${id} li`).first().textContent()).toBe(stored);
            await expect(page.locator(`#${id} b, #${id} script`)).toHaveCount(0);
            await open();
            await assertValue(stored);
            expect(await save(200)).toBe(stored); // A second untouched save must also be lossless.
            await expect(page.getByRole('dialog')).toHaveCount(0);
            await open();
            await assertValue(stored);

            // Keep an unrelated proposal while each native validation failure preserves its entire draft.
            expect((await request.post(`/editing-demo/save/${id}?editMode=USER_EDIT`, {form: {width: '4'}})).status()).toBe(200);
            const unchanged = await page.locator('#page-content').innerHTML();
            for (const [enteredDraft, nativeDraft] of [
                ['\n\n<script>雪 & "quoted" 😀</script>\t,\n \t', '\n\n<script>雪 & "quoted" 😀</script>\t,\n \t'],
                ['\r\n<b>comma,</b>\r\n\tend\r', '\n<b>comma,</b>\n\tend\n'],
                [' \t\n\n', ' \t\n\n'],
                [' \t a,b \t ', ' \t a,b \t ']
            ]) {
                await field.fill(enteredDraft);
                expect(await save(400)).toBe(nativeDraft);
                await assertValue(nativeDraft);
                await expect(page.locator('#editing-child-error')).toBeVisible();
                await expect(page.getByRole('dialog').locator('script,b')).toHaveCount(0);
                expect(await page.locator('#page-content').innerHTML()).toBe(unchanged);
                await uniqueHosts(page);
                const pending = await (await request.get('/editing-demo/pending-edits')).text();
                expect(pending).toContain(`Module: ${id}`);
                expect(pending).toContain('Fields: width, markdown');
            }
            await settleDialog(page);
            expect(await geometry(page)).toEqual({withinViewport: true, noOverflow: true, inputVisible: true});
            await page.screenshot({path: `/tmp/phase17-senior117-${host}-${width}-linebreak-error-${Date.now()}.png`});
            await page.keyboard.press('Escape');
            await expect(page.getByRole('dialog')).toHaveCount(0);
            await expect(opener(page, id)).toBeFocused();
            await open();
            await assertValue(stored);
            await settleDialog(page);
            expect(await geometry(page)).toEqual({withinViewport: true, noOverflow: true, inputVisible: true});
            await page.screenshot({path: `/tmp/phase17-senior117-${host}-${width}-linebreak-settled-${Date.now()}.png`});
            expect(pageErrors).toEqual([]);
        } finally { await cleanup(fixture); }
    });

    test(`phase16 native ${host} ${width}: generated DELETE cancel zero accept one, reindex and empty module`, async ({page, editingFixture}) => {
        test.setTimeout(90000);
        await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
        const fixture = await setup(editingFixture, {content: 'First,Last', canDelete: false});
        const {id, request, baseURL} = fixture;
        let deletes = 0;
        const errors = [];
        page.on('request', request => { if (request.method() === 'DELETE' && request.url().includes(`/delete-child/${id}/`)) deletes++; });
        page.on('pageerror', error => errors.push(error.message));
        try {
            await enter(page, host, baseURL);
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await expect(page.getByRole('button', {name: 'Delete Module', exact: true})).toHaveCount(0);
            expect((await request.delete(`/editing-demo/delete/${id}`)).status()).toBe(403);
            page.once('dialog', dialog => dialog.dismiss());
            await childDelete(page, id).click();
            await expect(page.getByRole('dialog')).toHaveCount(1);
            expect(deletes).toBe(0);
            page.once('dialog', dialog => dialog.accept());
            await clickResponse(page, childDelete(page, id), `/delete-child/${id}/item-0`);
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(deletes).toBe(1);
            await expect(page.locator(`#${id} li`)).toHaveText(['Last']);
            await expect(page.locator('#edit-modal-container')).toHaveCount(1);
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await expect(childEdit(page, id, 0)).toHaveCount(1);
            await expect(childEdit(page, id, 1)).toHaveCount(0);
            page.once('dialog', dialog => dialog.accept());
            await clickResponse(page, childDelete(page, id), `/delete-child/${id}/item-0`);
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(deletes).toBe(2);
            await expect(page.locator(`#${id}`)).toContainText('No items yet.');
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await expect(page.locator(`[hx-get^="/editing-demo/edit-child/${id}/"]`)).toHaveCount(0);
            await uniqueHosts(page);
            expect(errors).toEqual([]);
        } finally { await cleanup(fixture); }
    });
}

for (const width of [1440, 390]) for (const host of ['direct', 'fragment']) {
    test(`phase16 native ${host} ${width}: malformed identity error actions preserve components and mode on real retries`, async ({page, editingFixture}) => {
        test.setTimeout(150000);
        await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
        // OWNER default is intentional: losing the explicit USER query used to mutate live content.
        const fixture = await setup(editingFixture);
        const {id, request, baseURL} = fixture;
        const pageErrors = [];
        page.on('pageerror', error => pageErrors.push(error.message));
        const field = page.getByLabel('Text', {exact: true});
        const evidence = [];
        const pendingSnapshot = async () => page.evaluate(async () => {
            const html = await (await fetch('/editing-demo/pending-edits')).text();
            return [...new DOMParser().parseFromString(html, 'text/html').querySelectorAll('.card')].map(card => card.textContent);
        });
        try {
            await enter(page, host, baseURL);
            for (const mode of ['USER_EDIT', 'OWNER_EDIT']) {
                if (mode === 'OWNER_EDIT') expect((await request.post(`/editing-demo/save/${id}?editMode=USER_EDIT`, {form: {width: '4'}})).status()).toBe(200);
                const pending = await pendingSnapshot();
                for (const [suffix, encoded] of [
                    ['?evil?editMode=OWNER_EDIT', '%3Fevil%3FeditMode%3DOWNER_EDIT'],
                    ['#tail?editMode=OWNER_EDIT', '%23tail%3FeditMode%3DOWNER_EDIT'],
                    ['%30%3Fevil', '%2530%253Fevil'],
                    ['<&"\' >', '%3C%26%22%27%20%3E'],
                    ['雪😀', '%E9%9B%AA%F0%9F%98%80'],
                    [';editMode=OWNER_EDIT', '%3BeditMode%3DOWNER_EDIT'],
                    ['{id}', '%7Bid%7D']
                ]) for (const malformedModule of [false, true]) {
                    await clickResponse(page, opener(page, id), `/edit/${id}`);
                    const untouched = await page.locator('#page-content').innerHTML();
                    const moduleId = id + (malformedModule ? suffix : '');
                    const childId = 'item-0' + (malformedModule ? '' : suffix);
                    const encodedPath = `/${id}${malformedModule ? encoded : ''}/item-0${malformedModule ? '' : encoded}`;
                    const getUrl = `/editing-demo/edit-child${encodedPath}?editMode=${mode}`;
                    const action = `/editing-demo/save-child${encodedPath}?editMode=${mode}`;
                    const status = malformedModule ? 404 : 400;
                    const requests = [];
                    const track = request => {
                        if (request.url().includes('/editing-demo/edit-child/') || request.url().includes('/editing-demo/save-child/')) requests.push(request.url());
                    };
                    page.on('request', track);
                    try {
                        const initial = page.waitForResponse(response => response.url().endsWith(getUrl));
                        // Invoke real bundled HTMX from the real generated child button. Do not rewrite
                        // that button, the error response, or the generated form subsequently submitted.
                        await page.evaluate(({getUrl, id}) => {
                            const source = document.querySelector(`[hx-get^="/editing-demo/edit-child/${id}/"]`);
                            htmx.ajax('GET', getUrl, {source, target: '#edit-modal-container', swap: 'innerHTML'}).catch(() => {});
                        }, {getUrl, id});
                        let response = await initial;
                        const draft = '\n\n<雪> & "draft"\t\n';
                        for (let retry = 0; retry <= 2; retry++) {
                            expect(response.status()).toBe(status);
                            expect(response.headers()['x-editing-demo-modal']).toBe('true');
                            expect(response.headers()['vary']).toContain('HX-Request');
                            const raw = await response.text();
                            expect(raw).toContain(`hx-post="${action}"`);
                            expect(raw).toContain(`action="${action}"`);
                            expect(raw).not.toContain('hx-swap-oob');
                            await expect(page.locator('#editing-child-form')).toHaveAttribute('hx-post', action);
                            await expect(page.locator('#editing-child-form')).toHaveAttribute('action', action);
                            const absolute = await page.locator('#editing-child-form').evaluate(form => form.action);
                            const parsed = new URL(absolute);
                            expect(parsed.pathname).toBe('/editing-demo/save-child' + encodedPath);
                            expect(parsed.pathname.split('/').slice(-2).map(decodeURIComponent)).toEqual([moduleId, childId]);
                            expect([...parsed.searchParams]).toEqual([['editMode', mode]]);
                            expect(parsed.hash).toBe('');
                            await expect(page.locator('#editing-child-error')).toBeVisible();
                            await expect(field).toHaveValue(retry ? draft : '');
                            expect(await field.evaluate(element => element.defaultValue)).toBe(retry ? draft : '');
                            expect((await page.getByRole('dialog').textContent()).includes('Prior pending child and property changes are discarded')).toBe(mode === 'USER_EDIT');
                            await focusedVisible(field);
                            expect(await page.locator('#page-content').innerHTML()).toBe(untouched);
                            expect(await pendingSnapshot()).toEqual(pending);
                            const current = await request.get(`/editing-demo/edit-child/${id}/item-0`);
                            expect(current.status()).toBe(200);
                            expect(await current.text()).toContain('\nAlpha</textarea>');
                            evidence.push({moduleId, childId, mode, retry, status, action, submittedUrl: response.url(), absolute});
                            if (retry === 2) break;
                            await field.fill(draft);
                            const submitted = page.waitForRequest(request => request.method() === 'POST' && request.url().endsWith(action));
                            const result = page.waitForResponse(response => response.request().method() === 'POST' && response.url().includes('/save-child/'));
                            await page.getByRole('button', {name: 'Save Item', exact: true}).click();
                            response = await result;
                            const wire = await submitted;
                            expect(wire.url()).toBe(absolute);
                            expect(new URLSearchParams(wire.postData()).get('text')).toBe(draft);
                        }
                        await uniqueHosts(page);
                        await settleDialog(page);
                        expect(requests).toEqual([new URL(getUrl, page.url()).href, new URL(action, page.url()).href, new URL(action, page.url()).href]);
                        expect(await geometry(page)).toEqual({withinViewport: true, noOverflow: true, inputVisible: true});
                        await page.keyboard.press('Escape');
                        await expect(page.getByRole('dialog')).toHaveCount(0);
                        await expect(opener(page, id)).toBeFocused();
                    } finally { page.off('request', track); }
                }
            }
            expect(pageErrors).toEqual([]);
            fs.writeFileSync(`/tmp/phase17-senior117-uri-${host}-${width}-${Date.now()}.json`, JSON.stringify(evidence, null, 2), {flag: 'wx'});
        } finally { await cleanup(fixture); }
    });
}

test('phase16 native USER proposal replaces property/child proposals from live, reject and approve retain unrelated fields', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture, {mode: 'USER_EDIT', canDelete: false});
    const {id, request, baseURL} = fixture;
    try {
        await request.post(`/editing-demo/save/${id}?editMode=USER_EDIT`, {form: {title: 'Discarded', width: '4', content: 'Discarded'}});
        await enter(page, 'direct', baseURL);
        for (const [index, value] of [[0, 'Discarded child'], [1, 'Approved child']]) {
            await clickResponse(page, opener(page, id), `/edit/${id}`);
            await expect(childEdit(page, id, index)).toHaveAttribute('hx-get', new RegExp('editMode=USER_EDIT$'));
            await clickResponse(page, childEdit(page, id, index), `/edit-child/${id}/item-${index}`);
            await expect(page.getByRole('dialog')).toContainText('Prior pending child and property changes are discarded.');
            await page.getByLabel('Text', {exact: true}).fill(value);
            await clickResponse(page, page.getByRole('button', {name: 'Save Item', exact: true}), `/save-child/${id}/item-${index}`);
            await expect(page.getByRole('dialog')).toHaveCount(0);
            await expect(page.locator(`#${id} li`)).toHaveText(['Alpha', 'Beta', 'Gamma']);
        }
        await page.getByRole('button', {name: /Pending Edits/}).click();
        const card = page.locator('.card').filter({hasText: `Module: ${id}`});
        await expect(card).toContainText('Fields: content');
        await clickResponse(page, card.getByRole('button', {name: 'Reject', exact: true}), `/pending-edits/${id}/reject`);
        await expect(page.locator(`#${id} li`)).toHaveText(['Alpha', 'Beta', 'Gamma']);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await clickResponse(page, childEdit(page, id, 1), `/edit-child/${id}/item-1`);
        await page.getByLabel('Text', {exact: true}).fill('Approved child');
        await clickResponse(page, page.getByRole('button', {name: 'Save Item', exact: true}), `/save-child/${id}/item-1`);
        await expect(page.getByRole('dialog')).toHaveCount(0);
        await page.getByRole('button', {name: /Pending Edits/}).click();
        await clickResponse(page, page.locator('.card').filter({hasText: `Module: ${id}`}).getByRole('button', {name: 'Approve', exact: true}), `/pending-edits/${id}/approve`);
        await expect(page.getByRole('dialog')).toHaveCount(0);
        await expect(page.locator(`#${id} li`)).toHaveText(['Alpha', 'Approved child', 'Gamma']);
        await expect(page.locator(`#${id}`)).toContainText('Nested Editing Demo');
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await expect(page.getByRole('button', {name: 'Delete Module', exact: true})).toHaveCount(0);
        await expect(page.locator('select[name=width]')).toHaveValue('12');
        await expect(page.locator('input[name=title]')).toHaveValue('Nested Editing Demo');
        expect((await request.delete(`/editing-demo/delete/${id}`)).status()).toBe(403);
        await expect(page.locator(`#${id} li`)).toHaveText(['Alpha', 'Approved child', 'Gamma']);
        await uniqueHosts(page);
        expect(await fixture.stop()).toEqual({present: true, kind: 'LIST', title: 'Nested Editing Demo',
            content: 'Alpha,Approved child,Gamma', width: 12, useMarkdown: true,
            canEdit: true, canDelete: false, mode: 'USER_EDIT'});
    } finally { await cleanup(fixture); }
});

test('phase16 native error handling requires one owned modal host', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture);
    const {id, request, baseURL} = fixture;
    try {
        await enter(page, 'direct', baseURL);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', '<div id="edit-modal-container"></div>'));
        await expect(page.locator('#edit-modal-container')).toHaveCount(2);
        const draft = 'duplicate-host,draft';
        await page.getByLabel('Text', {exact: true}).fill(draft);
        const response = page.waitForResponse(response => response.url().includes(`/save-child/${id}/item-0`));
        await page.getByRole('button', {name: 'Save Item', exact: true}).click();
        expect((await response).status()).toBe(400);
        await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
        await expect(page.locator('#edit-modal-container').first().getByLabel('Text', {exact: true})).toHaveValue(draft);
    } finally { await cleanup(fixture); }
});

test('phase16 native errors remain default for unmarked, foreign-target and server failures, with no retry', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture);
    const {id, request, baseURL} = fixture;
    const errors = [];
    const pageErrors = [];
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    page.on('pageerror', error => pageErrors.push(error.message));
    try {
        await enter(page, 'fragment', baseURL);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`);
        await page.getByLabel('Text', {exact: true}).fill('Keep draft');
        await page.evaluate(() => {
            window.phase16Errors = [];
            document.addEventListener('htmx:responseError', event => window.phase16Errors.push(event.detail.xhr.status));
        });
        await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
        for (const [url, target, status] of [
            ['/editing-demo/not-a-route', '#edit-modal-container', 404],
            [`/editing-demo/edit-child/${id}/item-999`, '#dynamic-card-preview', 404],
            ['/error', '#edit-modal-container', 500]
        ]) {
            let requests = 0;
            const count = request => { if (new URL(request.url()).pathname === new URL(url, 'http://localhost').pathname) requests++; };
            page.on('request', count);
            const before = await page.locator(target).innerHTML();
            const response = page.waitForResponse(response => new URL(response.url()).pathname === new URL(url, 'http://localhost').pathname);
            // Real HTMX API to existing endpoints; no fake responses or transport corrections.
            await page.evaluate(({url, target}) => htmx.ajax('GET', url, {target, source: document.getElementById('editing-child-form')}), {url, target});
            expect((await response).status()).toBe(status);
            await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
            expect(await page.locator(target).innerHTML()).toBe(before);
            expect(requests).toBe(1);
            page.off('request', count);
        }
        expect(await page.evaluate(() => window.phase16Errors)).toEqual([404, 404, 500]);
        await expect(page.getByLabel('Text', {exact: true})).toHaveValue('Keep draft');
        await uniqueHosts(page);
        expect(pageErrors).toEqual([]);
        expect(errors.every(text => /404|500/.test(text))).toBe(true);
    } finally { await cleanup(fixture); }
});

test('phase16 locked list omits generated actions and denies direct child operations without mutation', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture, {canEdit: false, canDelete: true});
    const {id, request, baseURL} = fixture;
    try {
        await enter(page, 'direct', baseURL);
        await expect(opener(page, id)).toHaveCount(0);
        await expect(page.locator(`[hx-delete^="/editing-demo/delete/${id}"]`)).toHaveCount(1);
        await expect(page.locator(`[hx-get^="/editing-demo/edit-child/${id}/"], [hx-delete^="/editing-demo/delete-child/${id}/"]`)).toHaveCount(0);
        const lockedPage = await page.locator('#page-content').innerHTML();
        const pendingBefore = await pendingCards(page, request);
        expect(pendingBefore).toEqual([]);
        const deniedParent = await request.get(`/editing-demo/edit/${id}`);
        expect(deniedParent.status()).toBe(403);
        const parent = await deniedParent.text();
        expect(parent).not.toContain(`/editing-demo/edit-child/${id}`);
        expect(parent).not.toContain(`/editing-demo/delete-child/${id}`);
        expect(parent).not.toContain('hx-swap-oob');
        expect((await request.get(`/editing-demo/edit-child/${id}/item-0`)).status()).toBe(403);
        expect((await request.post(`/editing-demo/save-child/${id}/item-0`, {form: {text: 'Denied'}})).status()).toBe(403);
        expect((await request.delete(`/editing-demo/delete-child/${id}/item-0`)).status()).toBe(403);
        expect(await pendingCards(page, request)).toEqual(pendingBefore);
        expect(await page.locator('#page-content').innerHTML()).toBe(lockedPage);
        await page.reload();
        await expect(page.locator(`#${id} li`)).toHaveText(['Alpha', 'Beta', 'Gamma']);
        await expect(opener(page, id)).toHaveCount(0);
        await expect(page.locator(`[hx-delete^="/editing-demo/delete/${id}"]`)).toHaveCount(1);
    } finally { await cleanup(fixture); }
});


// The original combined case required two LISTs. Each subcase now owns the sole real seed.
test('phase16 generated stale GET shows marked404 without page OOB', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture);
    const {id, request, baseURL} = fixture;
    const pageErrors = [];
    page.on('pageerror', error => pageErrors.push(error.message));
    try {
        await enter(page, 'fragment', baseURL);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        const unchanged = await page.locator('#page-content').innerHTML();
        expect((await request.delete(`/editing-demo/delete/${id}`)).status()).toBe(200);
        const response = page.waitForResponse(response => response.url().includes(`/edit-child/${id}/item-0`));
        await clickResponse(page, childEdit(page, id), `/edit-child/${id}/item-0`, 404);
        const error = await response;
        expect(error.headers()['x-editing-demo-modal']).toBe('true');
        expect(await error.text()).not.toContain('hx-swap-oob');
        await expect(page.locator('#editing-child-error')).toContainText('Current list module not found.');
        expect(await page.locator('#page-content').innerHTML()).toBe(unchanged);
        await focusedVisible(page.getByLabel('Text', {exact: true}));
        await uniqueHosts(page);
        await page.keyboard.press('Escape');
        await expect(opener(page, id)).toBeFocused();
        expect((await request.get(`/editing-demo/edit/${id}`)).status()).toBe(404);
        expect((await request.delete(`/editing-demo/delete/${id}`)).status()).toBe(404);
        expect((await request.delete(`/editing-demo/pending-edits/${id}/reject`)).status()).toBe(404);
        expect(pageErrors).toEqual([]);
    } finally { await cleanup(fixture); }
});

test('phase16 generated lossy CSV DELETE shows marked400 without page OOB', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture, {content: 'a, ,b'});
    const {id, request, baseURL} = fixture;
    const pageErrors = [];
    page.on('pageerror', error => pageErrors.push(error.message));
    try {
        await enter(page, 'fragment', baseURL);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        const unchanged = await page.locator('#page-content').innerHTML();
        const pending = await pendingCards(page, request);
        expect(pending).toEqual([]);
        page.once('dialog', dialog => dialog.accept());
        const response = page.waitForResponse(response => response.url().includes(`/delete-child/${id}/item-2`));
        await clickResponse(page, childDelete(page, id, 2), `/delete-child/${id}/item-2`, 400);
        const error = await response;
        expect(error.headers()['x-editing-demo-modal']).toBe('true');
        expect(await error.text()).not.toContain('hx-swap-oob');
        await expect(page.locator('#editing-child-error')).toContainText('would lose a retained empty item');
        await expect(page.locator(`#${id} li`)).toHaveText(['a', '', 'b']);
        expect(await page.locator('#page-content').innerHTML()).toBe(unchanged);
        expect(await pendingCards(page, request)).toEqual(pending);
        await uniqueHosts(page);
        expect(pageErrors).toEqual([]);
    } finally { await cleanup(fixture); }
});

test('phase16 native USER DELETE stages live-based replacement and approval preserves the module', async ({page, editingFixture}) => {
    test.setTimeout(90000);
    const fixture = await setup(editingFixture, {content: 'Only', mode: 'USER_EDIT', canDelete: false});
    const {id, request, baseURL} = fixture;
    try {
        await enter(page, 'direct', baseURL);
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await expect(childDelete(page, id)).toHaveAttribute('hx-delete', new RegExp('editMode=USER_EDIT$'));
        await expect(page.getByRole('dialog')).toContainText('prior pending child and property changes are discarded');
        page.once('dialog', dialog => dialog.accept());
        await clickResponse(page, childDelete(page, id), `/delete-child/${id}/item-0`);
        await expect(page.getByRole('dialog')).toHaveCount(0);
        await expect(page.locator(`#${id} li`)).toHaveText(['Only']);
        await page.getByRole('button', {name: /Pending Edits/}).click();
        await clickResponse(page, page.locator('.card').filter({hasText: `Module: ${id}`}).getByRole('button', {name: 'Approve', exact: true}), `/pending-edits/${id}/approve`);
        await expect(page.getByRole('dialog')).toHaveCount(0);
        await expect(page.locator(`#${id}`)).toContainText('No items yet.');
        await clickResponse(page, opener(page, id), `/edit/${id}`);
        await expect(page.getByRole('button', {name: 'Delete Module', exact: true})).toHaveCount(0);
        await expect(page.locator(`[hx-get^="/editing-demo/edit-child/${id}/"]`)).toHaveCount(0);
        await expect(page.locator('select[name=width]')).toHaveValue('12');
        await expect(page.locator('input[name=title]')).toHaveValue('Nested Editing Demo');
        expect((await request.delete(`/editing-demo/delete/${id}`)).status()).toBe(403);
        await expect(page.locator(`#${id}`)).toContainText('No items yet.');
        expect(await fixture.stop()).toEqual({present: true, kind: 'LIST', title: 'Nested Editing Demo',
            content: '', width: 12, useMarkdown: true, canEdit: true, canDelete: false, mode: 'USER_EDIT'});
    } finally { await cleanup(fixture); }
});
