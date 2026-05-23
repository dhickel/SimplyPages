package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpinnerTest {

    @Test
    @DisplayName("Spinner should render size, color, and message")
    void testSpinnerRendering() {
        Spinner spinner = Spinner.create()
            .large()
            .withColor("primary")
            .withMessage("Loading");

        String html = spinner.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.spinner-wrapper")
            .hasElement("div.spinner.spinner-lg.spinner-primary[role=\"status\"][aria-label=\"Loading\"]")
            .elementTextEquals("div.spinner-message", "Loading");
    }

    @Test
    @DisplayName("Spinner message should render in context, nested, and template render paths")
    void testSpinnerContextAndTemplateRendering() {
        Spinner spinner = Spinner.create().large().withMessage("Loading");

        String contextHtml = spinner.render(RenderContext.empty());
        String nestedHtml = new Div().withChild(spinner).render(RenderContext.empty());
        String templateHtml = Template.of(new Div().withChild(spinner)).render(RenderContext.empty());

        HtmlAssert.assertThat(contextHtml)
            .hasElement("div.spinner-wrapper")
            .elementTextEquals("div.spinner-message", "Loading");

        HtmlAssert.assertThat(nestedHtml)
            .hasElement("div > div.spinner-wrapper")
            .elementTextEquals("div.spinner-message", "Loading");

        HtmlAssert.assertThat(templateHtml)
            .hasElement("div > div.spinner-wrapper")
            .elementTextEquals("div.spinner-message", "Loading");
    }
}
