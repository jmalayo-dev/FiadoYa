package pe.edu.cibertec.fiadoya.notificaciones.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;

/**
 * Anillo en memoria con las últimas N alertas (Anexo §8.3) y deduplicación por eventId (últimos 500).
 * Los métodos son synchronized: lo usan a la vez el consumidor de RabbitMQ y las peticiones HTTP.
 */
@Component
public class AlertasStore {

    private static final int IDS_RECORDADOS = 500;

    private final int capacidad;
    private final Deque<Alerta> anillo = new ArrayDeque<>();
    private final SequencedSet<String> vistos = new LinkedHashSet<>();

    public AlertasStore(@Value("${fiadoya.alertas.capacidad:100}") int capacidad) {
        this.capacidad = capacidad;
    }

    /** @return false si el evento ya se había recibido (entrega repetida de RabbitMQ). */
    public synchronized boolean agregar(Alerta alerta) {
        if (!vistos.add(alerta.id())) {
            return false;
        }
        if (vistos.size() > IDS_RECORDADOS) {
            vistos.removeFirst();
        }
        anillo.addFirst(alerta);
        if (anillo.size() > capacidad) {
            anillo.removeLast();
        }
        return true;
    }

    /** Más reciente primero; tipo opcional (STOCK_BAJO o LIMITE_CLIENTE). */
    public synchronized List<Alerta> ultimas(int limite, String tipo) {
        List<Alerta> resultado = new ArrayList<>();
        for (Alerta a : anillo) {
            if (tipo == null || tipo.equalsIgnoreCase(a.tipo())) {
                resultado.add(a);
                if (resultado.size() >= limite) {
                    break;
                }
            }
        }
        return resultado;
    }
}
