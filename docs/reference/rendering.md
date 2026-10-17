# Rendering API and contracts

Package: `io.mindspice.simplypages.core`.

| API | Contract |
| --- | --- |
| `Component.render(RenderContext)` | Required render method; returns HTML. |
| `Component.render()` | Renders with a fresh empty context. |
| `Component.render(Map)` | Converts values to a context and renders. |
| `Component.compile()` | Shortcut for `Template.of(component)`. |
| `HtmlTag.withInnerText(String)` | Escapes text; null becomes empty; clears a text slot. |
| `HtmlTag.withInnerText(SlotKey<String>)` | Resolves text from the render context. |
| `HtmlTag.withUnsafeHtml(String)` | Inserts trusted HTML and clears the previous text slot. |
| `HtmlTag.withAttribute(name, value)` | Replaces the same name case-insensitively; encodes the value. |
| `HtmlTag.withId(null)` | Removes the ID. |
| `HtmlTag.addStyle(property, value)` | Replaces the exact CSS property; rejects declaration breakout. |
| `HtmlTag.addTrustedStyle(property, value)` | Accepts trusted advanced CSS values; still validates the property name. |
| `Module.build()` | Lazy and idempotent after success. Failed builds restore child/attribute collections before retry. |
| `Module.withWidth/withMinWidth/withMaxWidth` | Always throw `UnsupportedOperationException`; size modules through layout. |

Tag and attribute names reject malformed markup delimiters. Attribute values that are empty
or null render as boolean attributes. Generic attributes do not validate JavaScript, URL
schemes, or business policy; attribute escaping does not make those contexts safe.

### URL and style method boundaries

`Link` constructors/`create`/`withHref` apply the http/https/mailto/tel/relative policy.
`Image` constructors/factories/`withSrc` reject javascript and non-image data prefixes,
retaining custom/FTP/blob schemes and the existing `data:image/` prefix behavior. For
classification only, both trim boundary C0/ASCII space, remove TAB/LF/CR and fold ASCII
case independently of locale. Normalized supported spellings such as `ht<TAB>tp:` and
`data:im<TAB>age/` are accepted. Storage/getters and encoded output keep the original value;
there is no entity/percent decoding, Unicode normalization, MIME/payload inspection or
destination authorization. Rejected typed setters leave prior state unchanged.

Generic href/src replacement bypasses those validators and need not update their getters.
HTMX endpoints, Dropdown/AccountWidget/nav entries (including nested constructors/utility
links), top/side builder links and aliases, Hero CTAs, Card/Gallery images, Audio/Video
sources/posters and Form/FormModule destinations remain application-controlled or separately
validated for their actual context. AccountBar's link conveniences already construct Link;
Banner's generated ordinary horizontal image constructs Image at build. Neither delegates
CSS backgrounds or dynamic endpoints to those policies.

`addStyle` supplies property-name/declaration-breakout checks, not semantic CSS/URL policy.
`addTrustedStyle` bypasses value-breakout checks; `Style.addInlineStyle` and raw style
attributes retain separators. Banner/Hero CSS background URLs need control of single-quoted
`url()` syntax, not merely Image validation. Their colors, AccountBar background color,
`Spacer.custom` and `ProgressBar.withHeight` are raw style assembly. Supply application-controlled
or separately CSS-context-validated inputs; width helpers' restricted token grammar is a
different contract. Compilation adds no URL/CSS checks and standard static snapshots retain
the configuration present at compile time.

## Keys and values

The `SlotKey<T>` canonical constructor and all `SlotKey.of` factories throw
`NullPointerException` immediately for a null name. Every non-null name is preserved literally,
including empty and blank strings, without trimming, case folding, or Unicode normalization.

Equality and hash code use the name, not the generic type or default provider. Keep a single
declared type for each name; generic typing does not validate differently typed keys that reuse
a name. Keys may have fixed defaults or defaults computed from a context. Default providers
may be null and are not evaluated during construction.

`Slot.of(key)` renders components by calling `render(context)` and escapes other values as
text. Missing values render empty unless a default exists. Explicit compiled values are
trusted HTML, including when used by a text slot.

## Context policy

`RenderContext` is mutable and not thread-safe. Create one per request.

| Operation or policy | Effect |
| --- | --- |
| `put(key, value)` | Stores a live value; null removes it. |
| `putCompiled(key, html)` | Stores trusted rendered HTML; null removes it. |
| `remove(key)` / `clear()` | Removes one or all entries. |
| `get(key)` | Returns a live value or key default. A compiled entry falls back to the default, not cached HTML. |
| `getCompiled(key)` | Returns cached HTML when present. |
| `NEVER_COMPILE` | Does not create new cached HTML; existing compiled entries still render. |
| `COMPILE_ON_FIRST_HIT` | Replaces explicitly stored live slot values with their first rendered HTML. |

Direct slots, text slots, and template slots honor compiled entries. Defaults are evaluated
live and are not automatically persisted. Replacing one input does not invalidate other
slots that were compiled using it; use a fresh context when dependencies change.

## Template compilation

Standard tags flatten into static strings and dynamic slot segments. Compilation respects
`getChildrenStream()`, including generated options, cards, lists, and table structure.
Modules build before compilation. A subclass overriding `render(RenderContext)` remains a
live component delegate so custom wrappers, authorization, and request logic still execute.
Other custom components also remain live delegates.

Templates therefore do not deep-copy or freeze arbitrary components. Reuse a template only
when its retained delegates are safe to share; use pre-rendered strings for fully immutable
static output. Implement custom rendering in `render(RenderContext)`, including propagation
to children. Overriding only `render()` does not affect context-aware parent rendering.

`TemplateComponent` binds a template to a fixed context and ignores the parent's context.
`ShellTemplate` adds a document doctype and exposes a content slot through `contentSlot()`.

See [rendering explanation](../explanation/rendering-and-lifecycle.md) and
[the original slot reference](../core/03-template-rendercontext-slotkey-reference.md).

[All reference](README.md)
