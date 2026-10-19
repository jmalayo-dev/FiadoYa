package pe.edu.cibertec.fiadoya.common.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.Evento;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.PagoRegistrado;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.contracts.VentaRegistrada;

/**
 * Los servicios publican eventos con ApplicationEventPublisher dentro de su transacción.
 * Este listener los envía a RabbitMQ solo DESPUÉS del commit (Anexo §8.1): si la transacción falla, no sale nada.
 */
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final RabbitTemplate rabbit;

    public PublicadorEventos(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicar(Evento evento) {
        String routingKey = routingKey(evento);
        try {
            rabbit.convertAndSend(Mensajeria.EXCHANGE, routingKey, evento);
            log.info("Evento {} publicado con {} (eventId={})", evento.tipo(), routingKey, evento.eventId());
        } catch (RuntimeException e) {
            // El negocio ya hizo commit: se registra el fallo para reprocesar, no se revierte la operación.
            log.error("No se pudo publicar el evento {} ({})", evento.tipo(), evento.eventId(), e);
        }
    }

    public static String routingKey(Evento evento) {
        return switch (evento) {
            case VentaRegistrada v -> Mensajeria.RK_VENTA_REGISTRADA;
            case StockBajo s -> Mensajeria.RK_STOCK_BAJO;
            case PagoRegistrado p -> Mensajeria.RK_PAGO_REGISTRADO;
            case ClienteLimiteCercano c -> Mensajeria.RK_CLIENTE_LIMITE_CERCANO;
        };
    }
}
