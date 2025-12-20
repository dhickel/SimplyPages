package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid container for displaying multiple cards.
 */
public class CardGrid extends HtmlTag {

    private final List<Card> cards = new ArrayList<>();
    private int columns = 3; // default 3 columns

    public CardGrid() {
        super("div");
        this.withAttribute("class", "card-grid");
    }

    public static CardGrid create() {
        return new CardGrid();
    }

    public CardGrid withColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public CardGrid addCard(Card card) {
        cards.add(card);
        return this;
    }

    public CardGrid withClass(String className) {
        String currentClass = "card-grid";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        this.withAttribute("class", "card-grid grid-cols-" + columns);
        cards.forEach(card -> super.withChild(card));
        return super.render();
    }
}
