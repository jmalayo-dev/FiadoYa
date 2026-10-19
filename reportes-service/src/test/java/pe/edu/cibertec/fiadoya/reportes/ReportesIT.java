package pe.edu.cibertec.fiadoya.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pe.edu.cibertec.fiadoya.common.security.TokensDePrueba;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;
import pe.edu.cibertec.fiadoya.reportes.client.ClientesClient;
import pe.edu.cibertec.fiadoya.reportes.repository.VentaResumenRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReportesIT {

    static final String BODEGUERO = TokensDePrueba.bearer("secreto-de-pruebas-con-mas-de-32-caracteres", 1, "BODEGUERO");

    @Container
    @ServiceConnection
    static MSSQLServerContainer sql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2025-latest").acceptLicense();

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @MockitoBean
    ClientesClient clientes;

    @Autowired
    MockMvc mvc;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    VentaResumenRepository ventas;

    @Test
    void mismoEventoDosVecesNoDuplicaYResumenCuenta() throws Exception {
        VentaRegistrada evento = VentaRegistrada.nueva(1L, null, "CONTADO", new BigDecimal("13.50"),
                List.of(new VentaRegistrada.Item(1L, "Arroz", 3, new BigDecimal("4.50"))));
        rabbitTemplate.convertAndSend(Mensajeria.EXCHANGE, Mensajeria.RK_VENTA_REGISTRADA, evento);
        rabbitTemplate.convertAndSend(Mensajeria.EXCHANGE, Mensajeria.RK_VENTA_REGISTRADA, evento);

        await().atMost(Duration.ofSeconds(10)).until(() -> ventas.count() == 1);
        Thread.sleep(1000);
        assertThat(ventas.count()).isEqualTo(1);

        mvc.perform(get("/api/reportes/ventas/resumen").header("Authorization", BODEGUERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentas").value(1))
                .andExpect(jsonPath("$.montoTotal").value(13.50))
                .andExpect(jsonPath("$.porDia.length()").value(7))
                .andExpect(jsonPath("$.topProductos[0].nombre").value("Arroz"));
    }

    @Test
    void consolidadoParaleloDevuelveZipYTiempos() throws Exception {
        when(clientes.deudores()).thenReturn(List.of(
                new ClientesClient.Deudor(1L, "María", "987654321", new BigDecimal("85.50"), new BigDecimal("150"))));

        byte[] zip = mvc.perform(get("/api/reportes/consolidado?comparar=true").header("Authorization", BODEGUERO))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Tiempo-Paralelo-Ms"))
                .andExpect(header().exists("X-Tiempo-Secuencial-Ms"))
                .andReturn().getResponse().getContentAsByteArray();

        List<String> nombres = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                nombres.add(e.getName());
            }
        }
        assertThat(nombres).hasSize(3).anyMatch(n -> n.endsWith(".pdf")).anyMatch(n -> n.endsWith(".xlsx"))
                .anyMatch(n -> n.endsWith(".json"));
    }

    @Test
    void deudoresConClientesCaidoResponde503() throws Exception {
        when(clientes.deudores()).thenThrow(new RuntimeException("Connection refused"));
        mvc.perform(get("/api/reportes/deudores.pdf").header("Authorization", BODEGUERO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICIO_NO_DISPONIBLE"));
    }
}
