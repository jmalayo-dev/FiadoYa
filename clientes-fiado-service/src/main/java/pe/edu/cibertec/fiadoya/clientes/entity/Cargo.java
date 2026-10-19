package pe.edu.cibertec.fiadoya.clientes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cargo")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "venta_id", nullable = false, unique = true)
    private Long ventaId;

    @Column(nullable = false)
    private BigDecimal monto;

    private String descripcion;

    @Column(name = "cantidad_items")
    private Integer cantidadItems;

    @Column(nullable = false)
    private boolean anulado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected Cargo() {
    }

    public Cargo(Long clienteId, Long ventaId, BigDecimal monto, String descripcion, Integer cantidadItems) {
        this.clienteId = clienteId;
        this.ventaId = ventaId;
        this.monto = monto;
        this.descripcion = descripcion;
        this.cantidadItems = cantidadItems;
    }

    public void anular() {
        anulado = true;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getCantidadItems() {
        return cantidadItems;
    }

    public boolean isAnulado() {
        return anulado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
