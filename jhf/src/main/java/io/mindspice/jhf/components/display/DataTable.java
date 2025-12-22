package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * DataTable component for displaying structured data from objects.
 * Supports mapping Java objects to table rows with type-safe column definitions.
 */
public class DataTable<T> extends HtmlTag {

    private final List<Column<T>> columns = new ArrayList<>();
    private final List<T> data = new ArrayList<>();
    private boolean striped = false;
    private boolean bordered = false;
    private boolean hoverable = false;
    private boolean sortable = false;

    public DataTable() {
        super("div");
        this.withAttribute("class", "data-table-wrapper");
    }

    public static <T> DataTable<T> create(Class<T> dataClass) {
        return new DataTable<>();
    }

    public DataTable<T> addColumn(String header, Function<T, String> extractor) {
        columns.add(new Column<>(header, extractor));
        return this;
    }

    public DataTable<T> addColumn(String header, Function<T, String> extractor, String cssClass) {
        columns.add(new Column<>(header, extractor, cssClass));
        return this;
    }

    public DataTable<T> withData(List<T> data) {
        this.data.addAll(data);
        return this;
    }

    public DataTable<T> addRow(T item) {
        this.data.add(item);
        return this;
    }

    public DataTable<T> striped() {
        this.striped = true;
        return this;
    }

    public DataTable<T> bordered() {
        this.bordered = true;
        return this;
    }

    public DataTable<T> hoverable() {
        this.hoverable = true;
        return this;
    }

    public DataTable<T> sortable() {
        this.sortable = true;
        return this;
    }

    @Override
    public String render() {
        // Build table class
        StringBuilder classBuilder = new StringBuilder("table data-table");
        if (striped) classBuilder.append(" table-striped");
        if (bordered) classBuilder.append(" table-bordered");
        if (hoverable) classBuilder.append(" table-hover");
        if (sortable) classBuilder.append(" table-sortable");

        HtmlTag table = new HtmlTag("table").withAttribute("class", classBuilder.toString());

        // Build header
        HtmlTag thead = new HtmlTag("thead");
        HtmlTag headerRow = new HtmlTag("tr");
        columns.forEach(col -> {
            HtmlTag th = new HtmlTag("th").withInnerText(col.header);
            if (col.cssClass != null) {
                th.withAttribute("class", col.cssClass);
            }
            if (sortable) {
                th.withAttribute("class", (col.cssClass != null ? col.cssClass + " " : "") + "sortable");
            }
            headerRow.withChild(th);
        });
        thead.withChild(headerRow);
        table.withChild(thead);

        // Build body
        HtmlTag tbody = new HtmlTag("tbody");
        data.forEach(item -> {
            HtmlTag row = new HtmlTag("tr");
            columns.forEach(col -> {
                String value = col.extractor.apply(item);
                HtmlTag td = new HtmlTag("td").withInnerText(value != null ? value : "");
                if (col.cssClass != null) {
                    td.withAttribute("class", col.cssClass);
                }
                row.withChild(td);
            });
            tbody.withChild(row);
        });
        table.withChild(tbody);

        super.withChild(table);
        return super.render();
    }

    private static class Column<T> {
        final String header;
        final Function<T, String> extractor;
        final String cssClass;

        Column(String header, Function<T, String> extractor) {
            this(header, extractor, null);
        }

        Column(String header, Function<T, String> extractor, String cssClass) {
            this.header = header;
            this.extractor = extractor;
            this.cssClass = cssClass;
        }
    }
}
