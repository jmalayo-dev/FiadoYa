package pe.edu.cibertec.fiadoya.gateway;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Matriz de acceso del Anexo §3.2 y ruteo hacia un backend simulado. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayTest {

    static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    static HttpServer backend;

    static {
        try {
            backend = HttpServer.create(new InetSocketAddress(0), 0);
            backend.createContext("/", ex -> {
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                byte[] cuerpo = ("{\"ruta\":\"" + ex.getRequestURI().getPath() + "\",\"conToken\":" + (auth != null) + "}")
                        .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, cuerpo.length);
                ex.getResponseBody().write(cuerpo);
                ex.close();
            });
            backend.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void rutas(DynamicPropertyRegistry r) {
        String url = "http://localhost:" + backend.getAddress().getPort();
        for (String s : new String[]{"auth", "inventario", "clientes", "ventas", "reportes"}) {
            r.add("fiadoya.rutas." + s, () -> url);
        }
        r.add("fiadoya.rutas.notificaciones", () -> "http://localhost:1");
    }

    @AfterAll
    static void detener() {
        backend.stop(0);
    }

    @Autowired
    WebTestClient web;

    static String token(String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fiadoya-auth").subject("1").issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(600)).claim("rol", rol).build();
        return "Bearer " + NimbusJwtEncoder.withSecretKey(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build().encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void sinTokenDa401ConProblemDetail() {
        web.get().uri("/api/productos").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.code").isEqualTo("AUTH_TOKEN_EXPIRED");
    }

    @Test
    void loginEsPublicoYSeRutea() {
        web.post().uri("/api/auth/login").bodyValue("{}").header("Content-Type", "application/json").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ruta").isEqualTo("/api/auth/login");
    }

    @Test
    void clienteNoEntraAProductosPeroSiASuCuenta() {
        web.get().uri("/api/productos").header("Authorization", token("CLIENTE")).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.code").isEqualTo("AUTH_FORBIDDEN");
        web.get().uri("/api/clientes/mi-cuenta").header("Authorization", token("CLIENTE")).exchange()
                .expectStatus().isOk();
    }

    @Test
    void bodegueroLlegaAlServicioConSuToken() {
        web.get().uri("/api/productos?page=0").header("Authorization", token("BODEGUERO")).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ruta").isEqualTo("/api/productos").jsonPath("$.conToken").isEqualTo(true);
    }

    @Test
    void internoNuncaSeExpone() {
        web.post().uri("/internal/inventario/descuentos").header("Authorization", token("BODEGUERO"))
                .header("X-Internal-Key", "lo-que-sea").exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void servicioCaidoDa503() {
        web.get().uri("/api/alertas").header("Authorization", token("BODEGUERO")).exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.code").isEqualTo("SERVICIO_NO_DISPONIBLE");
    }
}
