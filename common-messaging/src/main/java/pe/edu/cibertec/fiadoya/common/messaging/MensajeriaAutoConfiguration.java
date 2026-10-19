package pe.edu.cibertec.fiadoya.common.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import pe.edu.cibertec.fiadoya.contracts.Mensajeria;

/** Exchange principal y de errores (Plan §5.3) y conversor JSON con los records de fiadoya-contracts. */
@AutoConfiguration
public class MensajeriaAutoConfiguration {

    @Bean
    TopicExchange fiadoyaExchange() {
        return new TopicExchange(Mensajeria.EXCHANGE, true, false);
    }

    @Bean
    TopicExchange fiadoyaDlx() {
        return new TopicExchange(Mensajeria.DLX, true, false);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("pe.edu.cibertec.fiadoya.contracts");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    PublicadorEventos publicadorEventos(RabbitTemplate rabbitTemplate) {
        return new PublicadorEventos(rabbitTemplate);
    }
}
