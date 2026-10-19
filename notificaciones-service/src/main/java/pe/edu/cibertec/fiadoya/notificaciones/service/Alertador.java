package pe.edu.cibertec.fiadoya.notificaciones.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import pe.edu.cibertec.fiadoya.notificaciones.socket.ServidorAlertas;

/** Guarda la alerta (si no es repetida) y la reparte a la web (SSE) y a los monitores de caja (socket). */
@Service
public class Alertador {

    private static final Logger log = LoggerFactory.getLogger(Alertador.class);

    private final AlertasStore store;
    private final SseHub sse;
    private final ServidorAlertas socket;

    public Alertador(AlertasStore store, SseHub sse, ServidorAlertas socket) {
        this.store = store;
        this.sse = sse;
        this.socket = socket;
    }

    public void procesar(Alerta alerta) {
        if (!store.agregar(alerta)) {
            log.info("Alerta {} repetida: se ignora", alerta.id());
            return;
        }
        sse.difundir(alerta);
        socket.difundir(alerta);
        log.info("Alerta {} {} enviada a {} web y {} monitores", alerta.tipo(), alerta.nombre(), sse.conectados(),
                socket.conectados());
    }
}
