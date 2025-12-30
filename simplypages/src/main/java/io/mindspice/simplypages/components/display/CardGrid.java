package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.layout.Grid;

/**
 * Grid container for displaying multiple cards.
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
