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
@Table(name = "venta_resumen")
public class VentaResumen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false, unique = true)
    private Long ventaId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "tipo_pago", nullable = false)
    private String tipoPago;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private Instant fecha;

    protected VentaResumen() {
    }

    public VentaResumen(Long ventaId, Long clienteId, String tipoPago, BigDecimal total, Instant fecha) {
        this.ventaId = ventaId;
        this.clienteId = clienteId;
        this.tipoPago = tipoPago;
        this.total = total;
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getFecha() {
        return fecha;
    }
}
