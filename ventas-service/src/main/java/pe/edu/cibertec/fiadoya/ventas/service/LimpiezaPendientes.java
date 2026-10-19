package pe.edu.cibertec.fiadoya.ventas.service;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.fiadoya.common.web.ApiException;
import pe.edu.cibertec.fiadoya.ventas.entity.EstadoVenta;
import pe.edu.cibertec.fiadoya.ventas.entity.TipoPago;
import pe.edu.cibertec.fiadoya.ventas.entity.Venta;
import pe.edu.cibertec.fiadoya.ventas.repository.VentaRepository;

/**
 * Cada 2 minutos cierra las ventas que quedaron PENDIENTE más de 5 minutos (caída a mitad de la saga):
 * devuelve el stock, anula el cargo y las marca RECHAZADA con motivo "tiempo agotado".
 */
@Component
public class LimpiezaPendientes {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaPendientes.class);

    private final VentaRepository ventas;
    private final LlamadasRemotas remotas;
    private final PasosVenta pasos;
    private final Duration maximo;

    public LimpiezaPendientes(VentaRepository ventas, LlamadasRemotas remotas, PasosVenta pasos,
                              @Value("${fiadoya.ventas.pendiente-max-minutos:5}") long minutos) {
        this.ventas = ventas;
        this.remotas = remotas;
        this.pasos = pasos;
        this.maximo = Duration.ofMinutes(minutos);
    }

    @Scheduled(fixedDelayString = "${fiadoya.ventas.limpieza-cada-ms:120000}", initialDelay = 30000)
    public void limpiar() {
        for (Venta v : ventas.findByEstadoAndFechaBefore(EstadoVenta.PENDIENTE, Instant.now().minus(maximo))) {
            try {
                remotas.devolver(v.getId());
                if (v.getTipoPago() == TipoPago.FIADO) {
                    remotas.anularCargo(v.getId());
                }
                pasos.rechazar(v.getId(), "TIEMPO_AGOTADO", "Tiempo agotado");
                log.info("Venta {} PENDIENTE cerrada como RECHAZADA (tiempo agotado)", v.getId());
            } catch (ApiException e) {
                log.warn("No se pudo cerrar la venta {}: {}. Se reintentará.", v.getId(), e.getMessage());
            }
        }
    }
}
