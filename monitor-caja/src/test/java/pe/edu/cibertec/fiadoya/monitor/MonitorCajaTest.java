package pe.edu.cibertec.fiadoya.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MonitorCajaTest {

    @Test
    void imprimeAlertasYRespondePing() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        MonitorCaja monitor = new MonitorCaja("localhost", 7070, "k", new PrintStream(buffer, true, StandardCharsets.UTF_8));
        StringWriter enviado = new StringWriter();
        BufferedWriter salida = new BufferedWriter(enviado);

        monitor.procesar("{\"op\":\"STOCK_BAJO\",\"nombre\":\"Arroz 1kg\",\"stock\":3,\"stockMinimo\":5}", salida);
        monitor.procesar("{\"op\":\"PING\"}", salida);

        assertThat(buffer.toString(StandardCharsets.UTF_8)).contains("STOCK BAJO: Arroz 1kg").contains("quedan 3");
        assertThat(enviado.toString()).contains("PONG");
    }
}
