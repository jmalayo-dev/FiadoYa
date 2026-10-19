package pe.edu.cibertec.fiadoya.clientes.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.clientes.entity.Cargo;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    Optional<Cargo> findByVentaId(Long ventaId);
}
