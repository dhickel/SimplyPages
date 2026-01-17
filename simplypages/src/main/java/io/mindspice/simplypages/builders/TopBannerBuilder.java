package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Builder for creating top banners with optional image, title, subtitle, and custom styling.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TopBannerBuilder.create()
 *     .withTitle("Research Portal")
 *     .withSubtitle("Open source research platform")
 *     .withImage("/images/logo.png")
 *     .withBackgroundColor("#2c3e50")
 *     .build();
 * }</pre>
 */
public class TopBannerBuilder {

    private String title;
    private String subtitle;
    private String imageUrl;
    private String imageAlt;
    private Integer imageWidth;
    private Integer imageHeight;
    private String backgroundColor;
    private String textColor;
    private String customClass;
    private Component customContent;

    private TopBannerBuilder() {}

    public static TopBannerBuilder create() {
        return new TopBannerBuilder();
    }

    /**
     * Set the banner title.
     */
    public TopBannerBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the banner subtitle.
     */
    public TopBannerBuilder withSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Add an image/logo to the banner.
     */
    public TopBannerBuilder withImage(String imageUrl, String alt) {
        this.imageUrl = imageUrl;
        this.imageAlt = alt;
        return this;
    }

    /**
     * Set image dimensions.
     */
    public TopBannerBuilder withImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        return this;
    }

    /**
     * Set banner background color.
     */
    public TopBannerBuilder withBackgroundColor(String color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Set text color.
     */
    public TopBannerBuilder withTextColor(String color) {
        this.textColor = color;
        return this;
    }

    /**
     * Add custom CSS class.
     */
    public TopBannerBuilder withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Add custom content to the banner (overrides title/subtitle).
     */
    public TopBannerBuilder withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    /**
     * Build the top banner component.
     */
    public Component build() {
        HtmlTag banner = new HtmlTag("div");
        banner.withAttribute("class", "top-banner" + (customClass != null ? " " + customClass : ""));

        // Apply styles
        StringBuilder style = new StringBuilder();
        if (backgroundColor != null) {
            style.append("background-color: ").append(backgroundColor).append(";");
        }
        if (textColor != null) {
            style.append("color: ").append(textColor).append(";");
        }
        if (style.length() > 0) {
            banner.withAttribute("style", style.toString());
        }

        // Create content container
        HtmlTag content = new HtmlTag("div")
            .withAttribute("class", "top-banner-content");

        // Add custom content if provided
        if (customContent != null) {
            content.withChild(customContent);
        } else {
            // Add image if provided
            if (imageUrl != null) {
                Image img = new Image(imageUrl, imageAlt != null ? imageAlt : "Logo");
                img.withAttribute("class", "top-banner-image");
                if (imageWidth != null && imageHeight != null) {
                    img.withSize(imageWidth.toString(), imageHeight.toString());
                } else if (imageWidth != null) {
                    img.withAttribute("width", imageWidth.toString());
                } else if (imageHeight != null) {
                    img.withAttribute("height", imageHeight.toString());
                }
                content.withChild(img);
            }

            // Add text content
            if (title != null || subtitle != null) {
                HtmlTag textContainer = new HtmlTag("div")
                    .withAttribute("class", "top-banner-text");

                if (title != null) {
                    HtmlTag titleTag = new HtmlTag("h1")
                        .withAttribute("class", "top-banner-title")
                        .withInnerText(title);
                    textContainer.withChild(titleTag);
                }

                if (subtitle != null) {
                    HtmlTag subtitleTag = new HtmlTag("p")
                        .withAttribute("class", "top-banner-subtitle")
                        .withInnerText(subtitle);
                    textContainer.withChild(subtitleTag);
                }

                content.withChild(textContainer);
            }
        }

        banner.withChild(content);
        return banner;
    }
}
