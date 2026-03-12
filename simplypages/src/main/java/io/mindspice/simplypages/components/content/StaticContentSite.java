package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Tag;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Immutable static markdown content generator.
 */
public final class StaticContentSite {

    private static final List<Extension> TABLE_EXTENSIONS = List.of(TablesExtension.create());
    private static final Parser MARKDOWN_PARSER = Parser.builder().extensions(TABLE_EXTENSIONS).build();

    private static final DateTimeFormatter DATE_TIME_META_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_ONLY_META_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<ContentSectionConfig> sectionConfigs;

    StaticContentSite(Collection<ContentSectionConfig> sectionConfigs) {
        this.sectionConfigs = List.copyOf(Objects.requireNonNull(sectionConfigs, "sectionConfigs"));
    }

    public ContentSiteBundle generate() {
        List<ContentWarning> warnings = new ArrayList<>();

        Map<String, ContentSectionSite> sectionSites = new LinkedHashMap<>();
        Map<String, String> sectionBasePaths = new LinkedHashMap<>();
        Map<String, Map<Integer, Component>> indexBySectionPage = new LinkedHashMap<>();
        Map<String, Map<String, Component>> detailBySectionSlug = new LinkedHashMap<>();

        for (ContentSectionConfig config : sectionConfigs) {
            GeneratedSection generated = generateSection(config, warnings);
            sectionSites.put(config.sectionKey(), generated.site());
            sectionBasePaths.put(config.sectionKey(), config.basePath());
            indexBySectionPage.put(config.sectionKey(), generated.site().indexPagesByPage());
            detailBySectionSlug.put(config.sectionKey(), generated.site().detailPagesBySlug());
        }

        ContentRouteIndex routeIndex = new ContentRouteIndex(sectionBasePaths, indexBySectionPage, detailBySectionSlug);
        return new ContentSiteBundle(sectionSites, routeIndex, warnings);
    }

    private GeneratedSection generateSection(ContentSectionConfig config, List<ContentWarning> warnings) {
        List<LoadedEntry> discoveredEntries = discoverEntries(config, warnings);
        List<LoadedEntry> withUniqueSlugs = uniquifySlugs(discoveredEntries, config, warnings);

        List<LoadedEntry> renderableEntries = withUniqueSlugs.stream()
            .filter(entry -> !entry.draft())
            .sorted(Comparator
                .comparing(LoadedEntry::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LoadedEntry::slug))
            .toList();

        List<ContentEntryRecord> records = new ArrayList<>(renderableEntries.size());
        Map<String, Component> detailsBySlug = new LinkedHashMap<>();

        for (LoadedEntry entry : renderableEntries) {
            String detailRoute = buildDetailRoute(config.basePath(), entry.slug());
            ContentEntryRecord record = entry.toRecord(config.sectionKey(), detailRoute);
            records.add(record);
            detailsBySlug.put(entry.slug(), buildDetailPage(config, record));
        }

        Map<Integer, Component> indexByPage = buildIndexPages(config, records);

        ContentSectionSite sectionSite = new ContentSectionSite(
            config.sectionKey(),
            config.basePath(),
            indexByPage,
            detailsBySlug,
            records
        );
        return new GeneratedSection(sectionSite);
    }

