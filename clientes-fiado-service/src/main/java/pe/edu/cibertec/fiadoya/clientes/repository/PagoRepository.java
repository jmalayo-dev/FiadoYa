package pe.edu.cibertec.fiadoya.clientes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.clientes.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
