package pe.edu.cibertec.fiadoya.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;

/** Integración con SQL Server y RabbitMQ reales: ajuste, descuento por lote, idempotencia y evento stock.bajo. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InventarioIT {

    static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    static final String BODEGUERO = TokensDePrueba.bearer(SECRETO, 1, "BODEGUERO");
    static final String INTERNA = "clave-interna-pruebas";
    static final String COLA_PRUEBA = "prueba.stock-bajo";

    @Container
    @ServiceConnection
    static MSSQLServerContainer sql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2025-latest").acceptLicense();

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @Autowired
    MockMvc mvc;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RabbitAdmin admin;

    private long crearProducto(String nombre, int stock, int minimo) throws Exception {
        String body = mvc.perform(post("/api/productos").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"%s","precio":4.50,"stock":%d,"stockMinimo":%d,"categoria":"ABARROTES"}
                                """.formatted(nombre, stock, minimo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void clienteNoPuedeVerProductos() throws Exception {
        mvc.perform(get("/api/productos").header("Authorization", TokensDePrueba.bearer(SECRETO, 2, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_FORBIDDEN"));
    }

    @Test
    void ajusteQueDejaStockNegativoEsValidationError() throws Exception {
        long id = crearProducto("Producto ajuste", 3, 1);
        mvc.perform(post("/api/productos/" + id + "/ajuste").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-5,\"motivo\":\"merma\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].campo").value("delta"));
    }

    @Test
    void internoSinClaveDa401() throws Exception {
        mvc.perform(post("/internal/inventario/devoluciones").contentType(MediaType.APPLICATION_JSON).content("{\"ventaId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void descuentoPorLoteIdempotenteYEventoAlCruzarMinimo() throws Exception {
        Queue cola = new Queue(COLA_PRUEBA, false, false, true);
        admin.declareQueue(cola);
        admin.declareBinding(BindingBuilder.bind(cola).to(new TopicExchange(Mensajeria.EXCHANGE)).with(Mensajeria.RK_STOCK_BAJO));

        long arroz = crearProducto("Arroz IT", 10, 5);
        long aceite = crearProducto("Aceite IT", 2, 1);
        String lote = """
                {"ventaId":900,"items":[{"productoId":%d,"cantidad":5},{"productoId":%d,"cantidad":1}]}
                """.formatted(arroz, aceite);

        for (int i = 0; i < 2; i++) { // la segunda vez no debe volver a descontar
            mvc.perform(post("/internal/inventario/descuentos").header("X-Internal-Key", INTERNA)
                            .contentType(MediaType.APPLICATION_JSON).content(lote))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(2));
        }
        mvc.perform(get("/api/productos/" + arroz).header("Authorization", BODEGUERO))
                .andExpect(jsonPath("$.stock").value(5));

        Object recibido = rabbitTemplate.receiveAndConvert(COLA_PRUEBA, 5000);
        assertThat(recibido).isInstanceOf(StockBajo.class);
        assertThat(((StockBajo) recibido).productoId()).isIn(arroz, aceite);
    }

    @Test
    void stockInsuficienteEnSegundoItemNoDescuentaNada() throws Exception {
        long a = crearProducto("Galletas IT", 10, 1);
        long b = crearProducto("Gaseosa IT", 1, 0);
        mvc.perform(post("/internal/inventario/descuentos").header("X-Internal-Key", INTERNA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ventaId\":901,\"items\":[{\"productoId\":%d,\"cantidad\":2},{\"productoId\":%d,\"cantidad\":3}]}"
                                .formatted(a, b)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCTO_STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.extra.disponible").value(1));
        mvc.perform(get("/api/productos/" + a).header("Authorization", BODEGUERO))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void soapCaidoResponde503SinColgarse() throws Exception {
        long id = crearProducto("Detergente IT", 5, 1);
        long inicio = System.currentTimeMillis();
        mvc.perform(post("/api/reposiciones").header("Authorization", BODEGUERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":%d,\"cantidad\":10}".formatted(id)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICIO_NO_DISPONIBLE"));
        assertThat(System.currentTimeMillis() - inicio).isLessThan(5000);
    }
}
