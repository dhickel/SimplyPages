package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateful table builder with explicit header/body sections.
 *
 * <p>Mutable and not thread-safe. Header/cell rows are appended to this instance and rendered
 * in insertion order. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class Table extends HtmlTag {

    private final HtmlTag thead;
    private final HtmlTag tbody;
    private final HtmlTag headerRow;

    /**
     * Creates a table with preattached {@code thead > tr} and {@code tbody}.
     */
    public Table() {
        super("table");
        this.addClass("table");

        // Initialize structure
        this.thead = new HtmlTag("thead");
        this.headerRow = new HtmlTag("tr");
        this.thead.withChild(headerRow);

        this.tbody = new HtmlTag("tbody");

        // Add to children immediately so they are always rendered
        // Note: We add thead/tbody even if empty, but CSS usually hides empty ones or we can check before render.
        // However, for simplicity and statefulness, we add them.
        // If strict 'no empty thead' is needed, we can toggle visibility or add conditionally in a specialized render.
        // But standard HtmlTag behavior is to render what's in children.
        this.withChild(thead);
        this.withChild(tbody);
    }

    /**
     * Creates a new table.
     *
     * @return new table
     */
    public static Table create() {
        return new Table();
    }

    /**
     * Appends header cells to the existing header row.
     *
     * <p>This method appends and does not clear prior headers.</p>
     *
     * @param headerLabels header text values
     * @return this table
     */
    public Table withHeaders(String... headerLabels) {
        // Clear existing headers to avoid duplication if called multiple times
        // Since HtmlTag children list is not easily cleared by type, we assume this is called once or we clear children of headerRow
        // But HtmlTag doesn't expose clearChildren().
        // We will just append for now, assuming standard usage.
        for (String h : headerLabels) {
            HtmlTag th = new HtmlTag("th").withInnerText(h);
            headerRow.withChild(th);
        }
        return this;
    }

    /**
     * Appends a row with text cells.
     *
     * @param cellValues text cell values
     * @return this table
     */
    public Table addRow(String... cellValues) {
        tbody.withChild(new Row(cellValues));
        return this;
    }

    /**
     * Appends a row with component cells.
     *
     * @param cellComponents cell components
     * @return this table
     */
    public Table addRow(Component... cellComponents) {
        tbody.withChild(new Row(cellComponents));
        return this;
    }

    /**
     * Appends a prebuilt row.
     *
     * @param row row instance
     * @return this table
     */
    public Table addRow(Row row) {
        tbody.withChild(row);
        return this;
    }

    /**
     * Appends {@code table-striped} class.
     *
     * @return this table
     */
    public Table striped() {
        this.addClass("table-striped");
        return this;
    }

    /**
     * Appends {@code table-bordered} class.
     *
     * @return this table
     */
    public Table bordered() {
        this.addClass("table-bordered");
        return this;
    }

    /**
     * Appends {@code table-hover} class.
     *
     * @return this table
     */
    public Table hoverable() {
        this.addClass("table-hover");
        return this;
    }

    /**
     * Appends class token(s).
     *
     * @param className class token(s)
     * @return this table
     */
    @Override
    public Table withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Renderable table row model.
     *
     * <p>Mutable only during construction.</p>
     */
    public static class Row implements Component {
        private final List<Cell> cells = new ArrayList<>();

        /**
         * Creates a row from text cells.
         *
         * @param cellValues text values
         */
        public Row(String... cellValues) {
            for (String value : cellValues) {
                cells.add(new Cell(value));
            }
        }

        /**
         * Creates a row from component cells.
         *
         * @param cellComponents component values
         */
        public Row(Component... cellComponents) {
            for (Component comp : cellComponents) {
                cells.add(new Cell(comp));
            }
        }

        /**
         * Renders this row and all cells.
         *
         * @param context render context
         * @return row HTML
         */
        @Override
        public String render(RenderContext context) {
            StringBuilder sb = new StringBuilder("<tr>");
            cells.forEach(cell -> sb.append(cell.render(context)));
            sb.append("</tr>");
            return sb.toString();
        }

        /**
         * Renders using empty context.
         *
         * @return row HTML
         */
        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }

    /**
     * Renderable table cell model.
     *
     * <p>Exactly one of text or component is set per instance.</p>
     */
    public static class Cell implements Component {
        private final String textValue;
        private final Component componentValue;

        /**
         * Creates a text cell.
         *
         * @param textValue untrusted or trusted text
         */
        public Cell(String textValue) {
            this.textValue = textValue;
            this.componentValue = null;
        }

        /**
         * Creates a component cell.
         *
         * @param componentValue component value
         */
        public Cell(Component componentValue) {
            this.textValue = null;
            this.componentValue = componentValue;
        }

        /**
         * Renders a cell, escaping text values and delegating component rendering.
         *
         * @param context render context
         * @return cell HTML
         */
        @Override
        public String render(RenderContext context) {
            String content = textValue != null ? Encode.forHtml(textValue) : componentValue.render(context);
            return "<td>" + content + "</td>";
        }

        /**
         * Renders using empty context.
         *
         * @return cell HTML
         */
        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
