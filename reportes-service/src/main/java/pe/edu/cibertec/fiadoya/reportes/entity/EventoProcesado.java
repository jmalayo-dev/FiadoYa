package pe.edu.cibertec.fiadoya.reportes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evento_procesado")
public class EventoProcesado {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn = Instant.now();

    protected EventoProcesado() {
    }

    public EventoProcesado(String eventId, String tipo) {
        this.eventId = eventId;
        this.tipo = tipo;
    }
}
