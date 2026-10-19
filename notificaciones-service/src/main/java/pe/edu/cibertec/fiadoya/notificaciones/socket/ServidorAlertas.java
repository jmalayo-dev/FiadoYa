package pe.edu.cibertec.fiadoya.notificaciones.socket;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import tools.jackson.databind.json.JsonMapper;

/**
 * Flujo D: servidor TCP de alertas para los monitores de caja (Anexo §8.2).
 * <ul>
 *   <li>Un hilo acepta conexiones; cada cliente se atiende en un hilo de un ExecutorService.</li>
 *   <li>Los clientes autenticados viven en un Set concurrente (seguro para varios hilos).</li>
 *   <li>Protocolo: una línea JSON por mensaje (HELLO/OK/ERROR, PING/PONG, STOCK_BAJO, LIMITE_CLIENTE).</li>
 * </ul>
 */
@Component
public class ServidorAlertas {

    private static final Logger log = LoggerFactory.getLogger(ServidorAlertas.class);

    private final int puerto;
    private final String clave;
    private final int segundosPing;
    private final ExecutorService atencion;
    private final ScheduledExecutorService latidos = Executors.newSingleThreadScheduledExecutor(r -> hilo(r, "socket-ping"));
    private final Set<ConexionMonitor> clientes = ConcurrentHashMap.newKeySet();
    private final JsonMapper json = JsonMapper.builder().build();
    private volatile ServerSocket servidor;
    private volatile boolean activo;

    public ServidorAlertas(@Value("${fiadoya.socket.puerto:7070}") int puerto,
                           @Value("${fiadoya.socket.clave:}") String clave,
                           @Value("${fiadoya.socket.hilos:32}") int hilos,
                           @Value("${fiadoya.socket.ping-segundos:30}") int segundosPing) {
        this.puerto = puerto;
        this.clave = clave;
        this.segundosPing = segundosPing;
        AtomicInteger n = new AtomicInteger();
        this.atencion = Executors.newFixedThreadPool(hilos, r -> hilo(r, "monitor-" + n.incrementAndGet()));
    }

    @PostConstruct
    public void iniciar() throws IOException {
        servidor = new ServerSocket(puerto);
        activo = true;
        Thread aceptador = hilo(this::aceptar, "socket-aceptador");
        aceptador.start();
        latidos.scheduleAtFixedRate(this::pingATodos, segundosPing, segundosPing, TimeUnit.SECONDS);
        log.info("Servidor de alertas escuchando en el puerto {}", servidor.getLocalPort());
    }

    private void aceptar() {
        while (activo) {
            try {
                Socket socket = servidor.accept();
                atencion.submit(new ConexionMonitor(socket, this));
            } catch (SocketException cerrado) {
                if (activo) {
                    log.warn("Socket del servidor cerrado inesperadamente", cerrado);
                }
            } catch (IOException e) {
                log.warn("Error aceptando un monitor", e);
            }
        }
    }

    boolean claveValida(String recibida) {
        return clave != null && !clave.isBlank() && clave.equals(recibida);
    }

    void registrar(ConexionMonitor c) {
        clientes.add(c);
        log.info("Monitor conectado desde {} ({} en total)", c.origen(), clientes.size());
    }

    void quitar(ConexionMonitor c) {
        if (clientes.remove(c)) {
            log.info("Monitor {} desconectado ({} en total)", c.origen(), clientes.size());
        }
    }

    JsonMapper json() {
        return json;
    }

    /** Envía la alerta a todos los monitores conectados. */
    public void difundir(Alerta alerta) {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("op", alerta.tipo());
        mensaje.put("eventId", alerta.id());
        mensaje.put("nombre", alerta.nombre());
        if (Alerta.STOCK_BAJO.equals(alerta.tipo())) {
            mensaje.put("productoId", alerta.productoId());
            mensaje.put("stock", alerta.stock());
            mensaje.put("stockMinimo", alerta.stockMinimo());
        } else {
            mensaje.put("clienteId", alerta.clienteId());
            mensaje.put("saldo", alerta.saldo());
            mensaje.put("limiteCredito", alerta.limiteCredito());
        }
        String linea = json.writeValueAsString(mensaje);
        clientes.forEach(c -> c.enviar(linea));
    }

    private void pingATodos() {
        clientes.forEach(ConexionMonitor::ping);
    }

    public int conectados() {
        return clientes.size();
    }

    public int puertoReal() {
        return servidor.getLocalPort();
    }

    @PreDestroy
    public void detener() throws IOException {
        activo = false;
        clientes.forEach(ConexionMonitor::cerrar);
        latidos.shutdownNow();
        atencion.shutdownNow();
        if (servidor != null) {
            servidor.close();
        }
    }

    private static Thread hilo(Runnable r, String nombre) {
        Thread t = new Thread(r, nombre);
        t.setDaemon(true);
        return t;
    }
}
