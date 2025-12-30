package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comparison module for side-by-side comparisons.
 *
 * <p>Perfect for comparing strains, products, research methods, or any
 * entities with similar attributes.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Compare cannabis strains
 * ComparisonModule.create()
 *     .withTitle("Strain Comparison")
 *     .addColumn("Blue Dream")
 *     .addColumn("OG Kush")
 *     .addColumn("Sour Diesel")
 *     .addRow("Type", "Sativa-dominant", "Indica-dominant", "Sativa-dominant")
 *     .addRow("THC", "17-24%", "20-25%", "20-25%")
 *     .addRow("CBD", "<1%", "<1%", "<1%")
 *     .addRow("Effects", "Uplifting, Creative", "Relaxing, Euphoric", "Energizing");
 *
 * // Compare with highlights
 * ComparisonModule.create()
 *     .addColumn("Free")
 *     .addColumn("Premium", true) // Highlighted
 *     .addRow("Storage", "1GB", "Unlimited")
 *     .addRow("Support", "Email", "24/7 Phone");
 * }</pre>
 */
public class ComparisonModule extends Module {

    public static class ComparisonColumn {
        private final String name;
        private final boolean highlighted;

        public ComparisonColumn(String name, boolean highlighted) {
            this.name = name;
            this.highlighted = highlighted;
        }

        public String getName() { return name; }
        public boolean isHighlighted() { return highlighted; }
    }

    public static class ComparisonRow {
        private final String label;
        private final List<String> values;
        private final List<Component> customValues;

        public ComparisonRow(String label, List<String> values) {
            this.label = label;
            this.values = values;
            this.customValues = null;
        }

        public ComparisonRow(String label, List<Component> customValues, boolean isCustom) {
            this.label = label;
            this.values = null;
            this.customValues = customValues;
        }

        public String getLabel() { return label; }
        public List<String> getValues() { return values; }
        public List<Component> getCustomValues() { return customValues; }
    }

    private List<ComparisonColumn> columns = new ArrayList<>();
    private List<ComparisonRow> rows = new ArrayList<>();

    public ComparisonModule() {
        super("div");
        this.withClass("comparison-module");
    }

    public static ComparisonModule create() {
        return new ComparisonModule();
    }

    @Override
    public ComparisonModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ComparisonModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Adds a comparison column.
     *
     * @param name the column name/header
     */
    public ComparisonModule addColumn(String name) {
        this.columns.add(new ComparisonColumn(name, false));
        return this;
    }

    /**
     * Adds a comparison column with optional highlighting.
     *
     * @param name the column name/header
     * @param highlighted whether to highlight this column (e.g., "recommended")
     */
    public ComparisonModule addColumn(String name, boolean highlighted) {
        this.columns.add(new ComparisonColumn(name, highlighted));
        return this;
    }

    /**
     * Adds a comparison row with string values for each column.
     *
     * @param label the row label (attribute name)
     * @param values values for each column (must match column count)
     */
    public ComparisonModule addRow(String label, String... values) {
        this.rows.add(new ComparisonRow(label, List.of(values)));
        return this;
    }

    /**
     * Adds a comparison row with custom component values.
     *
     * @param label the row label (attribute name)
     * @param values custom components for each column
     */
    public ComparisonModule addRowWithComponents(String label, Component... values) {
        this.rows.add(new ComparisonRow(label, List.of(values), true));
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        // Create comparison table
        HtmlTag table = new HtmlTag("table").withAttribute("class", "comparison-table");

        // Header row with column names
        HtmlTag thead = new HtmlTag("thead");
        HtmlTag headerRow = new HtmlTag("tr");

        // Empty cell for row labels
        headerRow.withChild(new HtmlTag("th"));

        // Column headers
        for (ComparisonColumn column : columns) {
            HtmlTag th = new HtmlTag("th")
                .withAttribute("class", column.isHighlighted() ? "comparison-col-highlighted" : "")
                .withInnerText(column.getName());
            headerRow.withChild(th);
        }

        thead.withChild(headerRow);
        table.withChild(thead);

        // Body rows
        HtmlTag tbody = new HtmlTag("tbody");

        for (ComparisonRow row : rows) {
            HtmlTag tr = new HtmlTag("tr");

            // Row label
            HtmlTag labelCell = new HtmlTag("th")
                .withAttribute("class", "comparison-row-label")
                .withAttribute("scope", "row")
                .withInnerText(row.getLabel());
            tr.withChild(labelCell);

            // Row values
            if (row.getCustomValues() != null) {
                // Custom components
                for (int i = 0; i < row.getCustomValues().size(); i++) {
                    HtmlTag td = new HtmlTag("td")
                        .withAttribute("class", i < columns.size() && columns.get(i).isHighlighted() ?
                            "comparison-col-highlighted" : "");
                    td.withChild(row.getCustomValues().get(i));
                    tr.withChild(td);
                }
            } else {
                // String values
                for (int i = 0; i < row.getValues().size(); i++) {
                    HtmlTag td = new HtmlTag("td")
                        .withAttribute("class", i < columns.size() && columns.get(i).isHighlighted() ?
                            "comparison-col-highlighted" : "")
                        .withInnerText(row.getValues().get(i));
                    tr.withChild(td);
                }
            }

            tbody.withChild(tr);
        }

        table.withChild(tbody);
        super.withChild(table);
    }
}
