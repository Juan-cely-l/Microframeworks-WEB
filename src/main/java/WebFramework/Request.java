package WebFramework;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final String protocol;
    private final Map<String, String> headers;
    private final Map<String, List<String>> queryParams;

    public Request(
            String method,
            String path,
            String protocol,
            Map<String, String> headers,
            String query
    ) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.queryParams = Collections.unmodifiableMap(parseQuery(query));
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHeader(String name) {
        return headers.getOrDefault(name, "");
    }

    public String getValues(String key) {
        List<String> values = queryParams.get(key);
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.get(0);
    }

    public List<String> getAllValues(String key) {
        List<String> values = queryParams.get(key);
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    static Map<String, List<String>> parseQuery(String query) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return params;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
