package WebFramework;

import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private int statusCode = 200;
    private String contentType = "text/plain; charset=UTF-8";
    private final Map<String, String> headers = new LinkedHashMap<>();

    public Response status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Response type(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Response header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }
}
