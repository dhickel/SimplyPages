const {test, expect} = require('playwright/test');

async function installChatInstrumentation(page, state) {
    await page.addInitScript(() => {
        const NativeEventSource = window.EventSource;
        window.__spChatEventSourceUrls = [];
        window.__spChatEventSources = [];
        window.__spChatSignals = [];
        window.__spChatRequests = [];
        window.__spChatSwaps = [];
        window.EventSource = new Proxy(NativeEventSource, {
            construct(target, args) {
                window.__spChatEventSourceUrls.push(String(args[0]));
                const source = Reflect.construct(target, args);
                window.__spChatEventSources.push(source);
                source.addEventListener('chat-updated', event => window.__spChatSignals.push(event.data));
                return source;
            }
        });
        document.addEventListener('DOMContentLoaded', () => {
            document.body.addEventListener('htmx:beforeRequest', event => {
                const {xhr, requestConfig} = event.detail;
                window.__spChatRequests.push({xhr, verb: requestConfig.verb, finished: false});
            });
            document.body.addEventListener('htmx:afterRequest', event => {
                const request = window.__spChatRequests.find(request => request.xhr === event.detail.xhr);
                if (request) request.finished = true;
            });
            document.body.addEventListener('htmx:beforeSwap', event => {
                window.__spChatSwaps.push({connected: event.detail.target.isConnected});
            });
        });
    });

    state.pageErrors = [];
    state.consoleErrors = [];
    state.historyRequests = 0;
    state.postMessages = [];
    state.stage = 'setup';
    page.on('pageerror', error => state.pageErrors.push({stage: state.stage, error}));
    page.on('console', message => {
        if (message.type() === 'error') state.consoleErrors.push({stage: state.stage, text: message.text()});
    });
    page.on('request', request => {
        const pathname = new URL(request.url()).pathname;
        if (pathname === '/chat/history') state.historyRequests++;
        if (pathname === '/chat/messages' && request.method() === 'POST') {
            state.postMessages.push(new URLSearchParams(request.postData()).get('message'));
        }
    });
}

async function expectOpenStream(page) {
    await expect.poll(() => page.evaluate(() => window.__spChatEventSources
        .filter(source => source.readyState === EventSource.OPEN).length)).toBe(1);
}

async function expectChatDocument(page) {
    await expect(page).toHaveURL(/\/chat$/);
    await expect(page.locator('[data-sp-chat="true"]')).toHaveCount(1);
    await expect(page.locator('script[src="/js/chat-demo.js"]')).toHaveCount(1);
    await expect.poll(() => page.evaluate(() => window.__spChatEventSourceUrls.length)).toBe(1);
    await expect(page.locator('#chat-history')).toHaveCount(1);
    await expectOpenStream(page);
}

async function expectChatIdle(page) {
    // These counters are reset per document, unlike cumulative network observations.
    await expect.poll(() => page.evaluate(() => window.__spChatSignals.length)).toBeGreaterThan(0);
    await expect.poll(() => page.evaluate(() => window.__spChatRequests
        .filter(request => request.verb === 'get' && request.finished).length)).toBeGreaterThan(0);
    await expect.poll(() => page.evaluate(() => window.__spChatRequests
        .filter(request => !request.finished).length)).toBe(0);
    await expectChatDocument(page);
    expect(await page.evaluate(() => window.__spChatSwaps.every(swap => swap.connected))).toBe(true);
}

async function openChatFromDemos(page) {
    await page.goto('/demos');
    const chatLink = page.locator('#main-sidebar a.sidenav-item[href="/chat"]');
    await expect(chatLink).toHaveCount(1);
    await expect(chatLink).toHaveAttribute('hx-get', '/chat');
    await expect(chatLink).toHaveAttribute('hx-target', '#content-area');
    const redirectResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/chat'
        && response.request().headers()['hx-request'] === 'true');
    const documentRequest = page.waitForRequest(request => new URL(request.url()).pathname === '/chat'
        && request.isNavigationRequest());
    await chatLink.click();
    const response = await redirectResponse;
    expect(response.status()).toBe(200);
    expect(response.headers()['hx-redirect']).toBe('/chat');
    expect(response.headers()['vary']).toBe('HX-Request');
    await documentRequest;
    await expectChatDocument(page);
}

