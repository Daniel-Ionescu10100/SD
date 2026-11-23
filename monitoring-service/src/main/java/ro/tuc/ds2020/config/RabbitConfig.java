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

    @Value("${monitoring.queue.data}")
    private String dataQueueName;

    @Value("${monitoring.exchange.sync}")
    private String syncExchangeName;

    @Value("${monitoring.queue.sync}")
    private String syncQueueName;

    // Point-to-point: simulator -> device.data.queue -> monitoring
    @Bean
    public Queue dataQueue() {
        return QueueBuilder.durable(dataQueueName).build();
    }

    // Publish-subscribe: device-service -> sync.exchange -> monitoring-service.sync.queue
    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(syncExchangeName, true, false);
    }

    @Bean
    public Queue syncQueue() {
        return QueueBuilder.durable(syncQueueName).build();
    }

    @Bean
    public Binding deviceSyncBinding(Queue syncQueue, TopicExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange).with("device.*");
    }

    // JSON <-> Java for every @RabbitListener and for RabbitTemplate
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
