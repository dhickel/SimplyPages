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
        return resolve(listEndpoint, path, null);
    }

    public String navigate(String path) {
        return resolve(navigateEndpointTemplate, path, null);
    }

    public String view(String path) {
        return resolve(viewerEndpointTemplate, path, null);
    }

    public String inspect(String path) {
        return resolve(inspectorEndpointTemplate, path, null);
    }

    public String modal(String path) {
        return modal("delete", path);
    }

    public String modal(String action, String path) {
        return resolve(modalEndpointTemplate, path, action);
    }

    public String action(String path) {
        return action("run", path);
    }

    public String action(String action, String path) {
        return resolve(actionEndpointTemplate, path, action);
    }

    public String pickerSelect(String path) {
        return resolve(pickerSelectEndpointTemplate, path, null);
    }

    private static String resolve(String endpointOrTemplate, String path, String action) {
        if (endpointOrTemplate == null || endpointOrTemplate.isBlank()) { return endpointOrTemplate; }
        String resolved = endpointOrTemplate;
        if (action != null) {
            String encodedAction = encodeQueryValue(action);
            if (resolved.contains("{action}")) {
                resolved = resolved.replace("{action}", encodePathValue(action));
            } else {
                resolved = resolved + (resolved.contains("?") ? "&" : "?") + "action=" + encodedAction;
            }
        }
        if (path == null) { return resolved; }
        if (endpointOrTemplate.contains("{path}")) {
            return resolved.replace("{path}", encodePathValue(path));
        }
        return resolved + (resolved.contains("?") ? "&" : "?") + "path=" + encodeQueryValue(path);
    }

    private static String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodePathValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
