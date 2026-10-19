package pe.edu.cibertec.fiadoya.clientes.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.clientes.entity.MovimientoCuenta;

public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    List<MovimientoCuenta> findTop200ByClienteIdOrderByFechaDescIdDesc(Long clienteId);
}
