package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.UserSyncEvent;
import ro.tuc.ds2020.services.CredentialSyncService;

@Component
public class UserSyncListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncListener.class);

    private final CredentialSyncService credentialSyncService;

    public UserSyncListener(CredentialSyncService credentialSyncService) {
        this.credentialSyncService = credentialSyncService;
    }

    @RabbitListener(queues = "${auth.sync.queue}")
    public void onUserEvent(UserSyncEvent event) {
        LOGGER.info("Received sync event: {} user={}", event.eventType(), event.userId());
        credentialSyncService.handle(event);
    }
}
