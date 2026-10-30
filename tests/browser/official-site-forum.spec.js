const {test,expect} = require('playwright/test');
async function settled(page) {
    await expect(page.locator('.htmx-added,.htmx-settling,.htmx-request')).toHaveCount(0);
}

test('forum native disclosures, protected actions, quotes, drafts, pagination and visitor isolation', async ({page,browser}) => {
    const errors=[];
    page.on('pageerror',error => errors.push(error.message));
    page.on('response',response => { if (response.status() >= 400) errors.push(`${response.status()} ${response.url()}`); });
    await page.goto('/demos/features/forum');
    await page.getByRole('link',{name:'Announcements',exact:true}).click();
    const next = page.locator('.forum-topics-page-next');
    await expect(next).toBeEnabled();
    await next.click();
    await expect(page.locator('#forum-main')).toContainText('Page 2');
    await page.goto('/forum?view=topics&scope=cat-announcements');
    await page.locator('summary').filter({hasText:/^New Topic$/}).click();
    const title = 'Visitor topic ' + Date.now();
    await page.getByLabel('Title',{exact:true}).fill(title);
    await page.getByLabel('Body',{exact:true}).fill('A real browser-created temporary topic.');
    await page.getByRole('button',{name:'Post Topic',exact:true}).click();
    await page.getByRole('link',{name:title,exact:true}).click();
    await expect(page.locator('#forum-main')).toContainText('A real browser-created temporary topic.');
    await page.locator('summary').filter({hasText:/^New Comment$/}).click();
    await page.getByLabel('Comment',{exact:true}).fill('A visitor-owned comment');
    await page.getByRole('button',{name:'Post Comment',exact:true}).click();
    const comment = page.locator('.forum-comment').filter({hasText:'A visitor-owned comment'}).first();
    await settled(page);
    await comment.locator('.forum-action-quote').click();
    await settled(page);
    await expect(page.getByLabel('Comment',{exact:true})).toHaveValue(/\[\[quote::/);
    await comment.locator('.forum-action-edit').click();
    await settled(page);
    await page.getByLabel('Comment',{exact:true}).fill('A revised visitor-owned comment');
    await page.getByRole('button',{name:'Save Comment',exact:true}).click();
    const revised = page.locator('.forum-comment').filter({hasText:'A revised visitor-owned comment'}).first();
    await expect(revised).toBeVisible();
    await revised.locator('.forum-action-delete').click();
    await expect(page.locator('.forum-comment').filter({hasText:'A revised visitor-owned comment'})).toHaveCount(0);
    const peer = await browser.newContext();
    try {
        const other = await peer.newPage();
        await other.goto('http://127.0.0.1:18080/forum?view=topics&scope=cat-announcements');
        await expect(other.getByRole('link',{name:title,exact:true})).toHaveCount(0);
    } finally { await peer.close(); }
    expect(errors).toEqual([]);
});
