package ro.tuc.ds2026.simulator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MeasurementPublisher implements AutoCloseable {

    private static final String DEFAULT_EXCHANGE = "";
    private static final int PERSISTENT = 2;

    private final Connection connection;
    private final Channel channel;
    private final String queueName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AMQP.BasicProperties messageProperties;

    public MeasurementPublisher(String host, int port, String username, String password, String queueName)
            throws IOException, TimeoutException {

        this.queueName = queueName;

        // 1. Configurăm "cum ajungem" la broker
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        // 2. Deschidem conexiunea TCP (o singură dată)
        this.connection = factory.newConnection("device-simulator");

        // 3. Deschidem un canal în interiorul conexiunii
        this.channel = connection.createChannel();

        // 4. Ne asigurăm că există coada (idempotent)
        channel.queueDeclare(queueName, true, false, false, null);

        // 5. "Ștampilele" sunt aceleași pentru toate mesajele -> le construim o dată
        this.messageProperties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(PERSISTENT)
                .build();

        System.out.printf("Connected to RabbitMQ %s:%d, queue '%s'%n", host, port, queueName);
    }

    public void publish(Measurement measurement) throws IOException {
        // Obiect Java -> JSON -> byte[]
        byte[] body = objectMapper.writeValueAsBytes(measurement);

        // 6. Trimitem în exchange-ul default, cu routing key = numele cozii
        channel.basicPublish(DEFAULT_EXCHANGE, queueName, messageProperties, body);
    }

    @Override
    public void close() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        System.out.println("RabbitMQ connection closed");
    }
}
