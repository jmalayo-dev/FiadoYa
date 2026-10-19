package pe.edu.cibertec.fiadoya.notificaciones.messaging;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.cibertec.fiadoya.common.messaging.Colas;
import pe.edu.cibertec.fiadoya.contracts.ClienteLimiteCercano;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;
import pe.edu.cibertec.fiadoya.contracts.StockBajo;
import pe.edu.cibertec.fiadoya.notificaciones.dto.Alerta;
import pe.edu.cibertec.fiadoya.notificaciones.service.Alertador;

/** Flujo B (coreografía): reacciona a stock.bajo y cliente.limite-cercano sin que nadie lo llame. */
@Configuration
public class ConsumidorAlertas {

    private final Alertador alertador;

    public ConsumidorAlertas(Alertador alertador) {
        this.alertador = alertador;
    }

    @Bean
    Declarables colaStockBajo() {
        return Colas.conDlq(Mensajeria.Q_NOTIFICACIONES_STOCK_BAJO, Mensajeria.RK_STOCK_BAJO);
    }

    @Bean
    Declarables colaLimiteCliente() {
        return Colas.conDlq(Mensajeria.Q_NOTIFICACIONES_LIMITE_CLIENTE, Mensajeria.RK_CLIENTE_LIMITE_CERCANO);
    }

    @RabbitListener(queues = Mensajeria.Q_NOTIFICACIONES_STOCK_BAJO)
    public void stockBajo(StockBajo evento) {
        alertador.procesar(Alerta.de(evento));
    }

    @RabbitListener(queues = Mensajeria.Q_NOTIFICACIONES_LIMITE_CLIENTE)
    public void limiteCliente(ClienteLimiteCercano evento) {
        alertador.procesar(Alerta.de(evento));
    }
}
