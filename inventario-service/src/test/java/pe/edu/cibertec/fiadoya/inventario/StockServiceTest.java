package pe.edu.cibertec.fiadoya.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.inventario.dto.LoteDescuento;
import pe.edu.cibertec.fiadoya.inventario.entity.Categoria;
import pe.edu.cibertec.fiadoya.inventario.entity.MovimientoStock;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;
import pe.edu.cibertec.fiadoya.inventario.entity.TipoMovimiento;
import pe.edu.cibertec.fiadoya.inventario.repository.MovimientoStockRepository;
import pe.edu.cibertec.fiadoya.inventario.repository.ProductoRepository;
import pe.edu.cibertec.fiadoya.inventario.service.StockService;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    ProductoRepository productos;
    @Mock
    MovimientoStockRepository movimientos;
    @Mock
    ApplicationEventPublisher eventos;

    StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(productos, movimientos, eventos);
    }

    static Producto producto(long id, String nombre, String precio, int stock, int minimo) {
        Producto p = new Producto(nombre, new BigDecimal(precio), null, stock, minimo, Categoria.ABARROTES, null);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void descuentaYSumaProductosRepetidos() {
        Producto arroz = producto(1, "Arroz", "4.50", 40, 8);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(10L, TipoMovimiento.VENTA)).thenReturn(List.of());
        when(productos.bloquear(1L)).thenReturn(Optional.of(arroz));

        var r = service.descontar(new LoteDescuento(10L, List.of(new LoteDescuento.Item(1L, 2), new LoteDescuento.Item(1L, 3))));

        assertThat(arroz.getStock()).isEqualTo(35);
        assertThat(r.items()).singleElement().satisfies(i -> {
            assertThat(i.cantidad()).isEqualTo(5);
            assertThat(i.precioUnitario()).isEqualByComparingTo("4.50");
        });
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void cruzarElMinimoPublicaUnStockBajo() {
        Producto aceite = producto(2, "Aceite", "9.80", 7, 6);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(11L, TipoMovimiento.VENTA)).thenReturn(List.of());
        when(productos.bloquear(2L)).thenReturn(Optional.of(aceite));

        service.descontar(new LoteDescuento(11L, List.of(new LoteDescuento.Item(2L, 1))));

        verify(eventos, times(1)).publishEvent(any(StockBajo.class));
    }

    @Test
    void siYaEstabaBajoNoPublicaOtraVez() {
        Producto leche = producto(5, "Leche", "4.00", 5, 5);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(12L, TipoMovimiento.VENTA)).thenReturn(List.of());
        when(productos.bloquear(5L)).thenReturn(Optional.of(leche));

        service.descontar(new LoteDescuento(12L, List.of(new LoteDescuento.Item(5L, 1))));

        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void stockInsuficienteFallaConExtra() {
        Producto aceite = producto(2, "Aceite", "9.80", 3, 1);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(13L, TipoMovimiento.VENTA)).thenReturn(List.of());
        when(productos.bloquear(2L)).thenReturn(Optional.of(aceite));

        assertThatThrownBy(() -> service.descontar(new LoteDescuento(13L, List.of(new LoteDescuento.Item(2L, 4)))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.PRODUCTO_STOCK_INSUFICIENTE);
                    assertThat(e.getExtra()).containsEntry("disponible", 3).containsEntry("solicitado", 4);
                });
    }

    @Test
    void productoInactivoFalla() {
        Producto p = producto(3, "Viejo", "1.00", 10, 1);
        p.darDeBaja();
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(14L, TipoMovimiento.VENTA)).thenReturn(List.of());
        when(productos.bloquear(3L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.descontar(new LoteDescuento(14L, List.of(new LoteDescuento.Item(3L, 1)))))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.PRODUCTO_INACTIVO);
    }

    @Test
    void descuentoRepetidoDevuelveLoMismoSinDescontar() {
        var previo = new MovimientoStock(1L, TipoMovimiento.VENTA, -2, 15L, new BigDecimal("4.50"), null);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(15L, TipoMovimiento.VENTA)).thenReturn(List.of(previo));
        when(productos.findById(1L)).thenReturn(Optional.of(producto(1, "Arroz", "4.50", 38, 8)));

        var r = service.descontar(new LoteDescuento(15L, List.of(new LoteDescuento.Item(1L, 2))));

        assertThat(r.items()).singleElement().satisfies(i -> assertThat(i.cantidad()).isEqualTo(2));
        verify(productos, never()).bloquear(any());
    }

    @Test
    void devolverReingresaYEsIdempotente() {
        Producto arroz = producto(1, "Arroz", "4.50", 38, 8);
        var venta = new MovimientoStock(1L, TipoMovimiento.VENTA, -2, 16L, new BigDecimal("4.50"), null);
        when(movimientos.findByVentaIdAndTipoOrderByProductoId(16L, TipoMovimiento.VENTA)).thenReturn(List.of(venta));
        when(movimientos.existsByVentaIdAndProductoIdAndTipo(16L, 1L, TipoMovimiento.DEVOLUCION)).thenReturn(false, true);
        when(productos.bloquear(1L)).thenReturn(Optional.of(arroz));

        service.devolver(16L);
        service.devolver(16L);

        assertThat(arroz.getStock()).isEqualTo(40);
    }
}
