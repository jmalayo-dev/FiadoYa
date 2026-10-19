package pe.edu.cibertec.fiadoya.inventario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.inventario.entity.Reposicion;

public interface ReposicionRepository extends JpaRepository<Reposicion, Long> {
}
