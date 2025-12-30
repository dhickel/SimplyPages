package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.layout.*;
import org.springframework.stereotype.Component;

/**
 * HTMX patterns page - demonstrates HTMX integration.
 */
@Component
public class HtmxPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("HTMX Integration Patterns"))

                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## HTMX Philosophy in JHF

                        JHF follows a **server-first** approach with **minimal JavaScript**. HTMX is used
                        sparingly for dynamic updates without full page reloads.

                        ### When to Use HTMX

                        ✅ **USE for:**
                        * Module refreshing
                        * Lazy loading content
                        * Form submissions without page reload
                        * User interactions (editing, voting)
                        * Dynamic content updates

                        ❌ **DON'T USE for:**
                        * Initial page rendering (use SSR)
                        * Simple navigation (use standard links)
                        * Static content display

                        ## HTMX Attributes

                        All JHF components support HTMX attributes via `withAttribute()`:

                        ```java
                        component
                            .withAttribute("hx-get", "/api/endpoint")
                            .withAttribute("hx-target", "#content")
                            .withAttribute("hx-swap", "innerHTML");
                        ```

                        ### Common HTMX Attributes

                        * `hx-get` / `hx-post` - HTTP method and URL
                        * `hx-target` - Element to update (CSS selector)
                        * `hx-swap` - How to swap content (innerHTML, outerHTML, beforeend, etc.)
                        * `hx-trigger` - When to trigger (click, load, every Ns, revealed, etc.)
                        * `hx-push-url` - Update browser URL (true/false)
                        """)))

                // Pattern 1: Load More
                .addComponents(Header.H2("Pattern: Load More Button"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Load additional content without page refresh:

                        ```java
                        Button.create("Load More")
                            .withAttribute("hx-get", "/api/more-items")
                            .withAttribute("hx-target", "#items-list")
                            .withAttribute("hx-swap", "beforeend");  // Append
                        ```

                        Backend endpoint:
                        ```java
                        @GetMapping("/api/more-items")
                        @ResponseBody
                        public String moreItems() {
                            // Return just the new items HTML, not full page
                            return itemsList.render();
                        }
                        ```
                        """)))

                // Pattern 2: Auto-refresh
                .addComponents(Header.H2("Pattern: Auto-Refresh Module"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Automatically refresh content on an interval:

                        ```java
                        DataModule<Stats> liveStats = DataModule.create(Stats.class)
                            .withModuleId("live-stats")
                            .withAttribute("hx-get", "/api/stats")
                            .withAttribute("hx-trigger", "every 30s")  // Every 30 seconds
                            .withAttribute("hx-swap", "outerHTML");
                        ```

                        The entire module is replaced every 30 seconds with fresh data.
                        """)))

                // Pattern 3: Lazy Loading
                .addComponents(Header.H2("Pattern: Lazy Loading"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Load content when scrolled into view:

                        ```java
                        Div placeholder = new Div()
                            .withAttribute("id", "lazy-content")
                            .withAttribute("hx-get", "/api/heavy-data")
                            .withAttribute("hx-trigger", "revealed")  // Load when visible
                            .withAttribute("hx-swap", "outerHTML")
                            .withChild(new Paragraph().withInnerText("Loading..."));
                        ```

                        Perfect for expensive content below the fold.
                        """)))

                // Pattern 4: Form Submission
                .addComponents(Header.H2("Pattern: Form Submission"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Submit forms without page reload:

                        ```java
                        Form.create()
                            .withAttribute("hx-post", "/api/save")
                            .withAttribute("hx-target", "#result")
                            .withAttribute("hx-swap", "innerHTML")
                            .addField("Name", TextInput.create("name"))
                            .addField("", Button.submit("Save"));
                        ```

                        Backend:
                        ```java
                        @PostMapping("/api/save")
                        @ResponseBody
                        public String saveData(@RequestParam String name) {
                            // Process data
                            return Alert.success("Saved!").render();
                        }
                        ```
                        """)))

                // Pattern 5: Inline Editing
                .addComponents(Header.H2("Pattern: Inline Editing"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Edit content in-place:

                        ```java
                        // Display mode
                        Div displayMode = new Div()
                            .withAttribute("id", "editable-content")
                            .withAttribute("hx-get", "/api/edit-form")
                            .withAttribute("hx-target", "#editable-content")
                            .withAttribute("hx-swap", "outerHTML")
                            .withChild(new Paragraph().withInnerText("Click to edit"))
                            .withChild(Button.create("Edit"));

                        // Edit mode endpoint returns form
                        @GetMapping("/api/edit-form")
                        @ResponseBody
                        public String editForm() {
                            return Form.create()
                                .withAttribute("hx-post", "/api/save")
                                .withAttribute("hx-target", "#editable-content")
                                .addField("", TextArea.create("content"))
                                .addField("", Button.submit("Save"))
                                .render();
                        }
                        ```
                        """)))

                // Pattern 6: Search/Filter
                .addComponents(Header.H2("Pattern: Live Search"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        Search as you type:

                        ```java
                        TextInput.create("search")
                            .withPlaceholder("Search strains...")
                            .withAttribute("hx-get", "/api/search")
                            .withAttribute("hx-trigger", "keyup changed delay:500ms")
                            .withAttribute("hx-target", "#search-results")
                            .withAttribute("hx-include", "[name='search']");
                        ```

                        Waits 500ms after typing stops before searching.
                        """)))

                // Security
                .addComponents(Header.H2("HTMX Security"))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ### Verify HTMX Requests

                        ```java
                        @GetMapping("/api/data")
                        @ResponseBody
                        public String getData(
                                @RequestHeader(value = "HX-Request", required = false)
                                String hxRequest) {

                            if (!"true".equals(hxRequest)) {
                                throw new ForbiddenException("Invalid request");
                            }

                            return dataModule.render();
                        }
                        ```

                        ### Authorization

                        ```java
                        @GetMapping("/api/admin/data")
                        @PreAuthorize("hasRole('ADMIN')")
                        @ResponseBody
                        public String getAdminData() {
                            // Only admins can access
                            return adminData.render();
                        }
                        ```

                        ### CSRF Protection

                        Spring Security automatically includes CSRF tokens in HTMX requests
                        when using Thymeleaf forms.
                        """)))

                .build();

        return page.render();
    }
}
