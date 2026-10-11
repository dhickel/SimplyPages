[Previous](../operations/02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Components and Modules Catalog

This is a practical catalog, not exhaustive class-level Javadoc.

## Primitive Components (examples)

- Text/content: `Header`, `Paragraph`, `Markdown`, `Div`, `Code`, `Blockquote`
- Forms: `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`
- Display: `Card`, `CardGrid`, `DataTable`, `Table`, `Alert`, `Badge`, `Tag`, `InfoBox`, `Spinner`, `Modal`
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

## Rendering and control details

Generated children (including card sections, table rows, select options, and radio controls)
are included in compiled templates. Custom renderers retain their `render(RenderContext)`
behavior. Code blocks escape both code and title text; modal body/footer content receives
the render context.

### Modal

`Modal.create()` renders a `.modal-backdrop` containing a `.modal-container` dialog.
`withModalId(...)` accepts IDs starting with a letter followed by letters, digits, hyphens
or underscores. `withTitle(...)` supplies escaped visible text and the dialog's accessible
name through `aria-labelledby`. Missing or blank titles use `aria-label="Dialog"`;
`showCloseButton(false)` with no title omits the header.

Load `/css/framework.css` and `/js/framework.js` once in the page shell (ShellBuilder does
this by default). On initial rendering, direct DOM insertion or HTMX insertion, focus moves
to a usable `autofocus` element, otherwise the first body control, any dialog control, or
the dialog container when there are no controls. Tab/Shift+Tab stay inside the top dialog.
Initial and keyboard focus reveal the chosen control through the dialog's internal scroll
containers without scrolling the background page.
Only the top dialog handles Escape, backdrop and `.modal-close` clicks. All three close
options default to enabled; `closeOnEscape(false)`, `closeOnBackdrop(false)` and
`showCloseButton(false)` independently disable their corresponding paths. A disabled
Escape action does not close an underlying dialog. Clicking inside does not dismiss it.

Closing or removing a dialog, including HTMX/OOB replacement, restores its still-connected,
usable launcher. Replacing an edit dialog retains its original launcher; closing a nested
dialog returns to its launcher in the underlying dialog. If that launcher was removed,
focus stays in the remaining top dialog when present. Applications must provide a close
or completion path if they disable all dismissal options, and should supply a descriptive
title rather than relying on the generic fallback. These behaviors are not a claim of
complete application accessibility or application-owned authorization.

Checkbox labels preserve explicit IDs and follow later ID changes. Radio IDs are generated
per instance and should not be reconstructed from values; `required()` also applies to later
options. Tabs and accordions generate unique fallback IDs. Supply module IDs when consumers
need predictable selectors. Audio/video `withControls(false)` removes controls. Progress bars
require a positive maximum and clamp their rendered value to that maximum.
