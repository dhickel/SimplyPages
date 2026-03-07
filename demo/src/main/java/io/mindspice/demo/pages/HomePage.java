package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.layout.Page;
import org.springframework.stereotype.Component;

@Component
public class HomePage implements DemoPage {

    @Override
    public String render() {
        return Page.builder()
            .addComponents(Header.H1("SimplyPages"))
            .addComponents(new Paragraph("Home page placeholder for the upcoming main site refresh."))
            .build()
            .render();
    }
}
