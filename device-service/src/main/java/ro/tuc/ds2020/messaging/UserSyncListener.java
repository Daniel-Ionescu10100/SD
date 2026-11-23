package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.dtos.UserSyncEvent;
import ro.tuc.ds2020.services.PersonService;

/**
 * Keeps the local "persons" table in sync with user-service.
 */
@Component
public class UserSyncListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncListener.class);

    private final PersonService personService;

    public UserSyncListener(PersonService personService) {
        this.personService = personService;
    }

    @RabbitListener(queues = "${device.sync.queue}")
    public void onUserEvent(UserSyncEvent event) {
        LOGGER.info("Received sync event: {} user={}", event.eventType(), event.userId());

        switch (event.eventType()) {
            case "USER_CREATED", "USER_UPDATED" ->
                    personService.update(event.userId(),
                            new PersonDTO(event.userId(), event.name(), event.username()));
            case "USER_DELETED" -> personService.delete(event.userId());
            default -> LOGGER.warn("Unknown user event type: {}", event.eventType());
        }
    }
}
