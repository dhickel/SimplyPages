package io.mindspice.simplypages.modules.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public record FileExplorerEndpoints(
    String listEndpoint,
    String navigateEndpointTemplate,
    String viewerEndpointTemplate,
    String inspectorEndpointTemplate,
    String modalEndpointTemplate,
    String actionEndpointTemplate,
    String pickerSelectEndpointTemplate
) {
    public String list(String path) {
        return resolve(listEndpoint, path);
    }

    public String navigate(String path) {
        return resolve(navigateEndpointTemplate, path);
    }

    public String view(String path) {
        return resolve(viewerEndpointTemplate, path);
    }

    public String inspect(String path) {
        return resolve(inspectorEndpointTemplate, path);
    }

    public String modal(String path) {
        return resolve(modalEndpointTemplate, path);
    }

    public String action(String path) {
        return resolve(actionEndpointTemplate, path);
    }

    public String pickerSelect(String path) {
        return resolve(pickerSelectEndpointTemplate, path);
    }

    private static String resolve(String endpointOrTemplate, String path) {
        if (endpointOrTemplate == null || endpointOrTemplate.isBlank()) { return endpointOrTemplate; }
        if (path == null) { return endpointOrTemplate; }
        String encoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
        if (endpointOrTemplate.contains("{path}")) {
            return endpointOrTemplate.replace("{path}", encoded);
        }
        return endpointOrTemplate + (endpointOrTemplate.contains("?") ? "&" : "?") + "path=" + encoded;
    }
}
