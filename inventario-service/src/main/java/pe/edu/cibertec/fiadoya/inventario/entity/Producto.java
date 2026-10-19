package pe.edu.cibertec.fiadoya.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal precio;

    private BigDecimal costo;

    @Column(nullable = false)
    private int stock;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria = Categoria.OTROS;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    protected Producto() {
    }

    public Producto(String nombre, BigDecimal precio, BigDecimal costo, int stock, int stockMinimo,
                    Categoria categoria, String codigoBarras) {
        this.nombre = nombre;
        this.precio = precio;
        this.costo = costo;
        this.stock = stock;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria == null ? Categoria.OTROS : categoria;
        this.codigoBarras = codigoBarras;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    /** R9: true si el cambio de stock cruza el mínimo (antes &gt; mínimo y después &lt;= mínimo). */
    public boolean cruzaMinimo(int stockAntes) {
        return stockAntes > stockMinimo && stock <= stockMinimo;
    }

    public void cambiarStock(int delta) {
        int nuevo = stock + delta;
        if (nuevo < 0) {
            throw new IllegalStateException("El stock no puede quedar negativo");
        }
        stock = nuevo;
    }

    public void actualizar(String nombre, BigDecimal precio, BigDecimal costo, int stockMinimo,
                           Categoria categoria, String codigoBarras) {
        this.nombre = nombre;
        this.precio = precio;
        this.costo = costo;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria == null ? Categoria.OTROS : categoria;
        this.codigoBarras = codigoBarras;
    }

    public void darDeBaja() {
        activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public int getStock() {
        return stock;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public boolean isActivo() {
        return activo;
    }
}
