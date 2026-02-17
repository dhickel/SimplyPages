[Previous](01-security-boundaries-and-safe-rendering.md) | [Index](../INDEX.md)

# AuthWrapper and AuthorizationChecker Integration

Use these utilities to standardize authorization handling around edit operations.

## AuthorizationChecker Contract

Implement checker methods with your domain rules.

```java
public class PageAuthorizationChecker implements AuthorizationChecker {
    @Override
    public boolean canEdit(String resourceId, String username) {
        return aclService.userCanEdit(resourceId, username);
    }

    @Override
    public boolean canDelete(String resourceId, String username) {
        return aclService.userCanDelete(resourceId, username);
    }

    @Override
    public EditMode getEditMode(String resourceId, String username) {
        return aclService.userGetsOwnerEdit(resourceId, username)
            ? EditMode.OWNER_EDIT
            : EditMode.USER_EDIT;
    }
}
```

## Wrap Endpoints with AuthWrapper

```java
@GetMapping("/modules/{id}/edit")
@ResponseBody
public String edit(@PathVariable String id, Principal principal) {
    return AuthWrapper.requireForEdit(
        () -> authChecker.canEdit(id, principal.getName()),
        () -> editUiService.buildEditModal(id)
    );
}
```

```java
@DeleteMapping("/modules/{id}")
@ResponseBody
public String delete(@PathVariable String id, Principal principal) {
    return AuthWrapper.requireForDelete(
        () -> authChecker.canDelete(id, principal.getName()),
        () -> moduleService.deleteAndRenderResponse(id)
    );
}
```

## Practical Guidance

1. Keep checker logic in service layer, not in components.
2. Keep error messaging consistent.
3. Log authorization denials with request context.
