package pe.edu.cibertec.fiadoya.clientes.repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cibertec.fiadoya.clientes.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    /** El saldo se actualiza con la fila bloqueada: dos cargos simultáneos no pueden saltarse el límite. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> bloquear(@Param("id") Long id);

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByUsuarioIdAndIdNot(Long usuarioId, Long id);

    boolean existsByUsuarioId(Long usuarioId);

    long countByActivoTrue();

    long countByActivoTrueAndSaldoGreaterThan(BigDecimal saldo);

    @Query("select coalesce(sum(c.saldo), 0) from Cliente c where c.activo = true")
    BigDecimal deudaTotal();

    List<Cliente> findBySaldoGreaterThanOrderBySaldoDesc(BigDecimal saldo);
}
