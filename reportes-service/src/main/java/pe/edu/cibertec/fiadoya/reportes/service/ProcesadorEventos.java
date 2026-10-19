package pe.edu.cibertec.fiadoya.reportes.service;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.cibertec.fiadoya.common.web.Dinero;
import pe.edu.cibertec.fiadoya.contracts.PagoRegistrado;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;
import pe.edu.cibertec.fiadoya.reportes.entity.EventoProcesado;
import pe.edu.cibertec.fiadoya.reportes.entity.PagoResumen;
import pe.edu.cibertec.fiadoya.reportes.entity.ProductoVendido;
import pe.edu.cibertec.fiadoya.reportes.entity.VentaResumen;
import pe.edu.cibertec.fiadoya.reportes.repository.EventoProcesadoRepository;
import pe.edu.cibertec.fiadoya.reportes.repository.PagoResumenRepository;
import pe.edu.cibertec.fiadoya.reportes.repository.ProductoVendidoRepository;
import pe.edu.cibertec.fiadoya.reportes.repository.VentaResumenRepository;

/**
 * Consumo idempotente (Anexo §8.1): el eventId se guarda en evento_procesado en la misma transacción
 * que los datos. Si el evento llega otra vez, se ignora y el reporte no se duplica.
 */
@Service
public class ProcesadorEventos {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorEventos.class);

    private final EventoProcesadoRepository procesados;
    private final VentaResumenRepository ventas;
    private final ProductoVendidoRepository productos;
    private final PagoResumenRepository pagos;

    public ProcesadorEventos(EventoProcesadoRepository procesados, VentaResumenRepository ventas,
                             ProductoVendidoRepository productos, PagoResumenRepository pagos) {
        this.procesados = procesados;
        this.ventas = ventas;
        this.productos = productos;
        this.pagos = pagos;
    }

    @Transactional
    public boolean venta(VentaRegistrada e) {
        if (yaProcesado(e.eventId().toString())) {
            return false;
        }
        ventas.save(new VentaResumen(e.ventaId(), e.clienteId(), e.tipoPago(), e.total(), e.ocurridoEn()));
        for (VentaRegistrada.Item i : e.items()) {
            BigDecimal monto = Dinero.redondear(i.precioUnitario().multiply(BigDecimal.valueOf(i.cantidad())));
            productos.save(new ProductoVendido(e.ventaId(), i.productoId(), i.nombre(), i.cantidad(), monto, e.ocurridoEn()));
        }
        procesados.save(new EventoProcesado(e.eventId().toString(), e.tipo()));
        return true;
    }

    @Transactional
    public boolean pago(PagoRegistrado e) {
        if (yaProcesado(e.eventId().toString())) {
            return false;
        }
        pagos.save(new PagoResumen(e.pagoId(), e.clienteId(), e.monto(), e.metodo(), e.ocurridoEn()));
        procesados.save(new EventoProcesado(e.eventId().toString(), e.tipo()));
        return true;
    }

    private boolean yaProcesado(String eventId) {
        if (procesados.existsById(eventId)) {
            log.info("Evento {} ya procesado: se ignora", eventId);
            return true;
        }
        return false;
    }
}
