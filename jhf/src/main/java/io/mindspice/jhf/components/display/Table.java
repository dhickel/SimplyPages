package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;

/**
 * Table component for displaying tabular data.
 * Supports headers, rows, and various styling options.
 */
public class Table extends HtmlTag {

    private final List<String> headers = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private boolean striped = false;
    private boolean bordered = false;
    private boolean hoverable = false;

    public Table() {
        super("table");
        this.withAttribute("class", "table");
    }

    public static Table create() {
        return new Table();
    }

    public Table withHeaders(String... headerLabels) {
        headers.addAll(List.of(headerLabels));
        return this;
    }

    public Table addRow(String... cellValues) {
        rows.add(new Row(cellValues));
        return this;
    }

    public Table addRow(Component... cellComponents) {
        rows.add(new Row(cellComponents));
        return this;
    }

    public Table striped() {
        this.striped = true;
        return this;
    }

    public Table bordered() {
        this.bordered = true;
        return this;
    }

    public Table hoverable() {
        this.hoverable = true;
        return this;
    }

    public Table withClass(String className) {
        String currentClass = "table";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        // Build class string
        StringBuilder classBuilder = new StringBuilder("table");
        if (striped) classBuilder.append(" table-striped");
        if (bordered) classBuilder.append(" table-bordered");
        if (hoverable) classBuilder.append(" table-hover");
        this.withAttribute("class", classBuilder.toString());

        // Add header if present
        if (!headers.isEmpty()) {
            HtmlTag thead = new HtmlTag("thead");
            HtmlTag headerRow = new HtmlTag("tr");
            headers.forEach(h -> {
                HtmlTag th = new HtmlTag("th").withInnerText(h);
                headerRow.withChild(th);
            });
            thead.withChild(headerRow);
            super.withChild(thead);
        }

        // Add rows
        if (!rows.isEmpty()) {
            HtmlTag tbody = new HtmlTag("tbody");
            rows.forEach(row -> tbody.withChild(row));
            super.withChild(tbody);
        }

        return super.render();
    }

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
        public String render() {
            StringBuilder sb = new StringBuilder("<tr>");
            cells.forEach(cell -> sb.append(cell.render()));
            sb.append("</tr>");
            return sb.toString();
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
        public String render() {
            String content = textValue != null ? Encode.forHtml(textValue) : componentValue.render();
            return "<td>" + content + "</td>";
        }
    }
}
