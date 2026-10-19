package pe.edu.cibertec.fiadoya.reportes.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.reportes.entity.VentaResumen;

public interface VentaResumenRepository extends JpaRepository<VentaResumen, Long> {

    List<VentaResumen> findByFechaGreaterThanEqualAndFechaLessThan(Instant desde, Instant hasta);
}
