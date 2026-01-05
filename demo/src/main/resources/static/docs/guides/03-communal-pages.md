# Communal Pages

Communal pages are shared spaces where multiple users contribute, such as Forums, Collaborative Wikis, or Project Dashboards.

## Key Challenges

1.  **Concurrency**: Multiple users editing at once.
2.  **History**: Tracking who changed what.
3.  **Moderation**: Queuing updates for approval.

## Handling Concurrency

As detailed in [State Management](../advanced/01-state-management.md), never share module instances.

For communal pages, use Optimistic Locking in your database entities.

```java
@Version
private Long version;
```

If a user tries to save a stale version, catch the `OptimisticLockException` and prompt them to reload.

## Update Queues (Moderation)

Instead of applying edits immediately, you can queue them.

### Implementation Pattern

1.  **Edit Mode**: Determine if the user has `DIRECT_WRITE` or `PROPOSE` permission.
2.  **Storage**:
    *   `LiveTable`: Stores the current approved content.
    *   `ProposalTable`: Stores pending edits.

### Controller Logic

```java
@PostMapping("/update")
public String update(@RequestParam Map<String, String> formData) {
    if (user.canPublish()) {
        // Update LiveTable
        return "Content Updated";
    } else {
        // Save to ProposalTable
        proposalService.submit(formData);
        return "Changes queued for approval"; // HTMX response
    }
}
```

## Forum Components

SimplyPages includes specific components for forums.

*   `ForumPost`: Displays a main post.
*   `CommentThread`: Manages nested comments.
*   `PostList`: Lists threads or posts.

### Example: Thread View

```java
public class ThreadPage extends Page {
    public ThreadPage(ThreadData thread) {
        // Main Post
        this.addRow(new Row().addColumn(new Column(12)
            .addModule(new ForumPost(thread.getMainPost()))));

        // Comments
        this.addRow(new Row().addColumn(new Column(12)
            .addModule(new CommentThread(thread.getComments()))));
    }
}
```

## History Tracking

For wikis or collaborative docs:

1.  Store every revision in a `History` table.
2.  Use `ComparisonModule` to show diffs between versions.

```java
ComparisonModule diff = ComparisonModule.create()
    .withLeft("Version 1", oldContent)
    .withRight("Version 2", newContent);
```
