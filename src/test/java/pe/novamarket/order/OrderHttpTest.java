package pe.novamarket.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import pe.novamarket.event.OrderConfirmed;
import pe.novamarket.event.OrderObserver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderHttpTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private AtomicReference<OrderConfirmed> audit;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void servesThePageAndConfirmsAnOrderWithTheAddedObserver() throws Exception {
        HttpResponse<String> page = send("/", null);
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Centro de pedidos"));
        assertEquals(200, send("/app.js", null).statusCode());
        assertEquals(3, json.readTree(send("/api/politicas", null).body()).size());

        HttpResponse<String> registration = send("/api/pedidos",
                """
                {"customer":"Diego Salas","category":"FRECUENTE","amount":200}
                """);
        assertEquals(201, registration.statusCode());
        JsonNode order = json.readTree(registration.body());
        assertEquals("REGISTRADO", order.get("status").asString());
        assertEquals(20, order.get("discount").asInt());
        assertEquals(180, order.get("total").asInt());

        String path = "/api/pedidos/" + order.get("id").asLong() + "/confirmar";
        HttpResponse<String> response = send(path, "{}");
        assertEquals(200, response.statusCode());
        JsonNode confirmed = json.readTree(response.body());
        assertEquals("CONFIRMADO", confirmed.get("status").asString());
        UUID eventId = UUID.fromString(confirmed.get("confirmation").get("id").asString());
        assertNotNull(audit.get());
        assertEquals(eventId, audit.get().id());
        assertEquals(order.get("id").asLong(), audit.get().orderId());

        Set<String> areas = new HashSet<>();
        for (JsonNode reaction : confirmed.get("reactions")) {
            assertTrue(reaction.get("successful").asBoolean());
            areas.add(reaction.get("area").asString());
        }
        assertEquals(4, confirmed.get("reactions").size());
        assertEquals(Set.of("Cliente", "Almacén", "Administración", "Auditoría"), areas);
        assertEquals(confirmed, json.readTree(send(path, "{}").body()));
        assertEquals(confirmed, json.readTree(send("/api/pedidos", null).body()).get(0));
    }

    @Test
    void returnsProblemDetailsForInvalidOrders() throws Exception {
        HttpResponse<String> invalid = send("/api/pedidos",
                """
                {"customer":"Ana","category":"NUEVO","amount":0}
                """);
        assertEquals(400, invalid.statusCode());
        assertTrue(json.readTree(invalid.body()).get("detail").asString().contains("importe"));
        assertEquals(404, send("/api/pedidos/9999/confirmar", "{}").statusCode());
    }

    private HttpResponse<String> send(String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (body != null) {
            request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    static class AuditConfiguration {
        @Bean
        AtomicReference<OrderConfirmed> audit() {
            return new AtomicReference<>();
        }

        @Bean
        OrderObserver auditObserver(AtomicReference<OrderConfirmed> audit) {
            return new OrderObserver() {
                @Override
                public String area() {
                    return "Auditoría";
                }

                @Override
                public String onConfirmed(OrderConfirmed event) {
                    audit.set(event);
                    return "Evento recibido por el observador de prueba.";
                }
            };
        }
    }
}
