const {test, expect} = require('playwright/test');

test.describe.configure({mode: 'serial'});

async function instrument(page) {
    await page.addInitScript(() => {
        window.__phase17 = {sources: [], signals: [], errors: [], requests: []};
        const Native = window.EventSource;
        window.EventSource = new Proxy(Native, {construct(target, args) {
            const source = Reflect.construct(target, args);
            window.__phase17.sources.push(source);
            source.addEventListener('chat-updated', event => window.__phase17.signals.push(event.data));
            source.addEventListener('error', () => window.__phase17.errors.push(Date.now()));
            return source;
        }});
        document.addEventListener('DOMContentLoaded', () => {
            document.body.addEventListener('htmx:beforeRequest', event => {
                window.__phase17.requests.push({xhr: event.detail.xhr, verb: event.detail.requestConfig.verb, finished: false});
            });
            document.body.addEventListener('htmx:afterRequest', event => {
                const request = window.__phase17.requests.find(request => request.xhr === event.detail.xhr);
                if (request) request.finished = true;
            });
        });
    });
}
async function idle(page) {
    await expect.poll(() => page.evaluate(() => window.__phase17.requests.every(request => request.finished))).toBe(true);
    await expect(page.locator('#chat-history')).toHaveCount(1);
}
async function open(page) {
    await expect.poll(() => page.evaluate(() => window.__phase17.sources.filter(source => source.readyState === EventSource.OPEN).length)).toBe(1);
    await expect(page.locator('#chat-connection-message')).toContainText('Connected.');
    await expect(page.locator('#chat-resume')).toBeHidden();
    await expect.poll(() => page.evaluate(() => window.__phase17.signals.length)).toBeGreaterThan(0);
    await idle(page);
}
async function id(page) { return page.locator('[data-sp-chat="true"]').getAttribute('data-sp-chat-conversation-id'); }
async function submit(page, message) {
    await page.getByRole('textbox', {name: 'Message', exact: true}).fill(message);
    await page.getByRole('button', {name: 'Send', exact: true}).click();
}
function barrier() {
    let resolve;
    const promise = new Promise(done => {resolve = done;});
    return {promise, resolve};
}

test('separate native contexts cannot read, post or open foreign streams; APIs never allocate', async ({browser}) => {
    const owner = await browser.newContext(), peer = await browser.newContext();
    try {
        const page = await owner.newPage();
        await instrument(page);
        await page.goto('/chat');
        await open(page);
        const owned = await id(page);
        for (const action of ['history', 'messages', 'stream']) {
            for (const bad of [undefined, '', ' ', owned, 'unknown', ` ${owned}`, `${owned} `]) {
                const request = action === 'messages' ? peer.request.post.bind(peer.request) : peer.request.get.bind(peer.request);
                const response = await request(`/chat/${action}`, action === 'messages'
                    ? {form: {...(bad === undefined ? {} : {conversationId: bad}), message: 'foreign attempt'}}
                    : {params: bad === undefined ? {} : {conversationId: bad}});
                expect(response.status()).toBe(bad === undefined || !bad.trim() ? 400 : 404);
                expect(response.headers()['cache-control']).toBe('no-store');
                expect(response.headers()['set-cookie']).toBeUndefined();
                expect(await response.text()).not.toContain('Welcome.');
            }
        }
        const foreignPage = await peer.newPage();
        await foreignPage.goto('/home');
        const foreign = await foreignPage.evaluate(async owned => {
            return await new Promise(resolve => {
                const source = new EventSource('/chat/stream?conversationId=' + encodeURIComponent(owned));
                let opened = false;
                source.onopen = () => {opened = true;};
                source.onerror = () => {source.close(); resolve({opened, state: source.readyState});};
            });
        }, owned);
        expect(foreign).toEqual({opened: false, state: 2});
        const history = await owner.request.get('/chat/history', {params: {conversationId: owned}});
        expect(await history.text()).not.toContain('foreign attempt');
        await open(page); // Rejected foreign streams consumed no owner slot.
        expect(await id(page)).toBe(owned);
    } finally { await peer.close(); await owner.close(); }
});

