package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.*;
import org.springframework.stereotype.Component;

/**
 * Demo page showcasing newly added modules.
 *
 * <p>Demonstrates:</p>
 * <ul>
 *   <li>HeroModule - Banner sections with CTAs</li>
 *   <li>StatsModule - Statistics display</li>
 *   <li>TimelineModule - Chronological events</li>
 *   <li>AccordionModule - Collapsible sections</li>
 *   <li>TabsModule - Tabbed content</li>
 *   <li>QuoteModule - Testimonials and quotes</li>
 *   <li>CalloutModule - Highlighted info boxes</li>
 *   <li>ComparisonModule - Side-by-side comparisons</li>
 * </ul>
 */
@Component
public class NewModulesPage implements DemoPage {

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1("New Modules").center())

                // Introduction
                .addRow(row -> row.withChild(new Markdown(
                        """
                        # Recently Added Modules

                        This page showcases all the high-level modules that were recently added to the framework.
                        These modules combine multiple components into complete, reusable page sections.

                        The framework now has **13 built-in modules**, up from the original 5, bringing it to
                        feature parity with major web frameworks like Bootstrap and Material UI.
                        """
                )))

                .addComponents(Spacer.vertical().large())

                // HeroModule
                .addRow(row -> row.withChild(Header.H2("Hero Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **HeroModule** creates prominent banner sections, typically used at the top of pages.

                        **Features:**
                        - Title, subtitle, and description
                        - Background images and colors
                        - Primary and secondary CTA buttons
                        - Centered or left-aligned content
                        - Custom content support
                        """
                )))
                .addRow(row -> {
                    HeroModule hero = HeroModule.create()
                            .withTitle("Welcome to Cannabis Research Portal")
                            .withSubtitle("Open Source • Community-Driven • Scientific")
                            .withDescription("Join researchers worldwide in documenting cannabis science and cultivation data.")
                            .withPrimaryButton("Get Started", "/register")
                            .withSecondaryButton("Learn More", "/about")
                            .centered();
                    row.withChild(hero);
                })

                .addComponents(Spacer.vertical().large())

                // StatsModule
                .addRow(row -> row.withChild(Header.H2("Stats Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **StatsModule** displays statistics and metrics in a grid layout.

                        **Features:**
                        - Configurable grid columns (1-6)
                        - Value, label, and optional description
                        - Perfect for showcasing key numbers
                        """
                )))
                .addRow(row -> {
                    StatsModule stats = StatsModule.create()
                            .withTitle("Platform Statistics")
                            .withColumns(4)
                            .addStat("1,234", "Research Papers", "Peer-reviewed studies")
                            .addStat("567", "Active Researchers", "Contributing members")
                            .addStat("89", "Strains Cataloged", "Documented varieties")
                            .addStat("95%", "Data Accuracy", "Verification rate");
                    row.withChild(stats);
                })

                .addComponents(Spacer.vertical().large())

                // TimelineModule
                .addRow(row -> row.withChild(Header.H2("Timeline Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **TimelineModule** displays events in chronological order.

                        **Features:**
                        - Vertical or horizontal orientation
                        - Date markers with event content
                        - Perfect for grow journals, project history, or research progress
                        - Supports custom component content
                        """
                )))
                .addRow(row -> {
                    TimelineModule timeline = TimelineModule.create()
                            .withTitle("Grow Journal Timeline")
                            .vertical()
                            .addEvent("Day 1", "Germination", "Seeds placed in wet paper towel. Temperature maintained at 70°F.")
                            .addEvent("Day 7", "Seedling", "First true leaves appeared. Moved to small pots with soil.")
                            .addEvent("Day 21", "Vegetative Stage", "Switched to 18/6 light cycle. Plants showing strong growth.")
                            .addEvent("Day 45", "Flowering Initiated", "Changed to 12/12 light cycle. Added flowering nutrients.");
                    row.withChild(timeline);
                })

                .addComponents(Spacer.vertical().large())

                // AccordionModule
                .addRow(row -> row.withChild(Header.H2("Accordion Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **AccordionModule** creates collapsible content sections.

                        **Features:**
                        - Expandable/collapsible items
                        - Single or multiple items can be open
                        - First item can be expanded by default
                        - Perfect for FAQs, documentation, help sections
                        """
                )))
                .addRow(row -> {
                    AccordionModule accordion = AccordionModule.create()
                            .withTitle("Frequently Asked Questions")
                            .addItem("What is the Java HTML Framework?",
                                    "JHF is a server-side rendering framework that lets you build web UIs " +
                                    "entirely in Java. No JavaScript build process required.")
                            .addItem("How does it compare to JSP or Thymeleaf?",
                                    "Unlike template engines, JHF uses fluent Java APIs for type-safe UI " +
                                    "construction. You get compile-time checking and IDE autocomplete.")
                            .addItem("Can I use it with Spring Boot?",
                                    "Absolutely! JHF is designed to work seamlessly with Spring Boot. " +
                                    "This entire demo is a Spring Boot application.")
                            .addItem("Does it support HTMX?",
                                    "Yes! JHF has first-class HTMX support with helper methods for " +
                                    "all HTMX attributes.")
                            .withFirstExpanded();
                    row.withChild(accordion);
                })

                .addComponents(Spacer.vertical().large())

                // TabsModule
                .addRow(row -> row.withChild(Header.H2("Tabs Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **TabsModule** organizes content into tabbed panels.

                        **Features:**
                        - Multiple content panels with tab navigation
                        - Active tab selection
                        - ARIA accessibility support
                        - Supports both text and custom component content
                        """
                )))
                .addRow(row -> {
                    TabsModule tabs = TabsModule.create()
                            .withTitle("Strain Information")
                            .addTab("Overview", "Blue Dream is a sativa-dominant hybrid cross between " +
                                    "Blueberry and Haze. Known for balanced effects.")
                            .addTab("Effects", "Users report cerebral stimulation, euphoria, and creativity. " +
                                    "Helps with stress, pain, and depression.")
                            .addTab("Growing", "Flowering time: 9-10 weeks. Medium difficulty. " +
                                    "Prefers warm climate. High yield potential.")
                            .addTab("Cannabinoids", "THC: 17-24%, CBD: <1%, CBG: 0.5-1%");
                    row.withChild(tabs);
                })

                .addComponents(Spacer.vertical().large())

                // QuoteModule
                .addRow(row -> row.withChild(Header.H2("Quote Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **QuoteModule** displays testimonials, quotes, or highlighted text.

                        **Features:**
                        - Quote text with styling
                        - Author, attribution, and date
                        - Large or normal size
                        - Centered or left-aligned
                        """
                )))
                .addRow(row -> {
                    QuoteModule quote1 = QuoteModule.create()
                            .withQuote("This platform has revolutionized how we collect and share research data.")
                            .withAuthor("Dr. Jane Smith")
                            .withAttribution("Professor of Botany, UC Berkeley")
                            .withDate("January 2024");

                    QuoteModule quote2 = QuoteModule.create()
                            .withQuote("Collaborative research yields better results.")
                            .withAuthor("Dr. John Doe")
                            .large()
                            .centered();

                    row.withChild(quote1);
                    row.withChild(Spacer.vertical().medium());
                    row.withChild(quote2);
                })

                .addComponents(Spacer.vertical().large())

                // CalloutModule
                .addRow(row -> row.withChild(Header.H2("Callout Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **CalloutModule** creates highlighted informational boxes.

                        **Features:**
                        - Multiple types: info, warning, success, error, note
                        - Optional icon and title
                        - Dismissible option
                        - Custom content support
                        """
                )))
                .addRow(row -> {
                    Row calloutRow = new Row();

                    calloutRow.withChild(
                            CalloutModule.create()
                                    .info()
                                    .withTitle("Information")
                                    .withContent("Cannabis research is rapidly expanding worldwide.")
                    );

                    calloutRow.withChild(
                            CalloutModule.create()
                                    .warning()
                                    .withTitle("Warning")
                                    .withContent("Always verify data sources before citing in research.")
                    );

                    calloutRow.withChild(
                            CalloutModule.create()
                                    .success()
                                    .withTitle("Success")
                                    .withContent("Your data has been successfully submitted!")
                    );

                    calloutRow.withChild(
                            CalloutModule.create()
                                    .error()
                                    .withTitle("Error")
                                    .withContent("Failed to load data. Please try again.")
                    );

                    row.withChild(calloutRow);
                })

                .addComponents(Spacer.vertical().large())

                // ComparisonModule
                .addRow(row -> row.withChild(Header.H2("Comparison Module").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        The **ComparisonModule** displays side-by-side comparisons in table format.

                        **Features:**
                        - Multiple columns for comparison
                        - Highlighted/featured column option
                        - Row labels with values
                        - Perfect for strain comparison, pricing tables, feature matrices
                        """
                )))
                .addRow(row -> {
                    ComparisonModule comparison = ComparisonModule.create()
                            .withTitle("Strain Comparison")
                            .addColumn("Blue Dream")
                            .addColumn("OG Kush", true)  // Highlighted
                            .addColumn("Sour Diesel")
                            .addRow("Type", "Sativa-Dominant", "Indica-Dominant", "Sativa-Dominant")
                            .addRow("THC", "17-24%", "20-25%", "20-25%")
                            .addRow("CBD", "<1%", "<1%", "<1%")
                            .addRow("Flowering Time", "9-10 weeks", "8-9 weeks", "10-11 weeks")
                            .addRow("Effects", "Creative, Uplifting", "Relaxing, Euphoric", "Energizing, Focus")
                            .addRow("Difficulty", "Medium", "Easy", "Medium")
                            .addRow("Yield", "High", "Medium-High", "High");

                    row.withChild(comparison);
                })

                .addComponents(Spacer.vertical().large())

                // Summary
                .addRow(row -> row.withChild(Header.H2("Module Summary").withBottomBar()))
                .addRow(row -> row.withChild(new Markdown(
                        """
                        ## All Available Modules

                        The framework now includes **13 built-in modules**:

                        ### Original Modules (5)
                        1. **ContentModule** - Text and Markdown content display
                        2. **FormModule** - Structured forms with validation
                        3. **GalleryModule** - Image galleries
                        4. **ForumModule** - Discussion threads
                        5. **DataModule** - Type-safe data tables

                        ### New Modules (8)
                        6. **HeroModule** - Banner sections with CTAs
                        7. **StatsModule** - Statistics and metrics display
                        8. **TimelineModule** - Chronological event display
                        9. **AccordionModule** - Collapsible content sections
                        10. **TabsModule** - Tabbed content panels
                        11. **QuoteModule** - Testimonials and quotes
                        12. **CalloutModule** - Highlighted info boxes
                        13. **ComparisonModule** - Side-by-side comparisons

                        All modules follow the same patterns:
                        - Fluent builder API
                        - Static `create()` factory method
                        - `withTitle()` and `withModuleId()` methods
                        - Composable with other components
                        - HTMX integration support
                        """
                )))

                .build();

        return page.render();
    }
}
