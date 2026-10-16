package io.mindspice.simplypages.security;

import io.mindspice.simplypages.builders.AccountBarBuilder;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.Dropdown;
import io.mindspice.simplypages.components.Spacer;
import io.mindspice.simplypages.components.display.Card;
import io.mindspice.simplypages.components.display.ProgressBar;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forum.comments.DefaultForumCommentComponent;
import io.mindspice.simplypages.components.media.Audio;
import io.mindspice.simplypages.components.media.Gallery;
import io.mindspice.simplypages.components.media.Video;
import io.mindspice.simplypages.components.navigation.Breadcrumb;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.components.navigation.NavBar;
import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.modules.FormModule;
import io.mindspice.simplypages.modules.HeroModule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Preservation tests, not a request to extend Link/Image policy to raw helpers. */
@SuppressWarnings("deprecation")
class TrustedInputBoundaryTest {
    private static final String URL = "javascript:void(0)?a=\"value\"&b=<raw>";
    private static final String COLOR = "red; --application-owned: retained";
    private static final String BACKGROUND = "image'); --application-owned: url('other";

    private static Document output(Component component) {
        String compiled = component.compile().render(RenderContext.empty());
        String direct = component.render();
        assertEquals(direct, compiled);
        assertEquals(direct, component.render());
        assertFalse(direct.contains("a=\"value\""));
        return Jsoup.parseBodyFragment(direct);
    }

    private static void attributes(Document doc, String selector, String name, String expected, int count) {
        var nodes = doc.select(selector);
        assertEquals(count, nodes.size(), selector);
        nodes.forEach(node -> assertEquals(expected, node.attr(name), selector + "/" + name));
    }

    @Test
    void dropdownAndAllAccountDestinationsRemainRawEncodedValues() {
        attributes(output(Dropdown.create("Menu").addLink("<label>", URL).build()), "a", "href", URL, 1);
        var guest = output(AccountWidget.createGuest().withLoginUrl(URL).withSignupUrl(URL));
        attributes(guest, "a", "href", URL, 2);
        assertEquals(0, output(AccountWidget.createGuest().withSignupUrl(null)).select(".account-widget-signup").size());
        attributes(output(AccountWidget.createAuthenticated("<name>").withProfileUrl(URL).withLogoutUrl(URL)),
            "a", "href", URL, 2);
        attributes(output(AccountWidget.createDynamic(URL)), "[hx-get]", "hx-get", URL, 1);
    }

    @Test
    void navigationEntriesUtilityLinksAndPublicNestedConstructorsRetainTheirOwnPolicies() {
        var nav = NavBar.create().addItem("one", URL).addItem("two", URL, true)
            .addItem(new NavBar.NavItem("three", URL).withHxGet(URL))
            .addItem(new NavBar.NavItem("four", URL, true).withHxGet(URL))
            .addUtilityLink("utility", URL).addUtilityLink("active", URL, true);
        var document = output(nav);
        attributes(document, "a", "href", URL, 6);
        attributes(document, "a[hx-get]", "hx-get", URL, 2);
        assertEquals("one", document.select("a").first().text());
        assertEquals("active", document.select("a").last().text());
        attributes(output(NavBar.create().addUtilityLink(null, null)), "a", "href", "", 1);
        var side = SideNav.create().addItem("one", URL).addItem("two", URL, true)
            .addItem(new SideNav.NavItem("nested", URL, false).withHxGet(URL));
        attributes(output(side), "a", "href", URL, 3);
        attributes(output(side), "a[hx-get]", "hx-get", URL, 1);
        var crumbs = output(Breadcrumb.create().addItem("one", URL).addItem("plain", null).addActiveItem("current"));
        attributes(crumbs, "a", "href", URL, 1);
        assertEquals(3, crumbs.select("li").size());
    }

    @Test
    void topAndSideBuilderOverloadsAliasesAndDynamicEndpointsRemainUnrestricted() {
        var top = TopNavBuilder.create().addPrimaryLink("one", URL).addPrimaryLink("two", URL, true)
            .addUtilityLink("three", URL).addUtilityLink("four", URL, true)
            .addPortal("five", URL).addPortal("six", URL, true).withDynamicAccountWidget(URL).build();
        attributes(output(top), "a", "href", URL, 6);
        attributes(output(top), "[hx-get]", "hx-get", URL, 7);
        var side = SideNavBuilder.create().addLink("one", URL).addLink("two", URL, true)
            .addLink("three", URL, "icon").addLink("four", URL, true, "icon").build();
        attributes(output(side), "a", "href", URL, 4);
        attributes(output(side), "a", "hx-get", URL, 4);
        attributes(output(TopNavBuilder.create().addUtilityLink(null, null).build()), "a", "href", "", 1);
        assertEquals(0, output(TopNavBuilder.create().withHtmxNavigation(false).addPrimaryLink("one", URL).build())
            .select("[hx-get]").size());
    }

    @Test
    void rawImageAndMediaHelpersDoNotAcquireImageOrLinkValidation() {
        attributes(output(Card.create().withImage(URL, "<alt>")), "img", "src", URL, 1);
        var gallery = output(Gallery.create().addImage(URL, "alt").addImage(URL, "alt", "<caption>"));
        attributes(gallery, "img", "src", URL, 2);
        assertEquals("<caption>", gallery.selectFirst(".gallery-caption").text());
        for (Audio audio : new Audio[] {new Audio(URL), Audio.create(URL)}) {
            attributes(output(audio), "audio", "src", URL, 1);
        }
        for (Video video : new Video[] {new Video(URL), Video.create(URL)}) {
            attributes(output(video.withPoster(URL)), "video", "src", URL, 1);
            attributes(output(video), "video", "poster", URL, 1);
        }
    }

