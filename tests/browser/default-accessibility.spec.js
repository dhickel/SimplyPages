const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');
test.use({hasTouch: true});
const snapshots = path.resolve(__dirname, '../../simplypages/src/test/resources/snapshots');
const generated = fs.readFileSync(path.join(snapshots, 'integration/accessibility/default-components.snap.html'), 'utf8');
const accordionGenerated = fs.readFileSync(path.join(snapshots, 'integration/accessibility/accordion-components.snap.html'), 'utf8');
const modal = fs.readFileSync(path.join(snapshots, 'display/modal/default.snap.html'), 'utf8');

async function fixture(page) {
    await page.route('**/accessibility-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <script defer src="/js/framework.js"></script></head><body>${generated}<div id="modal-host"></div></body></html>`}));
    await page.goto('/accessibility-fixture');
}

async function accordionFixture(page) {
    await page.route('**/accordion-accessibility-fixture', route => route.fulfill({contentType: 'text/html', body:
        `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css">
        <script defer src="/js/framework.js"></script></head><body>${accordionGenerated}</body></html>`}));
    await page.goto('/accordion-accessibility-fixture');
}

async function evidence(page, testInfo, name) {
    if (await page.locator('.modal-container').count()) {
        await expect.poll(() => page.locator('.modal-container').last()
            .evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    }
    const screenshot = testInfo.outputPath(`${name}.png`);
    await page.screenshot({path: screenshot, fullPage: true});
    await testInfo.attach(name, {path: screenshot, contentType: 'image/png'});
}

async function jsonEvidence(testInfo, name, value) {
    const output = testInfo.outputPath(`${name}.json`);
    fs.writeFileSync(output, JSON.stringify(value, null, 2));
    await testInfo.attach(name, {path: output, contentType: 'application/json'});
}

function luminance(rgb) {
    return rgb.slice(0, 3).map(value => value / 255)
        .map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
        .reduce((sum, value, i) => sum + value * [.2126, .7152, .0722][i], 0);
}

function contrast(foreground, background) {
    return (Math.max(luminance(foreground), luminance(background)) + .05)
        / (Math.min(luminance(foreground), luminance(background)) + .05);
}

function parsePaints(style) {
    const source = style.backgroundImage === 'none' ? style.backgroundColor : style.backgroundImage;
    return [...source.matchAll(/rgba?\(([^)]+)\)/g)].map(match => match[1].split(',').map(Number));
}

for (const viewport of [{width: 1440, height: 900}, {width: 390, height: 844}]) {
    test(`generated form names and progress visual contract at ${viewport.width}px`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        await fixture(page);
        if (viewport.width <= 768) {
            const content = await page.locator('.content-wrapper').boundingBox();
            expect(content.x).toBeLessThan(50);
            expect(content.width).toBeGreaterThan(viewport.width - 60);
        }
        await expect(page.getByRole('textbox', {name: 'Name', exact: true})).toHaveCount(1);
        await expect(page.getByRole('textbox', {name: 'Notes', exact: true})).toHaveCount(1);
        await expect(page.getByRole('combobox', {name: 'Choice', exact: true})).toHaveCount(1);
        const checkbox = page.getByRole('checkbox', {name: /Notifications/});
        await page.locator('label.form-label').filter({hasText: 'Notifications'}).click();
        await expect(checkbox).toBeChecked();
        await expect(page.getByRole('radiogroup', {name: 'Plan', exact: true})).toHaveCount(1);
        await expect(page.getByRole('radio', {name: 'Basic', exact: true})).toHaveCount(1);
        const bars = await page.locator('.progress').evaluateAll(elements => elements.map(element => {
            const fill = element.querySelector('.progress-bar');
            const rootStyle = getComputedStyle(element);
            const style = getComputedStyle(fill);
            return {height: element.getBoundingClientRect().height, fillHeight: fill.getBoundingClientRect().height,
                width: fill.getBoundingClientRect().width / element.clientWidth, name: element.getAttribute('aria-label'),
                image: style.backgroundImage, animation: style.animationName, transition: style.transitionDuration,
                rootStyle: rootStyle.height, backgroundSize: style.backgroundSize};
        }));
        expect(bars).toHaveLength(6);
        for (const [index, bar] of bars.entries()) {
            expect(bar.height).toBe(index === 1 ? 32 : 20);
            expect(bar.fillHeight).toBeGreaterThanOrEqual(bar.height - 2);
            expect(bar.fillHeight).toBeLessThanOrEqual(bar.height);
            expect(bar.width).toBeCloseTo(index === 0 ? .5 : index === 1 ? .75 : index === 2 ? 1 : .25, 2);
            expect(bar.image).not.toBe('none');
        }
        expect(bars[0].animation).toBe('progress-stripes');
        const animatedFill = page.locator('.progress-bar-animated');
        const initialPosition = await animatedFill.evaluate(element => getComputedStyle(element).backgroundPosition);
        await expect.poll(() => animatedFill.evaluate(element => getComputedStyle(element).backgroundPosition))
            .not.toBe(initialPosition);
        expect(bars[0].image.match(/linear-gradient/g)).toHaveLength(2);
        expect(bars[1].image.match(/linear-gradient/g)).toHaveLength(2);
        expect(new Set(bars.slice(1, 5).map(bar => bar.image)).size).toBe(4);
        await expect(page.getByRole('progressbar', {name: 'Complete', exact: true})).toHaveAttribute('aria-valuenow', '80');
        await expect(page.getByRole('progressbar', {name: 'Three quarters', exact: true})).toHaveAttribute('aria-valuemax', '200');
        await page.getByRole('progressbar', {name: 'Half', exact: true}).scrollIntoViewIfNeeded();
        await evidence(page, testInfo, `progress-${viewport.width}`);
        await page.emulateMedia({reducedMotion: 'reduce'});
        await expect.poll(() => page.locator('.progress-bar-animated').evaluate(element => getComputedStyle(element).animationName)).toBe('none');
        expect(await page.locator('.progress-bar-animated').evaluate(element => getComputedStyle(element).transitionDuration)).toBe('0s');
    });

    test(`generated disclosures native keyboard, touch/click and modal Escape at ${viewport.width}px`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        await fixture(page);
        const trigger = page.getByRole('button', {name: 'Actions', exact: true});
        await expect(page.getByRole('link', {name: 'Overview', exact: true})).toHaveCount(0);
        await trigger.focus();
        await page.keyboard.press('Enter');
        await expect(trigger).toHaveAttribute('aria-expanded', 'true');
        await page.keyboard.press('Tab');
        await expect(page.getByRole('link', {name: 'Overview', exact: true})).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(page.getByRole('button', {name: 'Custom action', exact: true})).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(trigger).toBeFocused();
        await expect(trigger).toHaveAttribute('aria-expanded', 'false');
        await page.keyboard.press('Space');
        await expect(trigger).toHaveAttribute('aria-expanded', 'true');
        // A mobile menu may naturally overlay the next wrapped inline component.
        await page.getByRole('textbox', {name: 'Name', exact: true}).click();
        await expect(trigger).toHaveAttribute('aria-expanded', 'false');
        if (viewport.width === 390) {
            await trigger.tap();
            await expect(trigger).toHaveAttribute('aria-expanded', 'true');
            await trigger.tap();
            await expect(trigger).toHaveAttribute('aria-expanded', 'false');
        }
        const account = page.getByRole('button', {name: 'Hello, alice', exact: true});
        if (viewport.width === 390) await account.tap();
        else await account.click();
        await expect(account).toHaveAttribute('aria-expanded', 'true');
        await page.getByRole('textbox', {name: 'Name', exact: true}).click();
        await expect(account).toHaveAttribute('aria-expanded', 'false');
        await account.focus();
        await page.keyboard.press('Enter');
        await expect(account).toHaveAttribute('aria-expanded', 'true');
        await page.keyboard.press('Tab');
        await expect(page.getByRole('link', {name: 'Profile', exact: true})).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(page.getByRole('link', {name: 'Logout', exact: true})).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(account).toBeFocused();
        await expect(page.getByRole('link', {name: 'Profile', exact: true})).toHaveCount(0);
        await page.keyboard.press('Space');
        await expect(account).toHaveAttribute('aria-expanded', 'true');
        await evidence(page, testInfo, `account-disclosure-${viewport.width}`);
        await page.keyboard.press('Escape');
        await expect(account).toHaveAttribute('aria-expanded', 'false');
        await trigger.click();
        await evidence(page, testInfo, `disclosure-${viewport.width}`);
        const markup = await page.locator('.dropdown').evaluate(element => {
            const html = element.outerHTML;
            element.remove();
            return html;
        });
        // Reuse actual generated disclosure markup inside actual generated dialog markup.
        await page.locator('#modal-host').evaluate((host, html) => {host.innerHTML = html;},
            modal.replace('<div>Body content</div>', markup));
        const dialog = page.getByRole('dialog');
        const child = dialog.getByRole('button', {name: 'Actions', exact: true});
        await expect(child).toBeFocused();
        // The transferred open state survives insertion; close then activate by native keyboard.
        await child.click();
        await page.keyboard.press('Space');
        await page.keyboard.press('Tab');
        await expect(dialog.getByRole('link', {name: 'Overview', exact: true})).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(dialog).toHaveCount(1);
        await expect(child).toHaveAttribute('aria-expanded', 'false');
        await expect(child).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(dialog).toHaveCount(0);
    });
}