async function expectMessages(page, messages) {
    for (const message of messages) {
        await expect(page.locator('#chat-history').getByText(message, {exact: true})).toHaveCount(1);
        await expect(page.locator('#chat-history').getByText(`Received: ${message}`, {exact: true})).toHaveCount(1);
    }
}

async function submitMessage(page, message) {
    await expectOpenStream(page);
    await page.getByRole('textbox', {name: 'Message', exact: true}).fill(message);
    await page.getByRole('button', {name: 'Send', exact: true}).click();
}

async function expectLatestHistory(page) {
    const conversationId = await page.locator('[data-sp-chat="true"]').getAttribute('data-sp-chat-conversation-id');
    const response = await page.request.get('/chat/history', {params: {conversationId}});
    expect(response.status()).toBe(200);
    const serverHistory = await response.text();
    expect(await page.evaluate(html => {
        const server = new DOMParser().parseFromString(html, 'text/html').querySelector('#chat-history');
        return document.querySelector('#chat-history').textContent === server.textContent;
    }, serverHistory)).toBe(true);
}

async function publishExternalMessage(page, message) {
    const conversationId = await page.locator('[data-sp-chat="true"]').getAttribute('data-sp-chat-conversation-id');
    const response = await page.request.post('/chat/messages', {form: {conversationId, message}});
    expect(response.status()).toBe(200);
    expect(await response.text()).toContain(`Received: ${message}`);
}

async function expectNoBrowserErrors(state) {
    expect(state.pageErrors, state.pageErrors.map(({stage, error}) => `${stage}: ${error.stack || error.message}`).join('\n'))
        .toHaveLength(0);
    expect(state.consoleErrors, state.consoleErrors.map(({stage, text}) => `${stage}: ${text}`).join('\n'))
        .toHaveLength(0);
}

function barrier() {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return {promise, resolve};
}

async function holdFirstRealResponse(page, pathname) {
    const reached = barrier();
    const release = barrier();
    let held = false;
    await page.route(`**${pathname}*`, async route => {
        if (held) {
            await route.continue();
            return;
        }
        held = true;
        // Fetch the actual packaged server response and its real side effects; only
        // delivery is gated. EventSource remains native, live and un-intercepted.
        const response = await route.fetch();
        reached.resolve(await response.text());
        await release.promise;
        await route.fulfill({response});
    });
    return {reached: reached.promise, release: release.resolve};
}

test('direct chat, reload, and actual Demos sidebar re-entry keep one current document stream', async ({page}) => {
    const state = {};
    await installChatInstrumentation(page, state);
    const messages = [];

    state.stage = 'direct';
    await page.goto('/chat');
    await expectChatDocument(page);
    await expectChatIdle(page);
    messages.push(`Direct chat ${Date.now()}`);
    await submitMessage(page, messages.at(-1));
    await expectMessages(page, messages);
    await expectChatIdle(page);

    state.stage = 'reload';
    await page.reload();
    await expectChatDocument(page);
    await expectChatIdle(page);
    await expectMessages(page, messages);
    messages.push(`Reload chat ${Date.now()}`);
    await submitMessage(page, messages.at(-1));
    await expectMessages(page, messages);
    await expectChatIdle(page);

    state.stage = 'sidebar';
    await openChatFromDemos(page);
    await expectChatIdle(page);
    messages.push(`Sidebar chat ${Date.now()}`);
    await submitMessage(page, messages.at(-1));
    await expectMessages(page, messages);
    await expectChatIdle(page);

    state.stage = 'post-message-reload';
    await page.reload();
    await expectChatDocument(page);
    await expectChatIdle(page);
    await expectMessages(page, messages);
    await expectLatestHistory(page);
    await expectNoBrowserErrors(state);
});

