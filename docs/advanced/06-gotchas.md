# Gotchas and Common Pitfalls

Even with the best tools, it's easy to make mistakes. Here are some common "gotchas" in SimplyPages and how to avoid them.

## 1. The Singleton Controller Trap

**Mistake:** Storing `Page` or `Module` instances as fields in a Spring `@Controller`.

```java
@Controller
public class BadController {
    private Page myPage = new Page(); // ❌ WRONG! Shared by all users.

    @GetMapping("/")
    public String index() {
        return myPage.render();
    }
}
```

**Consequence:** User A sees User B's data. Concurrent modification exceptions.
**Fix:** Always create new instances inside the request method.

## 2. Fluent Method Ordering in Subclasses

**Mistake:** Calling subclass-specific methods *after* superclass methods in a fluent chain.

```java
// Assuming CustomTag extends HtmlTag
CustomTag tag = new CustomTag()
    .withAttribute("id", "foo") // Returns HtmlTag
    .withCustomFeature();       // ❌ Compile Error: method not found on HtmlTag
```

**Fix:** Call subclass methods first, or override methods to return the subclass type (covariant return types).

```java
CustomTag tag = new CustomTag()
    .withCustomFeature()
    .withAttribute("id", "foo"); // ✅ Works
```

## 3. HTMX Swaps and ID Conflicts

**Mistake:** Returning an element with `hx-swap="outerHTML"` but the ID doesn't match the target.

**Consequence:** The element might disappear, be duplicated, or the swap might fail silently.
**Fix:** Ensure the ID of the returned component matches the `hx-target` ID when using `outerHTML`.

## 4. `row.addModule()` vs `column.addModule()`

**Mistake:** Trying to add a module directly to a `Row` without a `Column`.

**Consequence:** Modules need a width wrapper.
**Fix:** Always wrap modules in a `Column`.

```java
row.addColumn(new Column(12).addModule(module));
```

## 5. Caching Fully Built Components

**Mistake:** Caching a `Table` component to improve performance.

**Consequence:** Sorting/Filtering state gets stuck. Validation errors persist across requests.
**Fix:** Cache the *data* (List<User>), not the *component*. Rebuild the table with the data on each request.

## 6. Forgot `buildContent()` Logic

**Mistake:** Putting layout logic in `render()` but not setting `rendered = true`, or calling `buildContent()` multiple times.

**Consequence:** Duplicate children appended on every render.
**Fix:** Use the `rendered` flag in `Module` or `Component` if you are lazy-loading children. Ideally, build structure in the constructor or configuration phase.

## 7. `withValue()` on Text Area

**Mistake:** Using `withValue(Slot.of(key))` on a `TextArea`.

**Consequence:** `TextArea` typically treats value as inner text.
**Fix:** Use `withChild(Slot.of(key))` for `TextArea` dynamic content.

## 8. Missing Form Labels

**Mistake:** Creating inputs without labels.

**Consequence:** Accessibility issues and poor styling.
**Fix:** Always ensure inputs have associated labels with matching `for`/`id` attributes.

## 9. Z-Index Wars with Modals

**Mistake:** Creating multiple modal containers.

**Consequence:** Modals appearing under backdrops.
**Fix:** Use the single `#edit-modal-container` pattern described in the documentation.

## 10. `SlotKey` Generics

**Mistake:** `SlotKey.of("name")` without type hint.

**Consequence:** Compiler warnings or runtime casting errors.
**Fix:** `SlotKey.<String>of("name")`.
