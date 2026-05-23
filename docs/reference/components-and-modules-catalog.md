[Previous](../operations/02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Components and Modules Catalog

This is a practical catalog, not exhaustive class-level Javadoc.

## Primitive Components (examples)

- Text/content: `Header`, `Paragraph`, `Markdown`, `Div`, `Code`, `Blockquote`
- Forms: `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`
- Display: `Card`, `CardGrid`, `DataTable`, `Table`, `Alert`, `Badge`, `Tag`, `InfoBox`, `Spinner`
- Media: `Image`, `Gallery`, `Video`, `Audio`
- Navigation: `Link`, `NavBar`, `SideNav`, `Breadcrumb`
- Forum: `ForumCategoryRenderer`, `ForumTopicRenderer`, `ForumCommentRenderer`, `ForumCategoryData`, `ForumTopicData`, `ForumCommentData`, `ForumTopicTitleLink`, `ForumTagParser`, `ForumTagResolverRegistry`
- Chat: `ChatTranscriptRenderer`, `ChatMessageData`, `ChatMessageComponent`, `DefaultChatMessageComponent`, `ChatUiConfig`, `ChatTransportMode`
- Static content helper: `StaticContentSiteBuilder`, `StaticContentSite`, `ContentSectionConfig`, `ContentRouteIndex`, `ContentListItemComponent`, `DefaultContentListItemComponent`

## Layout Components

- `Page`
- `Row`
- `Column`
- `Grid`
- `Container`
- `Section`

## Module Components (examples)

- `ContentModule`
- `FormModule`
- `DataModule`
- `GalleryModule`
- `HeroModule`
- `SimpleListModule`
- `ChatModule`
- `EditableModule` (wrapper/decorator)

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