for (const viewport of [{width: 1440, height: 900}, {width: 390, height: 844}]) {
    test(`generated accordion content, states and paints at ${viewport.width}px`, async ({page}, testInfo) => {
        await page.setViewportSize(viewport);
        await accordionFixture(page);

        const single = page.locator('#accordion-single');
        const firstHeader = single.locator('.accordion-header').first();
        const firstPanel = single.locator('.accordion-content').first();
        const closedHeader = single.locator('.accordion-header').nth(1);
        const closedPanel = single.locator('.accordion-content').nth(1);
        const closedControl = closedPanel.locator('button').filter({hasText: 'Closed section control'});

        await expect(closedPanel).toHaveAttribute('hidden', 'true');
        await expect(closedPanel).toHaveAttribute('inert');
        await expect(closedPanel).toHaveAttribute('aria-hidden', 'true');
        await closedControl.focus();
        await expect(closedControl).not.toBeFocused();
        await expect(closedHeader).not.toBeFocused();
        expect(await page.evaluate(() => document.activeElement?.id)).not.toBe('accordion-single-item-1-content');

        const normalPaint = await closedHeader.evaluate(element => {
            const style = getComputedStyle(element);
            return {foreground: style.color, backgroundImage: style.backgroundImage, backgroundColor: style.backgroundColor};
        });
        const normalForeground = parsePaints({backgroundImage: 'none', backgroundColor: normalPaint.foreground})[0];
        const normalBackgrounds = parsePaints(normalPaint);
        expect(normalBackgrounds.length).toBeGreaterThanOrEqual(2);
        expect(Math.min(...normalBackgrounds.map(background => contrast(normalForeground, background)))).toBeGreaterThanOrEqual(4.5);

        await closedHeader.hover();
        await page.waitForTimeout(350);
        const hoverPaint = await closedHeader.evaluate(element => {
            const style = getComputedStyle(element);
            return {color: style.color, backgroundImage: style.backgroundImage, backgroundColor: style.backgroundColor};
        });
        const hoverForeground = parsePaints({backgroundImage: 'none', backgroundColor: hoverPaint.color})[0];
        const hoverBackgrounds = parsePaints(hoverPaint);
        expect(hoverBackgrounds.length).toBeGreaterThanOrEqual(1);
        expect(Math.min(...hoverBackgrounds.map(background => contrast(hoverForeground, background)))).toBeGreaterThanOrEqual(4.5);

        const activePaint = await firstHeader.evaluate(element => {
            const style = getComputedStyle(element);
            return {color: style.color, backgroundImage: style.backgroundImage, backgroundColor: style.backgroundColor};
        });
        const activeForeground = parsePaints({backgroundImage: 'none', backgroundColor: activePaint.color})[0];
        const activeBackgrounds = parsePaints(activePaint);
        expect(activePaint.backgroundImage).toBe('none');
        expect(Math.min(...activeBackgrounds.map(background => contrast(activeForeground, background)))).toBeGreaterThanOrEqual(4.5);

        await firstHeader.focus();
        await page.keyboard.press('Enter');
        await expect(firstPanel).toBeHidden();
        await expect(firstPanel).toHaveAttribute('inert');
        await expect(firstHeader).toBeFocused();
        await closedHeader.focus();
        await page.keyboard.press('Space');
        await expect(closedPanel).not.toBeHidden();
        await expect(closedPanel).toHaveAttribute('aria-hidden', 'false');
        await expect(closedPanel).not.toHaveAttribute('inert');
        await expect(firstPanel).toBeHidden();
        await firstHeader.focus();
        await page.keyboard.press('Enter');
        await expect(firstPanel).not.toBeHidden();
        await expect(closedPanel).toBeHidden();

        const finalControl = firstPanel.getByRole('button', {name: 'Final accordion control'});
        await finalControl.focus();
        await firstHeader.evaluate(element => element.dispatchEvent(new MouseEvent('click', {bubbles: true})));
        await expect(firstHeader).toBeFocused();
        await expect(firstPanel).toBeHidden();
        await firstHeader.press('Enter');
        await expect(firstPanel).not.toBeHidden();

        const corners = await firstPanel.evaluate(panel => {
            const item = panel.closest('.accordion-item');
            const header = item.querySelector(':scope > .accordion-header');
            const body = panel.querySelector(':scope > .accordion-body');
            const radius = element => {
                const style = getComputedStyle(element);
                return {topLeft: style.borderTopLeftRadius, topRight: style.borderTopRightRadius,
                    bottomRight: style.borderBottomRightRadius, bottomLeft: style.borderBottomLeftRadius};
            };
            const itemStyle = getComputedStyle(item);
            return {itemOverflow: itemStyle.overflow, panelOverflow: getComputedStyle(panel).overflow,
                bodyOverflow: getComputedStyle(body).overflow, borderLeft: parseFloat(itemStyle.borderLeftWidth),
                borderRight: parseFloat(itemStyle.borderRightWidth), borderBottom: parseFloat(itemStyle.borderBottomWidth),
                itemRect: item.getBoundingClientRect().toJSON(), headerRect: header.getBoundingClientRect().toJSON(),
                panelRect: panel.getBoundingClientRect().toJSON(), bodyRect: body.getBoundingClientRect().toJSON(),
                headerRadius: radius(header), bodyRadius: radius(body)};
        });
        const closedRadius = await closedHeader.evaluate(header => {
            const style = getComputedStyle(header);
            return {topLeft: style.borderTopLeftRadius, topRight: style.borderTopRightRadius,
                bottomRight: style.borderBottomRightRadius, bottomLeft: style.borderBottomLeftRadius};
        });
        expect(corners.itemOverflow).toBe('visible');
        expect(corners.panelOverflow).toBe('visible');
        expect(corners.bodyOverflow).toBe('visible');
        expect(corners.headerRadius).toEqual({topLeft: '5px', topRight: '5px', bottomRight: '0px', bottomLeft: '0px'});
        expect(corners.bodyRadius).toEqual({topLeft: '0px', topRight: '0px', bottomRight: '6px', bottomLeft: '6px'});
        expect(closedRadius).toEqual({topLeft: '5px', topRight: '5px', bottomRight: '5px', bottomLeft: '5px'});
        expect(corners.headerRect.left).toBeGreaterThanOrEqual(corners.itemRect.left + corners.borderLeft - 0.01);
        expect(corners.headerRect.right).toBeLessThanOrEqual(corners.itemRect.right - corners.borderRight + 0.01);
        expect(corners.panelRect.bottom).toBeCloseTo(corners.itemRect.bottom - corners.borderBottom, 1);
        expect(corners.bodyRect.top).toBeCloseTo(corners.panelRect.top, 1);
        expect(corners.bodyRect.bottom).toBeCloseTo(corners.panelRect.bottom, 1);

        const metrics = await firstPanel.evaluate(panel => {
            const body = panel.querySelector('.accordion-body');
            const control = panel.querySelector('button:last-of-type');
            const panelRect = panel.getBoundingClientRect();
            const bodyRect = body.getBoundingClientRect();
            const controlRect = control.getBoundingClientRect();
            return {clientHeight: panel.clientHeight, scrollHeight: panel.scrollHeight,
                bodyHeight: bodyRect.height, panelBottom: panelRect.bottom, controlBottom: controlRect.bottom,
                controlId: control.id};
        });
        expect(metrics.clientHeight).toBeGreaterThan(3000);
        expect(metrics.scrollHeight).toBe(metrics.clientHeight);
        expect(metrics.bodyHeight).toBeGreaterThan(3000);
        expect(metrics.controlBottom).toBeLessThanOrEqual(metrics.panelBottom + 1);

        await firstPanel.locator('.accordion-body').evaluate(body => body.insertAdjacentHTML('beforeend',
            `<p id="late-accordion-content">${'Late accordion content '.repeat(300)}</p>
             <button id="late-accordion-control" type="button">Late accordion control</button>`));
        await expect.poll(() => firstPanel.evaluate(panel => panel.clientHeight)).toBeGreaterThan(metrics.clientHeight);
        const lateMetrics = await firstPanel.evaluate(panel => {
            const late = panel.querySelector('#late-accordion-control');
            const panelRect = panel.getBoundingClientRect();
            const lateRect = late.getBoundingClientRect();
            return {panelHeight: panel.clientHeight, scrollHeight: panel.scrollHeight,
                lateBottom: lateRect.bottom, panelBottom: panelRect.bottom};
        });
        expect(lateMetrics.panelHeight).toBe(lateMetrics.scrollHeight);
        expect(lateMetrics.lateBottom).toBeLessThanOrEqual(lateMetrics.panelBottom + 1);

        await finalControl.scrollIntoViewIfNeeded();
        const finalBox = await finalControl.boundingBox();
        const hit = await page.evaluate(({x, y}) => document.elementFromPoint(x, y)?.closest('#accordion-single-item-0-content')?.id,
            {x: finalBox.x + finalBox.width / 2, y: finalBox.y + finalBox.height / 2});
        expect(hit).toBe('accordion-single-item-0-content');
        await finalControl.evaluate(element => element.addEventListener('click', () => element.dataset.clicked = 'true', {once: true}));
        await finalControl.click();
        await expect(finalControl).toHaveAttribute('data-clicked', 'true');

        const actions = firstPanel.getByRole('button', {name: 'Accordion actions'});
        await actions.click();
        const actionLink = firstPanel.getByRole('link', {name: 'Accordion action'});
        await expect(actionLink).toBeVisible();
        const menuBox = await firstPanel.locator('.dropdown-menu').boundingBox();
        const linkBox = await actionLink.boundingBox();
        expect(linkBox.y + linkBox.height).toBeLessThanOrEqual(menuBox.y + menuBox.height + 1);
        const menuHit = await page.evaluate(({x, y}) => document.elementFromPoint(x, y)?.closest('.dropdown-link')?.textContent,
            {x: linkBox.x + linkBox.width / 2, y: linkBox.y + linkBox.height / 2});
        expect(menuHit).toBe('Accordion action');

        const multiple = page.locator('#accordion-multiple');
        await multiple.locator('.accordion-header').nth(0).click();
        await multiple.locator('.accordion-header').nth(1).click();
        await expect(multiple.locator('.accordion-content.expanded')).toHaveCount(2);

        const nestedOuter = page.locator('#accordion-nested .accordion-header').first();
        const nested = page.locator('#nested-accordion');
        await nested.locator('.accordion-header').nth(1).click();
        await expect(nested.locator('.accordion-content.expanded')).toHaveCount(2);
        await nestedOuter.click();
        await expect(nested.locator('.accordion-content.expanded')).toHaveCount(2);
        await nestedOuter.click();
        await expect(nested.locator('.accordion-content.expanded')).toHaveCount(2);

        await page.locator('body').evaluate(body => body.insertAdjacentHTML('beforeend', `
            <div class="accordion" id="dynamic-accordion" data-single-expand="true">
                <div class="accordion-item">
                    <button class="accordion-header" type="button" id="dynamic-header"
                        aria-expanded="false" aria-controls="dynamic-content">Dynamic accordion</button>
                    <div class="accordion-content" id="dynamic-content" role="region"
                        aria-labelledby="dynamic-header" aria-hidden="true" hidden="true" inert="true">
                        <div class="accordion-body"><button type="button">Dynamic control</button></div>
                    </div>
                </div>
            </div>`));
        const dynamicHeader = page.locator('#dynamic-header');
        const dynamicPanel = page.locator('#dynamic-content');
        await dynamicHeader.focus();
        await page.keyboard.press('Enter');
        await expect(dynamicPanel).not.toBeHidden();
        await expect(dynamicPanel).not.toHaveAttribute('inert');
        await page.keyboard.press('Enter');
        await expect(dynamicPanel).toBeHidden();
        await expect(dynamicHeader).toBeFocused();

        await page.emulateMedia({reducedMotion: 'reduce'});
        const reduced = await firstHeader.evaluate(element => ({
            headerTransition: getComputedStyle(element).transitionDuration,
            iconTransition: getComputedStyle(element.querySelector('.accordion-icon') || element).transitionDuration
        }));
        expect(reduced.headerTransition).toBe('0s');
        expect(reduced.iconTransition).toBe('0s');
        await evidence(page, testInfo, `accordion-${viewport.width}`);
    });
}

