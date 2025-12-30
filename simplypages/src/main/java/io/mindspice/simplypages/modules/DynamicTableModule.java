package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic table module that can be rendered with specific data.
 * This module can be updated independently using HTMX.
 */
public class DynamicTableModule extends Module {

    private List<String[]> tableData;
    private String[] columnHeaders;

    public DynamicTableModule() {
        super("div");
        this.withClass("table-module");
        this.columnHeaders = new String[]{"Column 1", "Column 2", "Column 3"};
        this.tableData = new ArrayList<>();
        // Add some default data
        this.tableData.add(new String[]{"Row 1 Col 1", "Row 1 Col 2", "Row 1 Col 3"});
        this.tableData.add(new String[]{"Row 2 Col 1", "Row 2 Col 2", "Row 2 Col 3"});
    }

    public static DynamicTableModule create() {
        return new DynamicTableModule();
    }

    public DynamicTableModule withTableData(String[] headers, List<String[]> data) {
        this.columnHeaders = headers;
        this.tableData = data;
        return this;
    }

    @Override
    public DynamicTableModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public DynamicTableModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        // Add title if present
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        // Create table
        HtmlTag tableWrapper = new HtmlTag("div").withAttribute("class", "table-responsive");
        HtmlTag table = new HtmlTag("table").withAttribute("class", "table table-striped table-sm");

        // Table header
        HtmlTag thead = new HtmlTag("thead");
        HtmlTag headerRow = new HtmlTag("tr");
        for (String columnHeader : columnHeaders) {
            HtmlTag th = new HtmlTag("th").withInnerText(columnHeader);
            headerRow.withChild(th);
        }
        thead.withChild(headerRow);
        table.withChild(thead);

        // Table body
        HtmlTag tbody = new HtmlTag("tbody");

        for (String[] rowData : tableData) {
            HtmlTag row = new HtmlTag("tr");
            for (int i = 0; i < columnHeaders.length; i++) {
                String cellData = i < rowData.length ? rowData[i] : "";
                HtmlTag td = new HtmlTag("td").withInnerText(cellData);
                row.withChild(td);
            }
            tbody.withChild(row);
        }

        table.withChild(tbody);
        tableWrapper.withChild(table);
        super.withChild(tableWrapper);
    }
}