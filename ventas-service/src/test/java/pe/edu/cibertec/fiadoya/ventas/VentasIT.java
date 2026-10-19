package pe.edu.cibertec.fiadoya.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.Request;
import feign.RetryableException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pe.edu.cibertec.fiadoya.common.security.TokensDePrueba;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;
import pe.edu.cibertec.fiadoya.ventas.client.ClientesClient;
import pe.edu.cibertec.fiadoya.ventas.client.InventarioClient;

/**
 * Saga de ventas con SQL Server y RabbitMQ reales. Los servicios remotos se simulan reemplazando
 * los clientes Feign; el circuit breaker y la idempotencia son los reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class VentasIT {

    static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres";
    static final String BODEGUERO = TokensDePrueba.bearer(SECRETO, 1, "BODEGUERO");

    @Container
    @ServiceConnection
    static MSSQLServerContainer sql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2025-latest").acceptLicense();

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @MockitoBean
    InventarioClient inventario;
    @MockitoBean
    ClientesClient clientes;

    @Autowired
    MockMvc mvc;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    RabbitAdmin admin;

    @Autowired
    org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory circuitos;

    @BeforeEach
    void precios() {
        circuitos.getCircuitBreakerRegistry().getAllCircuitBreakers().forEach(cb -> cb.reset());
        reset(inventario, clientes);
        when(inventario.descontar(any())).thenAnswer(inv -> {
            InventarioClient.Lote lote = inv.getArgument(0);
            return new InventarioClient.Resultado(lote.items().stream()
                    .map(i -> new InventarioClient.ItemPrecio(i.productoId(), "Producto " + i.productoId(),
                            new BigDecimal("4.50"), i.cantidad()))
                    .toList());
        });
    }

    private ResultActions vender(String clave, String cuerpo) throws Exception {
        return mvc.perform(post("/api/ventas").header("Authorization", BODEGUERO).header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    static final String CONTADO = "{\"clienteId\":null,\"tipoPago\":\"CONTADO\",\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":2,\"cantidad\":1}]}";
    static final String FIADO = "{\"clienteId\":3,\"tipoPago\":\"FIADO\",\"items\":[{\"productoId\":1,\"cantidad\":2}]}";

    @Test
    void contadoConfirmadoPublicaVentaRegistrada() throws Exception {
        Queue cola = new Queue("prueba.ventas", false, false, true);
        admin.declareQueue(cola);
        admin.declareBinding(BindingBuilder.bind(cola).to(new TopicExchange(Mensajeria.EXCHANGE)).with(Mensajeria.RK_VENTA_REGISTRADA));

        vender(UUID.randomUUID().toString(), CONTADO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.total").value(13.50))
                .andExpect(jsonPath("$.items.length()").value(2));

        Object evento = rabbitTemplate.receiveAndConvert("prueba.ventas", 5000);
        assertThat(evento).isInstanceOf(VentaRegistrada.class);
        assertThat(((VentaRegistrada) evento).total()).isEqualByComparingTo("13.50");
    }

    @Test
    void mismaClaveDosVecesDescuentaUnaSolaVez() throws Exception {
        String clave = UUID.randomUUID().toString();
        String id1 = vender(clave, CONTADO).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String id2 = vender(clave, CONTADO).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(com.jayway.jsonpath.JsonPath.<Integer>read(id1, "$.id"))
                .isEqualTo(com.jayway.jsonpath.JsonPath.<Integer>read(id2, "$.id"));
        verify(inventario, times(1)).descontar(any());

        vender(clave, FIADO).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void fiadoExcedidoQuedaRechazadaYDevuelveStock() throws Exception {
        when(clientes.validar(3L)).thenReturn(new ClientesClient.Validacion(true, true, true, "María Quispe"));
        when(clientes.cargar(anyLong(), any())).thenThrow(new ApiException(ErrorCode.CLIENTE_LIMITE_EXCEDIDO, "excede",
                Map.of("saldo", 80, "limite", 100, "monto", 9, "disponible", 5)));

        String cuerpo = vender(UUID.randomUUID().toString(), FIADO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENTE_LIMITE_EXCEDIDO"))
                .andExpect(jsonPath("$.extra.disponible").value(5))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Integer ventaId = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.extra.ventaId");
        verify(inventario).devolver(new InventarioClient.Devolucion(ventaId.longValue()));

        mvc.perform(get("/api/ventas/" + ventaId).header("Authorization", BODEGUERO))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("Supera el límite de crédito. Disponible: S/ 5.00"));
    }

    private int totalVentas() throws Exception {
        String r = mvc.perform(get("/api/ventas").header("Authorization", BODEGUERO)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return com.jayway.jsonpath.JsonPath.read(r, "$.totalElements");
    }

    @Test
    void stockInsuficienteNoDejaRegistro() throws Exception {
        int antes = totalVentas();
        doThrow(new ApiException(ErrorCode.PRODUCTO_STOCK_INSUFICIENTE, "sin stock",
                Map.of("productoId", 2, "nombre", "Aceite", "disponible", 0, "solicitado", 1))).when(inventario).descontar(any());
        vender(UUID.randomUUID().toString(), CONTADO)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCTO_STOCK_INSUFICIENTE"));
        assertThat(totalVentas()).isEqualTo(antes);
    }

    @Test
    void clientesCaidoResponde503YRechaza() throws Exception {
        when(clientes.validar(3L)).thenReturn(new ClientesClient.Validacion(true, true, true, "María Quispe"));
        when(clientes.cargar(anyLong(), any())).thenThrow(caida());
        vender(UUID.randomUUID().toString(), FIADO)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICIO_NO_DISPONIBLE"));
        verify(inventario).devolver(any());
    }

    @Test
    void inventarioCaidoAbreElCircuito() throws Exception {
        doThrow(caida()).when(inventario).descontar(any());
        for (int i = 0; i < 5; i++) {
            vender(UUID.randomUUID().toString(), CONTADO).andExpect(status().isServiceUnavailable());
        }
        mvc.perform(get("/actuator/circuitbreakers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circuitBreakers.inventario.state").value("OPEN"));
    }

    @Test
    void sinIdempotencyKeyEsValidationError() throws Exception {
        mvc.perform(post("/api/ventas").header("Authorization", BODEGUERO).contentType(MediaType.APPLICATION_JSON).content(CONTADO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static RetryableException caida() {
        Request req = Request.create(Request.HttpMethod.POST, "http://x", Map.of(), new byte[0], StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "Connection refused", Request.HttpMethod.POST, (Long) null, req);
    }
}
