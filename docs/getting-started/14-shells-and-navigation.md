# Building Shells and Navigation

The `ShellBuilder` is a core component in SimplyPages that allows you to construct the application layout, including the top navigation, side navigation, user account bar, and main content area.

## Basic Usage

The `ShellBuilder` uses a fluent API to configure the application shell.

```java
ShellBuilder shell = new ShellBuilder()
    .withTitle("My Application")
    .withSideNav(mySideNav)
    .withTopNav(myTopNav)
    .withAccountBar(myAccountBar);
```

## Side Navigation

The `SideNav` component is crucial for application navigation. It supports nested items, icons, and HTMX integration.

### Creating a SideNav

```java
SideNav sideNav = new SideNav()
    .addItem(new SideNavItem("Dashboard", "/dashboard", "icon-dashboard"))
    .addItem(new SideNavItem("Users", "/users", "icon-users")
        .addChild(new SideNavItem("List", "/users/list"))
        .addChild(new SideNavItem("Create", "/users/create"))
    );
```

### HTMX Integration

To ensure smooth navigation without full page reloads, use `withHxSwap`.

```java
// Configure for innerHTML swap and scroll to top
sideNav.withHxSwap("innerHTML show:top");
```

This ensures that when a link is clicked, the content is swapped into the main area and the view scrolls to the top.

## Top Navigation

The `TopNav` component provides horizontal navigation at the top of the page.

```java
TopNav topNav = new TopNav()
    .addLink("Home", "/")
    .addLink("About", "/about");
```

## Account Bar

The `AccountBar` displays user information and actions like profile links and logout.

```java
AccountBar accountBar = new AccountBar()
    .withUser(currentUser)
    .addMenuItem("Profile", "/profile")
    .addMenuItem("Settings", "/settings")
    .addMenuItem("Logout", "/logout");
```

## Example: Complete Shell Construction

```java
@GetMapping("/")
public String home() {
    SideNav sideNav = new SideNav()
        .addItem(new SideNavItem("Home", "/", "fa-home"))
        .withHxSwap("innerHTML show:top");

    return new ShellBuilder()
        .withTitle("Demo App")
        .withSideNav(sideNav)
        .withContent(new HomePage())
        .render();
}
```
