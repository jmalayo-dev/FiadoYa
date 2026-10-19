package pe.edu.cibertec.fiadoya.monitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * Monitor de caja (Flujo D, Anexo §8.2): se conecta al socket de notificaciones, se identifica con la clave
 * e imprime cada alerta. Si la conexión se cae, reintenta esperando 1, 2, 4, 8... hasta 30 segundos.
 *
 * Uso: java -jar monitor-caja.jar [host] [puerto] [clave]
 *      (o variables MONITOR_HOST, MONITOR_PORT, SOCKET_KEY)
 */
public class MonitorCaja {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int ESPERA_MAXIMA_S = 30;

    private final String host;
    private final int puerto;
    private final String clave;
    private final PrintStream consola;
    private final JsonMapper json = JsonMapper.builder().build();
    private volatile boolean activo = true;

    public MonitorCaja(String host, int puerto, String clave, PrintStream consola) {
        this.host = host;
        this.puerto = puerto;
        this.clave = clave;
        this.consola = consola;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : env("MONITOR_HOST", "localhost");
        int puerto = Integer.parseInt(args.length > 1 ? args[1] : env("MONITOR_PORT", "7070"));
        String clave = args.length > 2 ? args[2] : env("SOCKET_KEY", "");
        MonitorCaja monitor = new MonitorCaja(host, puerto, clave, System.out);
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::detener));
        monitor.ejecutar();
    }

    public void ejecutar() {
        int espera = 1;
        while (activo) {
            try {
                conectarYEscuchar();
                espera = 1; // la sesión funcionó: se reinicia la espera
            } catch (ClaveIncorrecta e) {
                consola.println("[ERROR] El servidor rechazó la clave. Revisa SOCKET_KEY.");
                return;
            } catch (IOException e) {
                consola.printf("[%s] Sin conexión con %s:%d (%s). Reintento en %d s...%n",
                        ahora(), host, puerto, e.getMessage(), espera);
            }
            dormir(espera);
            espera = Math.min(espera * 2, ESPERA_MAXIMA_S);
        }
    }

    void conectarYEscuchar() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 5000);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            escribir(salida, json.writeValueAsString(Map.of("op", "HELLO", "key", clave)));
            String respuesta = entrada.readLine();
            if (respuesta == null || !respuesta.contains("\"OK\"")) {
                throw new ClaveIncorrecta();
            }
            consola.printf("[%s] Conectado a %s:%d. Esperando alertas...%n", ahora(), host, puerto);
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                procesar(linea, salida);
            }
            throw new IOException("el servidor cerró la conexión");
        }
    }

    @SuppressWarnings("unchecked")
    void procesar(String linea, BufferedWriter salida) throws IOException {
        Map<String, Object> m = json.readValue(linea, Map.class);
        switch (String.valueOf(m.get("op"))) {
            case "PING" -> escribir(salida, "{\"op\":\"PONG\"}");
            case "STOCK_BAJO" -> consola.printf("[%s] ⚠ STOCK BAJO: %s — quedan %s (mínimo %s)%n",
                    ahora(), m.get("nombre"), m.get("stock"), m.get("stockMinimo"));
            case "LIMITE_CLIENTE" -> consola.printf("[%s] ⚠ LÍMITE DE CRÉDITO: %s debe S/ %s de S/ %s%n",
                    ahora(), m.get("nombre"), m.get("saldo"), m.get("limiteCredito"));
            default -> consola.printf("[%s] Mensaje: %s%n", ahora(), linea);
        }
    }

    public void detener() {
        activo = false;
    }

    private static void escribir(BufferedWriter salida, String linea) throws IOException {
        salida.write(linea);
        salida.write('\n');
        salida.flush();
    }

    private static void dormir(int segundos) {
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String ahora() {
        return LocalTime.now(ZoneId.of("America/Lima")).format(HORA);
    }

    private static String env(String nombre, String defecto) {
        String v = System.getenv(nombre);
        return v == null || v.isBlank() ? defecto : v;
    }

    static class ClaveIncorrecta extends IOException {
        ClaveIncorrecta() {
            super("clave incorrecta");
        }
    }
}
