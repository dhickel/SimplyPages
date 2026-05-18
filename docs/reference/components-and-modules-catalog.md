[Previous](../operations/02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Components and Modules Catalog

This is a practical catalog, not exhaustive class-level Javadoc.

## Primitive Components (examples)

- Text/content: `Header`, `Paragraph`, `Markdown`, `Div`, `Code`, `Blockquote`
- Forms: `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`
- Search/select forms: `Autocomplete` (field + HTMX option/status fragment helpers)
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
