package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;
import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Table component for displaying tabular data.
 * Supports headers, rows, and various styling options.
 */
public class Table extends HtmlTag {

    private final List<String> headers = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    public Table() {
        super("table");
        this.addClass("table");
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

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        // Add header if present
        if (!headers.isEmpty()) {
            HtmlTag thead = new HtmlTag("thead");
            HtmlTag headerRow = new HtmlTag("tr");
            headers.forEach(h -> {
                HtmlTag th = new HtmlTag("th").withInnerText(h);
                headerRow.withChild(th);
            });
            thead.withChild(headerRow);
            builder.add(thead);
        }

        // Add rows
        if (!rows.isEmpty()) {
            HtmlTag tbody = new HtmlTag("tbody");
            rows.forEach(row -> tbody.withChild(row));
            builder.add(tbody);
        }

        return Stream.concat(builder.build(), super.getChildrenStream());
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
