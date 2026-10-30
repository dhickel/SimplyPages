const {test, expect} = require('playwright/test');

async function protectedPost(page, path, form) {
    const token = (await (await page.request.get('/sandbox/token')).json()).token;
    return page.request.post(path, {headers:{Origin:'http://127.0.0.1:18080','X-Sandbox-CSRF':token}, form});
}

for (const width of [1440, 1024, 768, 390, 320]) {
    test(`official site navigation, layout, keyboard and visual evidence at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height:900});
        const failures = [];
        page.on('pageerror', error => failures.push(error.message));
        page.on('console', message => { if (message.type() === 'error') failures.push(message.text()); });
        page.on('response', response => { if (response.status() >= 400) failures.push(`${response.status()} ${response.url()}`); });
        page.on('request', request => { expect(new URL(request.url()).hostname).toBe('127.0.0.1'); });
        for (const [name, route] of [['home','/'], ['catalog','/demos'], ['forms','/demos/components/forms'],
            ['modules','/demos/modules/accordion'], ['builders','/demos/builders/shell-template'],
            ['docs','/docs'], ['api','/api'], ['editing','/demos/features/editing'], ['forum','/demos/features/forum']]) {
            await page.goto(route);
            await expect(page.locator('main')).toHaveCount(1);
            await expect(page.locator('main h1')).toHaveCount(1);
            await expect(page.locator('body')).toHaveCSS('background-color','rgb(247, 245, 240)');
            if (name === 'modules') await expect(page.locator('.accordion-header.active')).toHaveCSS('background-color','rgb(36, 92, 72)');
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), route).toBe(true);
            const shot = testInfo.outputPath(`${name}-${width}.png`);
            await page.screenshot({path:shot});
            await testInfo.attach(`${name}-${width}`, {path:shot, contentType:'image/png'});
        }
        await page.goto('/');
        await page.keyboard.press('Tab');
        await expect(page.getByRole('link', {name:'Skip to content'})).toBeFocused();
        await page.keyboard.press('Enter');
        await expect(page).toHaveURL(/#content-area$/);
        await expect(page.locator('#content-area')).toBeFocused();
        await page.goto('/docs');
        if (width < 640) {
            await expect(page.locator('.sp-site-docs-sidebar details')).not.toHaveAttribute('open', '');
            await page.locator('.sp-site-docs-sidebar summary').click();
        }
        const link = page.locator('.docs-nav a[href="/docs/tutorials/first-page.md"]');
        await link.click();
        await expect(page.locator('#docs-content')).toHaveCount(1);
        await expect(page).toHaveTitle(/first page/i);
        await expect(link).toHaveAttribute('aria-current','page');
        await page.locator('.docs-nav a[href="/docs/core/03-template-rendercontext-slotkey-reference.md"]').click();
        await expect(page.locator('#docs-content')).toHaveCount(1);
        await expect(page.locator('#docs-content #slotkey')).toHaveCount(1);
        await expect(page).toHaveTitle(/template|slotkey/i);
        expect(failures).toEqual([]);
    });
}

test('catalog destinations, source, exact docs and native generated API all resolve from the package', async ({page}) => {
    await page.goto('/demos');
    const targets = await page.locator('.sp-site-directory a').evaluateAll(links => links.map(a => a.getAttribute('href')));
    expect(targets.length).toBeGreaterThan(70);
    for (const target of targets) {
        const [path, anchor] = target.split('#');
        const response = await page.request.get(path);
        expect(response.status(), target).toBe(200);
        const html = await response.text();
        expect(html, target).toContain(`id="${anchor}"`);
    }
    for (const route of ['/demos/components/text-structure','/demos/components/forms','/demos/components/display-data','/demos/components/navigation','/demos/components/media','/demos/layout']) {
        await page.goto(route);
        const refs = await page.locator('.sp-site-reference-links a').evaluateAll(links => [...new Set(links.map(a => a.pathname))]);
        for (const ref of refs) expect((await page.request.get(ref)).status(), ref).toBe(200);
    }
    await page.goto('/javadocs/index.html');
    await expect(page.getByRole('link', {name:'Back to SimplyPages'})).toBeVisible();
    await page.getByRole('link', {name:'Search',exact:true}).click();
    const search = page.locator('input[type="text"],input[type="search"]').first();
    await search.fill('ContentModule');
    await expect(page.locator('a[href$="/ContentModule.html"]').first()).toBeVisible();
    await expect(page.locator('iframe')).toHaveCount(0);
});

test('real forms, typed templates, dynamic modules, safety, dialog and protected reset', async ({page}) => {
    await page.goto('/demos/components/forms');
    await page.getByLabel('Name', {exact:true}).fill('Ada <Lovelace>');
    await page.getByLabel('Email', {exact:true}).first().fill('ada@example.com');
    await page.getByRole('button', {name:'Render preview',exact:true}).click();
    await expect(page.locator('#form-preview')).toContainText('Ada <Lovelace>');
    for (const kind of ['dynamic-card','dynamic-list','dynamic-table']) {
        await page.goto('/demos/modules/' + kind);
        await page.getByLabel('Value', {exact:true}).fill('Changed, Again');
        await page.getByRole('button', {name:'Update ' + kind}).click();
        await expect(page.locator('#dynamic-result')).toContainText('Changed');
    }
    await page.goto('/demos/features/templates');
    await page.getByLabel('Title',{exact:true}).fill('Fresh typed value');
    await page.getByRole('button',{name:'Render with context'}).click();
    await expect(page.locator('#template-result')).toContainText('Fresh typed value');
    await page.goto('/demos/features/safe-rendering');
    await page.getByLabel('Untrusted text').fill('<img src=x onerror=alert(1)>');
    await page.getByRole('button',{name:'Render safely'}).click();
    await expect(page.locator('#safe-result')).toContainText('<img src=x onerror=alert(1)>');
    await expect(page.locator('#safe-result img')).toHaveCount(0);
    await page.getByLabel('Simulate permission granted').check();
    await page.getByRole('button',{name:'Evaluate policy'}).click();
    await expect(page.locator('#auth-result')).toContainText('allowed');
    await page.goto('/demos/components/display-data');
    const trigger = page.getByRole('button',{name:'Open dialog'});
    await trigger.click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(trigger).toBeFocused();
    await page.goto('/demos/features/editing');
    await page.getByLabel('Draft Title:', {exact:true}).fill('   ');
    await page.getByRole('button',{name:'Validate draft preview'}).click();
    await expect(page.locator('#edit-fields-result')).toContainText('Use a nonblank title');
    await expect(page.getByLabel('Draft Title:', {exact:true})).toHaveValue('   ');
    await page.getByLabel('Draft Title:', {exact:true}).fill('A checked draft');
    await page.getByRole('button',{name:'Validate draft preview'}).click();
    await expect(page.locator('#edit-fields-result')).toContainText('A checked draft');
    await expect(page.locator('#edit-form-builder')).toHaveCount(1);
    await expect(page.locator('#edit-fields-result')).toHaveCount(1);
    const rows = await page.locator('#page-content .editable-row-wrapper').count();
    await page.locator('#editablepage').getByRole('button',{name:'+ Insert Row Below',exact:true}).click();
    await expect(page.locator('#page-content .editable-row-wrapper')).toHaveCount(rows + 1);
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.getByRole('dialog').getByRole('button',{name:'Cancel',exact:true}).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('#editablepage').getByRole('button',{name:'+ Insert Row Below',exact:true})).toBeFocused();
    await page.getByRole('button',{name:'Reset editing & forum sandbox'}).click();
    await expect(page.locator('#page-content')).toBeVisible();
    expect((await protectedPost(page,'/demos/api/template-card',{title:'Native token',body:'Real result'})).status()).toBe(200);
});

test('native form fallback works without JavaScript and tokens never enter URLs', async ({browser}) => {
    const context = await browser.newContext({javaScriptEnabled:false});
    try {
        const page = await context.newPage();
        await page.goto('http://127.0.0.1:18080/demos/components/forms');
        await page.getByLabel('Name',{exact:true}).fill('Native Ada');
        await page.getByLabel('Email',{exact:true}).first().fill('native@example.com');
        await page.getByRole('button',{name:'Render preview',exact:true}).click();
        await expect(page.locator('body')).toContainText('Native Ada');
        expect(page.url()).not.toContain('_csrf');
    } finally { await context.close(); }
});
