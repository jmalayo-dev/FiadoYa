package pe.edu.cibertec.fiadoya.clientes.entity;

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

/** Una línea del estado de cuenta con el saldo que quedó después. */
@Entity
@Table(name = "movimiento_cuenta")
public class MovimientoCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "saldo_despues", nullable = false)
    private BigDecimal saldoDespues;

    private String descripcion;

    @Column(name = "cantidad_items")
    private Integer cantidadItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago")
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected MovimientoCuenta() {
    }

    public static MovimientoCuenta cargo(Cliente c, Cargo cargo) {
        return new MovimientoCuenta(c.getId(), TipoMovimiento.CARGO, cargo.getMonto(), cargo.getVentaId(), c.getSaldo(),
                cargo.getDescripcion(), cargo.getCantidadItems(), null);
    }

    public static MovimientoCuenta anulacion(Cliente c, Cargo cargo) {
        return new MovimientoCuenta(c.getId(), TipoMovimiento.ANULACION, cargo.getMonto(), cargo.getVentaId(), c.getSaldo(),
                cargo.getDescripcion(), cargo.getCantidadItems(), null);
    }

    public static MovimientoCuenta pago(Cliente c, Pago pago) {
        return new MovimientoCuenta(c.getId(), TipoMovimiento.PAGO, pago.getMonto(), null, c.getSaldo(), null, null,
                pago.getMetodo());
    }

    private MovimientoCuenta(Long clienteId, TipoMovimiento tipo, BigDecimal monto, Long ventaId, BigDecimal saldoDespues,
                             String descripcion, Integer cantidadItems, MetodoPago metodoPago) {
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.monto = monto;
        this.ventaId = ventaId;
        this.saldoDespues = saldoDespues;
        this.descripcion = descripcion;
        this.cantidadItems = cantidadItems;
        this.metodoPago = metodoPago;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public BigDecimal getSaldoDespues() {
        return saldoDespues;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getCantidadItems() {
        return cantidadItems;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public Instant getFecha() {
        return fecha;
    }
}
