package pe.edu.cibertec.fiadoya.inventario.service;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.Dinero;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.common.web.PageResponse;
import pe.edu.cibertec.fiadoya.common.web.Paginacion;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.inventario.dto.AjusteStock;
import pe.edu.cibertec.fiadoya.inventario.dto.ProductoDto;
import pe.edu.cibertec.fiadoya.inventario.dto.ProductoInput;
import pe.edu.cibertec.fiadoya.inventario.entity.Categoria;
import pe.edu.cibertec.fiadoya.inventario.entity.MovimientoStock;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;
import pe.edu.cibertec.fiadoya.inventario.entity.TipoMovimiento;
import pe.edu.cibertec.fiadoya.inventario.repository.MovimientoStockRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ProductoRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ProductoSpecs;

/** HU03: CRUD de productos y ajuste manual de stock. */
@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;
    private final ApplicationEventPublisher eventos;

    public ProductoService(ProductoRepository productos, MovimientoStockRepository movimientos,
                           ApplicationEventPublisher eventos) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoDto> listar(String q, Boolean soloBajos, Categoria categoria, String codigo,
                                            Integer page, Integer size) {
        Specification<Producto> spec = Stream.of(ProductoSpecs.activos(), ProductoSpecs.texto(q),
                        ProductoSpecs.soloBajos(soloBajos), ProductoSpecs.categoria(categoria), ProductoSpecs.codigo(codigo))
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElseThrow();
        var pagina = productos.findAll(spec, Paginacion.de(page, size, Sort.by("nombre").and(Sort.by("id"))));
        return PageResponse.de(pagina, ProductoDto::de);
    }

    @Transactional(readOnly = true)
    public ProductoDto obtener(Long id) {
        return ProductoDto.de(buscar(id));
    }

    @Transactional
    public ProductoDto crear(ProductoInput in) {
        if (in.stock() == null) {
            throw ApiException.validacion("stock", "es obligatorio al crear");
        }
        String codigo = normalizar(in.codigoBarras());
        if (codigo != null && productos.existsByCodigoBarras(codigo)) {
            throw codigoDuplicado(codigo);
        }
        Producto p = new Producto(in.nombre().trim(), Dinero.redondear(in.precio()),
                in.costo() == null ? null : Dinero.redondear(in.costo()),
                in.stock(), in.stockMinimo(), in.categoria(), codigo);
        return ProductoDto.de(productos.save(p));
    }

    @Transactional
    public ProductoDto actualizar(Long id, ProductoInput in) {
        Producto p = buscar(id);
        String codigo = normalizar(in.codigoBarras());
        if (codigo != null && productos.existsByCodigoBarrasAndIdNot(codigo, id)) {
            throw codigoDuplicado(codigo);
        }
        int minimoAnterior = p.getStockMinimo();
        p.actualizar(in.nombre().trim(), Dinero.redondear(in.precio()),
                in.costo() == null ? null : Dinero.redondear(in.costo()), in.stockMinimo(), in.categoria(), codigo);
        // Subir el mínimo por encima del stock actual también deja al producto "bajo": se avisa una sola vez.
        avisarSiCruzaMinimo(p, p.getStock(), p.getStock() > minimoAnterior && p.getStock() <= p.getStockMinimo());
        return ProductoDto.de(p);
    }

    /** Baja lógica (B12): no se vende ni se repone, pero sigue en ventas históricas. */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(id).darDeBaja();
    }

    /** Ajuste manual con motivo. B13: el stock nunca queda negativo. */
    @Transactional
    public ProductoDto ajustar(Long id, AjusteStock ajuste) {
        if (ajuste.delta() == 0) {
            throw ApiException.validacion("delta", "no puede ser 0");
        }
        Producto p = productos.bloquear(id).orElseThrow(() -> ApiException.noEncontrado("Producto", id));
        if (!p.isActivo()) {
            throw new ApiException(ErrorCode.PRODUCTO_INACTIVO, p.getNombre() + " está dado de baja.",
                    Map.of("productoId", p.getId(), "nombre", p.getNombre()));
        }
        if (p.getStock() + ajuste.delta() < 0) {
            throw ApiException.validacion("delta", "dejaría el stock en negativo (stock actual " + p.getStock() + ")");
        }
        int antes = p.getStock();
        p.cambiarStock(ajuste.delta());
        movimientos.save(new MovimientoStock(p.getId(), TipoMovimiento.AJUSTE, ajuste.delta(), null, null, ajuste.motivo().trim()));
        avisarSiCruzaMinimo(p, antes, p.cruzaMinimo(antes));
        return ProductoDto.de(p);
    }

    Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Producto", id));
    }

    private void avisarSiCruzaMinimo(Producto p, int antes, boolean cruza) {
        if (cruza) {
            eventos.publishEvent(StockBajo.nuevo(p.getId(), p.getNombre(), p.getStock(), p.getStockMinimo()));
        }
    }

    private static String normalizar(String codigo) {
        return codigo == null || codigo.isBlank() ? null : codigo.trim();
    }

    private static ApiException codigoDuplicado(String codigo) {
        return new ApiException(ErrorCode.CODIGO_DUPLICADO, "Ya existe un producto con el código " + codigo + ".",
                Map.of("codigoBarras", codigo));
    }
}
