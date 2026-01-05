# Building Pages

Pages in SimplyPages are the root containers for your content. They define the structure using rows and columns and manage the layout of modules.

## The Page Class

The `Page` class is the starting point for creating a new view.

```java
Page page = new Page();
```

## Layout Structure

SimplyPages uses a grid system based on Rows and Columns.

### Rows

Rows are horizontal containers that hold columns. You must instantiate `Row` directly.

```java
Row row = new Row();
```

> **Note:** `Row` does not have a static `create()` method. Use `new Row()`.

### Columns

Columns sit inside rows and hold your content modules. They handle responsive sizing.

```java
Column col = new Column(6); // 50% width on desktop
col.addModule(myModule);
```

### Grid System

You can also use the `Grid` component for more complex layouts.

```java
Grid grid = new Grid();
grid.addColumn(new Column(4));
grid.addColumn(new Column(4));
grid.addColumn(new Column(4));
```

## Adding Content

Modules are added to columns, which are added to rows, which are added to the page.

```java
public class HomePage extends Page {

    public HomePage() {
        // Create modules
        HeroModule hero = HeroModule.create()
            .withTitle("Welcome")
            .withDescription("This is the home page.");

        ContentModule content = ContentModule.create()
            .withTitle("About")
            .withContent("Some content here.");

        // Create layout
        Row topRow = new Row();
        topRow.addColumn(new Column(12).addModule(hero));

        Row contentRow = new Row();
        contentRow.addColumn(new Column(8).addModule(content));
        contentRow.addColumn(new Column(4).addModule(new SidebarModule()));

        // Add to page
        this.addRow(topRow);
        this.addRow(contentRow);
    }
}
```

## Sections

For grouping related rows, you can use `Section`.

```java
Section featureSection = new Section("Features");
featureSection.addRow(featureRow1);
featureSection.addRow(featureRow2);

page.addSection(featureSection);
```

## Fluent API vs Direct Manipulation

While `Page` and `Row` offer some fluent methods, the standard pattern is to instantiate, configure, and then add to the parent.

```java
// Preferred pattern
Row row = new Row();
row.addColumn(col1);
row.addColumn(col2);
page.addRow(row);
```

## Best Practices

1.  **Request Scoped:** Pages should be instantiated per request. Do not reuse `Page` instances across requests as they hold state.
2.  **Separation of Concerns:** Keep your page logic separate from your module logic. Use factory methods or builder patterns for complex page assemblies.