test('concurrent first normal GETs in an existing servlet session share one exact owned conversation', async ({page}) => {
    await instrument(page);
    await page.goto('/home');
    // The existing forum route initializes the servlet session without initializing chat.
    expect((await page.request.get('/forum')).status()).toBe(200);
    const results = await page.evaluate(async () => await Promise.all(Array.from({length: 12}, async () => {
        const response = await fetch('/chat');
        const html = new DOMParser().parseFromString(await response.text(), 'text/html');
        return {status: response.status, noStore: response.headers.get('Cache-Control'),
            id: html.querySelector('[data-sp-chat="true"]').getAttribute('data-sp-chat-conversation-id')};
    })));
    expect(results.every(result => result.status === 200 && result.noStore === 'no-store')).toBe(true);
    expect(new Set(results.map(result => result.id)).size).toBe(1);
    await page.goto('/chat');
    await open(page);
    expect(await id(page)).toBe(results[0].id);
});

test('native bounded message validation, astral boundaries and latest whole-pair pruning', async ({page}) => {
    await instrument(page);
    await page.goto('/chat');
    await open(page);
    const owned = await id(page);
    await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveAttribute('maxlength', '4096');
    for (const message of ['a'.repeat(4095), '😀'.repeat(2048), 'a'.repeat(4096)]) {
        const response = await page.request.post('/chat/messages', {form: {conversationId: owned, message}});
        expect(response.status()).toBe(200);
    }
    for (const message of ['a'.repeat(4097), ' ' + 'a'.repeat(4096), '😀'.repeat(2048) + ' ']) {
        const response = await page.request.post('/chat/messages', {form: {conversationId: owned, message}});
        expect(response.status()).toBe(400);
        expect(await response.text()).toContain('before trimming');
    }
    for (let i = 0; i < 28; i++) {
        const response = await page.request.post('/chat/messages', {form: {conversationId: owned, message: `Retained ${i}`}});
        expect(response.status()).toBe(200);
    }
    await expect(page.locator('#chat-history')).toContainText('Received: Retained 27');
    await idle(page);
    const retained = await page.request.get('/chat/history', {params: {conversationId: owned}});
    expect(await retained.text()).not.toContain('Retained 2<');
    expect(await retained.text()).toContain('Retained 3');
    await expect(page.locator('#chat-history .chat-message')).toHaveCount(51);
    await expect(page.locator('#chat-history [data-chat-role="user"]')).toHaveCount(25);
    await expect(page.locator('#chat-history [data-chat-role="assistant"]')).toHaveCount(26);
    // Validate renderer-independent visible pairs rather than inventing a framework hook.
    expect(await page.locator('#chat-history').getByText('Retained 3', {exact: true}).count()).toBe(1);
    expect(await page.locator('#chat-history').getByText('Retained 2', {exact: true}).count()).toBe(0);
    await open(page);
});

test('explicit Start New drains original real delayed submissions and coalesced history before replacement', async ({page}) => {
    await instrument(page);
    await page.goto('/chat');
    await open(page);
    const original = await id(page);
    const reached = barrier(), release = barrier();
    const posts = [], newRequests = [];
    page.on('request', request => {
        if (new URL(request.url()).pathname === '/chat/messages') posts.push(Object.fromEntries(new URLSearchParams(request.postData())));
        if (new URL(request.url()).pathname === '/chat/new') newRequests.push(request);
    });
    let held = false;
    await page.route('**/chat/messages', async route => {
        if (held) return route.continue();
        held = true;
        const response = await route.fetch();
        reached.resolve(await response.text());
        await release.promise;
        await route.fulfill({response});
    });
    try {
        await submit(page, 'Original first snapshot');
        expect(await reached.promise).toContain('Received: Original first snapshot');
        await submit(page, 'Original queued snapshot');
        await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Newest unsent draft');
        await page.getByRole('button', {name: 'Start New', exact: true}).click();
        await expect(page.getByRole('button', {name: 'Send', exact: true})).toBeDisabled();
        expect(newRequests).toHaveLength(0);
        expect(await id(page)).toBe(original);
        await expect.poll(() => page.evaluate(() => window.__phase17.sources[0].readyState)).toBe(2);
        await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Newer draft while draining');
        release.resolve();
        await expect.poll(() => id(page)).not.toBe(original);
        await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Newer draft while draining');
        await expect(page.locator('#chat-history')).not.toContainText('Original first snapshot');
        await expect(page.locator('#chat-history')).not.toContainText('Original queued snapshot');
        expect(posts.map(post => post.message)).toEqual(['Original first snapshot', 'Original queued snapshot']);
        expect(posts.every(post => post.conversationId === original)).toBe(true);
        expect(newRequests).toHaveLength(1);
        await open(page);
        await submit(page, 'New generation only');
        await expect(page.locator('#chat-history')).toContainText('Received: New generation only');
        await idle(page);
        expect(posts.at(-1).conversationId).not.toBe(original);
    } finally { release.resolve(); }
});

