[How-to index](README.md) | [Learning path](../getting-started/README.md)

# Build a dashboard

## Goal

Serve a work-queue dashboard at `/guide/dashboard`: three summary metrics, a table
of queues, and a review card in a responsive two-column layout. All totals come
from the same immutable snapshot. This example has no filters, sorting buttons,
export links, or other controls without handlers.

## Prerequisites

- A component-scanned Spring MVC application running on JDK 25 with SimplyPages
  **1.2** installed locally. Start with the [learning path](../getting-started/README.md),
  then [serve pages and fragments](serve-pages-and-fragments.md).
- Spring static-resource handling for `/css/framework.css`, `/js/framework.js`,
  and `/webjars/htmx.org/dist/htmx.min.js`. `ShellBuilder` links these assets;
  it does not install their HTTP mappings.
- A root-mounted application for the asset paths used here.

The supplied snapshot is a small, explicitly authored dataset, not a connection
to a monitoring system. Use only public or local example data. An internal
operations dashboard must authenticate and authorize its viewers in the
application before querying or rendering private data.

## 1. Add a controller with immutable view data

Save `DashboardController.java` below your application's component-scan root.
The package is `com.example.app.guides`; change it consistently to match your
application. The controller retains records and an immutable list, not built
components. Each GET creates a fresh page and its children.

```java
package com.example.app.guides;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Card;
import io.mindspice.simplypages.components.display.DataTable;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.StatsModule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private record WorkQueue(String name, int open, int overdue, String owner) {}

    private record Snapshot(LocalDate date, List<WorkQueue> queues) {
        Snapshot {
            queues = List.copyOf(queues);
        }
    }

    private final Snapshot snapshot = new Snapshot(LocalDate.of(2026, 3, 10), List.of(
        new WorkQueue("Access", 7, 2, "Alex"),
        new WorkQueue("Deploys", 4, 0, "Sam"),
        new WorkQueue("Billing", 9, 1, "Jo")
    ));

    @GetMapping(value = "/guide/dashboard", produces = "text/html")
    public String page() {
        Snapshot data = snapshot;
        int open = data.queues().stream().mapToInt(WorkQueue::open).sum();
        int overdue = data.queues().stream().mapToInt(WorkQueue::overdue).sum();
        long affected = data.queues().stream().filter(q -> q.overdue() > 0).count();

        StatsModule stats = StatsModule.create()
            .withModuleId("dashboard-stats")
            .withTitle("Queue summary")
            .withColumns(3)
            .addStat(Integer.toString(data.queues().size()), "Queues", "Tracked in this snapshot")
            .addStat(Integer.toString(open), "Open items", "Includes overdue items")
            .addStat(Integer.toString(overdue), "Overdue items", "Included in the open total");

        DataTable<WorkQueue> table = DataTable.create(WorkQueue.class)
            .addColumn("Queue", WorkQueue::name)
            .addColumn("Open", q -> Integer.toString(q.open()))
            .addColumn("Overdue", q -> Integer.toString(q.overdue()))
            .addColumn("Owner", WorkQueue::owner)
            .withData(data.queues())
            .striped()
            .bordered();

        Card queues = Card.create()
            .withHeader(Header.H2("Work queues"))
            .withBody(table)
            .withFooter("Snapshot date: " + data.date());
        Card review = Card.create()
            .withHeader(Header.H2("Review priorities"))
            .withBody(new Paragraph(overdue == 0
                ? "No overdue items in this snapshot."
                : overdue + " overdue items across " + affected + " queues need review."))
            .withFooter("Read-only example; reload does not fetch new source data.");

        Row details = new Row()
            .addColumn(Column.create().withWidth(8).withChild(queues))
            .addColumn(Column.create().withWidth(4).withChild(review));
        Page page = Page.builder()
            .addComponents(Header.H1("Operations dashboard"),
                new Paragraph("Work-queue snapshot for " + data.date()), stats)
            .addRow(details)
            .build();
        return ShellBuilder.create()
            .withPageTitle("Operations dashboard")
            .withContent(page)
            .build();
    }
}
```

## 2. Keep layout and rendering responsibilities separate

`StatsModule` composes the metric cards. `DataTable` extracts cell text from
`WorkQueue` records and escapes it; its extractors return strings, not HTML.
`Card` places its header, body, and footer in their defined slots. No demo-local
CSS or JavaScript is required.

The row contains explicit 8/12 and 4/12 `Column` containers. Their built-in
padding supplies the gutter; adding a flex gap to a full 12-column row can force
the second column to wrap. Set width on the containers, **not** on `StatsModule`:
module width methods are intentionally blocked. Framework CSS stacks the detail columns at mobile widths and reflows
the metric grid. Keep labels and table columns concise, and check your actual
data at narrow widths before adding more columns.

The route always returns a complete shell, even if a caller sends `HX-Request`.
There is no alternative fragment representation and no need to vary on that
header. Navigate to it with a normal link. If you later introduce fragment
refresh, use a stable target wrapper and the full/HX response contract from the
[serving guide](serve-pages-and-fragments.md).

`DataTable.sortable()` adds class markers only; it does not install sorting.
This example does not call it or imply that clicking a heading changes order.

## 3. Check the result

Start your application and open `/guide/dashboard`.

1. Expect **3** queues, **20** open items, and **3** overdue items. Overdue items
   are a subset of open items, not an additional workload total.
2. Confirm the table has Access (7/2/Alex), Deploys (4/0/Sam), and Billing
   (9/1/Jo), in that order. The review card reports overdue work in **2** queues.
3. Reload the URL directly. It returns a full document with one set of framework
   assets and the same snapshot date, not a live-data claim.
4. At a desktop width, check the table and review card align in their 8/4 row.
   At a narrow mobile width, check the stacked cards, readable metric labels,
   and table cells for overlap or clipping. No text should cover adjacent cards.
5. Confirm the page offers no nonfunctional refresh, edit, or export controls.

For a direct Java smoke check, construct `new DashboardController()` and call
`page()`. It should return a complete document containing the three queue names,
summary labels, and review text. Calling it twice must not duplicate table rows,
metric cards, or module IDs. This is the actual GET handler.

## 4. Replace the snapshot at the application boundary

To use live data, load one authorized, consistently scoped snapshot per request,
then calculate the metrics and render the table from that same snapshot. Enforce
resource limits and query bounds in the application; `DataTable` does not fetch
or paginate database rows for you. Set private-response caching policy when the
values belong to a user or organization.

Do not retain a built table in the singleton controller and append new data on
each request: `withData(...)` appends rather than replaces rows. Reconstruct
request-owned components as above, or use slots/templates where the structure
and sharing contract genuinely fit. SimplyPages provides presentation, not a
dashboard database, refresh scheduler, or authorization service.

## Related references

- [Layout reference](../core/02-layout-page-row-column-grid.md)
- [Components and modules catalog](../reference/components-and-modules-catalog.md)
- [Shell and asset reference](../reference/builders-shell-nav-banner-accountbar.md)
- [Rendering and lifecycle](../explanation/rendering-and-lifecycle.md)
- [Security boundaries](../security/01-security-boundaries-and-safe-rendering.md)
