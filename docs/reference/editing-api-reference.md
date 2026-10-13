[Previous](content-helper-api-reference.md) | [Index](../INDEX.md)

# Editing API Reference

## Editable<T>

Contract for editable modules.

Required methods:

- `Component buildEditView()`
- `T applyEdits(Map<String, String> formData)`

Optional methods:

- `ValidationResult validate(Map<String, String> formData)`
- `List<EditableChild> getEditableChildren()`

## EditMode

- `OWNER_EDIT`: direct application
- `USER_EDIT`: staged/approval-oriented flow

## EditModalBuilder

Primary builder for edit modal UI.

Common methods:

- `withTitle(...)`
- `withModuleId(...)`
- `withEditable(...)` or `withEditView(...)`
- `withSaveUrl(...)`
- `withDeleteUrl(...)`
- `withChildEditUrl(...)`
- `withChildDeleteUrl(...)`
- `withPageContainerId(...)`
- `withModalContainerId(...)`
- `hideDelete()`

The returned `Modal` disables backdrop closing; Escape and the header close button remain
enabled. The footer Close button clears the configured modal container. Load the framework
CSS/JS once in the page shell for focus entry, Tab containment and connected-launcher
restoration, including external HTMX/OOB removal. Save uses `hx-swap="none"`; applications
must return OOB updates to clear the modal and update content. If an update also replaces
the launcher, the old disconnected element cannot receive focus.

