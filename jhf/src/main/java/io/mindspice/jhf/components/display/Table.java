package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;

/**
 * Table component for displaying tabular data.
 * Supports headers, rows, and various styling options.
 */
public class Table extends HtmlTag {

    private final HtmlTag thead;
    private final HtmlTag tbody;
    private final HtmlTag headerRow;

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

    public static Table create() {
        return new Table();
    }

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

    public Table addRow(String... cellValues) {
        tbody.withChild(new Row(cellValues));
        return this;
    }

    public Table addRow(Component... cellComponents) {
        tbody.withChild(new Row(cellComponents));
        return this;
    }

    // Helper to add a Row object directly (e.g. from lambda)
    public Table addRow(Row row) {
        tbody.withChild(row);
        return this;
    }

    public Table striped() {
        this.addClass("table-striped");
        return this;
    }

    public Table bordered() {
        this.addClass("table-bordered");
        return this;
    }

    public Table hoverable() {
        this.addClass("table-hover");
        return this;
    }

    @Override
    public Table withClass(String className) {
        super.addClass(className);
        return this;
    }

    // Removed getChildrenStream override as we now manage children statefully

    public static class Row implements Component {
        private final List<Cell> cells = new ArrayList<>();

        public Row(String... cellValues) {
            for (String value : cellValues) {
                cells.add(new Cell(value));
            }
        }

        public Row(Component... cellComponents) {
            for (Component comp : cellComponents) {
                cells.add(new Cell(comp));
            }
        }

        @Override
        public String render(RenderContext context) {
            StringBuilder sb = new StringBuilder("<tr>");
            cells.forEach(cell -> sb.append(cell.render(context)));
            sb.append("</tr>");
            return sb.toString();
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }

    public static class Cell implements Component {
        private final String textValue;
        private final Component componentValue;

        public Cell(String textValue) {
            this.textValue = textValue;
            this.componentValue = null;
        }

        public Cell(Component componentValue) {
            this.textValue = null;
            this.componentValue = componentValue;
        }

        @Override
        public String render(RenderContext context) {
            String content = textValue != null ? Encode.forHtml(textValue) : componentValue.render(context);
            return "<td>" + content + "</td>";
        }

        @Override
        public String render() {
            return render(RenderContext.empty());
        }
    }
}