test('normal HTMX transport failures retain newer drafts without retry and subsequent work succeeds', async ({page}) => {
    await instrument(page);
    await page.goto('/chat');
    await open(page);
    let failures = 0, attempts = 0;
    await page.evaluate(() => {
        window.__normalErrors = 0;
        document.body.addEventListener('htmx:sendError', () => window.__normalErrors++);
    });
    await page.route('**/chat/messages', async route => {
        attempts++;
        if (!failures++) return route.abort('failed');
        return route.continue();
    });
    await submit(page, 'Failed original');
    await expect.poll(() => page.evaluate(() => window.__normalErrors)).toBe(1);
    await idle(page);
    await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Failed original');
    await submit(page, 'Subsequent valid');
    await expect(page.locator('#chat-history')).toContainText('Received: Subsequent valid');
    await expect(page.locator('#chat-history')).not.toContainText('Failed original');
    await idle(page);
    expect(attempts).toBe(2);
    await open(page);
});

async function settledModal(page) {
    await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
    const dialog = page.locator('.modal-container[role="dialog"][aria-modal="true"]');
    await expect(dialog).toHaveCount(1);
    await expect.poll(() => dialog.evaluate(node => node.getAnimations({subtree: true})
        .filter(animation => animation.pending || animation.playState === 'running').length)).toBe(0);
    await expect(dialog).toHaveCSS('opacity', '1');
    return dialog;
}
async function editClick(page, selector, suffix, status = 200) {
    await expect(page.locator('.htmx-added, .htmx-settling, .htmx-request')).toHaveCount(0);
    const response = page.waitForResponse(response => response.url().includes(suffix));
    await page.locator(selector).click();
    expect((await response).status()).toBe(status);
}
for (const width of [1440, 390]) for (const host of ['direct', 'fragment']) {
    test(`private seeded LIST native ${host} ${width}: LF/wire/error/focus/OOB/URI/proposals survive immutable kind`, async ({page, request}) => {
        test.setTimeout(120000);
        const errors = [], intentional = [];
        page.on('pageerror', error => errors.push(error.message));
        page.on('console', message => { if (message.type() === 'error') intentional.push(message.text()); });
        await page.setViewportSize({width, height: width === 1440 ? 900 : 844});
        const reset = async () => {
            await request.delete('/editing-demo/pending-edits/module-9/reject');
            expect((await request.post('/editing-demo/save/module-9', {form: {
                title: 'Renamed private list', content: 'First\nLine,Second,Last', width: '12', useMarkdown: 'true', editMode: 'OWNER_EDIT'
            }})).status()).toBe(200);
        };
        await reset();
        try {
            await page.goto(host === 'direct' ? '/editing-demo' : '/demos/htmx-editing');
            if (host === 'fragment') await page.getByRole('button', {name: 'Load editing demo fragment', exact: true}).click();
            await expect(page.locator('#page-content')).toContainText('Private shared, non-durable sandbox.');
            await expect(page.locator('script[src="/js/editing-demo.js"]')).toHaveCount(1);
            const parent = '[hx-get^="/editing-demo/edit/module-9?"]';
            const child = '[hx-get^="/editing-demo/edit-child/module-9/item-0?"]';
            const openChild = async () => {
                await editClick(page, parent, '/edit/module-9');
                await editClick(page, child, '/edit-child/module-9/item-0');
                await settledModal(page);
            };
            await openChild();
            const field = page.getByLabel('Text', {exact: true});
            await expect(field).toHaveValue('First\nLine');
            expect(await field.evaluate(node => node.defaultValue)).toBe('First\nLine');
            await expect(field).toBeFocused();
            const sent = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/save-child/module-9/item-0'));
            await editClick(page, '#editing-child-form button[type="submit"]', '/save-child/module-9/item-0');
            expect(new URLSearchParams((await sent).postData()).get('text')).toBe('First\nLine');
            await expect(page.getByRole('dialog')).toHaveCount(0);
            expect(await page.locator('#module-9 li').first().textContent()).toBe('First\nLine');
            await openChild();
            const untouched = await page.locator('#page-content').innerHTML();
            const draft = '\n\n<雪> & "draft" 😀\t,\r\n';
            const normalized = '\n\n<雪> & "draft" 😀\t,\n';
            for (let retry = 0; retry < 2; retry++) {
                await field.fill(draft);
                const wire = page.waitForRequest(request => request.method() === 'POST' && request.url().includes('/save-child/module-9/item-0'));
                await editClick(page, '#editing-child-form button[type="submit"]', '/save-child/module-9/item-0', 400);
                expect(new URLSearchParams((await wire).postData()).get('text')).toBe(normalized);
                await expect(field).toHaveValue(normalized);
                expect(await field.evaluate(node => node.defaultValue)).toBe(normalized);
                await expect(field).toBeFocused();
                expect(await page.locator('#page-content').innerHTML()).toBe(untouched);
            }
            let dialog = await settledModal(page);
            expect(await dialog.evaluate(node => {
                const rect = node.getBoundingClientRect(), input = node.querySelector('textarea').getBoundingClientRect();
                return rect.left >= -1 && rect.right <= innerWidth + 1 && rect.top >= -1 && rect.bottom <= innerHeight + 1
                    && node.scrollWidth <= node.clientWidth + 1 && input.top >= rect.top && input.bottom <= rect.bottom;
            })).toBe(true);
            await page.screenshot({path: `/tmp/phase17-senior117-editing-${host}-${width}-${Date.now()}.png`});
            await page.keyboard.press('Escape');
            await expect(page.locator(parent)).toBeFocused();
            // Use the actual generated button as the source for every opaque error route.
            for (const [suffix, encoded] of [[';editMode=OWNER_EDIT', '%3BeditMode%3DOWNER_EDIT'], ['{id}', '%7Bid%7D'],
                ['?bad#tail', '%3Fbad%23tail'], ['%30', '%2530'], ['雪😀', '%E9%9B%AA%F0%9F%98%80']]) {
                await editClick(page, parent, '/edit/module-9');
                const url = `/editing-demo/edit-child/module-9/item-0${encoded}?editMode=USER_EDIT`;
                const response = page.waitForResponse(response => response.url().endsWith(url));
                await page.evaluate(url => htmx.ajax('GET', url, {source: document.querySelector('[hx-get^="/editing-demo/edit-child/module-9/"]'),
                    target: '#edit-modal-container', swap: 'innerHTML'}).catch(() => {}), url);
                expect((await response).status()).toBe(400);
                await settledModal(page);
                const action = `/editing-demo/save-child/module-9/item-0${encoded}?editMode=USER_EDIT`;
                await expect(page.locator('#editing-child-form')).toHaveAttribute('hx-post', action);
                expect(decodeURIComponent(new URL(await page.locator('#editing-child-form').evaluate(form => form.action)).pathname))
                    .toBe('/editing-demo/save-child/module-9/item-0' + suffix);
                await field.fill('\n\nURI draft');
                await editClick(page, '#editing-child-form button[type="submit"]', '/save-child/module-9/', 400);
                await expect(field).toHaveValue('\n\nURI draft');
                await expect(page.locator('#editing-child-form')).toHaveAttribute('hx-post', action);
                await page.keyboard.press('Escape');
            }
            // USER remains live-derived and replaces the complete earlier proposal.
            await request.post('/editing-demo/save/module-9', {form: {title: 'Discarded proposal', width: '4', editMode: 'USER_EDIT'}});
            for (const [index, text] of [[0, 'Discarded child'], [1, 'Approved child']]) {
                await editClick(page, parent, '/edit/module-9');
                const url = `/editing-demo/edit-child/module-9/item-${index}?editMode=USER_EDIT`;
                const response = page.waitForResponse(response => response.url().endsWith(url));
                await page.evaluate(url => htmx.ajax('GET', url, {source: document.querySelector('[hx-get^="/editing-demo/edit-child/module-9/"]'),
                    target: '#edit-modal-container', swap: 'innerHTML'}), url);
                expect((await response).status()).toBe(200);
                await settledModal(page);
                await field.fill(text);
                await editClick(page, '#editing-child-form button[type="submit"]', `/save-child/module-9/item-${index}`);
                await expect(page.getByRole('dialog')).toHaveCount(0);
                await expect(page.locator('#module-9 li')).toHaveText(['First\nLine', 'Second', 'Last']);
            }
            const proposal = await request.get('/editing-demo/pending-edits');
            expect(await proposal.text()).toContain('Fields: content');
            const approval = await request.post('/editing-demo/pending-edits/module-9/approve');
            expect(approval.status()).toBe(200);
            await page.reload();
            if (host === 'fragment') await page.getByRole('button', {name: 'Load editing demo fragment', exact: true}).click();
            await expect(page.locator('#module-9 li')).toHaveText(['First\nLine', 'Approved child', 'Last']);
            await expect(page.locator('#module-9')).toContainText('Renamed private list');
            await expect(page.locator('#page-content')).toHaveCount(1);
            await expect(page.locator('#edit-modal-container')).toHaveCount(1);
            expect(errors).toEqual([]);
            expect(intentional.every(text => text.includes('status of 400') || text.startsWith('Response Status Error Code 400')),
                JSON.stringify(intentional)).toBe(true);
        } finally {
            await request.delete('/editing-demo/pending-edits/module-9/reject');
            await request.post('/editing-demo/save/module-9', {form: {title: 'Nested Editing Demo', content: 'Item 1,Item 2,Item 3',
                width: '12', useMarkdown: 'true', editMode: 'OWNER_EDIT'}});
        }
    });
}

