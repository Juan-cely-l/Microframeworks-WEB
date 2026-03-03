package WebFramework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpServerIntegrationTest {

    @BeforeEach
    void setUp() {
        HttpServer.clearRoutesForTests();
        HttpServer.staticfiles("/webroot");
        HttpServer.setRoutePrefix("/App");
        HttpServer.get("/hello", (req, resp) -> "Hello " + req.getValues("name"));
        HttpServer.get("/pi", (req, resp) -> String.valueOf(Math.PI));
    }

    @Test
    void shouldServeHelloRouteWithQueryValue() throws Exception {
        HttpTestResponse response = send("GET", "/App/hello?name=Peter");
        assertEquals(200, response.statusCode());
        assertEquals("Hello Peter", response.bodyAsText());
    }

    @Test
    void shouldServeHelloRouteWithoutQueryValue() throws Exception {
        HttpTestResponse response = send("GET", "/App/hello");
        assertEquals(200, response.statusCode());
        assertEquals("Hello ", response.bodyAsText());
    }

    @Test
    void shouldServePiRoute() throws Exception {
        HttpTestResponse response = send("GET", "/App/pi");
        assertEquals(200, response.statusCode());
        assertEquals(String.valueOf(Math.PI), response.bodyAsText());
    }

    @Test
    void shouldServeStaticHtmlFile() throws Exception {
        HttpTestResponse response = send("GET", "/index.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.header("content-type").startsWith("text/html"));
        assertTrue(response.bodyAsText().contains("Microframework Demo"));
    }

    @Test
    void shouldReturnNotFoundForUnknownPath() throws Exception {
        HttpTestResponse response = send("GET", "/no-existe");
        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldReturnMethodNotAllowedForPost() throws Exception {
        HttpTestResponse response = send("POST", "/App/pi");
        assertEquals(405, response.statusCode());
    }

    @Test
    void shouldBlockPathTraversal() throws Exception {
        HttpTestResponse response = send("GET", "/../pom.xml");
        assertTrue(response.statusCode() == 403 || response.statusCode() == 404);
    }

    @Test
    void shouldServePngStaticFile() throws Exception {
        HttpTestResponse response = send("GET", "/logo.png");
        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.header("content-type"));
        assertTrue(response.body().length > 0);
    }

    @Test
    void shouldDecodeEncodedQueryParameters() throws Exception {
        HttpTestResponse response = send("GET", "/App/hello?name=John+Smith");
        assertEquals(200, response.statusCode());
        assertEquals("Hello John Smith", response.bodyAsText());
    }

    private HttpTestResponse send(String method, String path) throws Exception {
        String rawRequest = method + " " + path + " HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        byte[] rawResponse = HttpServer.processRawRequestForTests(rawRequest);
        return HttpTestResponse.parse(rawResponse);
    }

    private record HttpTestResponse(int statusCode, java.util.Map<String, String> headers, byte[] body) {
        static HttpTestResponse parse(byte[] rawResponse) {
            int separatorIndex = -1;
            for (int i = 0; i < rawResponse.length - 3; i++) {
                if (rawResponse[i] == '\r'
                        && rawResponse[i + 1] == '\n'
                        && rawResponse[i + 2] == '\r'
                        && rawResponse[i + 3] == '\n') {
                    separatorIndex = i;
                    break;
                }
            }
            if (separatorIndex < 0) {
                throw new IllegalStateException("Invalid HTTP response");
            }

            String headerText = new String(rawResponse, 0, separatorIndex, StandardCharsets.UTF_8);
            String[] lines = headerText.split("\r\n");
            String[] statusLineParts = lines[0].split(" ");
            int statusCode = Integer.parseInt(statusLineParts[1]);

            java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                String name = lines[i].substring(0, colon).trim().toLowerCase();
                String value = lines[i].substring(colon + 1).trim();
                headers.put(name, value);
            }

            int bodyStart = separatorIndex + 4;
            byte[] body = java.util.Arrays.copyOfRange(rawResponse, bodyStart, rawResponse.length);
            return new HttpTestResponse(statusCode, headers, body);
        }

        String header(String name) {
            return headers.getOrDefault(name.toLowerCase(), "");
        }

        String bodyAsText() {
            Charset charset = StandardCharsets.UTF_8;
            return new String(body, charset);
        }
    }
}
