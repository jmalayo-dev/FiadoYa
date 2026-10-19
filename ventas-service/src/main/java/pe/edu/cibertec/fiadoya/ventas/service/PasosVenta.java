package pe.edu.cibertec.fiadoya.ventas.service;

import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;
import pe.edu.cibertec.fiadoya.ventas.client.InventarioClient;
import pe.edu.cibertec.fiadoya.ventas.entity.TipoPago;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;
import pe.edu.cibertec.fiadoya.ventas.repository.VentaRepository;

/**
 * Cada paso local de la saga en su propia transacción corta. Entre pasos ocurren las llamadas remotas,
 * así no se mantiene una transacción abierta esperando a otro servicio.
 */
@Service
public class PasosVenta {

    private final VentaRepository ventas;
    private final ApplicationEventPublisher eventos;

    public PasosVenta(VentaRepository ventas, ApplicationEventPublisher eventos) {
        this.ventas = ventas;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorClave(String clave) {
        Optional<Venta> v = ventas.findByClaveIdempotencia(clave);
        v.ifPresent(x -> x.getItems().size());
        return v;
    }

    @Transactional
    public Venta crearPendiente(Long clienteId, String clienteNombre, TipoPago tipo, String clave, String hash) {
        return ventas.saveAndFlush(new Venta(clienteId, clienteNombre, tipo, clave, hash));
    }

    /** Stock insuficiente: la venta no queda registrada (Anexo §6.3). */
    @Transactional
    public void borrar(Long ventaId) {
        ventas.deleteById(ventaId);
    }

    @Transactional
    public Venta registrarDetalle(Long ventaId, InventarioClient.Resultado resultado) {
        Venta v = cargar(ventaId);
        resultado.items().forEach(i -> v.agregarItem(i.productoId(), i.nombre(), i.cantidad(), i.precioUnitario()));
        v.calcularTotal();
        return v;
    }

    @Transactional
    public Venta rechazar(Long ventaId, String codigo, String motivo) {
        Venta v = cargar(ventaId);
        v.rechazar(codigo, recortar(motivo));
        v.getItems().size();
        return v;
    }

    /** Confirma y publica venta.registrada después del commit. */
    @Transactional
    public Venta confirmar(Long ventaId) {
        Venta v = cargar(ventaId);
        v.confirmar();
        eventos.publishEvent(VentaRegistrada.nueva(v.getId(), v.getClienteId(), v.getTipoPago().name(), v.getTotal(),
                v.getItems().stream().map(d -> new VentaRegistrada.Item(d.getProductoId(), d.getNombreProducto(),
                        d.getCantidad(), d.getPrecioUnitario())).toList()));
        return v;
    }

    private Venta cargar(Long id) {
        return ventas.findWithItemsById(id).orElseThrow(() -> ApiException.noEncontrado("Venta", id));
    }

    private static String recortar(String s) {
        return s == null || s.length() <= 200 ? s : s.substring(0, 200);
    }
}