async function nativePageModal(page) {
    await fixture(page);
    const markup = await page.locator('.page-content').evaluate(element => {
        const html = element.outerHTML;
        element.remove();
        return html;
    });
    await page.locator('#modal-host').evaluate((host, html) => {host.innerHTML = html;},
        modal.replace('<div>Body content</div>', markup));
    const dialog = page.getByRole('dialog');
    return {dialog, details: dialog.locator('.sticky-sidebar-mobile-collapse'),
        summary: dialog.locator('.sticky-sidebar-mobile-summary'), close: dialog.locator('.modal-close'),
        save: dialog.getByRole('button', {name: 'Save', exact: true}),
        link: dialog.locator('a[href="#details"]')};
}

test('generated Page summary in Modal participates in forward/reverse traversal and native Enter/Space', async ({page}, testInfo) => {
    await page.setViewportSize({width: 390, height: 844});
    const {dialog, details, summary, close, save, link} = await nativePageModal(page);
    await expect(summary).toHaveJSProperty('tabIndex', 0);
    expect(await summary.getAttribute('tabindex')).toBeNull();
    await expect(summary).toBeFocused(); // Initial body focus includes native summary.
    await close.focus();
    await page.keyboard.press('Tab');
    await expect(summary).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(link).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(save).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(link).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(summary).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(details).toHaveJSProperty('open', false);
    await close.focus();
    await page.keyboard.press('Tab');
    await expect(summary).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(save).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(summary).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(close).toBeFocused();
    await page.keyboard.press('Tab');
    await page.keyboard.press('Space');
    await expect(details).toHaveJSProperty('open', true);
    await expect(summary).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(link).toBeFocused();
    await page.keyboard.press('Shift+Tab');
    await expect(summary).toBeFocused();
    await expect.poll(() => dialog.evaluate(element => getComputedStyle(element).opacity)).toBe('1');
    const geometry = await summary.evaluate(element => {
        const rect = element.getBoundingClientRect();
        const dialog = element.closest('.modal-container').getBoundingClientRect();
        return {focused: document.activeElement === element, rect: rect.toJSON(), dialog: dialog.toJSON(),
            hit: document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2) === element,
            backgroundScroll: {x: scrollX, y: scrollY}};
    });
    expect(geometry.hit).toBe(true);
    expect(geometry.rect.top).toBeGreaterThanOrEqual(Math.max(0, geometry.dialog.top));
    expect(geometry.rect.bottom).toBeLessThanOrEqual(Math.min(844, geometry.dialog.bottom));
    expect(geometry.backgroundScroll).toEqual({x: 0, y: 0});
    await jsonEvidence(testInfo, 'native-summary-focus-geometry', geometry);
    await evidence(page, testInfo, 'native-summary-modal-mobile');
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
});

