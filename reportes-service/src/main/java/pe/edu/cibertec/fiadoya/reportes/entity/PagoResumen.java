package pe.edu.cibertec.fiadoya.reportes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago_resumen")
public class PagoResumen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pago_id", nullable = false, unique = true)
    private Long pagoId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private BigDecimal monto;

    private String metodo;

    @Column(nullable = false)
    private Instant fecha;

    protected PagoResumen() {
    }

    public PagoResumen(Long pagoId, Long clienteId, BigDecimal monto, String metodo, Instant fecha) {
        this.pagoId = pagoId;
        this.clienteId = clienteId;
        this.monto = monto;
        this.metodo = metodo;
        this.fecha = fecha;
    }
}
