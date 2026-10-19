package pe.edu.cibertec.fiadoya.reportes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.reportes.entity.EventoProcesado;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, String> {
}
