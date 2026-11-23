package ro.tuc.ds2020.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${device.sync.exchange}")
    private String syncExchangeName;

    @Value("${device.sync.queue}")
    private String syncQueueName;

    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(syncExchangeName, true, false);
    }

    // Consumer side: device-service's own queue, receives every user.* event
    @Bean
    public Queue syncQueue() {
        return QueueBuilder.durable(syncQueueName).build();
    }

    @Bean
    public Binding userSyncBinding(Queue syncQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange).with("user.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
