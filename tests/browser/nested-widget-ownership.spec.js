const path = require('node:path');
const {test, expect} = require('playwright/test');

const repositoryRoot = path.resolve(__dirname, '..', '..');
const frameworkCss = path.join(
    repositoryRoot,
    'simplypages',
    'src',
    'main',
    'resources',
    'static',
    'css',
    'framework.css'
);
const frameworkJs = path.join(
    repositoryRoot,
    'simplypages',
    'src',
    'main',
    'resources',
    'static',
    'js',
    'framework.js'
);

const widgetMarkup = `
<div id="fixture">
  <div class="tabs-container" id="outer-tabs">
    <ul class="tab-nav" role="tablist">
      <li class="tab-item"><button class="tab-button active" type="button" aria-selected="true" aria-controls="outer-panel-one">Outer one<span> child</span></button></li>
      <li class="tab-item"><button class="tab-button" type="button" aria-selected="false" aria-controls="outer-panel-two">Outer two<span> child</span></button></li>
      <li class="tab-item"><button class="tab-button" type="button" aria-selected="false" aria-controls="sibling-panel-one">Cross target<span> child</span></button></li>
      <li class="tab-item"><button class="tab-button" type="button" aria-selected="false">Missing target<span> child</span></button></li>
    </ul>
    <div class="tab-panels">
      <div class="tab-panel active" id="outer-panel-one" role="tabpanel">
        <div class="tabs-container" id="inner-tabs">
          <ul class="tab-nav" role="tablist">
            <li class="tab-item"><button class="tab-button active" type="button" aria-selected="true" aria-controls="inner-panel-one">Inner one<span> child</span></button></li>
            <li class="tab-item"><button class="tab-button" type="button" aria-selected="false" aria-controls="inner-panel-two">Inner two<span> child</span></button></li>
          </ul>
          <div class="tab-panels">
            <div class="tab-panel active" id="inner-panel-one" role="tabpanel">Inner one content</div>
            <div class="tab-panel" id="inner-panel-two" role="tabpanel">Inner two content</div>
          </div>
        </div>
      </div>
      <div class="tab-panel" id="outer-panel-two" role="tabpanel">Outer two content</div>
    </div>
  </div>

  <div class="tabs-container" id="sibling-tabs">
    <ul class="tab-nav" role="tablist">
      <li class="tab-item"><button class="tab-button active" type="button" aria-selected="true" aria-controls="sibling-panel-one">Sibling one</button></li>
      <li class="tab-item"><button class="tab-button" type="button" aria-selected="false" aria-controls="sibling-panel-two">Sibling two</button></li>
    </ul>
    <div class="tab-panels">
      <div class="tab-panel active" id="sibling-panel-one" role="tabpanel">Sibling one content</div>
      <div class="tab-panel" id="sibling-panel-two" role="tabpanel">Sibling two content</div>
    </div>
  </div>

  <div class="accordion" id="outer-accordion" data-single-expand="true">
    <div class="accordion-item">
      <button class="accordion-header active" type="button" aria-expanded="true" aria-controls="outer-accordion-one">Outer accordion one<span> child</span></button>
      <div class="accordion-content expanded" id="outer-accordion-one">
        <div class="accordion" id="inner-accordion">
          <div class="accordion-item">
            <button class="accordion-header active" type="button" aria-expanded="true" aria-controls="inner-accordion-one">Inner accordion one<span> child</span></button>
            <div class="accordion-content expanded" id="inner-accordion-one">Inner one content</div>
          </div>
          <div class="accordion-item">
            <button class="accordion-header" type="button" aria-expanded="false" aria-controls="inner-accordion-two">Inner accordion two<span> child</span></button>
            <div class="accordion-content" id="inner-accordion-two">Inner two content</div>
          </div>
        </div>
      </div>
    </div>
    <div class="accordion-item">
      <button class="accordion-header" type="button" aria-expanded="false" aria-controls="outer-accordion-two">Outer accordion two<span> child</span></button>
      <div class="accordion-content" id="outer-accordion-two">Outer two content</div>
    </div>
  </div>

  <div class="accordion" id="sibling-accordion" data-single-expand="true">
    <div class="accordion-item">
      <button class="accordion-header" type="button" aria-expanded="false" aria-controls="sibling-accordion-one">Sibling accordion<span> child</span></button>
      <div class="accordion-content" id="sibling-accordion-one">Sibling content</div>
    </div>
  </div>

  <div class="accordion" id="invalid-accordion" data-single-expand="true">
    <div class="accordion-item">
      <button class="accordion-header active" type="button" aria-expanded="true" aria-controls="invalid-accordion-one">Valid accordion</button>
      <div class="accordion-content expanded" id="invalid-accordion-one">Valid content</div>
    </div>
    <div class="accordion-item">
      <button class="accordion-header" type="button" aria-expanded="false" aria-controls="outer-accordion-two">Cross target</button>
      <div class="accordion-content" id="invalid-accordion-two">Should stay unchanged</div>
    </div>
    <div class="accordion-item">
      <button class="accordion-header" type="button" aria-expanded="false" aria-controls="missing-accordion-content">Missing target</button>
      <div class="accordion-content" id="invalid-accordion-three">Should stay unchanged</div>
    </div>
  </div>

  <div id="dynamic-host"></div>
</div>
`;

test.beforeEach(async ({page}) => {
    await page.setContent(widgetMarkup);
    await page.addStyleTag({path: frameworkCss});
    await page.addScriptTag({path: frameworkJs});
});

