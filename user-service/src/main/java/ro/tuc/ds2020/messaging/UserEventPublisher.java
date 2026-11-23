package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.UserSyncEvent;
import ro.tuc.ds2020.entities.User;

import java.util.UUID;

/**
 * Publishes user changes on sync.exchange. device-service and auth-service
 * each have their own queue bound with "user.*", so every event reaches both.
 */
@Component
public class UserEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public UserEventPublisher(RabbitTemplate rabbitTemplate,
                              @Value("${user.sync.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publishCreated(User user, String rawPassword) {
        send("user.created", toEvent("USER_CREATED", user, rawPassword));
    }

    public void publishUpdated(User user, String rawPasswordOrNull) {
        send("user.updated", toEvent("USER_UPDATED", user, rawPasswordOrNull));
    }

    public void publishDeleted(UUID userId) {
        send("user.deleted", new UserSyncEvent("USER_DELETED", userId, null, null, null, null));
    }

    private UserSyncEvent toEvent(String type, User user, String password) {
        String pwd = (password == null || password.isBlank()) ? null : password;
        return new UserSyncEvent(type, user.getId(), user.getName(), user.getUsername(), pwd, user.getRole());
    }

    private void send(String routingKey, UserSyncEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        LOGGER.info("Published {} for user {}", routingKey, event.userId());
    }
}
