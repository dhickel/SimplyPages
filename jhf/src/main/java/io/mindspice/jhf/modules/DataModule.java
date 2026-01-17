package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.display.DataTable;
import io.mindspice.jhf.core.HtmlTag;

import java.util.List;

/**
 * Module for displaying data in tables.
 * @param <T> The data type to display
 */
public class DataModule<T> extends Module {

    private DataTable<T> dataTable;

    public DataModule() {
        super("div");
        this.withClass("data-module");
        this.dataTable = (DataTable<T>) DataTable.create(Object.class);
    }

    public static <T> DataModule<T> create(Class<T> dataClass) {
        DataModule<T> module = new DataModule<>();
        module.dataTable = DataTable.create(dataClass);
        return module;
    }

    public DataModule<T> withDataTable(DataTable<T> table) {
        this.dataTable = table;
        return this;
    }

    public DataModule<T> withData(List<T> data) {
        this.dataTable.withData(data);
        return this;
    }

    @Override
    public DataModule<T> withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public DataModule<T> withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        super.withChild(dataTable);
    }
}
