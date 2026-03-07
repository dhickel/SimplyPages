package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Builds top-banner components across supported layout presets.
 *
 * <p>Contract: {@link #build()} returns a new banner component containing either custom content
 * or generated title/subtitle/image content based on the selected layout.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Use per composition flow and do
 * not share across requests/threads.</p>
 */
public class BannerBuilder {

    /**
     * Layout mode for the banner.
     */
    public enum BannerLayout {
        /** Image/logo on left, text on right (horizontal layout) */
        HORIZONTAL,
        /** Text centered horizontally */
        CENTERED,
        /** Text aligned to the left */
        LEFT,
        /** Text aligned to the right */
        RIGHT,
        /** Background image with text overlay */
        IMAGE_OVERLAY
    }

    /**
     * Text alignment for overlay banners.
     */
    public enum TextAlignment {
        LEFT, CENTER, RIGHT
    }

    private BannerLayout layout = BannerLayout.HORIZONTAL;
    private TextAlignment textAlignment = TextAlignment.CENTER;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String imageAlt;
    private Integer imageWidth;
    private Integer imageHeight;
    private String backgroundImage;
    private String backgroundColor;
    private String textColor;
    private String customClass;
    private Component customContent;
    private Integer minHeight;

    private BannerBuilder() {}

    /**
     * Creates a new builder.
     */
    public static BannerBuilder create() {
        return new BannerBuilder();
    }

    /**
     * Set the banner layout mode.
     */
    public BannerBuilder withLayout(BannerLayout layout) {
        this.layout = layout;
        return this;
    }

    /**
     * Set text alignment (used with IMAGE_OVERLAY layout).
     */
    public BannerBuilder withTextAlignment(TextAlignment alignment) {
        this.textAlignment = alignment;
        return this;
    }

    /**
     * Set the banner title.
     */
    public BannerBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the banner subtitle.
     */
    public BannerBuilder withSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /**
     * Add an image/logo to the banner (used with HORIZONTAL layout).
     */
    public BannerBuilder withImage(String imageUrl, String alt) {
        this.imageUrl = imageUrl;
        this.imageAlt = alt;
        return this;
    }

    /**
     * Set image dimensions.
     */
    public BannerBuilder withImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        return this;
    }

    /**
     * Set background image URL (used with IMAGE_OVERLAY layout).
     */
    public BannerBuilder withBackgroundImage(String imageUrl) {
        this.backgroundImage = imageUrl;
        return this;
    }

    /**
     * Set banner background color.
     */
    public BannerBuilder withBackgroundColor(String color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Set text color.
     */
    public BannerBuilder withTextColor(String color) {
        this.textColor = color;
        return this;
    }

    /**
     * Set minimum height in pixels (useful for IMAGE_OVERLAY).
     */
    public BannerBuilder withMinHeight(int height) {
        this.minHeight = height;
        return this;
    }

    /**
     * Add custom CSS class.
     */
    public BannerBuilder withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Add custom content to the banner (overrides title/subtitle).
     */
    public BannerBuilder withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    /**
     * Builds a new banner component snapshot from current configuration.
     */
    public Component build() {
        HtmlTag banner = new HtmlTag("div");

        // Build class list
        StringBuilder classBuilder = new StringBuilder("banner");
        classBuilder.append(" banner-").append(layout.name().toLowerCase().replace('_', '-'));
        if (customClass != null) {
            classBuilder.append(" ").append(customClass);
        }
        banner.withAttribute("class", classBuilder.toString());

        // Build inline styles
        StringBuilder style = new StringBuilder();
        if (backgroundColor != null) {
            style.append("background-color: ").append(backgroundColor).append(";");
        }
        if (textColor != null) {
            style.append("color: ").append(textColor).append(";");
        }
        if (backgroundImage != null && layout == BannerLayout.IMAGE_OVERLAY) {
            style.append("background-image: url('").append(backgroundImage).append("');");
            style.append("background-size: cover;");
            style.append("background-position: center;");
        }
        if (minHeight != null) {
            style.append("min-height: ").append(minHeight).append("px;");
        }
        if (style.length() > 0) {
            banner.withAttribute("style", style.toString());
        }

        // Create content container
        HtmlTag content = new HtmlTag("div");
        String contentClass = "banner-content";

        // Add alignment class for overlay
        if (layout == BannerLayout.IMAGE_OVERLAY) {
            contentClass += " align-" + textAlignment.name().toLowerCase();
        }

        content.withAttribute("class", contentClass);

        // Build content based on layout mode
        if (customContent != null) {
            content.withChild(customContent);
        } else {
            switch (layout) {
                case HORIZONTAL:
                    buildHorizontalContent(content);
                    break;
                case CENTERED:
                case LEFT:
                case RIGHT:
                case IMAGE_OVERLAY:
                    buildTextContent(content);
                    break;
            }
        }

        banner.withChild(content);
        return banner;
    }

    /**
     * Build horizontal layout (image + text side by side).
     */
    private void buildHorizontalContent(HtmlTag container) {
        // Add image if provided
        if (imageUrl != null) {
            Image img = new Image(imageUrl, imageAlt != null ? imageAlt : "Logo");
            img.withAttribute("class", "banner-image");
            if (imageWidth != null && imageHeight != null) {
                img.withSize(imageWidth.toString(), imageHeight.toString());
            } else if (imageWidth != null) {
                img.withAttribute("width", imageWidth.toString());
            } else if (imageHeight != null) {
                img.withAttribute("height", imageHeight.toString());
            }
            container.withChild(img);
        }

        // Add text content
        if (title != null || subtitle != null) {
            HtmlTag textContainer = new HtmlTag("div")
                .withAttribute("class", "banner-text");

            if (title != null) {
                HtmlTag titleTag = new HtmlTag("h1")
                    .withAttribute("class", "banner-title")
                    .withInnerText(title);
                textContainer.withChild(titleTag);
            }

            if (subtitle != null) {
                HtmlTag subtitleTag = new HtmlTag("p")
                    .withAttribute("class", "banner-subtitle")
                    .withInnerText(subtitle);
                textContainer.withChild(subtitleTag);
            }

            container.withChild(textContainer);
        }
    }

    /**
     * Build text-only content (for centered, left, right, and overlay layouts).
     */
    private void buildTextContent(HtmlTag container) {
        HtmlTag textContainer = new HtmlTag("div")
            .withAttribute("class", "banner-text");

        if (title != null) {
            HtmlTag titleTag = new HtmlTag("h1")
                .withAttribute("class", "banner-title")
                .withInnerText(title);
            textContainer.withChild(titleTag);
        }

        if (subtitle != null) {
            HtmlTag subtitleTag = new HtmlTag("p")
                .withAttribute("class", "banner-subtitle")
                .withInnerText(subtitle);
            textContainer.withChild(subtitleTag);
        }

        container.withChild(textContainer);
    }
}
