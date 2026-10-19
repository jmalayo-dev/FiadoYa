package pe.edu.cibertec.fiadoya.ventas.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.edu.cibertec.fiadoya.ventas.entity.EstadoVenta;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @EntityGraph(attributePaths = "items")
    Optional<Venta> findByClaveIdempotencia(String clave);

    @EntityGraph(attributePaths = "items")
    Optional<Venta> findWithItemsById(Long id);

    List<Venta> findByEstadoAndFechaBefore(EstadoVenta estado, Instant limite);
}