for (const visibilityAPI of [true, false]) {
    test(`native summary eligibility and nested closed details in generated Modal (visibility API ${visibilityAPI})`, async ({page}) => {
        await page.setViewportSize({width: 390, height: 844});
        if (!visibilityAPI) await page.addInitScript(() => {Element.prototype.checkVisibility = undefined;});
        const {details, summary, close, save, link} = await nativePageModal(page);
        await details.locator('.sticky-sidebar-content').evaluate(element => element.insertAdjacentHTML('beforeend', `
            <details id="inner-details"><summary id="inner-summary">Inner disclosure</summary>
                <a id="inner-link" href="#inner">Inner link</a>
                <details open><summary>Deep summary</summary><button>Deep button</button></details>
            </details>
            <details open><summary tabindex="-1">Negative summary</summary></details>
            <summary>Orphan summary</summary>
            <details open><summary tabindex="-1">First negative summary</summary><summary>Second summary</summary></details>
            <details inert open><summary>Inert summary</summary></details>
            <details hidden open style="display:block"><summary>Hidden summary</summary></details>`));
        await close.focus();
        await page.keyboard.press('Tab');
        await expect(summary).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(link).toBeFocused();
        await page.keyboard.press('Tab');
        const inner = page.locator('#inner-summary');
        await expect(inner).toBeFocused();
        await page.keyboard.press('Tab'); // Closed inner content and non-native/negative summaries are excluded.
        await expect(save).toBeFocused();
        await page.keyboard.press('Shift+Tab');
        await expect(inner).toBeFocused();
        await page.keyboard.press('Enter');
        await expect(page.locator('#inner-details')).toHaveJSProperty('open', true);
        await page.keyboard.press('Tab');
        await expect(page.locator('#inner-link')).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(details.locator('summary').filter({hasText: 'Deep summary'})).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(details.getByRole('button', {name: 'Deep button'})).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(save).toBeFocused();
        await details.evaluate(element => {element.open = false;}); // Outer closure hides even open nested details.
        await close.focus();
        await page.keyboard.press('Tab');
        await expect(summary).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(save).toBeFocused();
        await page.keyboard.press('Shift+Tab');
        await expect(summary).toBeFocused();
        await summary.evaluate(element => element.setAttribute('tabindex', '-1'));
        await close.focus();
        await page.keyboard.press('Tab');
        await expect(save).toBeFocused();
        await page.keyboard.press('Shift+Tab');
        await expect(close).toBeFocused();
    });
}

