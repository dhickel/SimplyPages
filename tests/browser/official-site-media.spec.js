const {test,expect} = require('playwright/test');

test('bundled audio and video actually play, and simulated account actions change the display', async ({page}) => {
    await page.goto('/demos/components/media');
    for (const tag of ['audio','video']) {
        const media = page.locator(tag);
        await expect.poll(() => media.evaluate(element => element.readyState)).toBeGreaterThanOrEqual(2);
        expect(await media.evaluate(element => element.duration)).toBeGreaterThan(1.5);
        await media.evaluate(element => element.play());
        await expect.poll(() => media.evaluate(element => element.currentTime)).toBeGreaterThan(0.05);
        await media.evaluate(element => element.pause());
    }
    await page.goto('/demos/components/navigation#accountwidget');
    await page.getByRole('button',{name:'Hello, Simulated Ada'}).click();
    await page.getByRole('link',{name:'Logout',exact:true}).click();
    await expect(page.locator('#accountwidget')).toContainText('Simulated guest display only');
    await page.getByRole('link',{name:'Login',exact:true}).click();
    await expect(page.getByRole('button',{name:'Hello, Simulated Ada'})).toBeVisible();
});

test('site chrome contrast and 200 percent CSS zoom remain usable', async ({page},testInfo) => {
    await page.setViewportSize({width:1440,height:900});
    await page.goto('/');
    const ratios = await page.evaluate(() => {
        const lum = color => {
            const values = color.match(/[\d.]+/g).slice(0,3).map(value => Number(value)/255)
                .map(value => value <= .04045 ? value/12.92 : ((value+.055)/1.055)**2.4);
            return values[0]*.2126+values[1]*.7152+values[2]*.0722;
        };
        return ['.sp-site-nav a','.sp-site-eyebrow','.sp-site-lede','.sp-site-primary'].map(selector => {
            const element = document.querySelector(selector), css = getComputedStyle(element);
            const bg = selector === '.sp-site-primary' ? css.backgroundColor : getComputedStyle(document.body).backgroundColor;
            const values = [lum(css.color),lum(bg)].sort((a,b) => b-a);
            return (values[0]+.05)/(values[1]+.05);
        });
    });
    expect(ratios.every(value => value >= 4.5)).toBe(true);
    await page.evaluate(() => { document.documentElement.style.zoom='2'; });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth+1)).toBe(true);
    await expect(page.getByRole('link',{name:'Explore working demos →'})).toBeVisible();
    await page.screenshot({path:testInfo.outputPath('home-css-zoom-200.png')});
});
