package io.mindspice.demo.pages;

import io.mindspice.jhf.components.*;
import io.mindspice.jhf.components.display.*;
import io.mindspice.jhf.components.forms.*;
import io.mindspice.jhf.layout.*;
import org.springframework.stereotype.Component;

/**
 * Display components page - tables, cards, lists, and data display elements.
 */
@Component
public class DisplayPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Display Components"))
                .addRow(row -> row.withChild(Alert.info(
                        "Display components present data to users in various formats.")))

                // Labels
                .addComponents(Header.H2("Labels"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        **Label** components for form field labels:

                        ```java
                        Label.create("Username")
                            .withFor("username-input");
                        ```
                        """)))
                .addRow(row -> {
                    Div labelExample = new Div();
                    labelExample.withChild(Label.create("Email Address").forInput("email"));
                    labelExample.withChild(TextInput.email("email"));
                    row.withChild(labelExample);
                })

                .build();

        return page.render();
    }
}