for (const visibilityAPI of [true, false]) {
    test(`ordinary nested native-hidden details with positive boxes remain outside modal traversal (visibility API ${visibilityAPI})`, async ({page}, testInfo) => {
        await page.setViewportSize({width: 1440, height: 900});
        if (!visibilityAPI) await page.addInitScript(() => {Element.prototype.checkVisibility = undefined;});
        await fixture(page);
        const pages = fs.readFileSync(path.resolve(__dirname,
            '../../simplypages/target/phase13-shell-page-fixtures/page-details.html'), 'utf8');
        await page.locator('#modal-host').evaluate((host, {html, pages}) => {
            const ordinary = new DOMParser().parseFromString(pages, 'text/html').getElementById('ordinary-outer');
            ordinary.open = true;
            ordinary.querySelectorAll('summary').forEach(summary => summary.setAttribute('tabindex', '-1'));
            host.innerHTML = html.replace('<div>Body content</div>',
                '<style>#ordinary-hidden-control { display: block !important; height: 32px; }</style>' + ordinary.outerHTML);
        }, {html: modal, pages});
        const dialog = page.getByRole('dialog');
        const link = dialog.locator('#ordinary-hidden-control');
        await dialog.locator('#ordinary-inner').evaluate(element => { element.open = true; });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        await dialog.locator('#ordinary-inner').evaluate(element => { element.open = false; });
        const probe = await link.evaluate(element => ({nativeOpen: element.closest('details').open,
            rects: element.getClientRects().length, rect: element.getBoundingClientRect().toJSON(),
            nativeVisible: element.checkVisibility ? element.checkVisibility({checkVisibilityCSS: true}) : null}));
        expect(probe.nativeOpen).toBe(false);
        expect(probe.rects).toBeGreaterThan(0);
        expect(probe.rect.height).toBeGreaterThan(0);
        expect(probe.nativeVisible).toBe(visibilityAPI ? false : null);
        await jsonEvidence(testInfo, 'native-hidden-positive-boxes', probe);
        const close = dialog.locator('.modal-close');
        const save = dialog.getByRole('button', {name: 'Save', exact: true});
        await expect(close).toBeFocused();
        await page.keyboard.press('Tab');
        await expect(save).toBeFocused();
        await page.keyboard.press('Shift+Tab');
        await expect(close).toBeFocused();
    });
}