test('mobile Demos sidebar is inert until its actual toggle opens it before Chat navigation', async ({page}) => {
    const state = {};
    await installChatInstrumentation(page, state);
    await page.setViewportSize({width: 390, height: 844});
    await page.goto('/demos');

    const sidebar = page.locator('#main-sidebar');
    const toggle = page.locator('.mobile-sidebar-toggle[aria-controls="main-sidebar"]');
    await expect(sidebar).toHaveJSProperty('inert', true);
    await expect(sidebar).toHaveAttribute('aria-hidden', 'true');
    await toggle.click();
    await expect(sidebar).toHaveClass(/mobile-open/);
    await expect(sidebar).toHaveJSProperty('inert', false);
    await expect(sidebar).not.toHaveAttribute('aria-hidden', 'true');

    await page.locator('#main-sidebar a.sidenav-item[href="/chat"]').click();
    await expectChatDocument(page);
    await expectChatIdle(page);
    state.stage = 'mobile-live-send';
    const message = `Mobile chat ${Date.now()}`;
    await submitMessage(page, message);
    await expectMessages(page, [message]);
    await expectChatIdle(page);
    const external = `Mobile SSE-only update ${Date.now()}`;
    await publishExternalMessage(page, external);
    await expectMessages(page, [message, external]);
    await expectChatIdle(page);
    await expectLatestHistory(page);
    await expectNoBrowserErrors(state);
});

test('initial real SSE history response pending during rapid sends preserves every submitted snapshot', async ({page}) => {
    const state = {};
    await installChatInstrumentation(page, state);
    const held = await holdFirstRealResponse(page, '/chat/history');
    try {
        state.stage = 'initial-history-held';
        await page.goto('/chat');
        await expectChatDocument(page);
        const initialHistory = await held.reached;
        await expect.poll(() => page.evaluate(() => window.__spChatSignals.length)).toBe(1);
        const messages = ['First queued snapshot', 'Second queued snapshot', 'Third queued snapshot'];
        for (const message of messages) await submitMessage(page, message);
        await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Unsent newer draft');
        expect(state.postMessages).toEqual([]);
        expect(state.historyRequests).toBe(1);

        const external = 'External update while initial history is held';
        await publishExternalMessage(page, external);
        await expect.poll(() => page.evaluate(() => window.__spChatSignals.length)).toBe(2);
        expect(initialHistory).not.toContain(external);
        for (const message of messages) expect(initialHistory).not.toContain(message);
        await expectOpenStream(page);
        expect(state.historyRequests).toBe(1);

        state.stage = 'drain-queued-messages';
        held.release();
        await expectMessages(page, [...messages, external]);
        await expectChatIdle(page);
        expect(state.postMessages).toEqual(messages);
        expect(state.historyRequests).toBeGreaterThan(1);
        await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Unsent newer draft');
        await expectLatestHistory(page);
        await expectNoBrowserErrors(state);
    } finally {
        held.release();
    }
});

test('real SSE updates during a delayed POST wait for its swap then refresh latest history', async ({page}) => {
    const state = {};
    await installChatInstrumentation(page, state);
    await page.goto('/chat');
    await expectChatDocument(page);
    await expectChatIdle(page);
    const historyBefore = state.historyRequests;
    const signalsBefore = await page.evaluate(() => window.__spChatSignals.length);
    const held = await holdFirstRealResponse(page, '/chat/messages');
    try {
        state.stage = 'post-response-held';
        const messages = ['Held POST snapshot', 'Rapid follow-up one', 'Rapid follow-up two'];
        await submitMessage(page, messages[0]);
        const postSnapshot = await held.reached;
        const external = 'External update after held POST snapshot';
        await publishExternalMessage(page, external);
        await expect.poll(() => page.evaluate(() => window.__spChatSignals.length)).toBe(signalsBefore + 2);
        expect(postSnapshot).toContain(`Received: ${messages[0]}`);
        expect(postSnapshot).not.toContain(external);
        await expect(page.locator('#chat-history')).not.toContainText(messages[0]);
        await expect(page.locator('#chat-history')).not.toContainText(external);
        for (const message of messages.slice(1)) await submitMessage(page, message);
        await page.getByRole('textbox', {name: 'Message', exact: true}).fill('Draft typed during POST');
        expect(state.postMessages).toEqual([messages[0]]);
        expect(state.historyRequests).toBe(historyBefore);
        await expectOpenStream(page);

        state.stage = 'post-and-pending-sse-drain';
        held.release();
        await expectMessages(page, [...messages, external]);
        await expectChatIdle(page);
        expect(state.postMessages).toEqual(messages);
        expect(state.historyRequests).toBeGreaterThan(historyBefore);
        await expect(page.getByRole('textbox', {name: 'Message', exact: true})).toHaveValue('Draft typed during POST');
        await expectLatestHistory(page);
        await expectNoBrowserErrors(state);
    } finally {
        held.release();
    }
});
