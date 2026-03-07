package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.layout.Grid;

/**
 * Grid specialization for arranging {@link Card} components.
 *
 * <p>Mutable and not thread-safe through inherited {@link Grid} state. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class CardGrid extends Grid {

    public CardGrid() {
        super("card-grid");
        // Default columns are 3, inherited from Grid
    }

    public static CardGrid create() {
        return new CardGrid();
    }

    @Override
    public CardGrid withColumns(int columns) {
        super.withColumns(columns);
        return this;
    }

    public CardGrid addCard(Card card) {
        super.addItem(card);
        return this;
    }

    @Override
    public CardGrid withClass(String className) {
        super.withClass(className);
        return this;
    }
}
