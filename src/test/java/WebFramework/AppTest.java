package WebFramework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    void shouldDecodeQueryValues() {
        Request request = new Request(
                "GET",
                "/App/hello",
                "HTTP/1.1",
                Map.of(),
                "name=John+Smith&name=Other"
        );

        assertEquals("John Smith", request.getValues("name"));
        assertIterableEquals(List.of("John Smith", "Other"), request.getAllValues("name"));
    }

    @Test
    void shouldReturnEmptyStringForUnknownQueryParameter() {
        Request request = new Request(
                "GET",
                "/App/hello",
                "HTTP/1.1",
                Map.of(),
                "name=Pedro"
        );

        assertEquals("", request.getValues("missing"));
        assertIterableEquals(List.of(), request.getAllValues("missing"));
    }
}
