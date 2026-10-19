package pe.edu.cibertec.fiadoya.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import pe.edu.cibertec.fiadoya.notificaciones.socket.ServidorAlertas;

/** Concurrencia (Plan §9): varios monitores conectados reciben la misma alerta. */
class ServidorAlertasTest {

    ServidorAlertas servidor;

    @BeforeEach
    void iniciar() throws Exception {
        servidor = new ServidorAlertas(0, "clave", 8, 30);
        servidor.iniciar();
    }

    @AfterEach
    void detener() throws Exception {
        servidor.detener();
    }

    private Callable<String> monitor() {
        return () -> {
            try (Socket s = new Socket("localhost", servidor.puertoReal());
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
                out.println("{\"op\":\"HELLO\",\"key\":\"clave\"}");
                assertThat(in.readLine()).contains("OK");
                return in.readLine();
            }
        };
    }

    @Test
    void tresMonitoresRecibenLaMismaAlerta() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<String>> recibidos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            recibidos.add(pool.submit(monitor()));
        }
        await().atMost(Duration.ofSeconds(5)).until(() -> servidor.conectados() == 3);

        servidor.difundir(Alerta.de(StockBajo.nuevo(1L, "Arroz 1kg", 3, 5)));

        for (Future<String> f : recibidos) {
            String linea = f.get(5, TimeUnit.SECONDS);
            assertThat(linea).contains("\"op\":\"STOCK_BAJO\"").contains("Arroz 1kg").contains("\"stock\":3");
        }
        pool.shutdown();
    }

    @Test
    void claveIncorrectaRecibeErrorYSeCierra() throws Exception {
        try (Socket s = new Socket("localhost", servidor.puertoReal());
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            out.println("{\"op\":\"HELLO\",\"key\":\"otra\"}");
            assertThat(in.readLine()).contains("ERROR");
            assertThat(in.readLine()).isNull();
        }
        assertThat(servidor.conectados()).isZero();
    }
}
