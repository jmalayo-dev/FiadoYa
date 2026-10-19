package pe.edu.cibertec.fiadoya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pe.edu.cibertec.fiadoya.common.security.TokensDePrueba;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.notificaciones.service.AlertasStore;

/** Coreografía con RabbitMQ real: evento → historial → SSE, y mensaje dañado → DLQ. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class NotificacionesIT {

    static final String TOKEN = TokensDePrueba.bearer("secreto-de-pruebas-con-mas-de-32-caracteres", 1, "BODEGUERO");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @LocalServerPort
    int port;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    AlertasStore store;

    @Test
    void stockBajoLlegaAlHistorialYAlStreamSse() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest sse = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/alertas/stream"))
                .header("Authorization", TOKEN).header("Accept", "text/event-stream").build();
        HttpResponse<InputStream> resp = http.send(sse, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(resp.statusCode()).isEqualTo(200);

        CompletableFuture<String> evento = CompletableFuture.supplyAsync(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                String linea;
                boolean esAlerta = false;
                while ((linea = r.readLine()) != null) {
                    if (linea.startsWith("event:alerta")) {
                        esAlerta = true;
                    } else if (esAlerta && linea.startsWith("data:")) {
                        return linea;
                    }
                }
                return null;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        long inicio = System.currentTimeMillis();
        rabbitTemplate.convertAndSend(Mensajeria.EXCHANGE, Mensajeria.RK_STOCK_BAJO, StockBajo.nuevo(7L, "Leche evaporada", 4, 5));

        String data = evento.get(5, TimeUnit.SECONDS);
        assertThat(System.currentTimeMillis() - inicio).isLessThan(2000);
        assertThat(data).contains("Leche evaporada").contains("\"tipo\":\"STOCK_BAJO\"");

        HttpResponse<String> historial = http.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/alertas?limite=5")).header("Authorization", TOKEN).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(historial.body()).contains("Leche evaporada");
    }

    @Test
    void mensajeDanadoTerminaEnLaDlq() {
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setHeader("__TypeId__", StockBajo.class.getName());
        rabbitTemplate.send(Mensajeria.EXCHANGE, Mensajeria.RK_STOCK_BAJO,
                new Message("{esto no es json".getBytes(StandardCharsets.UTF_8), props));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(rabbitTemplate.receive(Mensajeria.Q_NOTIFICACIONES_STOCK_BAJO + Mensajeria.DLQ_SUFIJO, 500))
                        .isNotNull());
    }

    @Test
    void clienteNoPuedeVerAlertas() throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/alertas"))
                .header("Authorization", TokensDePrueba.bearer("secreto-de-pruebas-con-mas-de-32-caracteres", 2, "CLIENTE"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(403);
    }
}
