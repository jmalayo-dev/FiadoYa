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

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    protected Pago() {
    }

    public Pago(Long clienteId, BigDecimal monto, MetodoPago metodo) {
        this.clienteId = clienteId;
        this.monto = monto;
        this.metodo = metodo;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public Instant getFecha() {
        return fecha;
    }
}
