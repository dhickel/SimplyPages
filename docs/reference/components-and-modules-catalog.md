[Previous](../operations/02-testing-and-troubleshooting-playbook.md) | [Index](../INDEX.md)

# Components and Modules Catalog

This is a practical catalog, not exhaustive class-level Javadoc.

## Primitive Components (examples)

- Text/content: `Header`, `Paragraph`, `Markdown`, `Div`, `Code`, `Blockquote`
- Forms: `Form`, `TextInput`, `TextArea`, `Select`, `Checkbox`, `RadioGroup`, `Button`
- Display: `Card`, `CardGrid`, `DataTable`, `Table`, `Alert`, `Badge`, `Tag`, `InfoBox`, `Spinner`, `ProgressBar`, `Modal`
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
Native disclosure summaries participate in tab order unless given a negative `tabindex`.
Closed `details` content is excluded, including nested disclosures; its first summary remains
reachable when visible. Enter/Space retain native disclosure activation.
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

### Form labels and radio groups

`Form.addField(label, control)` associates a nonempty label with `TextInput`, `TextArea`,
`Select`, `Checkbox` or `Button`. It preserves an existing ID (including one set through
`withAttribute`) and assigns a unique, stable ID during field assembly when absent. For Checkbox,
`withId` controls the internal input; generic attributes still belong to the wrapper. A checkbox
field targets the internal input, not its wrapper. Configure IDs before `addField`: changing a
control ID afterward does not retarget the already assembled field label. Checkbox's own
`withLabel` relationship still follows its ID setters.

For `RadioGroup`, the field caption names the `radiogroup` through `aria-labelledby`; each
option keeps its individual label. Standalone groups need an application-supplied `aria-label`
or `aria-labelledby`. Option IDs are generated per instance: do not reconstruct them from
values. `required()` applies to later options too. Arbitrary/custom or multi-control components
receive only a visible caption; applications must compose their own control labels/names.
`addField` does not inspect rendered HTML or guess a target inside a custom component.

### Tabs and accordions

Tabs and accordions generate unique fallback IDs. Supply module IDs for predictable selectors.
Tabs have one tab stop per owned tablist. With framework assets loaded, `aria-orientation`
follows the computed navigation layout on initialization, resize and dynamic insertion:
horizontal tabs use ArrowLeft/ArrowRight, while the default vertical layout through 768px
uses ArrowUp/ArrowDown. These arrows wrap and automatically activate the next owned tab;
Home/End select the first/last. Perpendicular arrows and modified keys retain native behavior.
Enter/Space use native button activation. Orientation synchronization preserves selection,
focus and nested state; activation keeps roving tabindex and panel `hidden` state consistent.
Server markup starts with horizontal orientation because the viewport is unknown during SSR.
Accordion regions are named by their own header buttons. Each `.accordion-content` panel
contains an `.accordion-body` wrapper for text or custom components. Default framework CSS
uses intrinsic expanded height without a fixed content cap, accommodates later content growth,
and does not clip child disclosures at the accordion's own item or panel boundaries.

Closed panels start with `hidden`, `inert` and `aria-hidden="true"`. With framework assets loaded,
native header button activation (including Enter/Space) synchronizes these attributes with
`aria-expanded` and the active/expanded classes. If focus is inside a panel being closed,
it returns to that panel's header before descendants become unavailable. After the complete
transition, the framework reveals that header with nearest scrolling. Inside a Modal, reveal
is limited to dialog-internal scroll/clipping boundaries without scrolling the page or backdrop;
oversized headers use nearest alignment rather than guaranteed full visibility. Single-expand behavior
is scoped to the nearest owning `.accordion`; `allowMultiple()` permits multiple open items
within that owner. Closing an outer panel preserves nested expansion state for reopening.

Application styles or surrounding containers can still constrain height, clip disclosures or
change contrast; applications must validate those custom layouts and palettes. These defaults
are not a claim of whole-application accessibility conformance.

### Dropdown and account disclosure navigation

`Dropdown.build()` and authenticated `AccountWidget` use named, non-submit buttons with
`aria-controls`/`aria-expanded`. Their closed panels are hidden and cannot receive keyboard focus.
Load framework JS/CSS once: click/touch and native Enter/Space toggle; ordinary Tab navigation
reaches links/custom items; outside clicks dismiss; Escape inside an open disclosure closes it
and returns focus to its trigger. A disclosure inside a modal handles its first Escape before
the outer dialog. These are disclosure navigation components, not ARIA menus; custom items keep
their native behavior. Choose alignment/placement that keeps open panels inside the host's visible
area; disclosure behavior does not reposition panels across clipping/scrolling ancestors.
Each built dropdown output has its own IDs; a configured account widget
keeps stable IDs across renders. Account rendering no longer clears inherited children and
forwards their request context. Finish configuration before sharing; retained custom child
delegates and contexts must independently be safe to share.

### Progress and media

`ProgressBar` requires a positive maximum and clamps the rendered value to it. The `.progress`
root carries the progressbar role, value/min/max and accessible name. `withLabel` supplies both
visible fill text and the name; the default name is `Progress`. Use existing `withAttribute`
for a more descriptive `aria-label` or external `aria-labelledby` relationship.

Default height is 20px; `withHeight` sets the wrapper height. `striped()` and `animated()` use
`progress-bar-striped`/`progress-bar-animated`; animation implies stripes and is disabled for
reduced motion. CSS retains the legacy container/stripe/animation aliases. Default labeled
variants use white text on the darker blue default/primary fills and dark text on success,
warning, error and info fills. These foregrounds and gradients maintain at least 4.5:1 text
contrast across the default gradient and stripe paints. Application color/theme overrides
must recheck their own contrast. Audio/video `withControls(false)` removes controls.
