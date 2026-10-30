const {test, expect} = require('playwright/test');

test('catalog listings retain inline code, divider context, Designer and complete request targets', async ({page}) => {
    await page.goto('/demos/components/text-structure');
    const code = page.locator('#code');
    await expect(code.locator('[data-example-preview] code.code-inline')).toHaveText('SlotKey<String>');
    await expect(code.locator('.sp-site-example-grid > .code-container code')).toContainText('Code.inline("SlotKey<String>")');
    for (const text of ['Above', 'Below']) {
        await expect(page.locator('#divider [data-example-preview]')).toContainText(text);
        await expect(page.locator('#divider .sp-site-example-grid > .code-container code')).toContainText(`new Paragraph("${text}")`);
    }
    await page.goto('/demos/components/forms');
    await page.getByLabel('Name', {exact:true}).fill('Designer preview');
    await page.getByLabel('Email', {exact:true}).first().fill('designer@example.com');
    await page.getByLabel('Role', {exact:true}).selectOption('designer');
    await expect(page.locator('#form .sp-site-example-grid > .code-container code')).toContainText('.addOption("designer", "Designer")');
    await page.getByRole('button', {name:'Render preview',exact:true}).click();
    await expect(page.locator('#form-preview')).toContainText('role=designer');
});

for (const width of [1024, 390]) {
    test(`editing contract examples execute protected request copies and real nested modal at ${width}`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height:900});
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.goto('/demos/features/editing');
        for (const anchor of ['editablepage','live-editing-contracts','edit-form-builder','module-edit-handler','authwrapper'])
            await expect(page.locator(`#${anchor}`)).toHaveCount(1);
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
        const result = page.locator('#handler-result');
        await result.getByLabel('Preview title:', {exact:true}).fill('Changed <title>');
        await result.getByLabel('Preview content:', {exact:true}).fill('Request-only <body>');
        await result.getByRole('button', {name:'Apply preview request'}).click();
        await expect(page.locator('#handler-copy')).toContainText('Updated request-owned copy');
        await expect(page.locator('#handler-copy')).toContainText('Request-only <body>');
        await result.getByLabel('Preview mode',{exact:true}).selectOption('USER_EDIT');
        await result.getByRole('button', {name:'Apply preview request'}).click();
        await expect(page.locator('#handler-copy')).toContainText('Proposal preview only');
        await result.getByLabel('Preview content:',{exact:true}).fill('   ');
        await result.getByRole('button', {name:'Apply preview request'}).click();
        await expect(result.getByRole('alert')).toContainText('Content cannot be empty');
        await expect(result.getByLabel('Preview content:',{exact:true})).toHaveValue('   ');
        await result.getByLabel('Preview content:',{exact:true}).fill('Copy to delete');
        await result.getByLabel('Operation',{exact:true}).selectOption('delete');
        await result.getByRole('button', {name:'Apply preview request'}).click();
        await expect(page.locator('#handler-copy')).toContainText('Delete proposal preview only');
        await expect(page.locator('#handler-copy .module')).toHaveCount(1);
        await result.getByLabel('Operation',{exact:true}).selectOption('delete');
        await result.getByLabel('Preview mode',{exact:true}).selectOption('OWNER_EDIT');
        await result.getByRole('button', {name:'Apply preview request'}).click();
        await expect(page.locator('#handler-copy')).toContainText('Deleted request-owned preview copy');
        await expect(page.locator('#handler-copy .module')).toHaveCount(0);
        await page.reload();
        await expect(page.locator('#handler-result input[name=title]')).toHaveValue('Request-owned title');
        await expect(page.locator('#page-content')).not.toContainText('Changed <title>');
        await page.locator('#authwrapper').getByLabel('Simulate permission granted').check();
        await page.getByRole('button',{name:'Evaluate policy'}).click();
        await expect(page.locator('#auth-result')).toContainText('Simulated action allowed');
        await expect(page.locator('#auth-result')).toContainText('Reviewer may delete: false');
        await expect(page.locator('#auth-result')).toContainText('Owner may delete: true');
        const trigger = page.getByRole('button',{name:'Open live nested proposal',exact:true});
        await trigger.click();
        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible();
        await expect(dialog).toContainText('staged for approval');
        await expect(dialog.locator('[hx-get^="/editing-demo/edit-child/module-9/"]')).toHaveCount(3);
        await expect(dialog.locator('[hx-get^="/editing-demo/edit-child/module-9/"]').first()).toHaveAttribute('hx-get', /editMode=USER_EDIT$/);
        await page.keyboard.press('Escape');
        await expect(dialog).toHaveCount(0);
        await expect(trigger).toBeFocused();
        await page.locator('#module-edit-handler').scrollIntoViewIfNeeded();
        const shot = testInfo.outputPath(`contract-examples-${width}.png`);
        await page.screenshot({path:shot});
        await testInfo.attach(`contract-examples-${width}`, {path:shot,contentType:'image/png'});
        expect(errors).toEqual([]);
    });
}
