package pe.edu.cibertec.fiadoya.inventario.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.common.web.Paginacion;
import pe.edu.cibertec.fiadoya.inventario.client.DistribuidoraClient;
import pe.edu.cibertec.fiadoya.inventario.dto.CatalogoItem;
import pe.edu.cibertec.fiadoya.inventario.dto.ReposicionDto;
import pe.edu.cibertec.fiadoya.inventario.dto.ReposicionInput;
import pe.edu.cibertec.fiadoya.inventario.entity.MovimientoStock;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;
import pe.edu.cibertec.fiadoya.inventario.entity.Reposicion;
import pe.edu.cibertec.fiadoya.inventario.entity.TipoMovimiento;
import pe.edu.cibertec.fiadoya.inventario.repository.MovimientoStockRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ProductoRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ReposicionRepository;

/** HU11: pedidos de reposición a la distribuidora por SOAP. */
@Service
public class ReposicionService {

    private final ReposicionRepository reposiciones;
    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;
    private final DistribuidoraClient distribuidora;

    public ReposicionService(ReposicionRepository reposiciones, ProductoRepository productos,
                             MovimientoStockRepository movimientos, DistribuidoraClient distribuidora) {
        this.reposiciones = reposiciones;
        this.productos = productos;
        this.movimientos = movimientos;
        this.distribuidora = distribuidora;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReposicionDto> listar(Integer page, Integer size) {
        var pagina = reposiciones.findAll(Paginacion.de(page, size, Sort.by(Sort.Direction.DESC, "creadoEn", "id")));
        return PageResponse.de(pagina, this::aDto);
    }

    public List<CatalogoItem> catalogo() {
        return distribuidora.consultarCatalogo();
    }

    /** La llamada SOAP va fuera de la transacción: no se mantiene una conexión a BD esperando a la distribuidora. */
    public ReposicionDto crear(ReposicionInput in) {
        Producto p = productos.findById(in.productoId()).orElseThrow(() -> ApiException.noEncontrado("Producto", in.productoId()));
        if (!p.isActivo()) {
            throw new ApiException(ErrorCode.PRODUCTO_INACTIVO, p.getNombre() + " está dado de baja y no se repone.",
                    Map.of("productoId", p.getId(), "nombre", p.getNombre()));
        }
        var pedido = distribuidora.registrarPedido(DistribuidoraClient.codigoDe(p.getId()), in.cantidad());
        Reposicion guardada = reposiciones.save(new Reposicion(p.getId(), in.cantidad(), pedido.nroPedido(), pedido.fechaEstimada()));
        return aDto(guardada);
    }

    /** Suma el stock y registra el movimiento REPOSICION. Repetirlo no vuelve a sumar. */
    @Transactional
    public ReposicionDto recibir(Long id) {
        Reposicion r = reposiciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("Reposición", id));
        if (!r.isRecibida()) {
            Producto p = productos.bloquear(r.getProductoId()).orElseThrow();
            p.cambiarStock(r.getCantidad());
            movimientos.save(new MovimientoStock(p.getId(), TipoMovimiento.REPOSICION, r.getCantidad(), null, null,
                    "Pedido " + r.getNroPedido()));
            r.marcarRecibida();
        }
        return aDto(r);
    }

    private ReposicionDto aDto(Reposicion r) {
        String nombre = productos.findById(r.getProductoId()).map(Producto::getNombre).orElse("");
        return new ReposicionDto(r.getId(), r.getProductoId(), nombre, r.getCantidad(), r.getNroPedido(),
                r.getFechaEstimada(), r.isRecibida());
    }
}
