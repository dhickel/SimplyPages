package io.mindspice.demo;

import io.mindspice.demo.forum.ForumDemoCategoryComponent;
import io.mindspice.demo.forum.ForumDemoService;
import io.mindspice.demo.forum.ForumViewer;
import io.mindspice.simplypages.builders.AccountBarBuilder;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.forum.ForumCollapsibleComposer;
import io.mindspice.simplypages.components.forum.actions.DefaultForumActionProvider;
import io.mindspice.simplypages.components.forum.actions.ForumActionProvider;
import io.mindspice.simplypages.components.forum.categories.ForumCategoryRenderer;
import io.mindspice.simplypages.components.forum.comments.ForumCommentRenderer;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolverRegistry;
import io.mindspice.simplypages.components.forum.tags.ForumTagResolvers;
import io.mindspice.simplypages.components.forum.tags.Tag;
import io.mindspice.simplypages.components.forum.topics.ForumTopicRenderer;
import io.mindspice.simplypages.components.forum.topics.ForumTopicTitleLink;
import io.mindspice.simplypages.components.forms.Form;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.components.forms.TextArea;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/forum")
public class ForumDemoController {

    private static final String SESSION_VIEWER_ID = "forum.viewer.id";
    private static final String SESSION_VIEWER_NAME = "forum.viewer.name";
    private static final String SESSION_VIEWER_MODERATOR = "forum.viewer.moderator";

    private static final int DEFAULT_TOPICS_PER_PAGE = 8;
    private static final int DEFAULT_COMMENTS_PER_PAGE = 8;
    private static final int MAX_PAGE_SIZE = 24;
    private static final ForumDisplaySettings FORUM_DISPLAY_SETTINGS = new ForumDisplaySettings(220);
    private static final Pattern FORUM_TAG_TOKEN_PATTERN = Pattern.compile("\\[\\[[^\\]]+]]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern WHITESPACE_BLOCK_PATTERN = Pattern.compile("[ \t]+");

    private final ForumDemoService forumService;

    public ForumDemoController(ForumDemoService forumService) {
        this.forumService = forumService;
    }

    @GetMapping
    @ResponseBody
    public String forumPage(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumState requested = parseState(params, null, null);
        ForumViewer viewer = resolveViewer(session);

        response.setHeader("Vary", "HX-Request");

        String main = renderForumMain(requested, viewer, null, null, null, FlashType.INFO);
        if (hxRequest != null) {
            return main;
        }

        return ShellBuilder.create()
            .withPageTitle("SimplyPages Forum Demo")
            .withTopBanner(BannerBuilder.create()
                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages")
                .withSubtitle("Forum helper end-to-end demo")
                .build())
            .withAccountBar(buildGlobalAccountBar())
            .withContent(new RawHtml(main))
            .build();
    }

    @GetMapping("/topics")
    @ResponseBody
    public String topicsFragment(
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");
        ForumState requested = parseState(params, null, ForumStage.TOPICS);
        return renderForumMain(requested, resolveViewer(session), null, null, null, FlashType.INFO);
    }

    @GetMapping("/topics/{topicId}/comments")
    @ResponseBody
    public String commentsFragment(
        @PathVariable String topicId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");
        ForumState requested = parseState(params, topicId, ForumStage.COMMENTS);
        return renderForumMain(requested, resolveViewer(session), null, null, null, FlashType.INFO);
    }

    @PostMapping("/viewer")
    @ResponseBody
    public String setViewer(
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        String name = safeTrim(params.get("displayName"));
        String userId = safeTrim(params.get("userId"));
        if (name == null) {
            name = "Demo User";
        }
        if (userId == null) {
            userId = "user-" + slugify(name);
        }

        boolean moderator = "moderator".equalsIgnoreCase(params.get("role")) || params.containsKey("moderator");

        session.setAttribute(SESSION_VIEWER_NAME, name);
        session.setAttribute(SESSION_VIEWER_ID, userId);
        session.setAttribute(SESSION_VIEWER_MODERATOR, moderator);

        response.setHeader("Vary", "HX-Request");
        ForumState requested = parseState(params, null, null);
        ForumViewer viewer = resolveViewer(session);
        String msg = "Viewer set to " + viewer.displayName() + (viewer.moderator() ? " (moderator)" : "");
        return renderForumMain(requested, viewer, null, null, msg, FlashType.SUCCESS);
    }

    @PostMapping("/topics/create")
    @ResponseBody
    public String createTopic(
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.TOPICS);
        String scopeId = resolveScope(requested.scopeId());

        Optional<ForumDemoService.TopicView> created = forumService.createTopic(
            scopeId,
            params.get("title"),
            params.get("body"),
            viewer
        );

        response.setHeader("Vary", "HX-Request");

        if (created.isEmpty()) {
            TopicDraft failedDraft = new TopicDraft(null, false, params.get("title"), params.get("body"));
            response.setStatus(400);
            return renderForumMain(
                new ForumState(scopeId, requested.topicId(), requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.TOPICS),
                viewer,
                failedDraft,
                null,
                "Topic title and body are required.",
                FlashType.WARNING
            );
        }

        ForumState next = new ForumState(scopeId, created.get().id(), 1, requested.topicSize(), 1, requested.commentSize(), ForumStage.TOPICS);
        return renderForumMain(next, viewer, null, null, "Topic created.", FlashType.SUCCESS);
    }

    @PostMapping("/comments/create")
    @ResponseBody
    public String createComment(
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, safeTrim(params.get("topic")), ForumStage.COMMENTS);
        String topicId = safeTrim(params.get("topicId"));
        if (topicId == null) {
            topicId = requested.topicId();
        }

        if (topicId == null) {
            response.setStatus(400);
            return renderForumMain(requested, viewer, null, null, "Select a topic before posting a comment.", FlashType.WARNING);
        }

