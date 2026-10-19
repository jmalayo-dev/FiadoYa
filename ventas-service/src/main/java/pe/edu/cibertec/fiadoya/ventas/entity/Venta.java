package pe.edu.cibertec.fiadoya.ventas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import pe.edu.cibertec.fiadoya.common.web.Dinero;

@Entity
@Table(name = "venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false)
    private TipoPago tipoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Column(nullable = false)
    private BigDecimal total = Dinero.CERO;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "codigo_rechazo")
    private String codigoRechazo;

    @Column(name = "clave_idempotencia", nullable = false, unique = true)
    private String claveIdempotencia;

    @Column(name = "hash_cuerpo", nullable = false)
    private String hashCuerpo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id")
    private List<DetalleVenta> items = new ArrayList<>();

    protected Venta() {
    }

    public Venta(Long clienteId, String clienteNombre, TipoPago tipoPago, String claveIdempotencia, String hashCuerpo) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.tipoPago = tipoPago;
        this.claveIdempotencia = claveIdempotencia;
        this.hashCuerpo = hashCuerpo;
    }

    public void agregarItem(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario) {
        items.add(new DetalleVenta(this, productoId, nombre, cantidad, precioUnitario));
    }

    /** B1: total = suma de precioUnitario × cantidad, HALF_UP a 2 decimales. */
    public void calcularTotal() {
        total = Dinero.redondear(items.stream().map(DetalleVenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public void confirmar() {
        exigirPendiente();
        estado = EstadoVenta.CONFIRMADA;
    }

    public void rechazar(String codigo, String motivo) {
        exigirPendiente();
        estado = EstadoVenta.RECHAZADA;
        codigoRechazo = codigo;
        motivoRechazo = motivo;
    }

    private void exigirPendiente() {
        if (estado != EstadoVenta.PENDIENTE) {
            throw new IllegalStateException("La venta " + id + " ya está " + estado);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public TipoPago getTipoPago() {
        return tipoPago;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public String getCodigoRechazo() {
        return codigoRechazo;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public String getHashCuerpo() {
        return hashCuerpo;
    }

    public Instant getFecha() {
        return fecha;
    }

    public List<DetalleVenta> getItems() {
        return items;
    }
}