    private Map<Integer, Component> buildIndexPages(ContentSectionConfig config, List<ContentEntryRecord> entries) {
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / config.pageSize()));
        Map<Integer, Component> pages = new LinkedHashMap<>();

        for (int page = 1; page <= totalPages; page++) {
            int startIndex = (page - 1) * config.pageSize();
            int endIndex = Math.min(startIndex + config.pageSize(), entries.size());
            List<ContentEntryRecord> pageEntries = startIndex < endIndex
                ? entries.subList(startIndex, endIndex)
                : List.of();
            pages.put(page, buildIndexPage(config, pageEntries, page, totalPages));
        }

        return pages;
    }

    private Component buildIndexPage(
        ContentSectionConfig config,
        List<ContentEntryRecord> entries,
        int page,
        int totalPages
    ) {
        HtmlTag shell = new HtmlTag("section").withAttribute("class", "sp-content-index-shell content-module");
        shell.withChild(Header.H1(config.sectionTitle()).withClass("sp-content-index-title"));

        HtmlTag listContainer = new HtmlTag("div").withAttribute("class", "sp-content-index-list");
        if (entries.isEmpty()) {
            listContainer.withChild(new Paragraph("No content entries found.").withClass("sp-content-index-empty"));
        } else {
            for (ContentEntryRecord entry : entries) {
                ContentListItemComponent listItem = Objects.requireNonNull(
                    config.listItemComponentSupplier().get(),
                    "listItemComponentSupplier returned null"
                );
                listContainer.withChild(listItem
                    .withSlug(entry.slug())
                    .withRoute(entry.route())
                    .withTitle(entry.title())
                    .withSummary(entry.summary())
                    .withAuthor(extractAuthor(entry))
                    .withPublishedAt(formatDate(entry.publishedAt()))
                    .withTags(entry.tags()));
            }
        }

        shell.withChild(listContainer);
        shell.withChild(buildPagination(config.basePath(), page, totalPages));

        Page builtPage = Page.builder().addComponents(shell).build();
        builtPage.addClass("sp-content-index-page");
        return builtPage;
    }

    private Component buildDetailPage(ContentSectionConfig config, ContentEntryRecord entry) {
        MarkdownRenderResult markdown = renderMarkdown(entry.markdown(), config.unsafeMarkdownEnabled());
        HtmlTag stickyToc = buildStickyToc(markdown.headings());

        HtmlTag article = new HtmlTag("article").withAttribute("class", "sp-content-detail-shell content-module");
        HtmlTag header = new HtmlTag("header").withAttribute("class", "sp-content-detail-header");

        String byline = formatByline(extractAuthor(entry), formatDate(entry.publishedAt()));
        if (!byline.isBlank()) {
            header.withChild(new Paragraph(byline).withClass("sp-content-detail-byline"));
        }

        if (!entry.tags().isEmpty()) {
            header.withChild(buildTagGroup(entry.tags(), "sp-content-detail-tags sp-content-detail-tags-top"));
        }
        HtmlTag markdownBody = new HtmlTag("div")
            .withAttribute("class", "module-content sp-content-markdown-body");

        if (!byline.isBlank() || !entry.tags().isEmpty()) {
            markdownBody.withChild(header);
        }

        HtmlTag detailContent = new HtmlTag("div")
            .withAttribute("class", "sp-content-detail-content")
            .withChild(RawHtml.create(markdown.html()));

        markdownBody.withChild(detailContent);

        article.withChild(markdownBody);
        article.withChild(buildDetailFooter(config));

        Page page = Page.builder()
            .withStickySidebar(stickyToc, 10, 2)
            .addComponents(article)
            .build();

        page.addClass("sp-content-detail-page");
        return page;
    }

    private HtmlTag buildDetailFooter(ContentSectionConfig config) {
        HtmlTag footer = new HtmlTag("footer").withAttribute("class", "sp-content-detail-footer");
        footer.withChild(new HtmlTag("a")
            .withAttribute("class", "sp-content-detail-back-link")
            .withAttribute("href", config.basePath())
            .withInnerText("Back to " + config.sectionTitle()));
        return footer;
    }

    private HtmlTag buildStickyToc(List<TocHeading> headings) {
        HtmlTag nav = new HtmlTag("nav")
            .withAttribute("class", "sidenav sp-content-toc")
            .withAttribute("aria-label", "Page navigation");

        nav.withChild(new HtmlTag("div")
            .withAttribute("class", "sidenav-section")
            .withInnerText("On this page"));

        if (headings.isEmpty()) {
            nav.withChild(new HtmlTag("div")
                .withAttribute("class", "sp-content-toc-empty")
                .withInnerText("No section headings found."));
            return nav;
        }

        for (TocHeading heading : headings) {
            nav.withChild(new HtmlTag("a")
                .withAttribute("href", "#" + heading.id())
                .withAttribute("title", heading.title())
                .withAttribute("class", "sidenav-item sp-content-toc-item level-" + heading.level())
                .withInnerText(heading.title()));
        }

        return nav;
    }

    private HtmlTag buildPagination(String basePath, int page, int totalPages) {
        HtmlTag pagination = new HtmlTag("div")
            .withAttribute("class", "sp-content-pagination")
            .withAttribute("data-page", String.valueOf(page))
            .withAttribute("data-total-pages", String.valueOf(totalPages));

        pagination.withChild(buildPaginationLink(basePath, page - 1, page > 1, "sp-content-page-prev", "Previous"));
        pagination.withChild(new HtmlTag("span")
            .withAttribute("class", "sp-content-page-status")
            .withInnerText("Page " + page + " of " + totalPages));
        pagination.withChild(buildPaginationLink(basePath, page + 1, page < totalPages, "sp-content-page-next", "Next"));

        return pagination;
    }

    private Component buildPaginationLink(String basePath, int targetPage, boolean enabled, String cssClass, String label) {
        if (!enabled) {
            return new HtmlTag("span")
                .withAttribute("class", "sp-content-page-link " + cssClass + " disabled")
                .withInnerText(label);
        }

        return new HtmlTag("a")
            .withAttribute("class", "sp-content-page-link " + cssClass)
            .withAttribute("href", buildIndexRoute(basePath, targetPage))
            .withInnerText(label);
    }

    private HtmlTag buildTagGroup(List<String> tags, String className) {
        HtmlTag tagGroup = new HtmlTag("div").withAttribute("class", className);
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            tagGroup.withChild(Tag.create(tag));
        }
        return tagGroup;
    }

    private String extractAuthor(ContentEntryRecord entry) {
        String author = entry.metadata().get("author");
        return author == null ? "" : author.trim();
    }

    private String formatByline(String author, String publishedAt) {
        boolean hasAuthor = author != null && !author.isBlank();
        boolean hasDate = publishedAt != null && !publishedAt.isBlank();

        if (hasAuthor && hasDate) {
            return "By " + author + " on " + publishedAt;
        }
        if (hasAuthor) {
            return "By " + author;
        }
        return hasDate ? publishedAt : "";
    }

    private List<LoadedEntry> discoverEntries(ContentSectionConfig config, List<ContentWarning> warnings) {
        Path root = config.directoryPath();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            warnings.add(new ContentWarning(config.sectionKey(), root, "Configured directory does not exist or is not a directory"));
            return List.of();
        }

        List<Path> markdownFiles;
        try (Stream<Path> stream = Files.walk(root, config.maxDepth())) {
            markdownFiles = stream
                .filter(Files::isRegularFile)
                .filter(this::isMarkdownFile)
                .sorted()
                .toList();
        } catch (IOException e) {
            warnings.add(new ContentWarning(config.sectionKey(), root, "Failed to scan section directory: " + e.getMessage()));
            return List.of();
        }

        List<LoadedEntry> entries = new ArrayList<>();
        for (Path sourcePath : markdownFiles) {
            LoadedEntry entry = parseEntryFile(config, sourcePath, warnings);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private LoadedEntry parseEntryFile(ContentSectionConfig config, Path sourcePath, List<ContentWarning> warnings) {
        String raw;
        try {
            raw = Files.readString(sourcePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            warnings.add(new ContentWarning(config.sectionKey(), sourcePath, "Failed to read markdown file: " + e.getMessage()));
            return null;
        }

        ParsedMarkdown parsed = parseFrontmatter(raw);
        for (String warning : parsed.warnings()) {
            warnings.add(new ContentWarning(config.sectionKey(), sourcePath, warning));
        }

        Map<String, String> metadata = parsed.metadata();
        String markdownBody = parsed.markdownBody();

        String rawSlug = metadata.get("slug");
        String fallbackSlug = fallbackSlug(config.directoryPath(), sourcePath);
        String slug = rawSlug == null || rawSlug.isBlank() ? fallbackSlug : slugify(rawSlug);
        if (slug.isBlank()) {
            slug = fallbackSlug;
        }

        String title = metadata.get("title");
        if (title == null || title.isBlank()) {
            title = firstHeading(markdownBody);
        }
        if (title == null || title.isBlank()) {
            title = titleFromSlug(slug);
        }

        String summary = metadata.get("summary");
        if (summary == null || summary.isBlank()) {
            summary = extractSummary(markdownBody);
        }

        LocalDateTime publishedAt = parseDate(metadata.get("date"), config.sectionKey(), sourcePath, warnings);
        List<String> tags = parseTags(metadata.get("tags"));
        boolean draft = parseDraft(metadata.get("draft"), config.sectionKey(), sourcePath, warnings);

        return new LoadedEntry(sourcePath, slug, title, summary, publishedAt, tags, markdownBody, metadata, draft);
    }

    private List<LoadedEntry> uniquifySlugs(
        List<LoadedEntry> entries,
        ContentSectionConfig config,
        List<ContentWarning> warnings
    ) {
        Map<String, Integer> slugCounts = new LinkedHashMap<>();
        List<LoadedEntry> adjusted = new ArrayList<>(entries.size());

        for (LoadedEntry entry : entries) {
            String baseSlug = entry.slug();
            int next = slugCounts.getOrDefault(baseSlug, 0) + 1;
            slugCounts.put(baseSlug, next);

            if (next == 1) {
                adjusted.add(entry);
                continue;
            }

            String adjustedSlug = baseSlug + "-" + next;
            warnings.add(new ContentWarning(
                config.sectionKey(),
                entry.sourcePath(),
                "Duplicate slug '" + baseSlug + "' adjusted to '" + adjustedSlug + "'"
            ));
            adjusted.add(entry.withSlug(adjustedSlug));
        }

        return adjusted;
    }

    private MarkdownRenderResult renderMarkdown(String markdown, boolean allowUnsafeMarkdown) {
        Node document = MARKDOWN_PARSER.parse(markdown == null ? "" : markdown);

        IdentityHashMap<Heading, String> headingIds = new IdentityHashMap<>();
        List<TocHeading> tocHeadings = collectHeadings(document, headingIds);

        HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(TABLE_EXTENSIONS)
            .escapeHtml(!allowUnsafeMarkdown)
            .sanitizeUrls(true)
            .attributeProviderFactory(context -> (node, tagName, attributes) -> {
                if (node instanceof Heading heading) {
                    String id = headingIds.get(heading);
                    if (id != null && !id.isBlank()) {
                        attributes.put("id", id);
                    }
                }
            })
            .build();

        return new MarkdownRenderResult(renderer.render(document), tocHeadings);
    }

    private List<TocHeading> collectHeadings(Node document, IdentityHashMap<Heading, String> headingIds) {
        List<TocHeading> toc = new ArrayList<>();
        Map<String, Integer> slugCounts = new LinkedHashMap<>();

        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                String title = normalizeWhitespace(nodeText(heading));
                if (title.isBlank()) {
                    title = "Section";
                }

                String base = slugify(title);
                if (base.isBlank()) {
                    base = "section";
                }

                int count = slugCounts.getOrDefault(base, 0) + 1;
                slugCounts.put(base, count);
                String id = count == 1 ? base : base + "-" + count;
                headingIds.put(heading, id);

                if (heading.getLevel() >= 2 && heading.getLevel() <= 4) {
                    toc.add(new TocHeading(heading.getLevel(), title, id));
                }

                visitChildren(heading);
            }
        });

        return List.copyOf(toc);
    }

    private ParsedMarkdown parseFrontmatter(String rawMarkdown) {
        String normalized = (rawMarkdown == null ? "" : rawMarkdown).replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return new ParsedMarkdown(Map.of(), normalized, List.of());
        }

        String[] lines = normalized.split("\n", -1);
        int closingLine = -1;
        for (int i = 1; i < lines.length; i++) {
            if ("---".equals(lines[i].trim())) {
                closingLine = i;
                break;
            }
        }

        if (closingLine < 0) {
            return new ParsedMarkdown(
                Map.of(),
                normalized,
                List.of("Frontmatter block starts with '---' but has no closing '---'; ignored")
            );
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        List<String> parseWarnings = new ArrayList<>();

        for (int i = 1; i < closingLine; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                parseWarnings.add("Ignoring malformed frontmatter line: " + line.trim());
                continue;
            }

            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            metadata.put(key, value);
        }

        String body = String.join("\n", java.util.Arrays.copyOfRange(lines, closingLine + 1, lines.length));
        return new ParsedMarkdown(Map.copyOf(metadata), body, List.copyOf(parseWarnings));
    }

    private LocalDateTime parseDate(
        String rawDate,
        String sectionKey,
        Path sourcePath,
        List<ContentWarning> warnings
    ) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String value = rawDate.trim();
        try {
            return LocalDateTime.parse(value, DATE_TIME_META_FORMAT);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        try {
            LocalDate parsed = LocalDate.parse(value, DATE_ONLY_META_FORMAT);
            return parsed.atStartOfDay();
        } catch (DateTimeParseException ignored) {
            warnings.add(new ContentWarning(
                sectionKey,
                sourcePath,
                "Invalid date format '" + rawDate + "' (accepted: yyyy-MM-dd or yyyy-MM-dd'T'HH:mm); value ignored"
            ));
            return null;
        }
    }

    private boolean parseDraft(
        String rawDraft,
        String sectionKey,
        Path sourcePath,
        List<ContentWarning> warnings
    ) {
        if (rawDraft == null || rawDraft.isBlank()) {
            return false;
        }

        String value = rawDraft.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }

        warnings.add(new ContentWarning(
            sectionKey,
            sourcePath,
            "Invalid draft value '" + rawDraft + "'; expected true/false. Using false."
        ));
        return false;
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }

        String normalized = rawTags.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        Set<String> deduped = new LinkedHashSet<>();
        for (String part : normalized.split(",")) {
            String tag = part.trim();
            if (!tag.isBlank()) {
                deduped.add(tag);
            }
        }

        return List.copyOf(deduped);
    }

    private String firstHeading(String markdownBody) {
        if (markdownBody == null || markdownBody.isBlank()) {
            return "";
        }

        for (String line : markdownBody.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                int idx = 0;
                while (idx < trimmed.length() && trimmed.charAt(idx) == '#') {
                    idx++;
                }
                if (idx < trimmed.length() && trimmed.charAt(idx) == ' ') {
                    return normalizeWhitespace(trimmed.substring(idx + 1));
                }
            }
        }
        return "";
    }

    private String extractSummary(String markdownBody) {
        if (markdownBody == null || markdownBody.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean inCodeFence = false;

        for (String line : markdownBody.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (inCodeFence || trimmed.isBlank()) {
                if (!sb.isEmpty()) {
                    break;
                }
                continue;
            }
            if (trimmed.startsWith("#")) {
                continue;
            }

            String plain = stripMarkdownDecorators(trimmed);
            if (plain.isBlank()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(plain);

            if (sb.length() >= 200) {
                break;
            }
        }

        String summary = normalizeWhitespace(sb.toString());
        if (summary.length() > 200) {
            summary = summary.substring(0, 197).trim() + "...";
        }
        return summary;
    }

    private String stripMarkdownDecorators(String value) {
        String cleaned = value
            .replaceAll("^[-*+]\\s+", "")
            .replaceAll("^\\d+\\.\\s+", "")
            .replaceAll("^>\\s+", "")
            .replaceAll("`", "")
            .replaceAll("\\*\\*", "")
            .replaceAll("__", "")
            .replaceAll("\\*", "")
            .replaceAll("_", "")
            .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1")
            .replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "$1")
            .trim();
        return normalizeWhitespace(cleaned);
    }

    private String fallbackSlug(Path sectionRoot, Path sourcePath) {
        Path relative = sectionRoot.toAbsolutePath().normalize().relativize(sourcePath.toAbsolutePath().normalize());
        String raw = relative.toString().replace('\\', '/');
        if (raw.toLowerCase(Locale.ROOT).endsWith(".md")) {
            raw = raw.substring(0, raw.length() - 3);
        }
        String slug = slugify(raw.replace('/', '-'));
        return slug.isBlank() ? "entry" : slug;
    }

    private static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String slug = lower
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+", "")
            .replaceAll("-+$", "")
            .replaceAll("-{2,}", "-");
        return slug;
    }

    private String titleFromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return "Untitled";
        }
        return java.util.Arrays.stream(slug.split("-"))
            .filter(part -> !part.isBlank())
            .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
            .collect(Collectors.joining(" "));
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATE_DISPLAY_FORMAT.format(value);
    }

    private String nodeText(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                sb.append(code.getLiteral());
            }

            @Override
            public void visit(SoftLineBreak softLineBreak) {
                sb.append(' ');
            }

            @Override
            public void visit(HardLineBreak hardLineBreak) {
                sb.append(' ');
            }

            @Override
            public void visit(FencedCodeBlock fencedCodeBlock) {
                sb.append(fencedCodeBlock.getLiteral());
            }
        });
        return sb.toString();
    }

    private boolean isMarkdownFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".md");
    }

    private static String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String buildIndexRoute(String basePath, int page) {
        if (page <= 1) {
            return basePath;
        }
        return basePath + "?page=" + page;
    }

    private static String buildDetailRoute(String basePath, String slug) {
        return basePath + "/" + slug;
    }

    private record LoadedEntry(
        Path sourcePath,
        String slug,
        String title,
        String summary,
        LocalDateTime publishedAt,
        List<String> tags,
        String markdown,
        Map<String, String> metadata,
        boolean draft
    ) {
        private LoadedEntry {
            tags = tags == null ? List.of() : List.copyOf(tags);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }

        private LoadedEntry withSlug(String adjustedSlug) {
            return new LoadedEntry(sourcePath, adjustedSlug, title, summary, publishedAt, tags, markdown, metadata, draft);
        }

        private ContentEntryRecord toRecord(String sectionKey, String route) {
            return new ContentEntryRecord(
                sectionKey,
                slug,
                title,
                summary,
                publishedAt,
                tags,
                markdown,
                sourcePath,
                route,
                metadata
            );
        }
    }

    private record ParsedMarkdown(
        Map<String, String> metadata,
        String markdownBody,
        List<String> warnings
    ) {}

    private record MarkdownRenderResult(String html, List<TocHeading> headings) {}

    private record TocHeading(int level, String title, String id) {}

    private record GeneratedSection(ContentSectionSite site) {}
}