test('actual native production five-minute stream ends once, explicit same-ID Resume and Start New preserve drafts', async ({page}) => {
    test.setTimeout(390000);
    await page.setViewportSize({width: 1440, height: 900});
    const unforced = [];
    page.on('pageerror', error => unforced.push(error.message));
    await instrument(page);
    const started = Date.now();
    await page.goto('/chat');
    await open(page);
    const original = await id(page);
    await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Five-minute unsent draft');
    await expect(page.locator('#chat-connection-message')).toContainText('Connection ended.', {timeout: 325000});
    const duration = Date.now() - started;
    expect(duration).toBeGreaterThanOrEqual(299000);
    expect(duration).toBeLessThan(330000);
    await expect.poll(() => page.evaluate(() => window.__phase17.sources[0].readyState)).toBe(2);
    await expect(page.locator('#chat-resume')).toBeVisible();
    await expect(page.locator('#chat-connection-message')).toHaveClass(/alert-warning/);
    await page.waitForTimeout(4000); // Beyond native's ordinary retry interval: no implicit second source.
    expect(await page.evaluate(() => window.__phase17.sources.length)).toBe(1);
    expect(await id(page)).toBe(original);
    await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Five-minute unsent draft');
    await page.screenshot({path: `/tmp/phase17-senior117-native-ended-1440-${Date.now()}.png`, fullPage: true});
    await page.getByRole('button', {name: 'Resume', exact: true}).click();
    await open(page);
    expect(await id(page)).toBe(original);
    expect(await page.evaluate(() => window.__phase17.sources.length)).toBe(2);
    await submit(page, 'Explicit resumed message');
    await expect(page.locator('#chat-history')).toContainText('Received: Explicit resumed message');
    await idle(page);
    await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Draft across actual New');
    await page.getByRole('button', {name: 'Start New', exact: true}).click();
    await expect.poll(() => id(page)).not.toBe(original);
    await open(page);
    await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Draft across actual New');
    await expect(page.locator('#chat-history')).not.toContainText('Explicit resumed message');
    await page.setViewportSize({width: 390, height: 844});
    await page.screenshot({path: `/tmp/phase17-senior117-native-new-390-${Date.now()}.png`, fullPage: true});
    expect(unforced).toEqual([]);
    console.log(JSON.stringify({proof: 'native-production-finite-stream', duration, sources: 3, explicitResume: true, explicitNew: true, draftPreserved: true}));
});