test('keeps nested tab state local and rejects invalid targets before mutation', async ({page}) => {
    await page.locator('#inner-tabs .tab-button[aria-controls="inner-panel-two"] span').click();
    await expect(page.locator('#inner-panel-two')).toHaveCSS('display', 'block');

    await page.locator('#outer-tabs .tab-button[aria-controls="outer-panel-two"] span').click();
    await page.locator('#outer-tabs .tab-button[aria-controls="outer-panel-one"] span').click();

    await expect(page.locator('#inner-tabs .tab-button[aria-controls="inner-panel-two"]')).toHaveClass(/active/);
    await expect(page.locator('#inner-panel-two')).toHaveClass(/active/);
    await expect(page.locator('#inner-panel-one')).not.toHaveClass(/active/);

    await page.locator('#sibling-tabs .tab-button[aria-controls="sibling-panel-two"]').click();
    await page.locator('#outer-tabs .tab-button').nth(2).locator('span').click();
    await page.locator('#outer-tabs .tab-button').nth(3).locator('span').click();

    await expect(page.locator('#outer-tabs .tab-button[aria-controls="outer-panel-one"]')).toHaveClass(/active/);
    await expect(page.locator('#outer-tabs .tab-button[aria-controls="outer-panel-two"]')).not.toHaveClass(/active/);
    await expect(page.locator('#sibling-tabs .tab-button[aria-controls="sibling-panel-two"]')).toHaveClass(/active/);
    await expect(page.locator('#sibling-tabs .tab-button[aria-controls="sibling-panel-one"]')).not.toHaveClass(/active/);
});

test('keeps nested accordion expansion local for single and multiple modes', async ({page}) => {
    await page.locator('#inner-accordion .accordion-header[aria-controls="inner-accordion-two"] span').click();
    await expect(page.locator('#inner-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-two')).toHaveClass(/expanded/);

    await page.locator('#outer-accordion .accordion-header[aria-controls="outer-accordion-two"] span').click();
    await page.locator('#outer-accordion .accordion-header[aria-controls="outer-accordion-one"] span').click();

    await expect(page.locator('#outer-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#outer-accordion-two')).not.toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-two')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion .accordion-header[aria-controls="inner-accordion-one"]'))
        .toHaveAttribute('aria-expanded', 'true');
    await expect(page.locator('#inner-accordion .accordion-header[aria-controls="inner-accordion-two"]'))
        .toHaveAttribute('aria-expanded', 'true');

    await page.locator('#outer-accordion .accordion-header[aria-controls="outer-accordion-one"] span').click();
    await expect(page.locator('#outer-accordion-one')).not.toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-two')).toHaveClass(/expanded/);

    await page.locator('#outer-accordion .accordion-header[aria-controls="outer-accordion-one"] span').click();
    await expect(page.locator('#outer-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-two')).toHaveClass(/expanded/);

    await page.locator('#sibling-accordion .accordion-header span').click();
    await expect(page.locator('#sibling-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#inner-accordion-two')).toHaveClass(/expanded/);

    await page.locator('#invalid-accordion .accordion-header').nth(1).click();
    await page.locator('#invalid-accordion .accordion-header').nth(2).click();
    await expect(page.locator('#invalid-accordion-one')).toHaveClass(/expanded/);
    await expect(page.locator('#invalid-accordion-two')).not.toHaveClass(/expanded/);
    await expect(page.locator('#invalid-accordion-three')).not.toHaveClass(/expanded/);
});

test('delegates events for dynamically inserted nested widgets without duplicate handlers', async ({page}) => {
    await page.locator('#dynamic-host').evaluate(host => {
        host.innerHTML = `
          <div class="tabs-container" id="dynamic-tabs">
            <ul class="tab-nav" role="tablist">
              <li><button class="tab-button active" type="button" aria-selected="true" aria-controls="dynamic-panel-one">Dynamic one</button></li>
              <li><button class="tab-button" type="button" aria-selected="false" aria-controls="dynamic-panel-two">Dynamic two<span> child</span></button></li>
            </ul>
            <div class="tab-panels">
              <div class="tab-panel active" id="dynamic-panel-one" role="tabpanel">
                <div class="accordion" id="dynamic-accordion" data-single-expand="true">
                  <div class="accordion-item">
                    <button class="accordion-header" type="button" aria-expanded="false" aria-controls="dynamic-accordion-one">Dynamic accordion<span> child</span></button>
                    <div class="accordion-content" id="dynamic-accordion-one">Dynamic content</div>
                  </div>
                </div>
              </div>
              <div class="tab-panel" id="dynamic-panel-two" role="tabpanel">Dynamic second</div>
            </div>
          </div>
        `;
    });

    await page.locator('#dynamic-tabs .tab-button[aria-controls="dynamic-panel-two"] span').click();
    await expect(page.locator('#dynamic-panel-two')).toHaveCSS('display', 'block');
    await expect(page.locator('#dynamic-panel-one')).not.toHaveClass(/active/);

    await page.locator('#dynamic-tabs .tab-button[aria-controls="dynamic-panel-one"]').click();
    await page.locator('#dynamic-accordion .accordion-header span').click();
    await expect(page.locator('#dynamic-accordion-one')).toHaveClass(/expanded/);

    await page.locator('#dynamic-accordion .accordion-header span').click();
    await expect(page.locator('#dynamic-accordion-one')).not.toHaveClass(/expanded/);
});
