package pe.edu.cibertec.fiadoya.clientes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pe.edu.cibertec.fiadoya.common.security.TokensDePrueba;

/** Casos HU02, HU05, HU06 y HU07 del Anexo §13 con SQL Server y RabbitMQ reales. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClientesIT {

    static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    static final String BODEGUERO = TokensDePrueba.bearer(SECRETO, 1, "BODEGUERO");
    static final String INTERNA = "clave-interna-pruebas";

    @Container
    @ServiceConnection
    static MSSQLServerContainer sql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2025-latest").acceptLicense();

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @Autowired
    MockMvc mvc;

    private long crearCliente(String nombre, String limite, boolean consentimiento, Long usuarioId) throws Exception {
        String body = mvc.perform(post("/api/clientes").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","telefono":"987654321","limiteCredito":%s,"consentimiento":%s,"usuarioId":%s}
                                """.formatted(nombre, limite, consentimiento, usuarioId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions cargar(long cliente, long venta, String monto) throws Exception {
        return mvc.perform(post("/internal/clientes/" + cliente + "/cargos").header("X-Internal-Key", INTERNA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ventaId\":%d,\"monto\":%s,\"descripcion\":\"Arroz, Aceite\",\"cantidadItems\":2}".formatted(venta, monto)));
    }

    @Test
    void sinConsentimientoNoSePuedeFiar() throws Exception {
        long id = crearCliente("Sin Permiso", "100", false, null);
        cargar(id, 500, "10.00").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENTE_SIN_CONSENTIMIENTO"));
    }

    @Test
    void noSeDesactivaAUnDeudor() throws Exception {
        long id = crearCliente("Deudor", "100", true, null);
        cargar(id, 501, "30.00").andExpect(status().isOk());
        mvc.perform(delete("/api/clientes/" + id).header("Authorization", BODEGUERO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENTE_CON_SALDO"));
    }

    @Test
    void pagoParcialYEstadoDeCuentaEnOrden() throws Exception {
        long id = crearCliente("Pagador", "150", true, null);
        cargar(id, 502, "40.00").andExpect(status().isOk());
        mvc.perform(post("/api/clientes/" + id + "/pagos").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":10.00,\"metodo\":\"EFECTIVO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoDespues").value(30.00));
        mvc.perform(post("/api/clientes/" + id + "/pagos").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":99.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAGO_EXCEDE_SALDO"));

        mvc.perform(get("/api/clientes/" + id + "/estado-cuenta").header("Authorization", BODEGUERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.saldo").value(30.00))
                .andExpect(jsonPath("$.movimientos[0].tipo").value("PAGO"))
                .andExpect(jsonPath("$.movimientos[0].saldoDespues").value(30.00))
                .andExpect(jsonPath("$.movimientos[1].tipo").value("CARGO"))
                .andExpect(jsonPath("$.movimientos[1].descripcion").value("Arroz, Aceite"));
    }

    @Test
    void limiteExcedidoYCompensacion() throws Exception {
        long id = crearCliente("Al Borde", "100", true, null);
        cargar(id, 503, "80.00").andExpect(status().isOk());
        cargar(id, 504, "35.50").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENTE_LIMITE_EXCEDIDO"))
                .andExpect(jsonPath("$.extra.disponible").value(20.00));
        mvc.perform(delete("/internal/clientes/cargos/503").header("X-Internal-Key", INTERNA))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id).header("Authorization", BODEGUERO))
                .andExpect(jsonPath("$.saldo").value(0.00))
                .andExpect(jsonPath("$.disponible").value(100.00));
    }

    @Test
    void elClienteSoloVeSuCuenta() throws Exception {
        crearCliente("Con Acceso", "100", true, 77L);
        mvc.perform(get("/api/clientes/mi-cuenta").header("Authorization", TokensDePrueba.bearer(SECRETO, 77, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nombre").value("Con Acceso"));
        mvc.perform(get("/api/clientes").header("Authorization", TokensDePrueba.bearer(SECRETO, 77, "CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dniDuplicadoDa409() throws Exception {
        String json = "{\"nombre\":\"Con DNI\",\"telefono\":\"912345678\",\"limiteCredito\":50,\"consentimiento\":true,\"dni\":\"45678912\"}";
        mvc.perform(post("/api/clientes").header("Authorization", BODEGUERO).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/clientes").header("Authorization", BODEGUERO).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DNI_DUPLICADO"));
    }

    @Test
    void consentimientoFijaFechaEnServidor() throws Exception {
        long id = crearCliente("Tardío", "100", false, null);
        mvc.perform(put("/api/clientes/" + id).header("Authorization", BODEGUERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tardío\",\"telefono\":\"987654321\",\"limiteCredito\":100,\"consentimiento\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaConsentimiento").isNotEmpty());
    }

    @Test
    void telefonoInvalidoDaValidationError() throws Exception {
        mvc.perform(post("/api/clientes").header("Authorization", BODEGUERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"telefono\":\"123\",\"limiteCredito\":-1,\"consentimiento\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }
}
