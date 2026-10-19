package pe.edu.cibertec.fiadoya.inventario.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.inventario.dto.LoteDescuento;
import pe.edu.cibertec.fiadoya.inventario.dto.ResultadoDescuento;
import pe.edu.cibertec.fiadoya.inventario.entity.MovimientoStock;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;
import pe.edu.cibertec.fiadoya.inventario.entity.TipoMovimiento;
import pe.edu.cibertec.fiadoya.inventario.repository.MovimientoStockRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ProductoRepository;

/**
 * Movimientos de stock que usa la saga de ventas (Anexo §10.1 y §10.2).
 * Todo el lote va en una transacción: si un ítem no alcanza, no se descuenta ninguno.
 */
@Service
public class StockService {

    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;
    private final ApplicationEventPublisher eventos;

    public StockService(ProductoRepository productos, MovimientoStockRepository movimientos,
                        ApplicationEventPublisher eventos) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.eventos = eventos;
    }

    @Transactional
    public ResultadoDescuento descontar(LoteDescuento lote) {
        List<MovimientoStock> previos = movimientos.findByVentaIdAndTipoOrderByProductoId(lote.ventaId(), TipoMovimiento.VENTA);
        if (!previos.isEmpty()) {
            return resultadoDe(previos); // idempotente: el mismo ventaId devuelve lo mismo sin volver a descontar
        }
        // Se suman los productos repetidos (B9) y se procesan en orden de id para evitar bloqueos cruzados.
        Map<Long, Integer> cantidades = new TreeMap<>();
        lote.items().forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));

        List<ResultadoDescuento.Item> resultado = new ArrayList<>();
        List<StockBajo> alertas = new ArrayList<>();
        for (var entrada : cantidades.entrySet()) {
            Producto p = productos.bloquear(entrada.getKey())
                    .orElseThrow(() -> ApiException.noEncontrado("Producto", entrada.getKey()));
            int solicitado = entrada.getValue();
            if (!p.isActivo()) {
                throw new ApiException(ErrorCode.PRODUCTO_INACTIVO, p.getNombre() + " ya no está disponible.",
                        Map.of("productoId", p.getId(), "nombre", p.getNombre()));
            }
            if (p.getStock() < solicitado) {
                Map<String, Object> extra = new LinkedHashMap<>();
                extra.put("productoId", p.getId());
                extra.put("nombre", p.getNombre());
                extra.put("disponible", p.getStock());
                extra.put("solicitado", solicitado);
                throw new ApiException(ErrorCode.PRODUCTO_STOCK_INSUFICIENTE,
                        "No hay stock suficiente de " + p.getNombre() + ".", extra);
            }
            int antes = p.getStock();
            p.cambiarStock(-solicitado);
            movimientos.save(new MovimientoStock(p.getId(), TipoMovimiento.VENTA, -solicitado, lote.ventaId(), p.getPrecio(), null));
            resultado.add(new ResultadoDescuento.Item(p.getId(), p.getNombre(), p.getPrecio(), solicitado));
            if (p.cruzaMinimo(antes)) {
                alertas.add(StockBajo.nuevo(p.getId(), p.getNombre(), p.getStock(), p.getStockMinimo()));
            }
        }
        // Se publican después del commit (PublicadorEventos): si algo falla, no sale ninguna alerta.
        alertas.forEach(eventos::publishEvent);
        return new ResultadoDescuento(resultado);
    }

    /** Compensación: reingresa lo descontado para la venta. Idempotente: si no hubo descuento, no hace nada. */
    @Transactional
    public void devolver(Long ventaId) {
        for (MovimientoStock venta : movimientos.findByVentaIdAndTipoOrderByProductoId(ventaId, TipoMovimiento.VENTA)) {
            if (movimientos.existsByVentaIdAndProductoIdAndTipo(ventaId, venta.getProductoId(), TipoMovimiento.DEVOLUCION)) {
                continue;
            }
            Producto p = productos.bloquear(venta.getProductoId()).orElseThrow();
            int cantidad = -venta.getCantidad();
            p.cambiarStock(cantidad);
            movimientos.save(new MovimientoStock(p.getId(), TipoMovimiento.DEVOLUCION, cantidad, ventaId,
                    venta.getPrecioUnitario(), "Compensación de venta " + ventaId));
        }
    }

    private ResultadoDescuento resultadoDe(List<MovimientoStock> previos) {
        List<ResultadoDescuento.Item> items = previos.stream().map(m -> {
            String nombre = productos.findById(m.getProductoId()).map(Producto::getNombre).orElse("Producto " + m.getProductoId());
            return new ResultadoDescuento.Item(m.getProductoId(), nombre, m.getPrecioUnitario(), -m.getCantidad());
        }).toList();
        return new ResultadoDescuento(items);
    }
}
