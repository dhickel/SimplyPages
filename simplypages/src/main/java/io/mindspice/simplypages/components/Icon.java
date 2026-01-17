package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Icon component wrapper for icon fonts and SVG icons.
 *
 * <p>Provides a consistent interface for using icons from various icon libraries
 * (Font Awesome, Material Icons, Bootstrap Icons, etc.).</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Font Awesome icon
 * Icon.fontAwesome("user");
 *
 * // Font Awesome with size
 * Icon.fontAwesome("check").large();
 *
 * // Material Icons
 * Icon.material("settings");
 *
 * // Bootstrap Icons
 * Icon.bootstrap("heart-fill");
 *
 * // Custom icon class
 * Icon.custom("my-icon-class");
 *
 * // Icon with aria label for accessibility
 * Icon.fontAwesome("info-circle").withAriaLabel("Information");
 * }</pre>
 */
public class Icon extends HtmlTag {

    public enum IconLibrary {
        FONT_AWESOME("fas fa-"),
        FONT_AWESOME_REGULAR("far fa-"),
        FONT_AWESOME_BRAND("fab fa-"),
        MATERIAL("material-icons"),
        BOOTSTRAP("bi bi-"),
        CUSTOM("");

        private final String prefix;

        IconLibrary(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public enum Size {
        SMALL("icon-sm"),
        MEDIUM(""),
        LARGE("icon-lg"),
        EXTRA_LARGE("icon-xl");

        private final String cssClass;

        Size(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private IconLibrary library;
    private String iconName;
    private Size size = Size.MEDIUM;

    private Icon(IconLibrary library, String iconName) {
        super(library == IconLibrary.MATERIAL ? "span" : "i");
        this.library = library;
        this.iconName = iconName;
        this.withAttribute("class", buildIconClass());
        this.withAttribute("aria-hidden", "true");
    }

    /**
     * Creates a Font Awesome solid icon.
     *
     * @param iconName the icon name (without "fa-" prefix)
     */
    public static Icon fontAwesome(String iconName) {
        return new Icon(IconLibrary.FONT_AWESOME, iconName);
    }

    /**
     * Creates a Font Awesome regular icon.
     *
     * @param iconName the icon name (without "fa-" prefix)
     */
    public static Icon fontAwesomeRegular(String iconName) {
        return new Icon(IconLibrary.FONT_AWESOME_REGULAR, iconName);
    }

    /**
     * Creates a Font Awesome brand icon.
     *
     * @param iconName the icon name (without "fa-" prefix)
     */
    public static Icon fontAwesomeBrand(String iconName) {
        return new Icon(IconLibrary.FONT_AWESOME_BRAND, iconName);
    }

    /**
     * Creates a Material Icon.
     *
     * @param iconName the material icon name
     */
    public static Icon material(String iconName) {
        Icon icon = new Icon(IconLibrary.MATERIAL, iconName);
        icon.withInnerText(iconName);
        return icon;
    }

    /**
     * Creates a Bootstrap Icon.
     *
     * @param iconName the icon name (without "bi-" prefix)
     */
    public static Icon bootstrap(String iconName) {
        return new Icon(IconLibrary.BOOTSTRAP, iconName);
    }

    /**
     * Creates a custom icon with custom CSS class.
     *
     * @param className the complete icon class name
     */
    public static Icon custom(String className) {
        Icon icon = new Icon(IconLibrary.CUSTOM, "");
        icon.withAttribute("class", className);
        return icon;
    }

    /**
     * Sets small size.
     */
    public Icon small() {
        this.size = Size.SMALL;
        updateClasses();
        return this;
    }

    /**
     * Sets large size.
     */
    public Icon large() {
        this.size = Size.LARGE;
        updateClasses();
        return this;
    }

    /**
     * Sets extra large size.
     */
    public Icon extraLarge() {
        this.size = Size.EXTRA_LARGE;
        updateClasses();
        return this;
    }

    /**
     * Adds an aria-label for accessibility.
     *
     * @param label the accessible label
     */
    public Icon withAriaLabel(String label) {
        this.withAttribute("aria-label", label);
        this.withAttribute("aria-hidden", "false");
        this.withAttribute("role", "img");
        return this;
    }

    private String buildIconClass() {
        String baseClass = "";
        if (library == IconLibrary.MATERIAL) {
            baseClass = library.getPrefix();
        } else if (library == IconLibrary.CUSTOM) {
            baseClass = iconName;
        } else {
            baseClass = library.getPrefix() + iconName;
        }

        if (!size.getCssClass().isEmpty()) {
            baseClass += " " + size.getCssClass();
        }

        return baseClass;
    }

    private void updateClasses() {
        this.withAttribute("class", buildIconClass());
    }
}
