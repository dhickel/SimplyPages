package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.navigation.Link;
import java.util.*;

/** Supporting contracts are grouped with their working feature, never empty record pages. */
public final class FeatureCatalog {
    public record Contract(String symbol, String feature, String anchor, String docs) {
        public String destination() { return "/demos/features/" + feature + "#" + anchor; }
        public String api() { return "/javadocs/" + symbol.replace('.', '/') + ".html"; }
        public String name() { return symbol.substring(symbol.lastIndexOf('.') + 1); }
    }
    private static final List<Contract> CONTRACTS = new ArrayList<>();
    static {
        group("core", "templates", "typed-slots", "core/03-template-rendercontext-slotkey-reference.md",
            "Component HtmlTag Slot SlotKey RenderContext Template");
        group("core", "templates", "module-entries", "core/03-template-rendercontext-slotkey-reference.md", "Module SlotEntry");
        group("core", "editing", "edit-form-builder", "core/03-template-rendercontext-slotkey-reference.md", "SlotKeyMap TypedValue");
        group("core", "templates", "bound-context", "core/03-template-rendercontext-slotkey-reference.md", "TemplateComponent");
        group("core", "safe-rendering", "url-style-safety", "security/01-security-boundaries-and-safe-rendering.md", "SafeUrl");
        group("core", "safe-rendering", "style-composition", "core/05-css-defaults-overrides-and-structure.md", "Attribute Style CssClassNames");
        group("editing", "editing", "editablepage", "reference/editing-api-reference.md", "EditablePage EditableRow");
        group("editing", "editing", "live-editing-contracts", "reference/editing-api-reference.md", "Editable EditableChild EditMode EditModalBuilder");
        group("editing", "editing", "edit-form-builder", "reference/editing-api-reference.md", "EditFormBuilder FormFieldHelper ValidationResult");
        group("editing", "editing", "module-edit-handler", "reference/editing-api-reference.md", "ModuleEditHandler");
        group("editing", "editing", "authwrapper", "security/02-authwrapper-authorizationchecker-integration.md", "AuthorizationChecker");
        group("editing", "editing", "authwrapper", "security/02-authwrapper-authorizationchecker-integration.md", "AuthWrapper");
        group("components.content", "static-content", "content-pipeline", "reference/content-helper-api-reference.md",
            "ContentEntryRecord ContentListItemComponent DefaultContentListItemComponent ContentSectionSite ContentRouteIndex ContentSectionConfig ContentSiteBundle ContentWarning StaticContentSite StaticContentSiteBuilder");
        group("components.chat", "chat", "chatmodule", "reference/chat-helper-api-reference.md",
            "ChatMessageComponent ChatMessageData ChatTranscriptRenderer ChatTransportMode ChatUiConfig DefaultChatMessageComponent");
        group("components.forum", "forum", "forum-main", "reference/forum-helper-api-reference.md", "ForumCollapsibleComposer");
        group("components.forum.actions", "forum", "forum-main", "reference/forum-helper-api-reference.md", "DefaultForumActionProvider ForumActionContext ForumActionProvider ForumActionType");
        group("components.forum.categories", "forum", "forum-main", "reference/forum-helper-api-reference.md", "ForumCategoryComponent ForumCategoryData ForumCategoryRenderer");
        group("components.forum.categories", "forum", "default-category", "reference/forum-helper-api-reference.md", "DefaultForumCategoryComponent");
        group("components.forum.comments", "forum", "forum-main", "reference/forum-helper-api-reference.md", "DefaultForumCommentComponent ForumCommentComponent ForumCommentData ForumCommentRenderer");
        group("components.forum.topics", "forum", "forum-main", "reference/forum-helper-api-reference.md", "DefaultForumTopicComponent ForumTopicComponent ForumTopicData ForumTopicRenderer ForumTopicTitleLink");
        group("components.forum.tags", "forum", "forum-main", "reference/forum-helper-api-reference.md", "ForumTagParser ForumTagResolver ForumTagResolverRegistry ForumTagResolvers Tag TagType");
    }
    private FeatureCatalog() {}
    private static void group(String pkg, String feature, String anchor, String docs, String names) {
        for (String name : names.split(" ")) CONTRACTS.add(new Contract("io.mindspice.simplypages." + pkg + "." + name, feature, anchor, "/docs/" + docs));
    }
    public static List<Contract> contracts() { return List.copyOf(CONTRACTS); }
    public static String references(String feature) {
        Div root = new Div().withClass("sp-site-contracts").withChild(Header.H2("Related API contracts"));
        Div links = new Div().withClass("sp-site-reference-links");
        CONTRACTS.stream().filter(e -> e.feature().equals(feature)).forEach(e -> links.withChild(Link.create(e.api(), e.name())));
        return root.withChild(links).render();
    }
    public static String directory() {
        Div root = new Div().withClass("sp-site-directory");
        CONTRACTS.stream().map(Contract::feature).distinct().forEach(feature -> {
            Div group = new Div().withClass("sp-site-directory-group").withChild(Header.H2(FeatureExamples.title(feature)));
            CONTRACTS.stream().filter(e -> e.feature().equals(feature))
                .forEach(e -> group.withChild(Link.create(e.destination(), e.name())));
            root.withChild(group);
        });
        return root.render();
    }
}
