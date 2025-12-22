package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Markdown;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Timeline module for displaying chronological events.
 *
 * <p>Perfect for grow journals, project history, research progress, or any
 * time-based sequence of events.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Grow journal timeline
 * TimelineModule.create()
 *     .withTitle("My Grow Journal")
 *     .addEvent("Day 1", "Germination", "Seeds placed in wet paper towel")
 *     .addEvent("Day 7", "Seedling", "First true leaves appeared")
 *     .addEvent("Day 21", "Vegetative", "Switched to 18/6 light cycle");
 *
 * // Research timeline with custom content
 * TimelineModule.create()
 *     .vertical()
 *     .addEvent("2020", "Phase 1", customComponent);
 * }</pre>
 */
public class TimelineModule extends Module {

    public static class TimelineEvent {
        private final String date;
        private final String eventTitle;
        private final String description;
        private final Component customContent;

        public TimelineEvent(String date, String eventTitle, String description) {
            this.date = date;
            this.eventTitle = eventTitle;
            this.description = description;
            this.customContent = null;
        }

        public TimelineEvent(String date, String eventTitle, Component customContent) {
            this.date = date;
            this.eventTitle = eventTitle;
            this.description = null;
            this.customContent = customContent;
        }

        public String getDate() { return date; }
        public String getEventTitle() { return eventTitle; }
        public String getDescription() { return description; }
        public Component getCustomContent() { return customContent; }
    }

    public enum Orientation {
        VERTICAL, HORIZONTAL
    }

    private List<TimelineEvent> events = new ArrayList<>();
    private Orientation orientation = Orientation.VERTICAL;

    public TimelineModule() {
        super("div");
        this.withClass("timeline-module");
    }

    public static TimelineModule create() {
        return new TimelineModule();
    }

    @Override
    public TimelineModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public TimelineModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Adds a timeline event with date, title, and description.
     *
     * @param date the date or timestamp for the event
     * @param eventTitle the event title
     * @param description the event description
     */
    public TimelineModule addEvent(String date, String eventTitle, String description) {
        this.events.add(new TimelineEvent(date, eventTitle, description));
        return this;
    }

    /**
     * Adds a timeline event with date, title, and custom content.
     *
     * @param date the date or timestamp for the event
     * @param eventTitle the event title
     * @param content custom component for event content
     */
    public TimelineModule addEvent(String date, String eventTitle, Component content) {
        this.events.add(new TimelineEvent(date, eventTitle, content));
        return this;
    }

    /**
     * Sets vertical orientation (default).
     */
    public TimelineModule vertical() {
        this.orientation = Orientation.VERTICAL;
        return this;
    }

    /**
     * Sets horizontal orientation.
     */
    public TimelineModule horizontal() {
        this.orientation = Orientation.HORIZONTAL;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        String orientationClass = "timeline-" + orientation.name().toLowerCase();
        Div timeline = new Div().withClass("timeline " + orientationClass);

        for (int i = 0; i < events.size(); i++) {
            TimelineEvent event = events.get(i);

            Div eventItem = new Div().withClass("timeline-item");

            // Date marker
            Div dateMarker = new Div().withClass("timeline-date");
            HtmlTag dateText = new HtmlTag("span")
                .withAttribute("class", "date-text")
                .withInnerText(event.getDate());
            dateMarker.withChild(dateText);
            eventItem.withChild(dateMarker);

            // Event content
            Div eventContent = new Div().withClass("timeline-content");

            // Event title
            if (event.getEventTitle() != null && !event.getEventTitle().isEmpty()) {
                eventContent.withChild(
                    Header.H3(event.getEventTitle()).withClass("event-title")
                );
            }

            // Event description or custom content
            if (event.getCustomContent() != null) {
                eventContent.withChild(event.getCustomContent());
            } else if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                HtmlTag description = new HtmlTag("div")
                    .withAttribute("class", "event-description")
                    .withInnerText(event.getDescription());
                eventContent.withChild(description);
            }

            eventItem.withChild(eventContent);
            timeline.withChild(eventItem);
        }

        super.withChild(timeline);
    }
}
