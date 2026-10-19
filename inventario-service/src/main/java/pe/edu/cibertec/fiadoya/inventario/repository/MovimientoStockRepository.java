package pe.edu.cibertec.fiadoya.inventario.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cibertec.fiadoya.inventario.entity.MovimientoStock;
import pe.edu.cibertec.fiadoya.inventario.entity.TipoMovimiento;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByVentaIdAndTipoOrderByProductoId(Long ventaId, TipoMovimiento tipo);

    boolean existsByVentaIdAndProductoIdAndTipo(Long ventaId, Long productoId, TipoMovimiento tipo);
}
