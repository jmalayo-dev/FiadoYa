package pe.edu.cibertec.fiadoya.clientes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import pe.edu.cibertec.fiadoya.common.web.Dinero;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    private String dni;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    private String direccion;

    @Column(name = "limite_credito", nullable = false)
    private BigDecimal limiteCredito;

    @Column(nullable = false)
    private BigDecimal saldo = Dinero.CERO;

    @Column(nullable = false)
    private boolean consentimiento;

    @Column(name = "fecha_consentimiento")
    private Instant fechaConsentimiento;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    protected Cliente() {
    }

    public Cliente(String nombre, String telefono, String dni, String direccion, BigDecimal limiteCredito,
                   boolean consentimiento, Long usuarioId) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.dni = dni;
        this.direccion = direccion;
        this.limiteCredito = limiteCredito;
        this.usuarioId = usuarioId;
        cambiarConsentimiento(consentimiento);
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    public void actualizar(String nombre, String telefono, String dni, String direccion, BigDecimal limiteCredito,
                           boolean consentimiento, Long usuarioId) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.dni = dni;
        this.direccion = direccion;
        this.limiteCredito = limiteCredito;
        this.usuarioId = usuarioId;
        cambiarConsentimiento(consentimiento);
    }

    /** B8: la fecha la fija el servidor al pasar a true. Revocar no borra la deuda. */
    private void cambiarConsentimiento(boolean nuevo) {
        if (nuevo && !consentimiento) {
            fechaConsentimiento = Instant.now();
        } else if (!nuevo) {
            fechaConsentimiento = null;
        }
        consentimiento = nuevo;
    }

    public BigDecimal disponible() {
        return Dinero.redondear(limiteCredito.subtract(saldo).max(BigDecimal.ZERO));
    }

    public void cargar(BigDecimal monto) {
        saldo = Dinero.redondear(saldo.add(monto));
    }

    /** Pago o anulación: el saldo nunca baja de 0. */
    public void abonar(BigDecimal monto) {
        saldo = Dinero.redondear(saldo.subtract(monto).max(BigDecimal.ZERO));
    }

    public void desactivar() {
        activo = false;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public boolean isConsentimiento() {
        return consentimiento;
    }

    public Instant getFechaConsentimiento() {
        return fechaConsentimiento;
    }

    public boolean isActivo() {
        return activo;
    }
}
