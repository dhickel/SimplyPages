package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Divider;
import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.editing.EditMode;
import io.mindspice.simplypages.editing.EditablePage;
import io.mindspice.simplypages.editing.EditableRow;
import io.mindspice.simplypages.layout.Column;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.layout.Row;
import io.mindspice.simplypages.modules.SimpleListModule;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Actual direct/fresh-compiled Java output; no replacement CSS, controls or HTTP endpoints. */
public class Phase18RenderingBrowserFixtureTest {
    private static final SlotKey<String> TEXT = SlotKey.of("phase18-native-text");
    private static final String VALUE = "<雪> & \"request\" 😀";

    @Test
    void generatesCurrentDirectAndFreshCompiledMarkupWithUnchangedStoredState() throws Exception {
        String html = generate();
        var document = Jsoup.parse(html);
        assertEquals(2, document.select(".phase18-sample").size());
        assertEquals(8, document.select(".divider-text").size());
        assertEquals(4, document.select(".editable-row-wrapper").size());
        assertEquals(4, document.select(".module-edit-btn").size());
        assertEquals(4, document.select(".module-delete-btn").size());
        assertEquals(6, document.select(".insert-row-section").size());
        assertTrue(document.select(".ignored-wrapper-child").isEmpty());
        assertEquals(document.select("[id]").size(), document.select("[id]").stream()
                .map(element -> element.id()).distinct().count());
        Path output = Path.of("target", "phase18-rendering-browser-fixture.html");
        Files.createDirectories(output.getParent());
        Files.writeString(output, html);
    }

    /** Standalone generation against an extracted current packaged library, not target/classes. */
    public static void main(String[] args) throws Exception {
        Files.writeString(Path.of(args[0]), generate(), StandardOpenOption.CREATE_NEW);
    }

