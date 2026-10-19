package pe.edu.cibertec.fiadoya.notificaciones.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Atiende a un monitor de caja en su propio hilo: saludo con clave, luego lee PONG hasta que se desconecte. */
class ConexionMonitor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConexionMonitor.class);
    private static final Duration SIN_RESPUESTA = Duration.ofSeconds(95);

    private final Socket socket;
    private final ServidorAlertas servidor;
    private BufferedWriter salida;
    private volatile Instant ultimoPong = Instant.now();

    ConexionMonitor(Socket socket, ServidorAlertas servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            salida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            socket.setSoTimeout(10_000);
            if (!saludo(entrada.readLine())) {
                enviar("{\"op\":\"ERROR\"}");
                return;
            }
            socket.setSoTimeout(0);
            enviar("{\"op\":\"OK\"}");
            servidor.registrar(this);
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.contains("PONG")) {
                    ultimoPong = Instant.now();
                }
            }
        } catch (IOException e) {
            log.debug("Monitor {} cerró la conexión: {}", origen(), e.getMessage());
        } finally {
            servidor.quitar(this);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean saludo(String linea) {
        if (linea == null) {
            return false;
        }
        try {
            Map<String, Object> hello = servidor.json().readValue(linea, Map.class);
            return "HELLO".equals(hello.get("op")) && servidor.claveValida(String.valueOf(hello.get("key")));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** synchronized: varios hilos (consumidor, ping) pueden escribir en el mismo socket. */
    synchronized void enviar(String linea) {
        try {
            salida.write(linea);
            salida.write('\n');
            salida.flush();
        } catch (IOException | NullPointerException e) {
            cerrar();
        }
    }

    void ping() {
        if (Duration.between(ultimoPong, Instant.now()).compareTo(SIN_RESPUESTA) > 0) {
            log.info("Monitor {} no responde PING: se desconecta", origen());
            cerrar();
            return;
        }
        enviar("{\"op\":\"PING\"}");
    }

    void cerrar() {
        servidor.quitar(this);
        try {
            socket.close();
        } catch (IOException ignorada) {
            log.debug("Socket ya cerrado");
        }
    }

    String origen() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