test('generated tabs rove with owned automatic activation, wrapping, hidden panels and native activation', async ({page}) => {
    await fixture(page);
    const first = page.locator('#outer-tabs-tab-0');
    const second = page.locator('#outer-tabs-tab-1');
    const third = page.locator('#outer-tabs-tab-2');
    await first.focus();
    await page.keyboard.press('ArrowRight');
    await expect(second).toBeFocused();
    await expect(second).toHaveAttribute('aria-selected', 'true');
    await expect(first).toHaveAttribute('tabindex', '-1');
    await expect(page.locator('#outer-tabs-panel-0')).toHaveAttribute('hidden', '');
    await expect(page.locator('#outer-tabs-panel-1')).not.toHaveAttribute('hidden', '');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('textbox', {name: 'Second control'})).toBeFocused();
    await second.focus();
    await page.keyboard.press('End');
    await expect(third).toBeFocused();
    await page.keyboard.press('ArrowRight');
    await expect(first).toBeFocused();
    await page.keyboard.press('ArrowLeft');
    await expect(third).toBeFocused();
    await page.keyboard.press('Home');
    await expect(first).toBeFocused();
    await page.locator('#inner-tabs-tab-0').focus();
    await page.keyboard.press('ArrowRight');
    await expect(page.locator('#inner-tabs-tab-1')).toBeFocused();
    await expect(first).toHaveAttribute('aria-selected', 'true');
    await second.click();
    await first.click();
    await expect(page.locator('#inner-tabs-tab-1')).toHaveAttribute('aria-selected', 'true');
    await third.focus();
    await page.keyboard.press('Space');
    await expect(third).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('#outer-tabs-panel-2')).toBeVisible();
    await expect(page.locator('.accordion-content[aria-labelledby="named-accordion-item-0-header"]')).toHaveCount(1);
    await expect(page.locator('.accordion-content[aria-labelledby="named-accordion-item-1-header"]')).toHaveCount(1);
});

test('invalid keyboard tab targets do not change owned state', async ({page}) => {
    await fixture(page);
    await page.locator('#outer-tabs-tab-1').evaluate(element => element.setAttribute('aria-controls', 'inner-tabs-panel-1'));
    await page.locator('#outer-tabs-tab-0').focus();
    await page.keyboard.press('ArrowRight');
    await expect(page.locator('#outer-tabs-tab-2')).toBeFocused();
    await expect(page.locator('#inner-tabs-tab-0')).toHaveAttribute('aria-selected', 'true');
    await page.locator('#outer-tabs-tab-1').click();
    await expect(page.locator('#outer-tabs-tab-2')).toHaveAttribute('aria-selected', 'true');
});

