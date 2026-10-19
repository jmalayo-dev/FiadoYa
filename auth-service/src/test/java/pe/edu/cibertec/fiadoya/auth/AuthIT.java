package pe.edu.cibertec.fiadoya.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

/** Casos HU01 del Anexo §13 contra SQL Server real (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIT {

    @Container
    @ServiceConnection
    static MSSQLServerContainer sql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2025-latest").acceptLicense();

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtEncoder encoder;

    private String login(String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(user, pass)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    @Test
    void loginCorrectoYMe() throws Exception {
        String token = login("bodeguero", "Admin12345");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bodeguero"))
                .andExpect(jsonPath("$.rol").value("BODEGUERO"));
    }

    @Test
    void contrasenaMalaDa401ConCodigo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bodeguero\",\"password\":\"mala12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void tokenVencidoDa401TokenExpired() throws Exception {
        Instant antes = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("fiadoya-auth").subject("1")
                .issuedAt(antes).expiresAt(antes.plusSeconds(60)).claim("rol", "BODEGUERO").build();
        String vencido = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void bodegueroCreaClienteYClienteNoPuedeCrearUsuarios() throws Exception {
        String admin = login("bodeguero", "Admin12345");
        mvc.perform(post("/api/auth/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rosa\",\"password\":\"Rosa12345\",\"rol\":\"CLIENTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        mvc.perform(post("/api/auth/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rosa\",\"password\":\"Rosa12345\",\"rol\":\"CLIENTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USERNAME_DUPLICADO"));

        String cliente = login("rosa", "Rosa12345");
        mvc.perform(post("/api/auth/usuarios").header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otro\",\"password\":\"Otro12345\",\"rol\":\"CLIENTE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void contrasenaDebilDaValidationError() throws Exception {
        String admin = login("bodeguero", "Admin12345");
        mvc.perform(post("/api/auth/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Mal Nombre\",\"password\":\"corta\",\"rol\":\"CLIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void bodegaDevuelveDatos() throws Exception {
        String admin = login("bodeguero", "Admin12345");
        mvc.perform(get("/api/bodega").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bodega Test"));
    }
}
