package io.mindspice.demo.pages;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.components.RawHtml;

/**
 * Page for rendering Javadocs in an iframe.
 */
@org.springframework.stereotype.Component
public class JavadocsPage implements DemoPage {

    @Override
    public String render() {
        return Page.builder()
                .addComponents(
                    new RawHtml("""
                        <div style="height: calc(100vh - 200px); width: 100%;">
                            <iframe src="/javadocs/index.html" 
                                    style="width: 100%; height: 100%; border: none; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                            </iframe>
                        </div>
                    """)
                )
                .build()
                .render();
    }
}
