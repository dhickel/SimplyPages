# User Craftable Pages (Profiles)

"User Craftable Pages" allow end-users to construct their own page layouts, typical for User Profiles or Dashboards.

## Core Concepts

1.  **Fixed Shell**: The application provides the navigation and outer shell.
2.  **User Content Area**: A specific region where users can add/remove/arrange modules.
3.  **Module Palette**: A set of allowed modules users can choose from.

## Building a Profile Page

### 1. The Container

Start with a standard `Page` or `Grid`.

```java
Page profilePage = new Page();
```

### 2. User Identity

Use `AccountWidget` or a custom Header module to display user info.

```java
HeroModule header = HeroModule.create()
    .withTitle(user.getDisplayName())
    .withDescription(user.getBio());
```

### 3. Editable Layouts

SimplyPages provides `EditableRow` and `EditablePage` (experimental) to assist with managing user-defined layouts.

For a profile, you might store the layout as a JSON definition in your database:

```json
{
  "rows": [
    {
      "columns": [
        { "width": 8, "module": { "type": "PostList", "limit": 5 } },
        { "width": 4, "module": { "type": "Stats", "userId": 123 } }
      ]
    }
  ]
}
```

### 4. Rendering

Iterate through the stored definition and reconstruct the modules.

```java
for (RowDef rowDef : userLayout.getRows()) {
    Row row = new Row();
    for (ColDef colDef : rowDef.getColumns()) {
        Module module = ModuleFactory.create(colDef.getType(), colDef.getConfig());

        // Wrap in EditableModule if viewing own profile
        if (isOwner) {
            row.addColumn(new Column(colDef.getWidth())
                .addModule(EditableModule.wrap(module)));
        } else {
            row.addColumn(new Column(colDef.getWidth()).addModule(module));
        }
    }
    profilePage.addRow(row);
}
```

### 5. Adding New Modules

Provide a "Add Module" button that opens a modal with the Module Palette.
When selected, send an HTMX request to add the module definition to the user's layout and return the new Row/Module.

## Best Practices

*   **Sanitization**: Even if users choose modules, ensure any text input they provide (e.g., custom text widgets) is sanitized.
*   **Quotas**: Limit the number of modules a user can add to prevent DoS.
*   **Caching**: Cache the user's layout definition, not the rendered HTML.
