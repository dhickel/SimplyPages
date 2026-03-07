package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Object-backed table renderer with column extractors.
 *
 * <p>Mutable and not thread-safe. Columns and rows are accumulated on this instance. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class DataTable<T> extends HtmlTag {

    private final List<Column<T>> columns = new ArrayList<>();
    private final List<T> data = new ArrayList<>();
    private boolean striped = false;
    private boolean bordered = false;
    private boolean hoverable = false;
    private boolean sortable = false;

    /**
     * Creates a data-table wrapper container.
     */
    public DataTable() {
        super("div");
        this.withAttribute("class", "data-table-wrapper");
    }

    /**
     * Creates a data table.
     *
     * @param dataClass ignored type token retained for fluent API ergonomics
     * @param <T> row object type
     * @return new data table
     */
    public static <T> DataTable<T> create(Class<T> dataClass) {
        return new DataTable<>();
    }

    /**
     * Adds a column with header and value extractor.
     *
     * @param header column header text
     * @param extractor maps row object to cell text
     * @return this table
     */
    public DataTable<T> addColumn(String header, Function<T, String> extractor) {
        columns.add(new Column<>(header, extractor));
        return this;
    }

    /**
     * Adds a column with optional CSS class.
     *
     * @param header column header text
     * @param extractor maps row object to cell text
     * @param cssClass optional class applied to header and cell
     * @return this table
     */
    public DataTable<T> addColumn(String header, Function<T, String> extractor, String cssClass) {
        columns.add(new Column<>(header, extractor, cssClass));
        return this;
    }

    /**
     * Appends all provided rows.
     *
     * @param data rows to append
     * @return this table
     */
    public DataTable<T> withData(List<T> data) {
        this.data.addAll(data);
        return this;
    }

    /**
     * Appends one row object.
     *
     * @param item row object
     * @return this table
     */
    public DataTable<T> addRow(T item) {
        this.data.add(item);
        return this;
    }

    /**
     * Enables striped table class.
     *
     * @return this table
     */
    public DataTable<T> striped() {
        this.striped = true;
        return this;
    }

    /**
     * Enables bordered table class.
     *
     * @return this table
     */
    public DataTable<T> bordered() {
        this.bordered = true;
        return this;
    }

    /**
     * Enables hover table class.
     *
     * @return this table
     */
    public DataTable<T> hoverable() {
        this.hoverable = true;
        return this;
    }

    /**
     * Enables sortable class markers.
     *
     * <p>This method adds CSS hooks only; client-side sorting behavior is application-defined.</p>
     *
     * @return this table
     */
    public DataTable<T> sortable() {
        this.sortable = true;
        return this;
    }

    /**
     * Materializes a table child and returns it with any additional inherited children.
     *
     * @return children stream
     */
    @Override
    protected Stream<Component> getChildrenStream() {
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

        // Return stream containing the built table followed by any other children
        return Stream.concat(Stream.of(table), super.getChildrenStream());
    }

    /**
     * Internal immutable column descriptor.
     */
    private static class Column<T> {
        final String header;
        final Function<T, String> extractor;
        final String cssClass;

        /**
         * Creates a column descriptor without CSS class.
         *
         * @param header header text
         * @param extractor value extractor
         */
        Column(String header, Function<T, String> extractor) {
            this(header, extractor, null);
        }

        /**
         * Creates a column descriptor.
         *
         * @param header header text
         * @param extractor value extractor
         * @param cssClass optional CSS class
         */
        Column(String header, Function<T, String> extractor, String cssClass) {
            this.header = header;
            this.extractor = extractor;
            this.cssClass = cssClass;
        }
    }
}