    private static String generate() throws Exception {
        Div fixture = new Div().withId("phase18-rendering-fixture");
        for (String mode : List.of("direct", "compiled")) {
            Div sample = new Div().withClass("phase18-sample").withAttribute("data-mode", mode);
            Paragraph paragraph = new Paragraph().center().withId(mode + "-paragraph");
            paragraph.withAttribute("CLASS", "caller align-right aligned-right");
            paragraph.withAttribute("data-escaped", VALUE);
            paragraph.withInnerText(TEXT);
            paragraph.withChild(new HtmlTag("span").withClass("paragraph-child").withInnerText(" Child"));
            sample.withChild(paragraph);
            List<HtmlTag> stable = new ArrayList<>();
            stable.add(paragraph);
            for (String kind : List.of("plain", "text", "empty", "vertical")) {
                Divider divider = kind.equals("vertical") ? Divider.vertical().withHeight("48px") : Divider.horizontal();
                divider.withId(mode + "-" + kind);
                divider.withAttribute("data-kind", kind);
                divider.withAttribute("CLASS", "divider divider-" + (kind.equals("vertical") ? "vertical" : "horizontal")
                        + " caller divider-with-textish" + (kind.equals("plain") ? " divider-with-text" : ""));
                divider.withAttribute("STYLE", kind.equals("vertical")
                        ? "border-left: 9px dotted red; display: none; height: 48px; --phase18-owned: retained;"
                        : "border-top: 9px solid red; width: 17px; display: none; line-height: 9;"
                        + " position: absolute; text-align: right; margin: 90px; --phase18-owned: retained;");
                divider.thick().dashed().withColor("rgb(12, 34, 56)");
                if (kind.equals("text")) divider.withText(VALUE);
                if (kind.equals("empty")) divider.withText("");
                if (kind.equals("vertical")) divider.withText("Ignored vertical text");
                if (kind.equals("text")) divider.withChild(new HtmlTag("span").withClass("divider-child").withInnerText(TEXT));
                stable.add(divider);
                sample.withChild(divider);
            }
            // One configured instance traverses all states through an already compiled live delegate.
            Divider transition = Divider.horizontal().withText(null);
            transition.withAttribute("style", "position: fixed; text-align: right; margin: 9px;"
                    + "border-top: 9px dotted red; width: 19px; display: none; line-height: 9; height: 4px;");
            var transitionTemplate = transition.compile();
            Object beforeTransition = state(transition);
            for (String step : List.of("plain", "text", "empty", "again")) {
                transition.withText(step.equals("plain") || step.equals("again") ? null : step.equals("empty") ? "" : VALUE);
                String rendered = mode.equals("compiled") ? transitionTemplate.render(RenderContext.empty()) : transition.render();
                // Trusted insertion of the actual rendered bytes, never handwritten component markup.
                sample.withChild(new Div().withAttribute("data-transition", step).withUnsafeHtml(rendered));
                require(beforeTransition.equals(state(transition)), "transition stored-state mutation");
            }
            {
                String prefix = mode + "-r0";
                Row source = new Row();
                source.withId(prefix + "-source");
                source.withAttribute("data-source", "preserved");
                source.addColumn(Column.create().withWidth(5).withChild(
                        Button.create("Source 0").withAttribute("data-source-control", "0")));
                source.addColumn(Column.create().withWidth(7).withChild(new Paragraph().withInnerText(TEXT)));
                SimpleListModule list = SimpleListModule.create().withTitle("Registered 0")
                        .addItem(ListItem.create("Raw unassigned")).addItem(ListItem.create(VALUE));
                list.build();
                EditableRow row = EditableRow.wrap(source, prefix, mode + "/page").withEditMode(EditMode.USER_EDIT)
                        .withCanAddModule(true).addEditableModule(list, prefix + "-list");
                row.withChild(new Div().withClass("ignored-wrapper-child").withInnerText("Must not emit"));
                EditablePage page = EditablePage.create(mode + "/page").addEditableRow(row);
                EditableRow second = secondRow(mode);
                stable.add(second);
                page.addEditableRow(second);
                page.withChild(new Div().withClass("ignored-wrapper-child"));
                stable.add(page);
                sample.withChild(page);
                stable.add(row);
                stable.add(source);
            }
            EditablePage emptyPage = EditablePage.create(mode + "-empty");
            stable.add(emptyPage);
            sample.withChild(emptyPage);
            AccountWidget account = AccountWidget.createAuthenticated("Native User");
            account.withId(mode + "-account");
            stable.add(account);
            sample.withChild(account);
            Page.PageBuilder builder = Page.builder().addComponents(new Paragraph("Standard Page preserved"));
            Page page = builder.build();
            require(builder.build() == page, "Page build identity");
            stable.add(page);
            sample.withChild(page);
            List<Object> states = new ArrayList<>();
            for (HtmlTag tag : stable) states.add(state(tag));
            var compiled = sample.compile(); // Before any direct rendering of this sample.
            for (int i = 0; i < stable.size(); i++) require(states.get(i).equals(state(stable.get(i))), "compile mutation");
            String rendered = mode.equals("compiled") ? compiled.render(RenderContext.of(java.util.Map.of(TEXT, VALUE)))
                    : sample.render(RenderContext.of(java.util.Map.of(TEXT, VALUE)));
            for (int i = 0; i < stable.size(); i++) require(states.get(i).equals(state(stable.get(i))), "render mutation");
            fixture.withChild(new Div().withUnsafeHtml(rendered));
        }
        return fixture.render();
    }

    private static EditableRow secondRow(String mode) {
        Row source = new Row();
        source.withId(mode + "-r1-source");
        source.addColumn(Column.create().withWidth(12).withChild(Button.create("Source 1")
                .withAttribute("data-source-control", "1")));
        SimpleListModule list = SimpleListModule.create().withTitle("Registered 1").addItem(ListItem.create("Second list"));
        list.build();
        return EditableRow.wrap(source, mode + "-r1", mode + "/page").withCanAddModule(false)
                .withEditMode(EditMode.USER_EDIT).addEditableModule(list, mode + "-r1-list");
    }

    private static Object state(HtmlTag tag) throws Exception {
        List<Object> state = new ArrayList<>();
        for (String name : List.of("attributes", "children", "id", "innerText", "innerTextSlot", "trustedHtml")) {
            Field field = HtmlTag.class.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(tag);
            state.add(value instanceof List<?> values ? new ArrayList<>(values) : value);
        }
        return state;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
