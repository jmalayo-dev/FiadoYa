package pe.edu.cibertec.fiadoya.common.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;

/**
 * Declara una cola de consumo enlazada al exchange principal y su DLQ en fiadoya.dlx.
 * Tras 3 intentos fallidos el mensaje se rechaza y RabbitMQ lo mueve a &lt;cola&gt;.dlq (Plan §5.3).
 */
public final class Colas {

    private Colas() {
    }

    public static Declarables conDlq(String cola, String routingKey) {
        String dlq = cola + Mensajeria.DLQ_SUFIJO;
        TopicExchange exchange = new TopicExchange(Mensajeria.EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(Mensajeria.DLX, true, false);
        Queue principal = QueueBuilder.durable(cola)
                .deadLetterExchange(Mensajeria.DLX)
                .deadLetterRoutingKey(dlq)
                .build();
        Queue muerta = QueueBuilder.durable(dlq).build();
        return new Declarables(principal, muerta,
                BindingBuilder.bind(principal).to(exchange).with(routingKey),
                BindingBuilder.bind(muerta).to(dlx).with(dlq));
    }
}
