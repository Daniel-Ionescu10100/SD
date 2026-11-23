package ro.tuc.ds2020.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${user.sync.exchange}")
    private String syncExchangeName;

    // user-service is only a producer: it knows the exchange, not the queues.
    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(syncExchangeName, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
