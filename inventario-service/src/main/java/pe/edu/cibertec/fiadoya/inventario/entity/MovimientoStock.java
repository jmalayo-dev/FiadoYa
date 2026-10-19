package pe.edu.cibertec.fiadoya.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movimiento_stock")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    /** Con signo: negativo en VENTA, positivo en DEVOLUCION y REPOSICION. */
    @Column(nullable = false)
    private int cantidad;

    @Column(name = "venta_id")
    private Long ventaId;

    /** Precio congelado al momento de la venta: permite responder igual si el descuento se repite. */
    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected MovimientoStock() {
    }

    public MovimientoStock(Long productoId, TipoMovimiento tipo, int cantidad, Long ventaId,
                           BigDecimal precioUnitario, String motivo) {
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.ventaId = ventaId;
        this.precioUnitario = precioUnitario;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
