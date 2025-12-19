package io.mindspice.jhf.modules;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Paragraph;
import io.mindspice.jhf.components.forms.Button;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Hero module for creating prominent banner/header sections.
 *
 * <p>Hero modules are large, attention-grabbing sections typically placed at the
 * top of a page. They can include a title, subtitle, description, call-to-action
 * buttons, and optional background image.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic hero
 * HeroModule.create()
 *     .withTitle("Welcome to Cannabis Research Portal")
 *     .withSubtitle("Open source research data and community")
 *     .withDescription("Join researchers worldwide in documenting cannabis science");
 *
 * // Hero with CTA buttons
 * HeroModule.create()
 *     .withTitle("Start Your Grow Journal")
 *     .withPrimaryButton("Get Started", "/register")
 *     .withSecondaryButton("Learn More", "/about");
 *
 * // Hero with background
 * HeroModule.create()
 *     .withTitle("Research Hub")
 *     .withBackgroundImage("/images/hero-bg.jpg")
 *     .centered();
 * }</pre>
 */
public class HeroModule extends Module {

    private String subtitle;
    private String description;
    private String backgroundImage;
    private String backgroundColor;
    private String primaryButtonText;
    private String primaryButtonUrl;
    private String secondaryButtonText;
    private String secondaryButtonUrl;
    private boolean centered = false;
    private Component customContent;

    public HeroModule() {
        super("div");
        this.withClass("hero-module");
    }

    public static HeroModule create() {
        return new HeroModule();
    }

    @Override
    public HeroModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public HeroModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Sets the subtitle text displayed below the title.
     */
    public HeroModule withSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Sets the description text displayed below the subtitle.
     */
    public HeroModule withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets a background image for the hero section.
     *
     * @param imageUrl URL or path to the background image
     */
    public HeroModule withBackgroundImage(String imageUrl) {
        this.backgroundImage = imageUrl;
        return this;
    }

    /**
     * Sets a background color for the hero section.
     *
     * @param color CSS color value
     */
    public HeroModule withBackgroundColor(String color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Adds a primary call-to-action button.
     *
     * @param text button text
     * @param url button link URL
     */
    public HeroModule withPrimaryButton(String text, String url) {
        this.primaryButtonText = text;
        this.primaryButtonUrl = url;
        return this;
    }

    /**
     * Adds a secondary call-to-action button.
     *
     * @param text button text
     * @param url button link URL
     */
    public HeroModule withSecondaryButton(String text, String url) {
        this.secondaryButtonText = text;
        this.secondaryButtonUrl = url;
        return this;
    }

    /**
     * Centers all content in the hero section.
     */
    public HeroModule centered() {
        this.centered = true;
        return this;
    }

    /**
     * Adds custom content to the hero section.
     *
     * @param content custom component to display
     */
    public HeroModule withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    @Override
    protected void buildContent() {
        // Apply background styling
        StringBuilder style = new StringBuilder();
        if (backgroundImage != null) {
            style.append("background-image: url('").append(backgroundImage).append("'); ")
                .append("background-size: cover; background-position: center;");
        }
        if (backgroundColor != null) {
            style.append("background-color: ").append(backgroundColor).append(";");
        }
        if (style.length() > 0) {
            super.withAttribute("style", style.toString());
        }

        // Create content wrapper
        Div contentWrapper = new Div().withClass("hero-content");
        if (centered) {
            contentWrapper.withClass("hero-content align-center");
        }

        // Add title
        if (title != null && !title.isEmpty()) {
            Header h1 = Header.H1(title).withClass("hero-title");
            if (centered) {
                h1.center();
            }
            contentWrapper.withChild(h1);
        }

        // Add subtitle
        if (subtitle != null && !subtitle.isEmpty()) {
            Header h2 = Header.H2(subtitle).withClass("hero-subtitle");
            if (centered) {
                h2.center();
            }
            contentWrapper.withChild(h2);
        }

        // Add description
        if (description != null && !description.isEmpty()) {
            Paragraph p = new Paragraph(description).withClass("hero-description");
            if (centered) {
                p.center();
            }
            contentWrapper.withChild(p);
        }

        // Add buttons
        if (primaryButtonText != null || secondaryButtonText != null) {
            Div buttonGroup = new Div().withClass("hero-buttons");

            if (primaryButtonText != null) {
                HtmlTag primaryBtn = new HtmlTag("a")
                    .withAttribute("href", primaryButtonUrl != null ? primaryButtonUrl : "#")
                    .withAttribute("class", "btn btn-primary")
                    .withInnerText(primaryButtonText);
                buttonGroup.withChild(primaryBtn);
            }

            if (secondaryButtonText != null) {
                HtmlTag secondaryBtn = new HtmlTag("a")
                    .withAttribute("href", secondaryButtonUrl != null ? secondaryButtonUrl : "#")
                    .withAttribute("class", "btn btn-secondary")
                    .withInnerText(secondaryButtonText);
                buttonGroup.withChild(secondaryBtn);
            }

            contentWrapper.withChild(buttonGroup);
        }

        // Add custom content
        if (customContent != null) {
            contentWrapper.withChild(customContent);
        }

        super.withChild(contentWrapper);
    }
}