test('mobile navigation closed state, focus, resize and retained desktop collapse at the 768px boundary', async ({page}, testInfo) => {
    await page.setViewportSize({width: 769, height: 900});
    await fixture(page);
    const nav = page.locator('#main-sidebar');
    const toggle = page.locator('.mobile-sidebar-toggle');
    await page.getByRole('button', {name: 'Toggle sidebar'}).click();
    await expect(nav).toHaveClass(/collapsed/);
    await page.getByRole('link', {name: 'Navigation one'}).focus();
    await page.setViewportSize({width: 768, height: 900});
    await expect(nav).toHaveAttribute('inert', '');
    await expect(nav).toHaveAttribute('aria-hidden', 'true');
    await expect(toggle).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await expect(nav).not.toHaveAttribute('inert', '');
    await expect(page.getByRole('link', {name: 'Navigation one'})).toBeFocused();
    await expect.poll(() => nav.evaluate(element => element.getBoundingClientRect().left)).toBe(0);
    const width = await nav.evaluate(element => element.getBoundingClientRect().width);
    expect(width).toBeGreaterThan(200);
    await page.keyboard.press('Escape');
    await expect(toggle).toBeFocused();
    await expect(nav).toHaveAttribute('inert', '');
    await page.keyboard.press('Tab');
    await expect(page.getByRole('textbox', {name: 'Name', exact: true})).toBeFocused();
    await toggle.click();
    await page.mouse.click(740, 100);
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await toggle.focus();
    await page.setViewportSize({width: 769, height: 900});
    await expect(nav).not.toHaveAttribute('inert', '');
    await expect(nav).not.toHaveAttribute('aria-hidden', 'true');
    await expect(nav).toHaveClass(/collapsed/);
    await expect(page.locator('.main-container')).toHaveClass(/sidebar-collapsed/);
    await expect(toggle).not.toBeFocused();
    await page.setViewportSize({width: 390, height: 844});
    await toggle.click();
    await expect.poll(() => nav.evaluate(element => element.getBoundingClientRect().left)).toBe(0);
    await evidence(page, testInfo, 'mobile-navigation-open');
});

test('repeated responsive visibility changes preserve navigation focus without stealing content focus', async ({page}) => {
    await page.setViewportSize({width: 769, height: 900});
    await fixture(page);
    const nav = page.locator('#main-sidebar');
    const toggle = page.locator('.mobile-sidebar-toggle');
    const link = nav.locator('a').first();
    for (let attempt = 0; attempt < 8; attempt++) {
        await link.focus();
        await expect(link).toBeFocused();
        await page.setViewportSize({width: 768, height: 900});
        await expect(toggle).toBeFocused();
        await expect(nav).toHaveAttribute('inert', '');
        await page.setViewportSize({width: 769, height: 900});
        await expect.poll(() => nav.evaluate(element => element.contains(document.activeElement))).toBe(true);
    }
    const input = page.getByRole('textbox', {name: 'Name', exact: true});
    await input.focus();
    await page.setViewportSize({width: 390, height: 844});
    await expect(nav).toHaveAttribute('inert', '');
    await expect(input).toBeFocused();
    await page.setViewportSize({width: 1440, height: 900});
    await expect(nav).not.toHaveAttribute('inert', '');
    await expect(input).toBeFocused();
});

test('mobile modal controls win actual hit testing and CSS owns base/stack layers', async ({page}, testInfo) => {
    await page.setViewportSize({width: 390, height: 844});
    await fixture(page);
    const toggle = page.locator('.mobile-sidebar-toggle');
    await toggle.focus();
    const toggleBox = await toggle.boundingBox();
    await page.locator('#modal-host').evaluate((host, html) => {host.innerHTML = html;},
        modal.replace('<div>Body content</div>', '<div style="height:900px">Tall body</div><button>Inside</button>'));
    const dialog = page.getByRole('dialog');
    await expect(dialog).toHaveCount(1);
    await expect(toggle).toHaveAttribute('inert', '');
    await expect(toggle).toBeHidden();
    const hit = await page.evaluate(box => document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2)
        .closest('.modal-backdrop') !== null, toggleBox);
    expect(hit).toBe(true);
    await page.keyboard.press('Shift+Tab'); // Reveal the header through the actual modal focus path.
    const close = dialog.locator('.modal-close');
    await expect(close).toBeFocused();
    await expect.poll(() => close.evaluate(element => {
        const box = element.getBoundingClientRect();
        const hit = document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2);
        return !!hit && hit.closest('.modal-close') === element;
    })).toBe(true);
    await page.evaluate(() => document.documentElement.style.setProperty('--sp-modal-layer', '2400'));
    await expect.poll(() => page.locator('.modal-backdrop').evaluate(element => getComputedStyle(element).zIndex)).toBe('2400');
    await page.locator('#modal-host').evaluate((host, html) => host.insertAdjacentHTML('beforeend', html),
        modal.replaceAll('test-modal', 'modal-upper'));
    await expect(page.getByRole('dialog')).toHaveCount(2);
    const layers = await page.locator('.modal-backdrop').evaluateAll(elements => elements.map(element =>
        ({inline: element.style.zIndex, layer: getComputedStyle(element).zIndex})));
    expect(layers).toEqual([{inline: '', layer: '2400'}, {inline: '', layer: '2401'}]);
    await page.keyboard.press('Escape');
    await page.keyboard.press('Escape');
    await expect(toggle).toBeFocused();
    await expect(toggle).not.toHaveAttribute('inert', '');
    await toggle.click();
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await page.keyboard.press('Escape');
    await page.locator('#modal-host').evaluate((host, html) => {host.innerHTML = html;}, modal);
    await evidence(page, testInfo, 'mobile-modal-hit-testing');
});

