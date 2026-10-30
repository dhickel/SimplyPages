const {test, expect} = require('playwright/test');

for (const width of [1440, 1024, 768, 390, 320]) {
    test(`grouped demo navigation and card-led home at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height:900});
        await page.goto('/');
        await expect(page.locator('.sp-site-home .sp-site-category-card')).toHaveCount(6);
        await expect(page.locator('.sp-site-workflow-card')).toHaveCount(3);
        await expect(page.locator('.sp-site-home .sp-site-directory')).toHaveCount(0);
        await expect(page.locator('.sp-site-home-example .content-module')).toBeVisible();
        for (const href of await page.locator('.sp-site-category-card, .sp-site-workflow-card').evaluateAll(links => links.map(a => a.getAttribute('href')))) {
            expect((await page.request.get(href)).status(), href).toBe(200);
        }
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
        await page.screenshot({path:testInfo.outputPath(`home-${width}.png`), fullPage:true});
        await page.goto('/demos/modules/insights');
        const sidebar = page.locator('.sp-site-demo-sidebar');
        const disclosure = sidebar.locator('details');
        const summary = sidebar.locator('summary');
        await expect(page.locator('.sp-site-nav [aria-current]')).toHaveText('Demos');
        await expect(sidebar.locator('[aria-current]')).toHaveText('Data & insights');
        await page.keyboard.press('Tab');
        await expect(page.getByRole('link', {name:'Skip to content'})).toBeFocused();
        await page.keyboard.press('Enter');
        await expect(page.locator('#content-area')).toBeFocused();
        await page.keyboard.press('Tab');
        expect(await page.evaluate(() => !!document.activeElement.closest('.sp-site-demo-sidebar'))).toBe(false);
        await page.evaluate(() => window.scrollTo(0, 0));
        await expect(page.locator('[data-catalog-symbol]')).toHaveCount(4);
        if (width > 900) {
            await expect(disclosure).toHaveAttribute('open');
            const geometry = await page.evaluate(() => {
                const nav = document.querySelector('.sp-site-demo-sidebar').getBoundingClientRect();
                const content = document.querySelector('.sp-site-content').getBoundingClientRect();
                return {left:nav.right <= content.left, width:nav.width};
            });
            expect(geometry.left).toBe(true);
            expect(geometry.width).toBeGreaterThan(200);
            await page.evaluate(() => window.scrollTo(0, 700));
            await expect.poll(() => sidebar.evaluate(e => Math.round(e.getBoundingClientRect().top))).toBe(16);
            await page.evaluate(() => window.scrollTo(0, 0));
        } else {
            await expect(disclosure).not.toHaveAttribute('open');
            await summary.focus();
            await page.keyboard.press('Enter');
            await expect(disclosure).toHaveAttribute('open');
            await page.screenshot({path:testInfo.outputPath(`navigation-open-${width}.png`)});
            await page.keyboard.press('Tab');
            await expect(sidebar.getByRole('link', {name:'Explore the demos',exact:true})).toBeFocused();
            await page.keyboard.press('Escape');
            await expect(disclosure).not.toHaveAttribute('open');
            await expect(summary).toBeFocused();
            await page.keyboard.press('Space');
            await sidebar.getByRole('link',{name:'DataModule',exact:true}).click();
            await expect(disclosure).not.toHaveAttribute('open');
            await expect(page.locator('#datamodule')).toBeFocused();
            await expect(page).toHaveURL(/#datamodule$/);
            await page.evaluate(() => window.scrollTo(0, 0));
        }
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
        await page.screenshot({path:testInfo.outputPath(`insights-${width}.png`)});
        await page.getByLabel('Value', {exact:true}).fill('A grouped result');
        await page.getByRole('button',{name:'Update dynamic-table'}).click();
        await expect(page.locator('#dynamic-result')).toContainText('A grouped result');
    });
}

test('collection links retain original examples and each current-page subsection resolves', async ({page}, testInfo) => {
    await page.setViewportSize({width:1440,height:900});
    for (const route of ['/demos/modules/reading', '/demos/modules/insights', '/demos/modules/interaction', '/demos/modules/storytelling', '/demos/builders/composition', '/demos/features/templates', '/demos/features/htmx', '/demos/features/editing', '/demos/features/forum', '/demos/features/chat']) {
        await page.goto(route);
        await expect(page.locator('main h1')).toHaveCount(1);
        const targets = await page.locator('.sp-site-nav-sections a').evaluateAll(links => links.map(a => ({path:a.pathname, id:a.hash.slice(1)})));
        expect(targets.length, route).toBeGreaterThan(0);
        for (const target of targets) {
            expect(target.path).toBe(route);
            await expect(page.locator(`[id="${target.id}"]`), route + '#' + target.id).toHaveCount(1);
        }
        const ids = await page.locator('[id]').evaluateAll(nodes => nodes.map(e => e.id));
        expect(new Set(ids).size, route).toBe(ids.length);
        await page.screenshot({path:testInfo.outputPath(route.replaceAll('/', '-') + '.png')});
    }
    await page.goto('/demos/builders');
    await expect(page.locator('[data-catalog-symbol]')).toHaveCount(6);
    await expect(page.locator('.sp-site-demo-sidebar [aria-current]')).toHaveAttribute('href','/demos/builders/composition');
    await page.goto('/demos/modules/content#contentmodule');
    await expect(page.locator('#contentmodule [data-example-preview]')).toBeVisible();
    await expect(page.locator('.sp-site-demo-sidebar [aria-current]')).toHaveAttribute('href','/demos/modules/reading');
    await page.goto('/demos/builders/shell-template#shelltemplate');
    await expect(page.locator('#shelltemplate')).toBeVisible();
    await expect(page.locator('.sp-site-demo-sidebar [aria-current]')).toHaveAttribute('href','/demos/builders/composition');
});

// Control media notification ordering while retaining the real disclosure, native
// summary activation, queued toggle events and packaged navigation script.
async function controlDemoMedia(page) {
    await page.setViewportSize({width:390,height:900});
    await page.addInitScript(() => {
        const nativeMatchMedia = window.matchMedia.bind(window);
        window.matchMedia = query => {
            const media = nativeMatchMedia(query);
            if (query !== '(max-width:900px)') return media;
            let matches = true;
            Object.defineProperty(media, 'matches', {get:() => matches});
            window.demoMedia = {
                setMatches(value) { matches = value; },
                notify() { media.dispatchEvent(new Event('change')); },
                resize(value) { this.setMatches(value); this.notify(); }
            };
            return media;
        };
    });
    await page.goto('/demos/components/forms');
}

for (const open of [true, false]) {
    test(`mobile disclosure remembers native ${open ? 'open' : 'closed'} before queued toggle delivery`, async ({page}) => {
        await controlDemoMedia(page);
        const disclosure = page.locator('.sp-site-demo-disclosure');
        if (!open) {
            await disclosure.evaluate(details => new Promise(resolve => {
                details.addEventListener('toggle', () => resolve(), {once:true});
                details.querySelector('summary').click();
            }));
        }
        const states = await disclosure.evaluate(async details => {
            const toggled = new Promise(resolve => details.addEventListener('toggle', () => resolve(), {once:true}));
            details.querySelector('summary').click();
            // Neither native toggle delivery nor observer callbacks can run between these writes.
            window.demoMedia.resize(false);
            const desktop = details.open;
            window.demoMedia.resize(true);
            const mobile = details.open;
            await toggled;
            const settled = details.open;
            window.demoMedia.resize(false);
            window.demoMedia.resize(true);
            return {desktop, mobile, settled, restored:details.open};
        });
        expect(states).toEqual({desktop:true, mobile:open, settled:open, restored:open});
        const afterResize = await disclosure.evaluate(async details => {
            const toggled = new Promise(resolve => details.addEventListener('toggle', () => resolve(), {once:true}));
            window.demoMedia.resize(false);
            window.demoMedia.resize(true);
            // A native activation after responsive writes must not be discarded with them.
            details.querySelector('summary').click();
            await toggled;
            const settled = details.open;
            window.demoMedia.resize(false);
            const desktop = details.open;
            window.demoMedia.resize(true);
            return {desktop, settled, restored:details.open};
        });
        expect(afterResize).toEqual({desktop:true, settled:!open, restored:!open});
    });
}

test('mobile disclosure ignores desktop toggle delivery before the media change notification', async ({page}) => {
    await controlDemoMedia(page);
    const disclosure = page.locator('.sp-site-demo-disclosure');
    const states = await disclosure.evaluate(async details => {
        const toggled = new Promise(resolve => details.addEventListener('toggle', () => resolve(), {once:true}));
        window.demoMedia.resize(false);
        const desktop = details.open;
        // A media query can already match the new viewport before its change listener runs.
        window.demoMedia.setMatches(true);
        await toggled;
        window.demoMedia.notify();
        return {desktop, mobile:details.open};
    });
    expect(states).toEqual({desktop:true, mobile:false});
});

test('mobile disclosure survives resize and preserves native navigation without JavaScript', async ({page, browser}) => {
    await page.setViewportSize({width:390,height:900});
    await page.goto('/demos/components/forms');
    const summary = page.locator('.sp-site-demo-disclosure summary');
    const disclosure = page.locator('.sp-site-demo-disclosure');
    await summary.click();
    await page.setViewportSize({width:1440,height:900});
    await expect(disclosure).toHaveAttribute('open');
    await page.setViewportSize({width:390,height:900});
    await expect(disclosure).toHaveAttribute('open');
    await summary.click();
    await page.setViewportSize({width:1440,height:900});
    await expect(disclosure).toHaveAttribute('open');
    await page.locator('.sp-site-demo-sidebar a[href="/demos/components/media"]').focus();
    await page.setViewportSize({width:390,height:900});
    await expect(disclosure).not.toHaveAttribute('open');
    await expect(summary).toBeFocused();
    const context = await browser.newContext({javaScriptEnabled:false, viewport:{width:320,height:900}});
    try {
        const native = await context.newPage();
        await native.goto('http://127.0.0.1:18080/demos');
        await native.locator('.sp-site-demo-sidebar a[href="/demos/modules/reading"]').click();
        await expect(native.locator('#contentmodule')).toBeVisible();
        const nativeSummary = native.locator('.sp-site-demo-disclosure summary');
        await nativeSummary.focus();
        await native.keyboard.press('Enter');
        await expect(native.locator('.sp-site-demo-disclosure')).not.toHaveAttribute('open');
        await native.keyboard.press('Enter');
        await native.locator('.sp-site-demo-sidebar a[href="/demos/components/forms"]').click();
        await native.getByLabel('Name',{exact:true}).fill('No-JS navigation');
        await native.getByLabel('Email',{exact:true}).first().fill('native@example.com');
        await native.getByRole('button',{name:'Render preview',exact:true}).click();
        await expect(native.locator('body')).toContainText('No-JS navigation');
    } finally { await context.close(); }
});

for (const width of [1440, 390]) {
    test(`new demo rail opens a complete working chat document at ${width}`, async ({page}) => {
        await page.setViewportSize({width,height:900});
        await page.addInitScript(() => {
            const Native = window.EventSource;
            window.__redesignStreams = [];
            window.EventSource = new Proxy(Native, {construct(target,args) {
                const source = Reflect.construct(target,args);
                window.__redesignStreams.push(source);
                return source;
            }});
        });
        await page.goto('/demos');
        if (width < 901) await page.locator('.sp-site-demo-disclosure summary').click();
        const navigation = page.waitForRequest(request => request.isNavigationRequest() && new URL(request.url()).pathname === '/demos/features/chat');
        await page.locator('.sp-site-demo-sidebar a[href="/demos/features/chat"]').click();
        await navigation;
        await expect(page.locator('[data-sp-chat="true"]')).toHaveCount(1);
        await expect(page.locator('script[src="/js/chat-demo.js"]')).toHaveCount(1);
        await expect.poll(() => page.evaluate(() => window.__redesignStreams.filter(s => s.readyState === EventSource.OPEN).length)).toBe(1);
        await page.getByRole('textbox',{name:'Message',exact:true}).fill('From the grouped demo rail');
        await page.getByRole('button',{name:'Send',exact:true}).click();
        await expect(page.locator('#chat-history').getByText('From the grouped demo rail',{exact:true})).toHaveCount(1);
        await expect(page.locator('#chat-history').getByText('Received: From the grouped demo rail',{exact:true})).toHaveCount(1);
        await expect(page.locator('.sp-site-demo-sidebar [aria-current]')).toHaveAttribute('href','/demos/features/chat');
        if (width > 900) await expect(page.locator('.sp-site-demo-sidebar [aria-current]')).toBeInViewport();
    });
}

test('new accent surfaces retain readable text contrast', async ({page}) => {
    const selectors = ['.sp-site-nav a', '.sp-site-hero-note p', '.sp-site-hero-note h2', '.sp-site-category-card p', '.sp-site-category-card h3', '.sp-site-workflow-card p', '.sp-site-workflow-card h3', '.sp-site-workflow-card span', '.sp-site-learning a'];
    async function contrast(selectors) {
        return page.evaluate(selectors => {
            const luminance = color => {
                const rgb = color.match(/[\d.]+/g).slice(0,3).map(v => Number(v)/255).map(v => v <= .04045 ? v/12.92 : ((v+.055)/1.055)**2.4);
                return rgb[0]*.2126 + rgb[1]*.7152 + rgb[2]*.0722;
            };
            return selectors.flatMap(selector => [...document.querySelectorAll(selector)].map(element => {
                let surface = element;
                while (surface.parentElement && getComputedStyle(surface).backgroundColor === 'rgba(0, 0, 0, 0)') surface = surface.parentElement;
                const values = [luminance(getComputedStyle(element).color), luminance(getComputedStyle(surface).backgroundColor)].sort((a,b) => b-a);
                return {selector, ratio:(values[0]+.05)/(values[1]+.05)};
            }));
        }, selectors);
    }
    await page.goto('/');
    for (const result of await contrast(selectors)) expect(result.ratio, result.selector).toBeGreaterThanOrEqual(4.5);
    await page.goto('/demos/components/forms');
    for (const result of await contrast(['.sp-site-demo-sidebar a', '.sp-site-nav-label'])) expect(result.ratio, result.selector).toBeGreaterThanOrEqual(4.5);
    await page.goto('/demos/builders/composition');
    await expect(page.locator('.sp-site-preview .banner-title')).toHaveCSS('color','rgb(248, 250, 252)');
    for (const result of await contrast(['.sp-site-preview .banner-title'])) expect(result.ratio, result.selector).toBeGreaterThanOrEqual(4.5);
    await page.goto('/demos/modules/storytelling');
    await expect(page.locator('.sp-site-preview .hero-title')).toHaveCSS('color','rgb(248, 250, 252)');
    await expect(page.locator('.sp-site-preview .hero-subtitle')).toHaveCSS('color','rgba(255, 255, 255, 0.95)');
});
