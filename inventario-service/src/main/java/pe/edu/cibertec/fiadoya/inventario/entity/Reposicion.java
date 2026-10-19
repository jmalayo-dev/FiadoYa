package pe.edu.cibertec.fiadoya.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reposicion")
public class Reposicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "nro_pedido", nullable = false)
    private String nroPedido;

    @Column(name = "fecha_estimada", nullable = false)
    private LocalDate fechaEstimada;

    @Column(nullable = false)
    private boolean recibida;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    protected Reposicion() {
    }

    public Reposicion(Long productoId, int cantidad, String nroPedido, LocalDate fechaEstimada) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.nroPedido = nroPedido;
        this.fechaEstimada = fechaEstimada;
    }

    public void marcarRecibida() {
        recibida = true;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getNroPedido() {
        return nroPedido;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public boolean isRecibida() {
        return recibida;
    }
}
