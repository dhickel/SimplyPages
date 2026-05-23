# Remediation Plan: Slot Key Refactor (Compiled Slot Pattern)

## 1. Executive Summary
This plan outlines a refactor of the Slot Key and Template system to support a "Compiled Slot" pattern. By introducing an Algebraic Data Type (ADT) for slot entries, we allow the framework to distinguish between dynamic content that needs rendering and pre-compiled HTML that can be appended directly. This addresses performance bottlenecks in complex component trees and simplifies state management for partial updates, while adhering to the framework's "low-bullshit" philosophy by giving control to the end-user.

## 2. Core Concepts & Terminology

We will introduce two invariants for slot entries:

*   **Live Slot (Standard)**: The existing behavior. The slot contains a raw data object (String, Integer, etc.) or a `Component`. It is rendered on every request, ensuring it always reflects the latest state but incurring a rendering cost.
*   **Compiled Slot (Cached)**: The "Baked" variant. The slot contains a pre-rendered HTML string. The renderer appends this string directly to the output buffer without any tree traversal, escaping, or logic execution.

## 3. Proposed Approach

### 3.1 Slot Entry ADT
Introduce a `SlotEntry` sum type (or update `TypedValue`) to represent these two states. Since we are using Java 17, we can use sealed interfaces/records for a clean internal implementation.

```java
public sealed interface SlotEntry permits LiveEntry, CompiledEntry {
    Object getValue();
    boolean isCompiled();
}
```

### 3.2 Enhanced `SlotKeyMap`
`SlotKeyMap` will serve as the primary container for these entries and the main interface for users to manage cached state.

*   **Storage**: Internal storage will move from `Map<String, TypedValue>` to `Map<String, SlotEntry>`.
*   **New API**:
    *   `putCompiled(String name, String html)`: Manually inject pre-rendered HTML.
    *   `isCompiled(String name)`: Check the state of a specific slot.
    *   `getCompiled(String name)`: Retrieve the baked HTML if present.
*   **Unification**: Bridge the gap between `String` names and `SlotKey<T>` by adding overloads that accept `SlotKey`.

***DEV NOTE: Since we have an adt we should need to have if checks and branching, we can just switch on the adt type***


### 3.3 `Template` Rendering Flow
The `Template` engine will be updated to handle the selection between Live and Compiled entries.

*   **New Render Signature**: `render(SlotKeyMap map, boolean autoCompile)`
*   **Logic**:
    1.  For each `SlotSegment` encountered:
    2.  Check the `SlotKeyMap` for an entry matching the segment's key.
    3.  If it is a `CompiledEntry`: Append the pre-rendered HTML directly.
    4.  If it is a `LiveEntry` (or missing):
        -   Render the value (applying `Encode.forHtml()` for strings or `.render(ctx)` for components).
        -   If `autoCompile` is `true`: Call `map.putCompiled(key, renderedHtml)` to store the result back into the map.
        -   Append the rendered HTML.

***DEV QUESTION: Should we compile beforehand or keep this internal to render?***


### 3.4 User-Managed Caching
This design moves cache management to the user. They can store a `SlotKeyMap` in a session, a distributed cache, or a local variable. They decide when to "bake" the slots by passing `autoCompile = true` and when to "invalidate" by putting a new `Live` value into the map.

## 4. Targets & Scope

***DEV QUESTION: Does RenderContext store the slot keymap? How do these work together?***

*   **`io.mindspice.simplypages.core.SlotKeyMap`**: Primary target for data storage refactor.
*   **`io.mindspice.simplypages.core.RenderContext`**: Update to carry the `SlotEntry` metadata to the renderer.
*   **`io.mindspice.simplypages.core.Template`**: Update `SlotSegment` and `TextSlotSegment` logic.
*   **`io.mindspice.simplypages.core.TypedValue`**: Evaluate if this should be replaced by or wrapped by `SlotEntry`.
*   **`io.mindspice.simplypages.components.RawHtml`**: Ensure this component is used as the underlying representation for `CompiledEntry` to maintain the "everything is a component" consistency.

## 5. Expected Behavior

*   **Initial Render**: With `autoCompile = true`, the first render will perform full tree traversal for all slots and then update the `SlotKeyMap` with the resulting HTML.
*   **Subsequent Renders**: The renderer will find `CompiledEntry`s and perform simple string concatenation, resulting in significant performance gains for complex modules.
*   **Partial Updates**: Users can update only specific slots in the `SlotKeyMap` while keeping others compiled. The template will only re-render the updated/live slots.
*   **No Magic**: The system only compiles if explicitly told to via the `autoCompile` boolean, maintaining the framework's predictable nature.

## 6. Edge Cases & Risks

*   **Nested Templates**: When a slot contains a `TemplateComponent`, the `autoCompile` flag should propagate or the `TemplateComponent` should handle its own compilation.
*   **Context Dependency**: If a component's rendering depends on other values in the `RenderContext` (besides its own slot), caching its HTML might lead to stale results if the other values change. Documentation must warn that Compiled Slots should be self-contained.
*   **Thread Safety**: While `SlotKeyMap` is mutable, the framework convention is that it is request-scoped or session-scoped. Multi-threaded mutation of the map during `autoCompile` must be considered (e.g., using `ConcurrentHashMap` internally if necessary, though preferred to keep it simple and document it as thread-local).
*   **Generic Erasure**: Ensure that `SlotKey<T>` can still be used to retrieve the original `Live` object if the user needs it for logic before rendering.

## 7. Implementation Steps

1.  **Define `SlotValue` ADT** (Internal variants: `Live`, `Compiled`).
2.  **Refactor `SlotKeyMap`** to use `SlotValue` and add compilation methods.
3.  **Update `RenderContext`** to expose `SlotValue` retrieval.
4.  **Modify `Template.SlotSegment`** to implement the "Check Compiled -> Render -> Optional Store" logic.
5.  **Add `Template.render(SlotKeyMap, boolean)`** as the public entry point for this feature.
6.  **Add unit tests** specifically for the transition from Live to Compiled state and the resulting HTML consistency.
