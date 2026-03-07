package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.display.DataTable;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.List;

/**
 * Module wrapper around {@link DataTable} for tabular data rendering.
 *
 * <p>Lifecycle: mutable configuration methods update backing table state before module build.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Configure per request/render flow. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 *
 * @param <T> row type rendered by the wrapped table
 */
public class DataModule<T> extends Module {

    private DataTable<T> dataTable;

    /** Creates a module with default table instance. */
    public DataModule() {
        super("div");
        this.withClass("data-module");
        this.dataTable = (DataTable<T>) DataTable.create(Object.class);
    }

    /** Creates a module configured with a typed {@link DataTable} instance. */
    public static <T> DataModule<T> create(Class<T> dataClass) {
        DataModule<T> module = new DataModule<>();
        module.dataTable = DataTable.create(dataClass);
        return module;
    }

    /** Replaces the backing table component. */
    public DataModule<T> withDataTable(DataTable<T> table) {
        this.dataTable = table;
        return this;
    }

    /** Replaces table row data on the backing table. */
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
