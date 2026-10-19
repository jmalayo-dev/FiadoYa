package pe.edu.cibertec.fiadoya.reportes.messaging;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.cibertec.fiadoya.common.messaging.Colas;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.PagoRegistrado;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;
import pe.edu.cibertec.fiadoya.reportes.service.ProcesadorEventos;

/** Flujo B: reportes se alimenta de venta.registrada y pago.registrado. */
@Configuration
public class ConsumidorEventos {

    private final ProcesadorEventos procesador;

    public ConsumidorEventos(ProcesadorEventos procesador) {
        this.procesador = procesador;
    }

    @Bean
    Declarables colaVentas() {
        return Colas.conDlq(Mensajeria.Q_REPORTES_VENTAS, Mensajeria.RK_VENTA_REGISTRADA);
    }

    @Bean
    Declarables colaPagos() {
        return Colas.conDlq(Mensajeria.Q_REPORTES_PAGOS, Mensajeria.RK_PAGO_REGISTRADO);
    }

    @RabbitListener(queues = Mensajeria.Q_REPORTES_VENTAS)
    public void venta(VentaRegistrada evento) {
        procesador.venta(evento);
    }

    @RabbitListener(queues = Mensajeria.Q_REPORTES_PAGOS)
    public void pago(PagoRegistrado evento) {
        procesador.pago(evento);
    }
}
