package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

/**
 * Module wrapper for timeline transcript components.
 */
public class TimelineTranscriptModule extends Module {

    private String description;
    private Component transcript;

    public TimelineTranscriptModule() {
        super("section");
        this.withClass("timeline-transcript-module");
    }

    public static TimelineTranscriptModule create() {
        return new TimelineTranscriptModule();
    }

    @Override
    public TimelineTranscriptModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public TimelineTranscriptModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    public TimelineTranscriptModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public TimelineTranscriptModule withTranscript(Component transcript) {
        this.transcript = transcript;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }
        if (description != null && !description.isEmpty()) {
            super.withChild(new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description));
        }
        super.withChild(transcript == null
            ? new HtmlTag("div").withAttribute("class", "timeline-transcript-empty").withInnerText("No transcript entries yet.")
            : transcript);
    }
}
