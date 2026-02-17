package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Shell builders demo - demonstrates the new shell builder system.
 *
 * <p>Shows how to use:</p>
 * <ul>
 *   <li>ShellBuilder - Complete application shell</li>
 *   <li>BannerBuilder - Flexible banners with multiple layout modes</li>
 *   <li>AccountBarBuilder - Secondary navigation with left/right alignment</li>
 *   <li>Collapsible sidebar functionality</li>
 * </ul>
 */
@Component
public class ShellDemoPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("Shell Builder System"))

                .addRow(row -> row.withChild(Alert.info(
                        "The Shell Builder system allows you to programmatically construct the entire " +
                        "application layout with full control over every aspect.")))

                // ShellBuilder Overview
                .addComponents(Header.H2("ShellBuilder - Complete Application Shell"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Build the entire application shell with top banner, account bar, side navigation,
                        and content area:

                        ```java
                        ShellBuilder.create()
                            .withPageTitle("My Application")
                            .withTopBanner(
                                BannerBuilder.create()
                                    .withLayout(BannerLayout.HORIZONTAL)
                                    .withTitle("App Name")
                                    .withSubtitle("Tagline")
                                    .withImage("/logo.png", "Logo")
                                    .build()
                            )
                            .withAccountBar(
                                AccountBarBuilder.create()
                                    .addLeftLink("Home", "/")
                                    .addRightLink("Login", "/login")
                                    .build()
                            )
                            .withSideNav(sideNav, true)  // true = collapsible
                            .withContentTarget("content-area")
                            .build();
                        ```

                        **Features:**
                        * Programmatic layout construction
                        * Optional components (can omit banner, account bar, or sidebar)
                        * Collapsible sidebar with localStorage persistence
                        * Full HTML document generation or body-only mode
                        * Custom CSS and HTMX integration
                        """)))

                // BannerBuilder (NEW!)
                .addComponents(Header.H2("BannerBuilder - Flexible Banner System (NEW!)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        `BannerBuilder` provides **5 flexible layout modes**:

                        ### Layout Modes

                        **1. HORIZONTAL** (default) - Image/logo on left, text on right:
                        ```java
                        BannerBuilder.create()
                            .withLayout(BannerLayout.HORIZONTAL)
                            .withImage("/logo.png", "Logo")
                            .withTitle("My Application")
                            .withSubtitle("Tagline here")
                            .build();
                        ```

                        **2. CENTERED** - Text centered horizontally:
                        ```java
                        BannerBuilder.create()
                            .withLayout(BannerLayout.CENTERED)
                            .withTitle("Welcome")
                            .withSubtitle("To our platform")
                            .build();
                        ```

                        **3. LEFT** - Text aligned to the left:
                        ```java
                        BannerBuilder.create()
                            .withLayout(BannerLayout.LEFT)
                            .withTitle("Research Portal")
                            .withSubtitle("Open source research platform")
                            .build();
                        ```

                        **4. RIGHT** - Text aligned to the right:
                        ```java
                        BannerBuilder.create()
                            .withLayout(BannerLayout.RIGHT)
                            .withTitle("Important Notice")
                            .withSubtitle("Updates and announcements")
                            .build();
                        ```

                        **5. IMAGE_OVERLAY** - Background image with text overlay:
                        ```java
                        BannerBuilder.create()
                            .withLayout(BannerLayout.IMAGE_OVERLAY)
                            .withBackgroundImage("/images/hero-bg.jpg")
                            .withTitle("Research Portal")
                            .withSubtitle("Advancing scientific research")
                            .withTextAlignment(TextAlignment.CENTER)  // LEFT, CENTER, or RIGHT
                            .withMinHeight(300)  // Optional height
                            .build();
                        ```

                        **Features:**
                        * Five layout modes for different use cases
                        * Background images with text overlay
                        * Flexible text alignment
                        * Custom colors (background, text)
                        * Responsive (adapts to mobile)
                        """)))

                // AccountBarBuilder with AccountWidget
                .addComponents(Header.H2("AccountBarBuilder with AccountWidget (NEW!)"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Build account/user bars with **left/right alignment** and **dynamic account widgets**:

                        ### Basic Account Bar
                        ```java
                        AccountBarBuilder.create()
                            // Left-aligned navigation
                            .addLeftLink("Home", "/")
                            .addLeftLink("Browse", "/browse")
                            .addLeftLink("About", "/about")

                            // Right-aligned account widget (dynamic HTMX)
                            .addRightAccountWidget("/api/account-status")
                            .build();
                        ```

                        ### Account Widget - Guest State
                        ```java
                        // Static guest widget (no HTMX)
                        AccountWidget.createGuest()
                            .withLoginUrl("/login")
                            .withSignupUrl("/signup")
                            .render();
                        ```

                        ### Account Widget - Authenticated State
                        ```java
                        // Static authenticated widget
                        AccountWidget.createAuthenticated("john_doe")
                            .withProfileUrl("/profile")
                            .withLogoutUrl("/logout")
                            .render();
                        ```

                        ### Account Widget - Dynamic (HTMX)
                        ```java
                        // Dynamic widget that loads via HTMX
                        AccountWidget.createDynamic("/api/account-status");

                        // Backend endpoint returns HTML based on auth status:
                        @GetMapping("/api/account-status")
                        public String accountStatus() {
                            if (isAuthenticated()) {
                                return AccountWidget.createAuthenticated(username).render();
                            } else {
                                return AccountWidget.createGuest().render();
                            }
                        }
                        ```

                        ### Complete Example
                        ```java
                        AccountBarBuilder.create()
                            .addLeftLink("Home", "/")
                            .addLeftLink("Research", "/research")
                            .addLeftItem(Badge.info("Beta"))
                            .addRightAccountWidget("/api/account-status")  // Dynamic!
                            .build();
                        ```

                        **Features:**
                        * Horizontal flexbox (hbox) layout
                        * Left section for navigation links
                        * Right section for account/auth widgets
                        * **HTMX dynamic loading** for account status
                        * Dropdown menu for authenticated users
                        * Responsive (stacks on mobile)
                        """)))

                // Collapsible Sidebar
                .addComponents(Header.H2("Collapsible Side Navigation"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The framework supports **collapsible sidebars** out of the box:

                        ```java
                        ShellBuilder.create()
                            .withSideNav(
                                SideNavBuilder.create()
                                    .addSection("Main")
                                    .addLink("Dashboard", "/dashboard")
                                    .addLink("Profile", "/profile")
                                    .build(),
                                true  // true = collapsible
                            )
                            .build();
                        ```

                        **Features:**
                        * **Desktop**: Click toggle button to collapse/expand
                        * **Saves state** in localStorage (persists across page loads)
                        * **Collapsed state** shows only icons
                        * **Mobile**: Off-canvas pattern (slides in from left)
                        * **Smooth animations** with CSS transitions

                        The sidebar is fully responsive and adapts to screen size automatically.
                        """)))

                // Practical Example
                .addComponents(Header.H2("Complete Example"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Here's a complete example building a research portal shell:

                        ```java
                        @GetMapping("/custom-shell")
                        @ResponseBody
                        public String customShell() {
                            return ShellBuilder.create()
                                .withPageTitle("Research Portal")
                                .withTopBanner(
                                    BannerBuilder.create()
                                        .withLayout(BannerLayout.HORIZONTAL)
                                        .withImage("/images/research-logo.png", "Logo")
                                        .withTitle("Research Portal")
                                        .withSubtitle("Open source research platform")
                                        .withBackgroundColor("#2c3e50")
                                        .withTextColor("#ffffff")
                                        .build()
                                )
                                .withAccountBar(
                                    AccountBarBuilder.create()
                                        .addLeftLink("Home", "/")
                                        .addLeftLink("Browse", "/browse")
                                        .addLeftLink("About", "/about")
                                        .addRightItem(
                                            new Div()
                                                .withAttribute("class", "flex items-center gap-3")
                                                .withChild(Badge.info("3 notifications"))
                                                .withChild(Link.create("/profile", "My Profile"))
                                                .withChild(Link.create("/logout", "Logout"))
                                        )
                                        .build()
                                )
                                .withSideNav(
                                    SideNavBuilder.create()
                                        .addSection("Research")
                                        .addLink("Products Database", "/products", "📦")
                                        .addLink("Studies", "/studies", "📚")
                                        .addLink("Data Analysis", "/data", "📊")
                                        .addSection("Community")
                                        .addLink("Forums", "/forums", "💬")
                                        .addLink("Data Journals", "/journals", "📓")
                                        .addSection("My Account")
                                        .addLink("Profile", "/profile", "👤")
                                        .addLink("Settings", "/settings", "⚙️")
                                        .build(),
                                    true  // collapsible
                                )
                                .withContentTarget("main-content")
                                .withCustomCss("/css/custom-theme.css")
                                .build();
                        }
                        ```

                        This generates a complete HTML document with all features enabled!
                        """)))

                // Alignment & Spacing
                .addComponents(Header.H2("Alignment & Spacing Utilities"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The framework includes comprehensive utility classes for customization:

                        **Flexbox Alignment:**
                        ```java
                        .withAttribute("class", "flex justify-between items-center")
                        .withAttribute("class", "flex justify-end items-start")
                        ```

                        **Spacing (Tailwind-style):**
                        ```java
                        // Margins
                        .withAttribute("class", "mt-4 mb-2 mx-auto")  // top, bottom, horizontal
                        .withAttribute("class", "ml-auto")            // push to right

                        // Padding
                        .withAttribute("class", "px-4 py-2")          // horizontal, vertical
                        ```

                        **Display & Width:**
                        ```java
                        .withAttribute("class", "d-flex w-100")       // flex, full width
                        .withAttribute("class", "d-md-none")          // hidden on mobile
                        ```

                        These utilities work with ALL components and layouts!
                        """)))

                // Key Takeaways
                .addComponents(Header.H2("Key Takeaways"))
                .addRow(row -> {
                    Row infoBoxRow = new Row()
                            .withComponents(
                                    InfoBox.create()
                                            .withIcon("🏗️")
                                            .withTitle("Programmatic")
                                            .withValue("Build in Java"),
                                    InfoBox.create()
                                            .withIcon("🎨")
                                            .withTitle("Flexible")
                                            .withValue("Full Control"),
                                    InfoBox.create()
                                            .withIcon("📱")
                                            .withTitle("Responsive")
                                            .withValue("Mobile-First"),
                                    InfoBox.create()
                                            .withIcon("⚡")
                                            .withTitle("Collapsible")
                                            .withValue("With State")
                            );

                    row.withChild(ContentModule.create()
                            .withTitle("Shell Builder Benefits")
                            .withCustomContent(infoBoxRow));
                })

                .addRow(row -> row.withChild(Alert.success(
                        "Everything is adjustable! Use utility classes for margins, padding, alignment, " +
                        "or add custom CSS classes. The framework provides structure but gives you " +
                        "complete control over styling and layout.")))

                .build();

        return page.render();
    }
}
