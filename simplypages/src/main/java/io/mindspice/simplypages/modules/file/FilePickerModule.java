package io.mindspice.simplypages.modules.file;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.Button;

public class FilePickerModule extends FileExplorerModule {
    private FilePickerModule(FileExplorerState state, FileExplorerConfig config) {
        super(state, config, FileExplorerInspectorSpec.defaults());
        withClass("file-picker-module");
    }

    public static FilePickerModule create(FileExplorerState state, FileExplorerEndpoints endpoints) {
        return new FilePickerModule(state, FileExplorerConfig.defaults(endpoints));
    }

    public static FilePickerModule create(FileExplorerState state, FileExplorerConfig config) {
        return new FilePickerModule(state, config);
    }

    public FilePickerModule withPickerTargetId(String pickerTargetId) {
        return new FilePickerModule(state, new FileExplorerConfig(
            config.endpoints(),
            config.explorerMode(),
            config.pickerMode(),
            config.rootId(),
            config.listTargetId(),
            config.inspectorTargetId(),
            config.viewerTargetId(),
            config.modalContainerId(),
            pickerTargetId,
            config.allowCreateFolder(),
            config.allowCreateText(),
            config.allowCreateMarkdown(),
            config.allowRename(),
            config.allowDelete(),
            config.allowCopyMove(),
            config.allowTags()
        ));
    }

    @Override
    protected void buildContent() {
        super.buildContent();
        Div footer = new Div().withClass("file-picker-footer")
            .withChild(new Paragraph("Selected path:").withClass("file-picker-label"))
            .withChild(new Div().withId(config.pickerCallbackTargetId()).withClass("file-picker-value").withInnerText(state.pickerValue() == null ? "None" : state.pickerValue()));
        if (config.endpoints().pickerSelect(state.selectedEntry() == null ? null : state.selectedEntry().path()) != null
            && config.pickerMode().allows(state.selectedEntry())) {
            Button select = Button.create("Select").withStyle(Button.ButtonStyle.SUCCESS);
            select.withAttribute("hx-post", config.endpoints().pickerSelect(state.selectedEntry().path()))
                .withAttribute("hx-target", "#" + config.pickerCallbackTargetId())
                .withAttribute("hx-swap", "outerHTML");
            footer.withChild(select);
        }
        withChild(footer);
    }
}