See the [Modal rendering and interaction contract](components-and-modules-catalog.md#modal)
and [editing flow guide](../how-to/edit-content.md).

`withSaveUrl` and `withDeleteUrl` accept **complete caller URLs**. They do not substitute
braced tokens or encode identifiers. `withModuleId` does not construct these URLs.

Child edit/delete URLs are templates with only `{id}`. Tokens must occupy a whole path
segment or a whole query parameter value, for example `/children/{id}/edit` or
`/children/edit?id={id}`. Repeated tokens, omitted tokens and fixed URLs are allowed.
`null` disables the corresponding child action. Non-null templates are validated by the
setter; raw child ids are validated and encoded only when an action actually substitutes
`{id}` during `build()`. Empty child lists and templates without `{id}` do not validate ids.
`EditableChild` stores its id without global validation.

**Migration:** legacy child templates such as `/child-{id}`, `/{id}.json`, `?{id}=value`,
`https://{id}/edit` or `#child-{id}` now throw `IllegalArgumentException`. Move the token into
a full segment or query value. Pass raw ids, not pre-encoded strings. See
[URL template and identifier rules](#url-template-and-identifier-rules).

## EditablePage

`EditablePage.create(pageId)` emits a page wrapper and row-insert controls. It renders one
control with position `0` for an empty page, or one after each supplied row with positions
`1`, `2`, and so on. Insert controls retain `hx-target="closest .insert-row-section"` and
`hx-swap="beforebegin"`.

`withInsertRowUrlTemplate(template)` sets only these insert URLs. Its default is
`/api/pages/{pageId}/rows/insert?position={position}`; supported tokens are `{pageId}` and
`{position}`. Position is the framework's decimal insertion index. The page does **not**
change endpoint configuration on rows supplied through `addEditableRow`.

Templates are checked at the setter; a used page id is checked and encoded at render time.
Creation does not validate the page id. Rendering and compilation leave stored wrapper
attributes, children and row configuration unchanged. Generic inherited children remain stored
but are not emitted. Templates retain this renderer live, including later row/URL configuration.
Finish configuration before sharing; row delegates, descendants and request contexts have separate
sharing obligations. These generic insertion indices are not a private controller's position policy.

## EditableRow

`EditableRow.wrap(Row row, String rowId, String pageId)` creates an outer
`div.editable-row-wrapper` with id `row-{rowId}`. Its content renders in this order:

1. The supplied `Row`, unchanged, with its own id, classes, attributes, text and children.
2. A **separate** row containing modules registered through `addEditableModule(module, moduleId)`.
3. The add-module control, when adding is permitted and registration capacity remains.

The supplied row and registered modules are independent content, not a single editable grid.
The wrapper does not merge, deduplicate or move source children into editable columns.
**Callers must avoid registering content already present in the supplied row.** Outer wrapper
attributes do not replace or propagate to the supplied row. An empty source row is retained;
`null` omits only that source row, not the generated row or eligible control.

Each wrapper render passes the same `RenderContext` to the supplied row exactly once,
including a custom `render(RenderContext)` override. Rendering through `Template` retains
this dispatch and resolves changing context values rather than flattening the source renderer.
The wrapper does not mutate the supplied row's structure; custom renderers and descendants
remain responsible for their own behavior. The wrapper also leaves its own stored attributes,
children and registration configuration unchanged during rendering and compilation. Generic
inherited children remain stored but are not emitted. Templates keep this renderer live, including
later registration, gate, mode and URL changes. Finish configuration before sharing; registered
modules, source delegates and contexts must independently meet their lifecycle/sharing requirements.

Configuration methods:

- `addEditableModule(module, moduleId)`: appends a registration in order and assigns that id
  to the underlying module. It throws `IllegalStateException` if capacity is reached.
- `withMaxModules(max)`: sets registration capacity (default `3`, minimum `1`). Source
  children do not count toward this capacity.
- `withEditMode(mode)`: sets the generated module controls' edit mode (default `OWNER_EDIT`).
- `withCanAddModule(canAdd)`: permits or hides the add control (default `true`); it does not
  gate the registered modules' edit/delete controls or enforce server authorization.

Generated columns use integer width `12 / registeredModuleCount`; counts above twelve use
fill columns. Source columns retain their configured widths. Three independent setters
configure generated endpoint conventions:

| Setter | Default | Supported tokens |
|---|---|---|
| `withModuleEditUrlTemplate(template)` | `/api/pages/{pageId}/modules/{moduleId}/edit` | `{pageId}`, `{rowId}`, `{moduleId}` |
| `withModuleDeleteUrlTemplate(template)` | `/api/pages/{pageId}/modules/{moduleId}/delete` | `{pageId}`, `{rowId}`, `{moduleId}` |
| `withAddModuleFormUrlTemplate(template)` | `/api/pages/{pageId}/rows/{rowId}/add-module-form` | `{pageId}`, `{rowId}` |

Setters check the template immediately. Render validates and encodes only ids actually used
by generated URLs. No registered modules means no module URLs; a hidden or capacity-blocked
add control does not expand its template. Default module URLs do not use `rowId`.

The edit mode is appended to edit/delete URLs as `editMode`, **before** any fragment. Edit
and add controls target `#edit-modal-container` with `innerHTML`; delete targets its generated
module wrapper with `outerHTML`. Applications supply these endpoints and enforce authorization,
CSRF and business validation. Wrapper controls alone do not grant permission.

A row-scoped route configuration, independent of page insertion:

```java
package example;

import io.mindspice.simplypages.editing.EditablePage;
import io.mindspice.simplypages.editing.EditableRow;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.ContentModule;

public final class EditingRoutes {
    public static EditablePage page() {
        String pageId = "tenant/a"; // raw business id, not "tenant%2Fa"
        EditableRow row = EditableRow.wrap(new Row(), "row-1", pageId)
                .withModuleEditUrlTemplate("/editor/{pageId}/{rowId}/{moduleId}/edit")
                .withModuleDeleteUrlTemplate("/editor/{pageId}/{rowId}/{moduleId}/delete")
                .withAddModuleFormUrlTemplate("/editor/add?page={pageId}&row={rowId}")
                .addEditableModule(ContentModule.create().withContent("Body"), "module-1");
        return EditablePage.create(pageId)
                .withInsertRowUrlTemplate("/editor/insert?page={pageId}&position={position}")
                .addEditableRow(row);
    }
}
```

This emits `tenant%2Fa` wherever the page token occurs. It does not register server routes
or establish that a server accepts encoded slashes.

## URL template and identifier rules

These rules apply to the four page/row template setters and modal child templates:

- Tokens occupy **whole path segments or whole query parameter values**. Scheme, authority,
  query-key, fragment and partial-segment/value placements are rejected. Unknown tokens or
  unmatched braces are rejected. New page/row template setters also reject `null`.
- In the browser-relevant structure, token-bearing absolute URLs use
  `scheme://nonempty-literal-authority`; protocol-relative URLs use
  `//nonempty-literal-authority`. Root-relative paths, relative paths and query-only
  references are supported. Scheme shorthands (`https:{id}`, `https:/{id}`), empty-authority
  forms (`https:///{id}`, `///{id}`), and opaque schemes such as templated `mailto:` are not
  supported. This is a template grammar, not a scheme or destination allowlist.
- Placement is checked against both the literal template and browser-relevant structure:
  leading/trailing ASCII C0 controls/spaces are trimmed, TAB/LF/CR are ignored, and path
  backslashes are treated as separators **for classification only**. For example,
  ` https://{id}/edit`, `h<TAB>ttps://{id}/edit` and `\\/{id}/edit` are rejected because browsers
  would put the id in the authority. Harmless literal variants remain byte-for-byte unchanged.
- Repeated or omitted tokens and fixed URLs are supported. An empty fixed URL contains no
  tokens; it does not validate ids. Fixed URLs do not acquire this structural validation,
  including non-hierarchical URLs. Literal URL portions remain exactly as configured.
- Expansion makes one pass over the original template. Braces inside an id are data, not
  additional tokens. The framework does not normalize literal URL text or encode the whole URL.
- Ids are raw opaque values. UTF-8 bytes other than RFC 3986 unreserved characters
  (`A-Z a-z 0-9 - . _ ~`) are percent-encoded. Spaces become `%20`, not `+`. Slash, backslash,
  `?`, `#`, quotes and braces are encoded. `a%2Fb` becomes `a%252Fb`, not `a%2Fb`.
- Used ids must be non-null, nonempty, not whitespace/space-character-only, not exactly `.`
  or `..`, and well-formed UTF-16. Supplementary Unicode is supported. Mixed nonblank values
  retain surrounding spaces; no trimming, case folding, Unicode normalization or decoding occurs.
- Invalid ids throw `IllegalArgumentException` only at actual interpolation (page/row render,
  or modal child-action build). Omitted tokens and disabled/absent actions do not introduce
  descriptor-wide validation. DOM ids and CSS selectors are not changed by this URL policy.

**Migration:** framework-generated page/row and child URLs now encode raw ids. Do not pass
pre-encoded ids; literal percent signs are deliberately encoded again. Exact dot ids are rejected
because browsers normalize dot path segments even when their dots are percent-encoded. Partial
legacy child-token placements must move to full segments or query values.

Literal URL configuration is trusted application input. Applications own route mappings,
encoded-slash acceptance, the number of decoding steps, business-id lookup, authorization,
CSRF and endpoint policy. Component encoding does not supply URI allowlisting or certify
server-side request handling. Broad URL/style validation is not part of this contract.

## EditableModule complete URLs

`EditableModule.withEditUrl(url)` and `withDeleteUrl(url)` accept complete caller URLs, not
implicit templates. Braces and existing percent sequences remain untouched. Null/empty URLs
or disabled permission flags hide the respective control.

With a non-null edit mode, the wrapper appends `editMode=OWNER_EDIT` or `editMode=USER_EDIT`
to the pre-fragment query. It retains all existing parameters, including an existing `editMode`,
and preserves the fragment exactly. Empty queries and trailing `?`/`&` receive no extra separator.
For example, `/edit?tab=details#section?note` becomes
`/edit?tab=details&editMode=OWNER_EDIT#section?note`. A null mode leaves the complete URL unchanged.

## SimpleListModule item identity and lifecycle

`findItem(id)` returns the first item with a **non-null raw** `ListItem.getId()` exactly equal to
a non-null query. `removeItem(id)` removes every such duplicate. Null queries match/remove
nothing; unassigned ids are skipped. Non-null spelling is literal, including empty strings,
spaces, case and Unicode. No trimming, normalization or descriptor fallback occurs.

`getEditableChildren()` remains model-derived. An unassigned raw id receives a positional
`item-N` descriptor without assigning that id to the item. Descriptors can renumber and can
collide with caller-assigned raw ids; they are not stable persistence identities. `getItems()`
returns a copied list, not the mutable model collection.

Adding/removing items is configuration-only: it changes the model and current descriptors but
does not build or rebuild an already-built child tree. Configure before building. `applyEdits`
explicitly rebuilds, making current model changes visible in markup. Applications own their
editing lookup/persistence policy and must not infer it from these raw convenience methods.

## ModuleEditHandler<T>

Service-layer contract for edit, update, and delete handlers.

- `renderEditForm(String moduleId)`
- `handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode)`
- `handleDelete(String moduleId, EditMode editMode)`

## AuthWrapper

Authorization wrappers:

- `require(...)`
- `requireForEdit(...)`
- `requireForDelete(...)`
- `requireForCreate(...)`

## AuthorizationChecker

App-owned permission contract:

- `canEdit(String moduleId, String userId)`
- `canDelete(String moduleId, String userId)`
- `getEditMode(String moduleId, String userId)`
