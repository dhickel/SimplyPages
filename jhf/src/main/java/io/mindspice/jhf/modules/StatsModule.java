package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Module for displaying statistics and metrics in a grid layout.
 *
 * <p>Stats modules are useful for showcasing key numbers, achievements, or
 * data points in a visually organized manner.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic stats
 * StatsModule.create()
 *     .withTitle("Research Stats")
 *     .addStat("1,234", "Studies Documented")
 *     .addStat("567", "Active Researchers")
 *     .addStat("89", "Strains Cataloged");
 *
 * // Custom column layout
 * StatsModule.create()
 *     .withColumns(4)
 *     .addStat("95%", "Accuracy", "Data verification rate")
 *     .addStat("24/7", "Available", "Platform uptime");
 * }</pre>
 */
public class StatsModule extends Module {

    public static class Stat {
        private final String value;
        private final String label;
        private final String description;

        public Stat(String value, String label, String description) {
            this.value = value;
            this.label = label;
            this.description = description;
        }

        public Stat(String value, String label) {
            this(value, label, null);
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }

    private List<Stat> stats = new ArrayList<>();
    private int columns = 3; // Default 3 columns

    public StatsModule() {
        super("div");
        this.withClass("stats-module");
    }

    public static StatsModule create() {
        return new StatsModule();
    }

    @Override
    public StatsModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public StatsModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Adds a statistic with value and label.
     *
     * @param value the numeric or text value to display
     * @param label the label describing the statistic
     */
    public StatsModule addStat(String value, String label) {
        this.stats.add(new Stat(value, label));
        return this;
    }

    /**
     * Adds a statistic with value, label, and description.
     *
     * @param value the numeric or text value to display
     * @param label the label describing the statistic
     * @param description additional context or description
     */
    public StatsModule addStat(String value, String label, String description) {
        this.stats.add(new Stat(value, label, description));
        return this;
    }

    /**
     * Sets the number of columns for the stats grid.
     *
     * @param columns number of columns (1-6)
     */
    public StatsModule withColumns(int columns) {
        this.columns = Math.min(Math.max(columns, 1), 6);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        // Create stats grid
        Div statsGrid = new Div()
            .withClass("stats-grid")
            .withAttribute("style", "display: grid; grid-template-columns: repeat(" + columns + ", 1fr); gap: 2rem;");

        for (Stat stat : stats) {
            Div statItem = new Div().withClass("stat-item");

            // Value
            HtmlTag value = new HtmlTag("div")
                .withAttribute("class", "stat-value")
                .withInnerText(stat.getValue());
            statItem.withChild(value);

            // Label
            HtmlTag label = new HtmlTag("div")
                .withAttribute("class", "stat-label")
                .withInnerText(stat.getLabel());
            statItem.withChild(label);

            // Description (optional)
            if (stat.getDescription() != null && !stat.getDescription().isEmpty()) {
                HtmlTag desc = new HtmlTag("div")
                    .withAttribute("class", "stat-description")
                    .withInnerText(stat.getDescription());
                statItem.withChild(desc);
            }

            statsGrid.withChild(statItem);
        }

        super.withChild(statsGrid);
    }
}