    @Test
    void formActionsHtmxCsrfAndModuleSubmitKeepApplicationOwnedUrls() {
        Form form = Form.create().withAction(URL).withHxGet(URL).withHxPost(URL).withHxPut(URL).withHxDelete(URL)
            .withCsrfToken("supplied-token").withHxPostCsrf(URL, "supplied-token");
        var doc = output(form);
        for (String name : new String[] {"action", "hx-get", "hx-post", "hx-put", "hx-delete"}) {
            attributes(doc, "form", name, URL, 1);
        }
        assertEquals("{\"X-CSRF-TOKEN\": \"supplied-token\"}", doc.selectFirst("form").attr("hx-headers"));
        assertEquals("supplied-token", doc.selectFirst("input[name=_csrf]").attr("value"));
        attributes(output(FormModule.create().withSubmitUrl(URL)), "form", "hx-post", URL, 1);
        Link endpoint = Link.create("/validated", "link").withHxGet(URL).withHxPost(URL);
        attributes(output(endpoint), "a", "hx-get", URL, 1);
        attributes(output(endpoint), "a", "hx-post", URL, 1);
    }

    @Test
    void heroCtasAndRawCssAssemblyRetainLiteralSyntaxAndFallbacks() {
        var hero = output(HeroModule.create().withPrimaryButton("primary", URL).withSecondaryButton("secondary", URL)
            .withBackgroundImage(BACKGROUND).withBackgroundColor(COLOR));
        attributes(hero, "a", "href", URL, 2);
        assertEquals("background-image: url('" + BACKGROUND + "'); background-size: cover; background-position: center;"
            + "background-color: " + COLOR + ";", hero.selectFirst(".hero-module").attr("style"));
        attributes(output(HeroModule.create().withPrimaryButton("primary", null).withSecondaryButton("secondary", null)),
            "a", "href", "#", 2);
        var banner = output(BannerBuilder.create().withLayout(BannerBuilder.BannerLayout.IMAGE_OVERLAY)
            .withBackgroundImage(BACKGROUND).withBackgroundColor(COLOR).withTextColor(COLOR).build());
        assertEquals("background-color: " + COLOR + ";color: " + COLOR + ";background-image: url('" + BACKGROUND
            + "');background-size: cover;background-position: center;", banner.selectFirst(".banner").attr("style"));
        var account = output(AccountBarBuilder.create().withBackgroundColor(COLOR).addRightAccountWidget(URL).build());
        assertEquals("background-color: " + COLOR, account.selectFirst(".account-bar").attr("style"));
        attributes(account, "[hx-get]", "hx-get", URL, 1);
        assertEquals("height: 10px; --application-owned: retained;", output(Spacer.vertical()
            .custom("10px; --application-owned: retained")).selectFirst(".spacer").attr("style"));
        assertEquals("width: 10px; --application-owned: retained;", output(Spacer.horizontal()
            .custom("10px; --application-owned: retained")).selectFirst(".spacer").attr("style"));
        assertEquals("height: 10px; --application-owned: retained;", output(ProgressBar.create(25)
            .withHeight("10px; --application-owned: retained")).selectFirst(".progress").attr("style"));
    }

    @Test
    void existingLinkAndImageDelegationRetainsItsOriginalTimingAndAvatarFallback() {
        AccountBarBuilder account = AccountBarBuilder.create();
        assertThrows(IllegalArgumentException.class, () -> account.addLeftLink("bad", "java\tscript:x"));
        assertThrows(IllegalArgumentException.class, () -> account.addRightLink("bad", "custom:x"));
        var links = output(account.addLeftLink("left", "ht\ttp:/x").addRightLink("right", "MAIL\rTO:x").build());
        attributes(links, ".account-bar-left a", "href", "ht\ttp:/x", 1);
        attributes(links, ".account-bar-right a", "href", "MAIL\rTO:x", 1);
        BannerBuilder banner = BannerBuilder.create().withImage("java\nscript:x", "alt");
        assertThrows(IllegalArgumentException.class, banner::build);
        assertDoesNotThrow(() -> BannerBuilder.create().withLayout(BannerBuilder.BannerLayout.CENTERED).withImage(URL, "alt").build());
        attributes(output(BannerBuilder.create().withImage("data:im\tage/gif,x", "alt").build()),
            "img", "src", "data:im\tage/gif,x", 1);
        var avatar = DefaultForumCommentComponent.create().withAuthor("Author").withAvatarUrl("java\rscript:x");
        var snapshot = avatar.compile();
        assertEquals(1, output(avatar).select(".forum-comment-avatar-slot").size());
        assertEquals(0, output(avatar).select("img").size());
        avatar.withAvatarUrl("data:im\tage/gif,x");
        assertEquals(1, output(avatar).select("img").size());
        assertEquals(avatar.render(), snapshot.render(RenderContext.empty()));
        for (String empty : new String[] {null, "", " \t\r\n"}) {
            avatar.withAvatarUrl(empty);
            assertEquals(0, output(avatar).select("img").size());
        }
    }
}
