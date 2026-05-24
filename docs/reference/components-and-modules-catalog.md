[Previous](../operations/02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Components and Modules Catalog

This is a practical catalog, not exhaustive class-level Javadoc.

## Primitive Components

- Text/content: `Header`, `Paragraph`, `Markdown`, `Div`, `Code`, `Blockquote`
- Forms: `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`
- Search/select forms: `Autocomplete` (field + HTMX option/status fragment helpers)
- Display: `Card`, `CardGrid`, `DataTable`, `Table`, `Alert`, `Badge`, `Tag`, `InfoBox`, `Spinner`
- Media: `Image`, `Gallery`, `Video`, `Audio`
- Navigation: `Link`, `NavBar`, `SideNav`, `Breadcrumb`
- Forum: `ForumCategoryRenderer`, `ForumTopicRenderer`, `ForumCommentRenderer`, `ForumCategoryData`, `ForumTopicData`, `ForumCommentData`, `ForumTopicTitleLink`, `ForumTagParser`, `ForumTagResolverRegistry`
- Chat: `ChatTranscriptRenderer`, `ChatMessageData`, `ChatMessageComponent`, `DefaultChatMessageComponent`, `ChatUiConfig`, `ChatTransportMode`
- Static content helper: `StaticContentSiteBuilder`, `StaticContentSite`, `ContentSectionConfig`, `ContentRouteIndex`, `ContentListItemComponent`, `DefaultContentListItemComponent`

Common primitive contract:

- Prefer `create(...)` factories where available.
- Use fluent mutators for static structure during page composition.
- Use `render(RenderContext)` for context-aware output and `render()` only for empty-context output.
- Text setters escape plain text by default; `RawHtml` and `withUnsafeHtml(...)` are trusted-input
  paths.

Form primitives:

- `Form` owns method/action/HTMX post wiring and CSRF helper output.
- `TextInput`, `TextArea`, `Select`, `Checkbox`, and `RadioGroup` render name/value controls for
  normal `@RequestParam` form handling.
- `Button` is a command primitive; attach HTMX attributes explicitly or through module/builders.

Navigation primitives:

- `Link`, `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, and `AccountWidget` validate public link
  targets with `SafeUrl`.
- Use root-relative URLs for application routes and `http`, `https`, `mailto`, or `tel` for
  external links.

## Layout Components

- `Page`
- `Row`
- `Column`
- `Grid`
- `Container`
- `Section`

Layout contract:

- `Row.withChild(...)` wraps non-column content in a default `.col`.
- `Row.addColumn(...)` appends a preconfigured `Column`.
- `Row.copy()` shallow-copies row attributes/text/children for wrappers that add render-time
  structure.
- `Column.withWidth(1..12)`, `auto()`, and `fill()` own grid sizing.
- `Grid.withColumns(1..6)` and `Grid.withGap(...)` emit constrained CSS tokens.

## Module Components

- `ContentModule`
- `FormModule`
- `DataModule`
- `GalleryModule`
- `HeroModule`
- `SimpleListModule`
- `ChatModule`
- `EditableModule` (wrapper/decorator)

Module contract:

- Modules build lazily and cache structure until invalidated by their own mutation methods.
- Consumers should not call module width helpers; use `Row`/`Column` layout controls instead.
- `ContentModule`, `RichContentModule`, and `SimpleListModule` implement editing contracts for
  common content-editing flows.
- `EditableModule` decorates another module with edit/delete controls; endpoint authorization
  remains the application controller's responsibility.

## Selection Guidance

1. Start with primitives for one-off UI elements.
2. Use modules for reusable business sections.
3. Wrap modules with `EditableModule` when edit controls are needed.
4. Keep layout responsibilities in `Row`/`Column`, not in module width settings.

## Safety Notes

- `Link`, `NavBar`, `SideNav`, `Breadcrumb`, `Dropdown`, `AccountWidget`, `HeroModule`
  button URLs, and related builders validate link targets through `SafeUrl`.
- `HeroModule.withBackgroundImage(...)` validates CSS-embedded image URLs and rejects values that
  could break out of the generated `url(...)` value.
- `HeroModule.withBackgroundColor(...)` uses the hardened inline style path and rejects
  declaration-breakout characters.

## Rendering Notes

- `Code.block(...)`, `Spinner.withMessage(...)`, `Paragraph` alignment helpers, `Modal`
  body/footer content, and `Column` class normalization render consistently in direct, nested,
  context-aware, and template render paths.
- Custom renderers should treat `render(RenderContext)` as canonical and delegate zero-argument
  `render()` to an empty context.

## Layout And Module Contract Notes

- `Row`, `Grid`, `Container`, and `Section` preserve base/custom classes when applying layout
  modifiers.
- `StatsModule.withColumns(...)` accepts `1..6` and emits CSS-backed `stats-cols-*` classes.
- `TabsModule` generates per-instance fallback tab/panel IDs when no module id is supplied.
- `ComparisonModule` requires each row to provide exactly one value per configured column.
- `SimpleListModule` handles null item ids and rebuilds rendered content after item add/remove
  mutations.
