package pe.edu.cibertec.fiadoya.notificaciones.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;

/**
 * Conexiones SSE abiertas por la web (HU08). Al conectar envía las últimas 20 alertas,
 * luego cada alerta nueva como "event: alerta" y un comentario ": ping" cada 25 s.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private static final long UNA_HORA_MS = 3_600_000L;

    private final List<SseEmitter> conexiones = new CopyOnWriteArrayList<>();
    private final AlertasStore store;
    private final int alConectar;

    public SseHub(AlertasStore store, @Value("${fiadoya.alertas.al-conectar:20}") int alConectar) {
        this.store = store;
        this.alConectar = alConectar;
    }

    public SseEmitter conectar() {
        SseEmitter emitter = new SseEmitter(UNA_HORA_MS);
        conexiones.add(emitter);
        emitter.onCompletion(() -> conexiones.remove(emitter));
        emitter.onTimeout(() -> conexiones.remove(emitter));
        emitter.onError(e -> conexiones.remove(emitter));
        // Historial del más antiguo al más nuevo: el cliente los va anteponiendo y queda el más nuevo arriba.
        List<Alerta> historial = store.ultimas(alConectar, null).reversed();
        for (Alerta a : historial) {
            if (!enviar(emitter, a)) {
                break;
            }
        }
        return emitter;
    }

    public void difundir(Alerta alerta) {
        conexiones.forEach(e -> enviar(e, alerta));
    }

    @Scheduled(fixedDelayString = "#{${fiadoya.alertas.ping-sse-segundos:25} * 1000}")
    public void ping() {
        for (SseEmitter e : conexiones) {
            try {
                e.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                cerrar(e);
            }
        }
    }

    public int conectados() {
        return conexiones.size();
    }

    private boolean enviar(SseEmitter emitter, Alerta alerta) {
        try {
            emitter.send(SseEmitter.event().name("alerta").id(alerta.id()).data(alerta, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException ex) {
            cerrar(emitter);
            return false;
        }
    }

    private void cerrar(SseEmitter emitter) {
        conexiones.remove(emitter);
        try {
            emitter.complete();
        } catch (RuntimeException ignorada) {
            log.debug("Conexión SSE ya cerrada");
        }
    }
}
