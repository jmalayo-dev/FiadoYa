package pe.edu.cibertec.fiadoya.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.common.web.ErrorCode;
import pe.edu.cibertec.fiadoya.ventas.client.ClientesClient;
import pe.edu.cibertec.fiadoya.ventas.client.InventarioClient;
import pe.edu.cibertec.fiadoya.ventas.dto.ItemInput;
import pe.edu.cibertec.fiadoya.ventas.dto.VentaInput;
import pe.edu.cibertec.fiadoya.ventas.entity.TipoPago;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;
import pe.edu.cibertec.fiadoya.ventas.service.LlamadasRemotas;
import pe.edu.cibertec.fiadoya.ventas.service.PasosVenta;
import pe.edu.cibertec.fiadoya.ventas.service.SagaVentas;

@ExtendWith(MockitoExtension.class)
class SagaVentasTest {

    @Mock
    PasosVenta pasos;
    @Mock
    LlamadasRemotas remotas;

    SagaVentas saga;
    final String clave = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        saga = new SagaVentas(pasos, remotas);
    }

    static Venta venta(long id, TipoPago tipo, Long cliente) {
        Venta v = new Venta(cliente, cliente == null ? null : "María", tipo, "k", "h");
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }

    static InventarioClient.Resultado precios() {
        return new InventarioClient.Resultado(List.of(
                new InventarioClient.ItemPrecio(1L, "Arroz", new BigDecimal("4.50"), 2),
                new InventarioClient.ItemPrecio(2L, "Aceite", new BigDecimal("9.80"), 1)));
    }

    private Venta conDetalle(Venta v) {
        precios().items().forEach(i -> v.agregarItem(i.productoId(), i.nombre(), i.cantidad(), i.precioUnitario()));
        v.calcularTotal();
        return v;
    }

    private final VentaInput contado = new VentaInput(null, TipoPago.CONTADO,
            List.of(new ItemInput(1L, 2), new ItemInput(2L, 1)));
    private final VentaInput fiado = new VentaInput(3L, TipoPago.FIADO,
            List.of(new ItemInput(1L, 2), new ItemInput(2L, 1)));

    @Test
    void ventaAlContadoSeConfirma() {
        Venta v = venta(10, TipoPago.CONTADO, null);
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());
        when(pasos.crearPendiente(any(), any(), any(), anyString(), anyString())).thenReturn(v);
        when(remotas.descontar(eq(10L), any())).thenReturn(precios());
        when(pasos.registrarDetalle(eq(10L), any())).thenReturn(conDetalle(v));
        when(pasos.confirmar(10L)).thenAnswer(inv -> {
            v.confirmar();
            return v;
        });

        var dto = saga.registrar(clave, contado);

        assertThat(dto.estado()).isEqualTo("CONFIRMADA");
        assertThat(dto.total()).isEqualByComparingTo("18.80");
        verify(remotas, never()).cargar(anyLong(), any());
    }

    @Test
    void mismaClaveMismoCuerpoDevuelveLaVentaSinRepetirLaSaga() {
        Venta previa = conDetalle(venta(11, TipoPago.CONTADO, null));
        previa.confirmar();
        String hash = (String) ReflectionTestUtils.invokeMethod(SagaVentas.class, "hash", contado,
                ReflectionTestUtils.invokeMethod(SagaVentas.class, "agrupar", contado.items()));
        ReflectionTestUtils.setField(previa, "hashCuerpo", hash);
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.of(previa));

        var dto = saga.registrar(clave, contado);

        assertThat(dto.id()).isEqualTo(11L);
        verify(remotas, never()).descontar(anyLong(), any());
    }

    @Test
    void mismaClaveConOtroCuerpoEsConflicto() {
        Venta previa = venta(12, TipoPago.CONTADO, null);
        ReflectionTestUtils.setField(previa, "hashCuerpo", "otro");
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.of(previa));

        assertThatThrownBy(() -> saga.registrar(clave, contado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void stockInsuficienteNoRegistraLaVenta() {
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());
        when(pasos.crearPendiente(any(), any(), any(), anyString(), anyString())).thenReturn(venta(13, TipoPago.CONTADO, null));
        when(remotas.descontar(eq(13L), any())).thenThrow(new ApiException(ErrorCode.PRODUCTO_STOCK_INSUFICIENTE, "sin stock"));

        assertThatThrownBy(() -> saga.registrar(clave, contado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.PRODUCTO_STOCK_INSUFICIENTE);
        verify(pasos).borrar(13L);
    }

    @Test
    void fiadoQueExcedeElLimiteDevuelveStockYRechaza() {
        Venta v = venta(14, TipoPago.FIADO, 3L);
        when(remotas.validarCliente(3L)).thenReturn(new ClientesClient.Validacion(true, true, true, "María"));
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());
        when(pasos.crearPendiente(any(), any(), any(), anyString(), anyString())).thenReturn(v);
        when(remotas.descontar(eq(14L), any())).thenReturn(precios());
        when(pasos.registrarDetalle(eq(14L), any())).thenReturn(conDetalle(v));
        when(remotas.cargar(eq(3L), any())).thenThrow(new ApiException(ErrorCode.CLIENTE_LIMITE_EXCEDIDO, "excede",
                Map.of("disponible", 5)));

        assertThatThrownBy(() -> saga.registrar(clave, fiado))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(ErrorCode.CLIENTE_LIMITE_EXCEDIDO);
                    assertThat(e.getExtra()).containsEntry("ventaId", 14L).containsEntry("disponible", 5);
                });
        verify(remotas).devolver(14L);
        verify(pasos).rechazar(14L, "CLIENTE_LIMITE_EXCEDIDO", "Supera el límite de crédito. Disponible: S/ 5.00");
    }

    @Test
    void clientesCaidoDevuelveStockYResponde503() {
        Venta v = venta(15, TipoPago.FIADO, 3L);
        when(remotas.validarCliente(3L)).thenReturn(new ClientesClient.Validacion(true, true, true, "María"));
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());
        when(pasos.crearPendiente(any(), any(), any(), anyString(), anyString())).thenReturn(v);
        when(remotas.descontar(eq(15L), any())).thenReturn(precios());
        when(pasos.registrarDetalle(eq(15L), any())).thenReturn(conDetalle(v));
        ApiException caido = new ApiException(ErrorCode.SERVICIO_NO_DISPONIBLE, "caído", Map.of("servicio", "clientes"));
        when(remotas.cargar(eq(3L), any())).thenThrow(caido);
        doThrow(caido).when(remotas).anularCargo(15L);

        assertThatThrownBy(() -> saga.registrar(clave, fiado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.SERVICIO_NO_DISPONIBLE);
        verify(remotas).devolver(15L);
        verify(pasos).rechazar(eq(15L), eq("SERVICIO_NO_DISPONIBLE"), anyString());
    }

    @Test
    void siNoSePuedeDevolverElStockLaVentaQuedaPendiente() {
        Venta v = venta(16, TipoPago.CONTADO, null);
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());
        when(pasos.crearPendiente(any(), any(), any(), anyString(), anyString())).thenReturn(v);
        ApiException caido = new ApiException(ErrorCode.SERVICIO_NO_DISPONIBLE, "caído", Map.of("servicio", "inventario"));
        when(remotas.descontar(eq(16L), any())).thenThrow(caido);
        doThrow(caido).when(remotas).devolver(16L);

        assertThatThrownBy(() -> saga.registrar(clave, contado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.SERVICIO_NO_DISPONIBLE);
        verify(pasos, never()).rechazar(anyLong(), anyString(), anyString());
    }

    @Test
    void clienteSinConsentimientoFallaAntesDeTocarStock() {
        when(remotas.validarCliente(3L)).thenReturn(new ClientesClient.Validacion(true, true, false, "María"));
        when(pasos.buscarPorClave(clave)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saga.registrar(clave, fiado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CLIENTE_SIN_CONSENTIMIENTO);
        verify(remotas, never()).descontar(anyLong(), any());
    }

    @Test
    void reglasDeEntrada() {
        assertThatThrownBy(() -> saga.registrar(clave, new VentaInput(3L, TipoPago.CONTADO, contado.items())))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> saga.registrar("no-es-uuid", contado))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> saga.registrar(clave, new VentaInput(null, TipoPago.CONTADO,
                List.of(new ItemInput(1L, 600), new ItemInput(1L, 600)))))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void totalUsaHalfUp() {
        Venta v = venta(17, TipoPago.CONTADO, null);
        v.agregarItem(1L, "A", 3, new BigDecimal("0.335"));
        v.calcularTotal();
        assertThat(v.getTotal()).isEqualByComparingTo("1.01");
    }

    @Test
    void descripcionResumeLosProductos() {
        var r = new InventarioClient.Resultado(List.of(
                new InventarioClient.ItemPrecio(1L, "Arroz", BigDecimal.ONE, 1),
                new InventarioClient.ItemPrecio(2L, "Aceite", BigDecimal.ONE, 1),
                new InventarioClient.ItemPrecio(3L, "Leche", BigDecimal.ONE, 1),
                new InventarioClient.ItemPrecio(4L, "Pan", BigDecimal.ONE, 1)));
        String d = ReflectionTestUtils.invokeMethod(SagaVentas.class, "describir", r);
        assertThat(d).isEqualTo("Arroz, Aceite, Leche y 1 más");
    }
}
