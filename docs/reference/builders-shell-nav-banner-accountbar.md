[Previous](components-and-modules-catalog.md) | [Index](../INDEX.md)

# Builders Reference: Shell, Navigation, Banner, Account Bar

## ShellBuilder

`ShellBuilder` creates full app shell HTML.

Core options:

- `withTopBanner(...)`
- `withAccountBar(...)`
- `withSideNav(...)`
- `withContentTarget(...)`
- `withPageTitle(...)`
- `withHtmx(boolean)`
- `withCustomCss(...)`

Returns full HTML document string from `build()`.

## SideNavBuilder and TopNavBuilder

Use nav builders to produce stable navigation components.

Guidance:

1. Keep labels and URLs as data, not hardcoded in controllers.
2. Keep active-state logic deterministic.

## BannerBuilder

Use for app or area-level brand/title/banner composition.

## AccountBarBuilder

Use for account context links and utility actions.

## Minimal Shell Example

```java
String shell = ShellBuilder.create()
    .withPageTitle("Admin Portal")
    .withTopBanner(BannerBuilder.create()
        .withTitle("Admin Portal")
        .build())
    .withSideNav(SideNavBuilder.create()
        .addSection("Main")
        .addLink("Dashboard", "/dashboard", "")
        .build(), true)
    .build();
```