test('dynamic generated disclosure/tab/sidebar insertion stays delegated and idempotent', async ({page}) => {
    await fixture(page);
    const components = await page.locator('.dropdown, #outer-tabs').evaluateAll(elements => elements.map(element => {
        const html = element.outerHTML;
        element.remove();
        return html;
    }).join(''));
    for (let attempt = 0; attempt < 3; attempt++) {
        await page.locator('#modal-host').evaluate((host, html) => {host.innerHTML = html;}, components);
        const toggle = page.getByRole('button', {name: 'Actions', exact: true});
        await toggle.click();
        await expect(toggle).toHaveAttribute('aria-expanded', 'true');
        await toggle.click();
        await expect(toggle).toHaveAttribute('aria-expanded', 'false');
        await page.locator('#outer-tabs-tab-0').focus();
        await page.keyboard.press('ArrowRight');
        await expect(page.locator('#outer-tabs-tab-1')).toHaveAttribute('aria-selected', 'true');
        await page.locator('#outer-tabs-tab-0').click();
    }
    await page.setViewportSize({width: 390, height: 844});
    const shell = await page.locator('#main-sidebar').evaluate(element => {
        const html = element.outerHTML;
        element.remove();
        return html;
    });
    await page.locator('.main-container').evaluate((host, html) => host.insertAdjacentHTML('afterbegin', html), shell);
    await expect(page.locator('#main-sidebar')).toHaveAttribute('inert', '');
    await page.getByRole('button', {name: 'Toggle navigation'}).click();
    await expect(page.locator('#main-sidebar')).not.toHaveAttribute('inert', '');
});

test('generated Page native details reopens on desktop and remembers each mobile preference', async ({page}) => {
    await page.setViewportSize({width: 390, height: 844});
    await fixture(page);
    const details = page.locator('.sticky-sidebar-mobile-collapse');
    const summary = details.locator('summary');
    await summary.click();
    await expect(details).toHaveJSProperty('open', false);
    await expect(page.getByRole('link', {name: 'Details link'})).toHaveCount(0);
    await page.setViewportSize({width: 1440, height: 900});
    await expect.poll(() => details.evaluate(element => element.open)).toBe(true);
    await expect(page.getByRole('link', {name: 'Details link'})).toBeVisible();
    await page.setViewportSize({width: 390, height: 844});
    await expect.poll(() => details.evaluate(element => element.open)).toBe(false);
    await summary.click();
    await expect(details).toHaveJSProperty('open', true);
    await page.setViewportSize({width: 1440, height: 900});
    await expect.poll(() => details.evaluate(element => element.open)).toBe(true);
    await page.setViewportSize({width: 390, height: 844});
    await expect.poll(() => details.evaluate(element => element.open)).toBe(true);
});

for (const width of [1440, 390]) {
    test(`packaged Basics/Forms, Modules, Display/Data and shell at ${width}px`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height: width === 390 ? 844 : 900});
        await page.goto('/demos/basics-forms');
        await expect(page.getByRole('textbox', {name: 'Name', exact: true})).toHaveCount(1);
        await expect(page.getByRole('radiogroup', {name: 'Plan', exact: true})).toHaveCount(1);
        const menu = page.getByRole('button', {name: 'Open menu', exact: true});
        await menu.click();
        await expect(menu).toHaveAttribute('aria-expanded', 'true');
        await menu.focus();
        await page.keyboard.press('Tab');
        await expect(page.locator('.dropdown-menu a').first()).toBeFocused();
        await page.keyboard.press('Escape');
        await expect(menu).toHaveAttribute('aria-expanded', 'false');
        await page.goto('/demos/modules');
        const tab = page.locator('.tab-button').first();
        const tablist = page.locator('.tab-nav').first();
        await expect(tablist).toHaveCSS('flex-direction', width <= 768 ? 'column' : 'row');
        await expect(tablist).toHaveAttribute('aria-orientation', width <= 768 ? 'vertical' : 'horizontal');
        await tab.focus();
        await page.keyboard.press(width <= 768 ? 'ArrowDown' : 'ArrowRight');
        await expect(page.locator('.tab-button').nth(1)).toBeFocused();
        await expect(page.locator('.tab-button').nth(1)).toHaveAttribute('aria-selected', 'true');
        await page.keyboard.press('Home');
        await expect(tab).toBeFocused();
        await expect.poll(() => page.locator('.tab-panel.active:visible').evaluateAll(elements =>
            elements.every(element => getComputedStyle(element).opacity === '1'))).toBe(true);
        const tabsScreenshot = testInfo.outputPath(`packaged-tabs-${width}.png`);
        await page.locator('.tabs-container').first().screenshot({path: tabsScreenshot});
        await testInfo.attach('packaged-tabs', {path: tabsScreenshot, contentType: 'image/png'});
        const region = page.locator('.accordion-content').first();
        const headerId = await region.getAttribute('aria-labelledby');
        expect(headerId).toBeTruthy();
        await expect(page.locator(`[id="${headerId}"]`)).toHaveClass(/accordion-header/);
        await page.goto('/demos/display-data');
        await expect(page.getByRole('dialog')).toHaveCount(1);
        await page.keyboard.press('Escape'); // Display catalog intentionally renders a live modal sample.
        await expect(page.getByRole('dialog')).toHaveCount(0);
        const progress = page.getByRole('progressbar');
        await expect(progress).toHaveAccessibleName('Progress');
        await expect(progress).toHaveAttribute('aria-valuenow', '72');
        expect((await progress.boundingBox()).height).toBe(20);
        await progress.scrollIntoViewIfNeeded();
        await evidence(page, testInfo, `packaged-display-${width}`);
        if (width === 390) {
            await expect(page.locator('#main-sidebar')).toHaveAttribute('inert', '');
            await page.getByRole('button', {name: 'Toggle navigation'}).click();
            await expect(page.locator('#main-sidebar')).not.toHaveAttribute('inert', '');
            await page.keyboard.press('Escape');
            await expect(page.getByRole('button', {name: 'Toggle navigation'})).toBeFocused();
        }
    });
}
