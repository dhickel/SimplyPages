const fs = require('node:fs');
const path = require('node:path');
const {test, expect} = require('playwright/test');
const generated = fs.readFileSync(path.resolve(__dirname,
    '../../simplypages/src/test/resources/snapshots/integration/accessibility/labeled-progress-states.snap.html'), 'utf8');

// Measure the computed paints, including both stripe inks and every gradient interval.
// The default gradients interpolate in sRGB. Sampling also catches a bad interior stop;
// each interval is channel-monotonic, so its darkest/lightest endpoint bounds luminance.
async function paints(page) {
    return page.locator('.progress-bar').evaluateAll(elements => {
        const luminance = rgb => rgb.slice(0, 3).map(value => value / 255)
            .map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
            .reduce((sum, value, i) => sum + value * [.2126, .7152, .0722][i], 0);
        const contrast = (a, b) => (Math.max(luminance(a), luminance(b)) + .05)
            / (Math.min(luminance(a), luminance(b)) + .05);
        const colors = text => [...text.matchAll(/rgba?\(([^)]+)\)/g)]
            .map(match => match[1].split(',').map(Number))
            .map(color => [...color.slice(0, 3), color[3] ?? 1]);
        return elements.map(element => {
            const style = getComputedStyle(element);
            const gradients = style.backgroundImage.split(/, (?=linear-gradient)/);
            const base = colors(gradients.at(-1));
            const stripes = gradients.length === 2 ? colors(gradients[0]) : [[0, 0, 0, 0]];
            const foreground = colors(style.color)[0];
            let minimum = Infinity;
            for (let interval = 1; interval < base.length; interval++) {
                for (let step = 0; step <= 1024; step++) {
                    const background = base[interval - 1].map((value, channel) =>
                        value + (base[interval][channel] - value) * step / 1024);
                    for (const stripe of stripes) {
                        const paint = background.slice(0, 3).map((value, channel) =>
                            stripe[channel] * stripe[3] + value * (1 - stripe[3]));
                        minimum = Math.min(minimum, contrast(foreground, paint));
                    }
                }
            }
            return {label: element.textContent, classes: element.className, color: style.color,
                fontSize: style.fontSize, fontWeight: style.fontWeight, background: style.backgroundImage,
                gradients: gradients.length, base, stripes, foreground, minimum,
                animation: style.animationName, height: element.closest('.progress').getBoundingClientRect().height};
        });
    });
}

for (const width of [1440, 390]) {
    test(`generated default labeled progress paints meet 4.5 across states at ${width}px`, async ({page}, testInfo) => {
        await page.setViewportSize({width, height: width === 390 ? 844 : 900});
        await page.route('**/progress-contrast-fixture', route => route.fulfill({contentType: 'text/html', body:
            `<!doctype html><html><head><meta charset="UTF-8"><link rel="stylesheet" href="/css/framework.css"></head>
            <body><main style="max-width:720px;margin:auto">${generated}</main></body></html>`}));
        await page.goto('/progress-contrast-fixture');
        const measurements = [];
        for (const reducedMotion of ['no-preference', 'reduce']) {
            await page.emulateMedia({reducedMotion});
            const bars = await paints(page);
            expect(bars).toHaveLength(18);
            for (const bar of bars) {
                expect(bar.base).toHaveLength(2); // Do not silently accept an unparsed paint.
                expect(bar.base.every(color => color[3] === 1)).toBe(true);
                expect(bar.foreground[3]).toBe(1);
                expect(bar.gradients).toBe(bar.label.endsWith('plain') ? 1 : 2);
                expect(bar.minimum, `${bar.label}: ${bar.background} vs ${bar.color}`).toBeGreaterThanOrEqual(4.5);
                expect(bar.height).toBe(20);
                expect(bar.fontSize).toBe('13.6px');
                expect(bar.fontWeight).toBe('600');
                expect(bar.animation).toBe(bar.label.endsWith('animated') && reducedMotion !== 'reduce'
                    ? 'progress-stripes' : 'none');
            }
            measurements.push({reducedMotion, bars});
        }
        // Legacy aliases must use the same readable paints, not a stale parallel palette.
        await page.locator('.progress-bar').evaluateAll(elements => elements.forEach(element => {
            element.className = element.className.replaceAll('progress-bar-striped', 'progress-striped')
                .replaceAll('progress-bar-animated', 'progress-animated');
        }));
        const aliases = await paints(page);
        for (const [i, bar] of aliases.entries()) {
            expect(bar.minimum).toBe(measurements[1].bars[i].minimum);
            expect(bar.background).toBe(measurements[1].bars[i].background);
        }
        const output = testInfo.outputPath(`progress-paints-${width}.json`);
        fs.writeFileSync(output, JSON.stringify(measurements, null, 2));
        await testInfo.attach('computed-paints', {path: output, contentType: 'application/json'});
        const screenshot = testInfo.outputPath(`progress-paints-${width}.png`);
        await page.screenshot({path: screenshot, fullPage: true});
        await testInfo.attach('labeled-progress', {path: screenshot, contentType: 'image/png'});
    });
}
