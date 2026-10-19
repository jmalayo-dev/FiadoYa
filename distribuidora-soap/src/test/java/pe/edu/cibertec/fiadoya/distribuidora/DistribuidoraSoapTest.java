package pe.edu.cibertec.fiadoya.distribuidora;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import pe.edu.cibertec.fiadoya.distribuidora.ws.DistribuidoraPortType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DistribuidoraSoapTest {

    @LocalServerPort
    int port;

    private DistribuidoraPortType cliente() {
        JaxWsProxyFactoryBean f = new JaxWsProxyFactoryBean();
        f.setServiceClass(DistribuidoraPortType.class);
        f.setAddress("http://localhost:" + port + "/ws/distribuidora");
        return (DistribuidoraPortType) f.create();
    }

    @Test
    void wsdlResponde() throws Exception {
        var resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ws/distribuidora?wsdl")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("registrarPedido").contains("consultarCatalogo");
    }

    @Test
    void consultarCatalogoDevuelveDoceArticulos() {
        var items = cliente().consultarCatalogo();
        assertThat(items).hasSize(12);
        assertThat(items.getFirst().getCodigo()).isEqualTo("P0001");
    }

    @Test
    void registrarPedidoDevuelveNumeroYFecha() {
        var r = cliente().registrarPedido("P0002", 24);
        assertThat(r.getNroPedido()).startsWith("PD-" + LocalDate.now().getYear());
        assertThat(r.getFechaEstimada()).isNotNull();
    }

    @Test
    void diasHabilesSaltanFinDeSemana() {
        // viernes 23 oct 2026 + 2 hábiles = martes 27 oct 2026
        assertThat(DistribuidoraEndpoint.sumarDiasHabiles(LocalDate.of(2026, 10, 23), 2))
                .isEqualTo(LocalDate.of(2026, 10, 27));
    }
}
