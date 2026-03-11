package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.HeroModule;
import org.springframework.stereotype.Component;

@Component
public class HomePage implements DemoPage {

    @Override
    public String render() {
        HeroModule hero = HeroModule.create()
            .withTitle("SimplyPages")
            .withSubtitle("Java-first server-rendered UI composition")
            .withDescription(
                "Build maintainable SSR applications with composable modules, pragmatic HTMX integration, " +
                    "and framework defaults that stay easy to customize."
            )
            .withPrimaryButton("Explore Demos", "/demos")
            .withSecondaryButton("Read Docs", "/docs")
            .centered();

        Div navShell = new Div()
            .withClass("home-landing-nav-shell")
            .withChild(Header.H2("Jump Into The Demo Surface").withClass("home-landing-nav-title"))
            .withChild(new Paragraph(
                "Quick links to the primary demo destinations from the global top navigation."
            ).withClass("home-landing-nav-subtitle"));

        Div navGrid = new Div().withClass("home-landing-nav-grid")
            .withChild(navCard("Overview", "Home", "Landing overview and framework entry point.", "/home"))
            .withChild(navCard("Examples", "Demos", "Component and module demos organized by surface area.", "/demos"))
            .withChild(navCard("API", "Javadocs", "Generated API docs for the framework module.", "/javadocs-view"))
            .withChild(navCard("Community", "Forum", "Forum helper rendering and interaction demo flow.", "/forum"))
            .withChild(navCard("Guides", "Docs", "Fundamentals, core guides, and reference docs.", "/docs"));

        navShell.withChild(navGrid);

        return Page.builder()
            .addComponents(hero)
            .addComponents(navShell)
            .build()
            .render();
    }

    private Link navCard(String badge, String title, String body, String href) {
        Link card = Link.create(href, "").withClass("home-landing-nav-card");
        card.withChild(new HtmlTag("span")
            .withAttribute("class", "home-landing-nav-card-badge")
            .withInnerText(badge));
        card.withChild(new HtmlTag("span")
            .withAttribute("class", "home-landing-nav-card-title")
            .withInnerText(title));
        card.withChild(new HtmlTag("span")
            .withAttribute("class", "home-landing-nav-card-body")
            .withInnerText(body));
        card.withChild(new HtmlTag("span")
            .withAttribute("class", "home-landing-nav-card-cta")
            .withInnerText("Open ->"));
        return card;
    }
}
