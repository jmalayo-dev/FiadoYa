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
@Table(name = "producto_vendido")
public class ProductoVendido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private Instant fecha;

    protected ProductoVendido() {
    }

    public ProductoVendido(Long ventaId, Long productoId, String nombreProducto, int cantidad, BigDecimal monto, Instant fecha) {
        this.ventaId = ventaId;
        this.productoId = productoId;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.monto = monto;
        this.fecha = fecha;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
