package pe.edu.cibertec.fiadoya.reportes.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.reportes.entity.ProductoVendido;

public interface ProductoVendidoRepository extends JpaRepository<ProductoVendido, Long> {

    List<ProductoVendido> findByFechaGreaterThanEqualAndFechaLessThan(Instant desde, Instant hasta);
}
