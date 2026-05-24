package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Button;

public class FilePickerModule extends FileExplorerModule {
    private String pickerTargetId = "file-picker-value";

    private FilePickerModule(FileExplorerState state, FileExplorerEndpoints endpoints) {
        super(state, endpoints, FileExplorerInspectorSpec.defaults());
        withClass("file-picker-module");
    }

    public static FilePickerModule create(FileExplorerState state, FileExplorerEndpoints endpoints) {
        return new FilePickerModule(state, endpoints);
    }

    public FilePickerModule withPickerTargetId(String pickerTargetId) {
        this.pickerTargetId = pickerTargetId;
        return this;
    }

    @Override
    protected void buildContent() {
        super.buildContent();
        Div footer = new Div().withClass("file-picker-footer")
            .withChild(new Paragraph("Selected path:").withClass("file-picker-label"))
            .withChild(new Div().withId(pickerTargetId).withClass("file-picker-value").withInnerText(state.pickerValue() == null ? "None" : state.pickerValue()));
        if (endpoints.pickerSelectEndpoint() != null && state.selectedEntry() != null) {
            Button select = Button.create("Select").withStyle(Button.ButtonStyle.SUCCESS);
            select.withAttribute("hx-post", withPath(endpoints.pickerSelectEndpoint(), state.selectedEntry().path()))
                .withAttribute("hx-target", "#" + pickerTargetId)
                .withAttribute("hx-swap", "outerHTML");
            footer.withChild(select);
        }
        withChild(footer);
    }
}