        Optional<ForumDemoService.CommentView> created = forumService.createComment(
            topicId,
            params.get("parentId"),
            params.get("body"),
            viewer
        );

        response.setHeader("Vary", "HX-Request");

        if (created.isEmpty()) {
            CommentDraft failedDraft = new CommentDraft(null, false, params.get("body"));
            response.setStatus(400);
            return renderForumMain(
                new ForumState(requested.scopeId(), topicId, requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS),
                viewer,
                null,
                failedDraft,
                "Comment body is required.",
                FlashType.WARNING
            );
        }

        ForumState next = new ForumState(requested.scopeId(), topicId, requested.topicPage(), requested.topicSize(), 1, requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, null, "Comment posted.", FlashType.SUCCESS);
    }

    @PostMapping("/topics/{topicId}/quote")
    @ResponseBody
    public String quoteTopic(
        @PathVariable String topicId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, topicId, ForumStage.COMMENTS);
        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(topicId);

        response.setHeader("Vary", "HX-Request");

        if (topic.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Topic not found.", FlashType.DANGER);
        }

        CommentDraft draft = new CommentDraft(null, false, appendQuoteTag(params.get("body"), topicId));
        ForumState next = new ForumState(topic.get().categoryId(), topicId, requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, draft, "Quote inserted into comment composer.", FlashType.INFO);
    }

    @PostMapping("/comments/{commentId}/quote")
    @ResponseBody
    public String quoteComment(
        @PathVariable String commentId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.COMMENTS);
        Optional<ForumDemoService.CommentView> comment = forumService.findComment(commentId);

        response.setHeader("Vary", "HX-Request");

        if (comment.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Comment not found.", FlashType.DANGER);
        }

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(comment.get().topicId());
        String scopeId = topic.map(ForumDemoService.TopicView::categoryId).orElse(requested.scopeId());
        CommentDraft draft = new CommentDraft(null, false, appendQuoteTag(params.get("body"), commentId));

        ForumState next = new ForumState(scopeId, comment.get().topicId(), requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, draft, "Quote inserted into comment composer.", FlashType.INFO);
    }

    @PostMapping("/topics/{topicId}/edit")
    @ResponseBody
    public String editTopicDraft(
        @PathVariable String topicId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, topicId, ForumStage.TOPICS);
        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(topicId);

        response.setHeader("Vary", "HX-Request");

        if (topic.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Topic not found.", FlashType.DANGER);
        }
        if (!forumService.canEditTopic(topicId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to edit this topic.", FlashType.DANGER);
        }

        TopicDraft draft = new TopicDraft(topicId, true, topic.get().title(), topic.get().body());
        ForumState next = new ForumState(topic.get().categoryId(), topicId, requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.TOPICS);
        return renderForumMain(next, viewer, draft, null, "Editing topic.", FlashType.INFO);
    }

    @PostMapping("/comments/{commentId}/edit")
    @ResponseBody
    public String editCommentDraft(
        @PathVariable String commentId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.COMMENTS);
        Optional<ForumDemoService.CommentView> comment = forumService.findComment(commentId);

        response.setHeader("Vary", "HX-Request");

        if (comment.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Comment not found.", FlashType.DANGER);
        }
        if (!forumService.canEditComment(commentId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to edit this comment.", FlashType.DANGER);
        }

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(comment.get().topicId());
        String scopeId = topic.map(ForumDemoService.TopicView::categoryId).orElse(requested.scopeId());
        CommentDraft draft = new CommentDraft(commentId, true, comment.get().body());

        ForumState next = new ForumState(scopeId, comment.get().topicId(), requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, draft, "Editing comment.", FlashType.INFO);
    }

    @PostMapping("/topics/{topicId}/update")
    @ResponseBody
    public String updateTopic(
        @PathVariable String topicId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, topicId, ForumStage.TOPICS);

        response.setHeader("Vary", "HX-Request");

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(topicId);
        if (topic.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Topic not found.", FlashType.DANGER);
        }
        if (!forumService.canEditTopic(topicId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to update this topic.", FlashType.DANGER);
        }

        Optional<ForumDemoService.TopicView> updated = forumService.updateTopic(topicId, params.get("title"), params.get("body"));
        if (updated.isEmpty()) {
            response.setStatus(400);
            TopicDraft failedDraft = new TopicDraft(topicId, true, params.get("title"), params.get("body"));
            return renderForumMain(requested, viewer, failedDraft, null, "Topic title and body are required.", FlashType.WARNING);
        }

        ForumState next = new ForumState(updated.get().categoryId(), topicId, requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.TOPICS);
        return renderForumMain(next, viewer, null, null, "Topic updated.", FlashType.SUCCESS);
    }

    @PostMapping("/comments/{commentId}/update")
    @ResponseBody
    public String updateComment(
        @PathVariable String commentId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.COMMENTS);

        response.setHeader("Vary", "HX-Request");

        Optional<ForumDemoService.CommentView> comment = forumService.findComment(commentId);
        if (comment.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Comment not found.", FlashType.DANGER);
        }
        if (!forumService.canEditComment(commentId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to update this comment.", FlashType.DANGER);
        }

        Optional<ForumDemoService.CommentView> updated = forumService.updateComment(commentId, params.get("body"));
        if (updated.isEmpty()) {
            response.setStatus(400);
            CommentDraft failedDraft = new CommentDraft(commentId, true, params.get("body"));
            return renderForumMain(requested, viewer, null, failedDraft, "Comment body is required.", FlashType.WARNING);
        }

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(updated.get().topicId());
        String scopeId = topic.map(ForumDemoService.TopicView::categoryId).orElse(requested.scopeId());
        ForumState next = new ForumState(scopeId, updated.get().topicId(), requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, null, "Comment updated.", FlashType.SUCCESS);
    }

    @PostMapping("/topics/{topicId}/delete")
    @ResponseBody
    public String deleteTopic(
        @PathVariable String topicId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.TOPICS);

        response.setHeader("Vary", "HX-Request");

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(topicId);
        if (topic.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Topic not found.", FlashType.DANGER);
        }
        if (!forumService.canDeleteTopic(topicId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to delete this topic.", FlashType.DANGER);
        }

        forumService.deleteTopic(topicId);

        String scopeId = topic.get().categoryId();
        String nextTopic = Objects.equals(requested.topicId(), topicId) ? null : requested.topicId();
        ForumState next = new ForumState(scopeId, nextTopic, requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.TOPICS);
        return renderForumMain(next, viewer, null, null, "Topic deleted.", FlashType.SUCCESS);
    }

    @PostMapping("/comments/{commentId}/delete")
    @ResponseBody
    public String deleteComment(
        @PathVariable String commentId,
        @RequestParam Map<String, String> params,
        HttpSession session,
        HttpServletResponse response
    ) {
        ForumViewer viewer = resolveViewer(session);
        ForumState requested = parseState(params, null, ForumStage.COMMENTS);

        response.setHeader("Vary", "HX-Request");

        Optional<ForumDemoService.CommentView> comment = forumService.findComment(commentId);
        if (comment.isEmpty()) {
            response.setStatus(404);
            return renderForumMain(requested, viewer, null, null, "Comment not found.", FlashType.DANGER);
        }
        if (!forumService.canDeleteComment(commentId, viewer)) {
            response.setStatus(403);
            return renderForumMain(requested, viewer, null, null, "You are not allowed to delete this comment.", FlashType.DANGER);
        }

        forumService.deleteComment(commentId);
        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(comment.get().topicId());
        String scopeId = topic.map(ForumDemoService.TopicView::categoryId).orElse(requested.scopeId());
        ForumState next = new ForumState(scopeId, comment.get().topicId(), requested.topicPage(), requested.topicSize(), requested.commentPage(), requested.commentSize(), ForumStage.COMMENTS);
        return renderForumMain(next, viewer, null, null, "Comment deleted.", FlashType.SUCCESS);
    }

    private String renderForumMain(
        ForumState requested,
        ForumViewer viewer,
        TopicDraft topicDraft,
        CommentDraft commentDraft,
        String flashMessage,
        FlashType flashType
    ) {
        List<ForumDemoService.CategoryGroupView> groups = forumService.listCategoryGroups();
        List<ForumDemoService.TopicView> allTopics = forumService.listAllTopicsNewestFirst();

        Optional<ForumDemoService.TopicView> selectedTopic = requested.topicId() == null
            ? Optional.empty()
            : forumService.findTopic(requested.topicId());

        String resolvedScope;
        if (selectedTopic.isPresent()) {
            resolvedScope = selectedTopic.get().categoryId();
        } else if (requested.stage() == ForumStage.CATEGORIES) {
            resolvedScope = forumService.categoryExists(requested.scopeId()) ? requested.scopeId() : "";
        } else {
            resolvedScope = resolveScope(requested.scopeId());
        }

        List<ForumDemoService.TopicView> scopedTopics = resolvedScope.isBlank()
            ? List.of()
            : allTopics.stream()
                .filter(topic -> resolvedScope.equals(topic.categoryId()))
                .toList();

        int totalTopics = scopedTopics.size();
        int topicPage = normalizePage(requested.topicPage(), requested.topicSize(), totalTopics);
        int totalComments = selectedTopic.map(topic -> forumService.countCommentsForTopic(topic.id())).orElse(0);
        int commentPage = normalizePage(requested.commentPage(), requested.commentSize(), totalComments);

        ForumStage stage = requested.stage();
        if (stage == ForumStage.COMMENTS && selectedTopic.isEmpty()) {
            stage = resolvedScope.isBlank() ? ForumStage.CATEGORIES : ForumStage.TOPICS;
        }
        if (stage == ForumStage.TOPICS && resolvedScope.isBlank()) {
            stage = ForumStage.CATEGORIES;
        }

        ForumState state = new ForumState(
            resolvedScope,
            selectedTopic.map(ForumDemoService.TopicView::id).orElse(null),
            topicPage,
            requested.topicSize(),
            commentPage,
            requested.commentSize(),
            stage
        );

        Div root = new Div().withId("forum-main").withClass("forum-demo-main");

        if (flashMessage != null && !flashMessage.isBlank()) {
            root.withChild(buildFlash(flashType, flashMessage));
        }

        if (groups.isEmpty()) {
            root.withChild(Alert.warning("No forum categories are configured for this demo instance."));
            return Page.builder().addComponents(root).build().render();
        }

        root.withChild(buildViewerPanel(state, viewer));

        ForumDemoService.CategoryView scopedCategory = findCategory(groups, state.scopeId()).orElse(null);
        root.withChild(buildBreadcrumbs(state, scopedCategory, selectedTopic.orElse(null)));

        if (state.stage() == ForumStage.CATEGORIES) {
            root.withChild(buildCategorySection(groups, state.withScope("")));
        } else if (state.stage() == ForumStage.TOPICS) {
            if (scopedCategory == null) {
                root.withChild(Alert.warning("Select a category to view topics."));
                root.withChild(buildCategorySection(groups, state.withStage(ForumStage.CATEGORIES).withScope("")));
            } else {
                TopicDraft effectiveTopicDraft = topicDraft == null
                    ? new TopicDraft(null, false, null, null)
                    : topicDraft;
                root.withChild(buildTopicComposer(state, effectiveTopicDraft));
                root.withChild(buildTopicsSection(state, viewer, scopedTopics, totalTopics));
            }
        } else if (selectedTopic.isPresent()) {
            List<ForumDemoService.CommentView> allComments = forumService.listAllCommentsOldestFirst();
            root.withChild(buildCommentsSection(state, viewer, selectedTopic.get(), allComments, totalComments));

            CommentDraft effectiveCommentDraft = commentDraft == null
                ? new CommentDraft(null, false, null)
                : commentDraft;
            root.withChild(buildCommentComposer(state, selectedTopic.get(), effectiveCommentDraft));
        } else {
            root.withChild(Alert.warning("Select a topic to view comments."));
        }

        return Page.builder()
            .addComponents(root)
            .build()
            .render();
    }

    private Component buildBreadcrumbs(
        ForumState state,
        ForumDemoService.CategoryView category,
        ForumDemoService.TopicView topic
    ) {
        Div crumbs = new Div().withClass("forum-demo-breadcrumbs");

        crumbs.withChild(new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "forum-action")
            .withAttribute("hx-get", endpointWithState("/forum", new ForumState(
                "",
                null,
                1,
                state.topicSize(),
                1,
                state.commentSize(),
                ForumStage.CATEGORIES
            )))
            .withAttribute("hx-target", "#forum-main")
            .withAttribute("hx-swap", "outerHTML")
            .withAttribute("hx-push-url", "/forum")
            .withInnerText("Categories"));

        if (category != null) {
            crumbs.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-breadcrumb-sep")
                .withInnerText("/"));

            crumbs.withChild(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "forum-action")
                .withAttribute("hx-get", endpointWithState("/forum/topics", state
                    .withScope(category.id())
                    .withTopic(null)
                    .withStage(ForumStage.TOPICS)
                    .withTopicPage(1)
                    .withCommentPage(1)))
                .withAttribute("hx-target", "#forum-main")
                .withAttribute("hx-swap", "outerHTML")
                .withAttribute("hx-push-url", "/forum?view=topics&scope=" + encode(category.id()))
                .withInnerText(category.title()));
        }

        if (topic != null && state.stage() == ForumStage.COMMENTS) {
            crumbs.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-breadcrumb-sep")
                .withInnerText("/"));
            crumbs.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-breadcrumb-current")
                .withInnerText(topic.title()));
        }

        return crumbs;
    }

    private Component buildViewerPanel(ForumState state, ForumViewer viewer) {
        Div panel = new Div().withClass("forum-demo-panel");
        panel.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText("Viewer Session"));
        panel.withChild(new HtmlTag("p")
            .withAttribute("class", "forum-demo-panel-subtitle")
            .withInnerText("Current viewer: " + viewer.displayName() + " [" + viewer.userId() + "]" + (viewer.moderator() ? " · moderator" : "")));

        Form form = Form.create()
            .withClass("forum-demo-form forum-demo-inline-form")
            .withHxPost("/forum/viewer")
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML");

        appendStateHiddenFields(form, state);

        form.addField("Display Name", TextInput.create("displayName").withValue(viewer.displayName()).required());
        form.addField("User ID", TextInput.create("userId").withValue(viewer.userId()).required());

        Select role = Select.create("role")
            .addOption("user", "User", !viewer.moderator())
            .addOption("moderator", "Moderator", viewer.moderator());
        form.addField("Role", role);

        form.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "btn btn-primary")
            .withInnerText("Set Viewer"));

        panel.withChild(form);
        return panel;
    }

    private Component buildCategorySection(List<ForumDemoService.CategoryGroupView> groups, ForumState state) {
        Div section = new Div().withClass("forum-demo-panel");
        section.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText("Categories"));
        section.withChild(new HtmlTag("p")
            .withAttribute("class", "forum-demo-panel-subtitle")
            .withInnerText("Pick a category to drill into topics."));

        ForumCategoryRenderer<ForumDemoService.CategoryView, ForumState> renderer = ForumCategoryRenderer
            .<ForumDemoService.CategoryView, ForumState>builder()
            .withCategoryComponentSupplier(() -> ForumDemoCategoryComponent.create(state.topicSize(), state.commentSize()))
            .build();

        for (ForumDemoService.CategoryGroupView group : groups) {
            Div groupWrap = new Div().withClass("forum-demo-category-group");
            groupWrap.withChild(new HtmlTag("h3").withAttribute("class", "forum-demo-group-title").withInnerText(group.title()));
            groupWrap.withChild(renderer.render(group.categories(), state));
            section.withChild(groupWrap);
        }

        return section;
    }

    private Component buildTopicsSection(
        ForumState state,
        ForumViewer viewer,
        List<ForumDemoService.TopicView> scopedTopics,
        int totalTopics
    ) {
        Div section = new Div().withClass("forum-demo-panel");
        section.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText("Topics"));

        section.withChild(buildTopicSizeForm(state));

        DefaultForumActionProvider<ForumDemoService.TopicView, ForumViewer> defaults = DefaultForumActionProvider
            .<ForumDemoService.TopicView, ForumViewer>create()
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML")
            .withQuoteHxInclude("#forum-comment-compose-body")
            .showEditWhen(ctx -> canModifyTopic(ctx.context(), ctx.source()))
            .showDeleteWhen(ctx -> canModifyTopic(ctx.context(), ctx.source()))
            .withQuoteEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/quote", state.withTopic(ctx.itemId()).withStage(ForumStage.COMMENTS)))
            .withEditEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/edit", state.withTopic(ctx.itemId()).withStage(ForumStage.TOPICS)))
            .withDeleteEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/delete", state.withTopic(ctx.itemId()).withStage(ForumStage.TOPICS)));

        ForumActionProvider<ForumDemoService.TopicView, ForumViewer> topicActions = defaults::provide;

        ForumTopicRenderer<ForumDemoService.TopicView, ForumViewer> topicRenderer = ForumTopicRenderer
            .<ForumDemoService.TopicView, ForumViewer>builder()
            .withActionProvider(topicActions)
            .withBodyTextResolver((topic, context) -> buildTopicPreview(topic.body()))
            .withTitleLinkResolver((topic, context) -> buildThreadTitleLink(state, topic))
            .withResolverRegistry(buildDemoTagResolverRegistry())
            .withPaginationHxTarget("#forum-main")
            .withPaginationHxSwap("outerHTML")
            .withPaginationEndpointResolver((scopeId, page, size) -> endpointWithState(
                "/forum/topics",
                new ForumState(scopeId, null, page, size, state.commentPage(), state.commentSize(), ForumStage.TOPICS)
            ))
            .build();

        section.withChild(topicRenderer.render(
            scopedTopics,
            viewer,
            new ForumTopicRenderer.TopicPagination(state.scopeId(), state.topicPage(), state.topicSize(), totalTopics)
        ));

        if (totalTopics == 0) {
            section.withChild(Alert.info("No topics in this category yet. Use New Topic above."));
        }

        return section;
    }

    private Component buildTopicComposer(ForumState state, TopicDraft topicDraft) {
        Div section = new Div().withClass("forum-demo-panel forum-demo-composer");

        boolean editing = topicDraft.editing() && topicDraft.topicId() != null;
        String title = editing ? "Edit Topic" : "Create Topic";
        section.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText(title));

        String action = editing
            ? endpointWithState("/forum/topics/" + topicDraft.topicId() + "/update", state.withTopic(topicDraft.topicId()).withStage(ForumStage.TOPICS))
            : endpointWithState("/forum/topics/create", state.withStage(ForumStage.TOPICS));

        Form form = Form.create()
            .withClass("forum-demo-form")
            .withHxPost(action)
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML");

        appendStateHiddenFields(form, state);
        form.withChild(hidden("scope", state.scopeId()));

        form.addField("Title", TextInput.create("title").withValue(defaultString(topicDraft.title())).required());
        form.addField("Body", TextArea.create("body").withRows(10).withValue(defaultString(topicDraft.body())).required());

        Div actions = new Div().withClass("forum-demo-form-actions");
        actions.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "btn btn-primary")
            .withInnerText(editing ? "Save Topic" : "Post Topic"));

        if (editing) {
            actions.withChild(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "forum-action")
                .withAttribute("hx-get", endpointWithState("/forum/topics", state.withTopic(null).withStage(ForumStage.TOPICS)))
                .withAttribute("hx-target", "#forum-main")
                .withAttribute("hx-swap", "outerHTML")
                .withInnerText("Cancel"));
        }

        form.withChild(actions);
        section.withChild(form);
        return ForumCollapsibleComposer.create(editing ? "Edit Topic" : "New Topic", section)
            .withClass("forum-demo-composer-toggle")
            .withExpanded(editing);
    }

    private Component buildCommentsSection(
        ForumState state,
        ForumViewer viewer,
        ForumDemoService.TopicView topic,
        List<ForumDemoService.CommentView> allComments,
        int totalComments
    ) {
        Div section = new Div().withClass("forum-demo-panel");
        section.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText("Thread: " + topic.title()));
        section.withChild(new HtmlTag("p")
            .withAttribute("class", "forum-demo-panel-subtitle")
            .withInnerText("Author: " + topic.author() + " · " + topic.timestamp()));
        section.withChild(ForumCollapsibleComposer.create("Parent Post", buildParentTopicBody(state, viewer, topic))
            .withClass("forum-demo-parent-topic-toggle")
            .withExpanded(state.commentPage() == 1));

        section.withChild(buildCommentSizeForm(state));

        DefaultForumActionProvider<ForumDemoService.CommentView, ForumViewer> commentActions = DefaultForumActionProvider
            .<ForumDemoService.CommentView, ForumViewer>create()
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML")
            .withQuoteHxInclude("#forum-comment-compose-body")
            .showEditWhen(ctx -> canModifyComment(ctx.context(), ctx.source()))
            .showDeleteWhen(ctx -> canModifyComment(ctx.context(), ctx.source()))
            .withQuoteEndpoint(ctx -> endpointWithState("/forum/comments/" + ctx.itemId() + "/quote", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS)))
            .withEditEndpoint(ctx -> endpointWithState("/forum/comments/" + ctx.itemId() + "/edit", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS)))
            .withDeleteEndpoint(ctx -> endpointWithState("/forum/comments/" + ctx.itemId() + "/delete", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS)));

        ForumCommentRenderer<ForumDemoService.CommentView, ForumViewer> commentRenderer = ForumCommentRenderer
            .<ForumDemoService.CommentView, ForumViewer>builder()
            .withActionProvider(commentActions)
            .withResolverRegistry(buildDemoTagResolverRegistry())
            .withPaginationHxTarget("#forum-main")
            .withPaginationHxSwap("outerHTML")
            .withPaginationEndpointResolver((topicId, page, size) -> endpointWithState(
                "/forum/topics/" + topicId + "/comments",
                new ForumState(state.scopeId(), topicId, state.topicPage(), state.topicSize(), page, size, ForumStage.COMMENTS)
            ))
            .build();

        section.withChild(commentRenderer.render(
            allComments,
            viewer,
            new ForumCommentRenderer.CommentPagination(topic.id(), state.commentPage(), state.commentSize(), totalComments)
        ));

        return section;
    }

    private Component buildCommentComposer(ForumState state, ForumDemoService.TopicView topic, CommentDraft commentDraft) {
        Div section = new Div().withClass("forum-demo-panel forum-demo-composer");

        boolean editing = commentDraft.editing() && commentDraft.commentId() != null;
        String title = editing ? "Edit Comment" : "Post Comment";
        section.withChild(new HtmlTag("h2").withAttribute("class", "forum-demo-panel-title").withInnerText(title));

        String action = editing
            ? endpointWithState("/forum/comments/" + commentDraft.commentId() + "/update", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS))
            : endpointWithState("/forum/comments/create", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS));

        Form form = Form.create()
            .withClass("forum-demo-form")
            .withHxPost(action)
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML");

        appendStateHiddenFields(form, state.withTopic(topic.id()));
        form.withChild(hidden("topicId", topic.id()));

        form.addField("Comment", TextArea.create("body")
            .withId("forum-comment-compose-body")
            .withRows(10)
            .withValue(defaultString(commentDraft.body()))
            .required());

        Div actions = new Div().withClass("forum-demo-form-actions");
        actions.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "btn btn-primary")
            .withInnerText(editing ? "Save Comment" : "Post Comment"));

        if (editing) {
            actions.withChild(new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "forum-action")
                .withAttribute("hx-get", endpointWithState("/forum/topics/" + topic.id() + "/comments", state.withTopic(topic.id()).withStage(ForumStage.COMMENTS)))
                .withAttribute("hx-target", "#forum-main")
                .withAttribute("hx-swap", "outerHTML")
                .withInnerText("Cancel"));
        }

        form.withChild(actions);
        section.withChild(form);
        return ForumCollapsibleComposer.create(editing ? "Edit Comment" : "New Comment", section)
            .withClass("forum-demo-composer-toggle")
            .withExpanded(editing || hasNonBlankText(commentDraft.body()));
    }

    private Component buildTopicSizeForm(ForumState state) {
        Form form = Form.create()
            .withClass("forum-demo-form forum-demo-size-form")
            .withHxGet("/forum/topics")
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML");

        form.withChild(hidden("scope", state.scopeId()));
        form.withChild(hidden("topic", state.topicId()));
        form.withChild(hidden("page", String.valueOf(state.topicPage())));
        form.withChild(hidden("view", ForumStage.TOPICS.paramValue()));
        form.withChild(hidden("commentPage", String.valueOf(state.commentPage())));
        form.withChild(hidden("commentSize", String.valueOf(state.commentSize())));

        Select size = Select.create("size")
            .addOption("5", "5 per page", state.topicSize() == 5)
            .addOption("8", "8 per page", state.topicSize() == 8)
            .addOption("12", "12 per page", state.topicSize() == 12);
        form.addField("Topics", size);

        form.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "forum-action")
            .withInnerText("Apply"));
        return form;
    }

    private Component buildCommentSizeForm(ForumState state) {
        Form form = Form.create()
            .withClass("forum-demo-form forum-demo-size-form")
            .withHxGet("/forum/topics/" + state.topicId() + "/comments")
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML");

        form.withChild(hidden("scope", state.scopeId()));
        form.withChild(hidden("topic", state.topicId()));
        form.withChild(hidden("view", ForumStage.COMMENTS.paramValue()));
        form.withChild(hidden("topicPage", String.valueOf(state.topicPage())));
        form.withChild(hidden("topicSize", String.valueOf(state.topicSize())));
        form.withChild(hidden("page", String.valueOf(state.commentPage())));

        Select size = Select.create("size")
            .addOption("5", "5 per page", state.commentSize() == 5)
            .addOption("8", "8 per page", state.commentSize() == 8)
            .addOption("12", "12 per page", state.commentSize() == 12);
        form.addField("Comments", size);

        form.withChild(new HtmlTag("button")
            .withAttribute("type", "submit")
            .withAttribute("class", "forum-action")
            .withInnerText("Apply"));
        return form;
    }

    private ForumTopicTitleLink buildThreadTitleLink(ForumState state, ForumDemoService.TopicView topic) {
        ForumState threadState = state
            .withScope(topic.categoryId())
            .withTopic(topic.id())
            .withStage(ForumStage.COMMENTS)
            .withCommentPage(1);

        String url = "/forum?" + threadState.toQueryString();
        return ForumTopicTitleLink.htmx(
            url,
            endpointWithState("/forum/topics/" + topic.id() + "/comments", threadState),
            "#forum-main",
            "outerHTML",
            url
        );
    }

    private Component buildParentTopicBody(ForumState state, ForumViewer viewer, ForumDemoService.TopicView topic) {
        DefaultForumActionProvider<ForumDemoService.TopicView, ForumViewer> topicActions = DefaultForumActionProvider
            .<ForumDemoService.TopicView, ForumViewer>create()
            .withHxTarget("#forum-main")
            .withHxSwap("outerHTML")
            .withQuoteHxInclude("#forum-comment-compose-body")
            .showEditWhen(ctx -> canModifyTopic(ctx.context(), ctx.source()))
            .showDeleteWhen(ctx -> canModifyTopic(ctx.context(), ctx.source()))
            .withQuoteEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/quote", state.withTopic(ctx.itemId()).withStage(ForumStage.COMMENTS)))
            .withEditEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/edit", state.withTopic(ctx.itemId()).withStage(ForumStage.TOPICS)))
            .withDeleteEndpoint(ctx -> endpointWithState("/forum/topics/" + ctx.itemId() + "/delete", state.withTopic(ctx.itemId()).withStage(ForumStage.TOPICS)));

        ForumTopicRenderer<ForumDemoService.TopicView, ForumViewer> topicRenderer = ForumTopicRenderer
            .<ForumDemoService.TopicView, ForumViewer>builder()
            .withActionProvider(topicActions)
            .withResolverRegistry(buildDemoTagResolverRegistry())
            .build();

        Div parent = new Div().withClass("forum-demo-parent-topic");
        parent.withChild(topicRenderer.render(List.of(topic), viewer));
        return parent;
    }

    private String buildTopicPreview(String body) {
        String raw = defaultString(body);
        if (raw.isBlank()) {
            return raw;
        }

        int maxChars = Math.max(32, FORUM_DISPLAY_SETTINGS.topicPreviewChars());
        return truncateForumText(raw, maxChars);
    }

    private ForumTagResolverRegistry buildDemoTagResolverRegistry() {
        return ForumTagResolverRegistry.create()
            .register(ForumTagResolvers.of("quote", tags -> {
                Map<Tag, Component> resolved = new LinkedHashMap<>();
                for (Tag tag : tags) {
                    resolved.put(tag, buildQuoteScaffold(tag.value()));
                }
                return resolved;
            }))
            .register(ForumTagResolvers.image())
            .register(ForumTagResolvers.mention())
            .register(ForumTagResolvers.link());
    }

    private Component buildQuoteScaffold(String quoteId) {
        Optional<ForumDemoService.CommentView> comment = forumService.findComment(quoteId);
        if (comment.isPresent()) {
            HtmlTag quote = new HtmlTag("span")
                .withAttribute("class", "forum-tag forum-tag-quote forum-demo-quote");
            quote.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-quote-meta")
                .withInnerText("Quoted comment by " + comment.get().author() + " · " + comment.get().timestamp()));
            quote.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-quote-body")
                .withInnerText(normalizeForumText(comment.get().body())));
            return quote;
        }

        Optional<ForumDemoService.TopicView> topic = forumService.findTopic(quoteId);
        if (topic.isPresent()) {
            HtmlTag quote = new HtmlTag("span")
                .withAttribute("class", "forum-tag forum-tag-quote forum-demo-quote");
            quote.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-quote-meta")
                .withInnerText("Quoted topic by " + topic.get().author() + " · " + topic.get().timestamp()));
            quote.withChild(new HtmlTag("span")
                .withAttribute("class", "forum-demo-quote-body")
                .withInnerText(topic.get().title() + " — " + normalizeForumText(topic.get().body())));
            return quote;
        }

        return new HtmlTag("span")
            .withAttribute("class", "forum-tag forum-tag-quote forum-demo-quote forum-demo-quote-missing")
            .withInnerText("Quoted reference unavailable: " + quoteId);
    }

    private String appendQuoteTag(String existingBody, String quoteId) {
        String quoteTag = "[[quote::" + quoteId + "]]";
        String body = existingBody == null ? "" : existingBody;
        if (body.isBlank()) {
            return quoteTag + "\n";
        }

        StringBuilder merged = new StringBuilder(body);
        if (!body.endsWith("\n")) {
            merged.append('\n');
        }
        merged.append(quoteTag).append('\n');
        return merged.toString();
    }

    private String truncateForumText(String body, int maxChars) {
        String normalized = normalizeForumText(body);
        if (normalized.length() <= maxChars) {
            return normalized;
        }

        int boundary = normalized.lastIndexOf(' ', maxChars);
        int cut = boundary > (maxChars / 2) ? boundary : maxChars;
        return normalized.substring(0, cut).trim() + "...";
    }

    private String normalizeForumText(String body) {
        String raw = defaultString(body);
        if (raw.isBlank()) {
            return raw;
        }
        String withoutTags = FORUM_TAG_TOKEN_PATTERN.matcher(raw).replaceAll(" ");
        String normalized = WHITESPACE_BLOCK_PATTERN.matcher(withoutTags).replaceAll(" ").trim();
        if (normalized.isBlank()) {
            normalized = WHITESPACE_BLOCK_PATTERN.matcher(raw).replaceAll(" ").trim();
        }
        return normalized;
    }

    private Component buildFlash(FlashType type, String message) {
        return switch (type) {
            case SUCCESS -> Alert.success(message);
            case WARNING -> Alert.warning(message);
            case DANGER -> Alert.danger(message);
            case INFO -> Alert.info(message);
        };
    }

    private void appendStateHiddenFields(Form form, ForumState state) {
        form.withChild(hidden("scope", state.scopeId()));
        form.withChild(hidden("topic", state.topicId()));
        form.withChild(hidden("view", state.stage().paramValue()));
        form.withChild(hidden("topicPage", String.valueOf(state.topicPage())));
        form.withChild(hidden("topicSize", String.valueOf(state.topicSize())));
        form.withChild(hidden("commentPage", String.valueOf(state.commentPage())));
        form.withChild(hidden("commentSize", String.valueOf(state.commentSize())));
    }

    private HtmlTag hidden(String name, String value) {
        return new HtmlTag("input", true)
            .withAttribute("type", "hidden")
            .withAttribute("name", name)
            .withAttribute("value", value == null ? "" : value);
    }

    private ForumViewer resolveViewer(HttpSession session) {
        Object idObj = session.getAttribute(SESSION_VIEWER_ID);
        Object nameObj = session.getAttribute(SESSION_VIEWER_NAME);
        Object modObj = session.getAttribute(SESSION_VIEWER_MODERATOR);

        String id = idObj instanceof String str ? str : "user-demo";
        String name = nameObj instanceof String str ? str : "Demo User";
        boolean moderator = modObj instanceof Boolean b && b;

        return new ForumViewer(id, name, moderator);
    }

    private ForumState parseState(Map<String, String> params, String topicOverride, ForumStage stageOverride) {
        String scope = safeTrim(params.get("scope"));

        String topic = topicOverride != null ? safeTrim(topicOverride) : safeTrim(params.get("topic"));
        if (topic == null) {
            topic = safeTrim(params.get("topicId"));
        }

        ForumStage stage = stageOverride == null
            ? ForumStage.fromParam(safeTrim(params.get("view")))
            : stageOverride;
        if (stage == null) {
            if (topic != null) {
                stage = ForumStage.COMMENTS;
            } else if (scope != null) {
                stage = ForumStage.TOPICS;
            } else {
                stage = ForumStage.CATEGORIES;
            }
        }

        int topicPage = parsePositive(params, "topicPage", 1);
        int topicSize = parseBounded(params, "topicSize", DEFAULT_TOPICS_PER_PAGE, MAX_PAGE_SIZE);
        int commentPage = parsePositive(params, "commentPage", 1);
        int commentSize = parseBounded(params, "commentSize", DEFAULT_COMMENTS_PER_PAGE, MAX_PAGE_SIZE);

        if (stage == ForumStage.TOPICS) {
            topicPage = parsePositive(params, "page", topicPage);
            topicSize = parseBounded(params, "size", topicSize, MAX_PAGE_SIZE);
        } else if (stage == ForumStage.COMMENTS) {
            commentPage = parsePositive(params, "page", commentPage);
            commentSize = parseBounded(params, "size", commentSize, MAX_PAGE_SIZE);
        }

        return new ForumState(scope, topic, topicPage, topicSize, commentPage, commentSize, stage);
    }

    private int parsePositive(Map<String, String> params, String key, int fallback) {
        String raw = params.get(key);
        if (raw == null || raw.isBlank()) {
            return Math.max(1, fallback);
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed < 1 ? Math.max(1, fallback) : parsed;
        } catch (NumberFormatException ignored) {
            return Math.max(1, fallback);
        }
    }

    private int parseBounded(Map<String, String> params, String key, int fallback, int max) {
        int parsed = parsePositive(params, key, fallback);
        return Math.min(max, Math.max(1, parsed));
    }

    private String resolveScope(String requestedScope) {
        if (requestedScope != null && forumService.categoryExists(requestedScope)) {
            return requestedScope;
        }

        List<String> ordered = forumService.categoryIdsInDisplayOrder();
        return ordered.isEmpty() ? "" : ordered.getFirst();
    }

    private Optional<ForumDemoService.CategoryView> findCategory(List<ForumDemoService.CategoryGroupView> groups, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return Optional.empty();
        }
        for (ForumDemoService.CategoryGroupView group : groups) {
            for (ForumDemoService.CategoryView category : group.categories()) {
                if (categoryId.equals(category.id())) {
                    return Optional.of(category);
                }
            }
        }
        return Optional.empty();
    }

    private int normalizePage(int requestedPage, int pageSize, int totalItems) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / Math.max(1, pageSize)));
        return Math.max(1, Math.min(requestedPage, totalPages));
    }

    private String endpointWithState(String path, ForumState state) {
        return path + "?" + state.toQueryString();
    }

    private boolean canModifyTopic(ForumViewer viewer, ForumDemoService.TopicView topic) {
        if (viewer == null || topic == null) {
            return false;
        }
        return viewer.moderator() || viewer.userId().equals(topic.ownerId());
    }

    private boolean canModifyComment(ForumViewer viewer, ForumDemoService.CommentView comment) {
        if (viewer == null || comment == null) {
            return false;
        }
        return viewer.moderator() || viewer.userId().equals(comment.ownerId());
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasNonBlankText(String value) {
        return value != null && !value.isBlank();
    }

    private static String slugify(String value) {
        StringBuilder slug = new StringBuilder();
        for (char c : value.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                slug.append(c);
            } else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '-') {
            slug.deleteCharAt(slug.length() - 1);
        }
        return slug.isEmpty() ? "user" : slug.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Component buildGlobalAccountBar() {
        return AccountBarBuilder.create()
            .addLeftLink("Home", "/home")
            .addLeftLink("Demos", "/demos")
            .addLeftLink("Javadocs", "/javadocs-view")
            .addLeftLink("Forum", "/forum")
            .addLeftLink("Docs", "/docs")
            .build();
    }

    private record ForumDisplaySettings(int topicPreviewChars) {
        private ForumDisplaySettings {
            topicPreviewChars = Math.max(32, topicPreviewChars);
        }
    }

    private enum FlashType {
        INFO,
        SUCCESS,
        WARNING,
        DANGER
    }

    private enum ForumStage {
        CATEGORIES("categories"),
        TOPICS("topics"),
        COMMENTS("comments");

        private final String paramValue;

        ForumStage(String paramValue) {
            this.paramValue = paramValue;
        }

        String paramValue() {
            return paramValue;
        }

        static ForumStage fromParam(String param) {
            if (param == null) {
                return null;
            }
            for (ForumStage stage : values()) {
                if (stage.paramValue.equalsIgnoreCase(param)) {
                    return stage;
                }
            }
            return null;
        }
    }

    private record ForumState(
        String scopeId,
        String topicId,
        int topicPage,
        int topicSize,
        int commentPage,
        int commentSize,
        ForumStage stage
    ) {
        private ForumState {
            scopeId = scopeId == null ? "" : scopeId;
            topicPage = Math.max(1, topicPage);
            topicSize = Math.max(1, topicSize);
            commentPage = Math.max(1, commentPage);
            commentSize = Math.max(1, commentSize);
            stage = stage == null ? ForumStage.CATEGORIES : stage;
        }

        ForumState withTopic(String topicId) {
            return new ForumState(scopeId, topicId, topicPage, topicSize, commentPage, commentSize, stage);
        }

        ForumState withScope(String scopeId) {
            return new ForumState(scopeId, topicId, topicPage, topicSize, commentPage, commentSize, stage);
        }

        ForumState withTopicPage(int page) {
            return new ForumState(scopeId, topicId, page, topicSize, commentPage, commentSize, stage);
        }

        ForumState withCommentPage(int page) {
            return new ForumState(scopeId, topicId, topicPage, topicSize, page, commentSize, stage);
        }

        ForumState withStage(ForumStage stage) {
            return new ForumState(scopeId, topicId, topicPage, topicSize, commentPage, commentSize, stage);
        }

        String toQueryString() {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("view", stage.paramValue());
            params.put("scope", scopeId);
            if (topicId != null && !topicId.isBlank()) {
                params.put("topic", topicId);
            }
            params.put("topicPage", String.valueOf(topicPage));
            params.put("topicSize", String.valueOf(topicSize));
            params.put("commentPage", String.valueOf(commentPage));
            params.put("commentSize", String.valueOf(commentSize));

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                sb.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
                first = false;
            }
            return sb.toString();
        }
    }

    private record TopicDraft(
        String topicId,
        boolean editing,
        String title,
        String body
    ) {}

    private record CommentDraft(
        String commentId,
        boolean editing,
        String body
    ) {}
}
