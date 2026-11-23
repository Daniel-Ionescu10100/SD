package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds2020.dtos.UserSyncEvent;
import ro.tuc.ds2020.entities.Credential;
import ro.tuc.ds2020.repositories.CredentialRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Applies user events from user-service to the credentials table.
 * The credential id is the same UUID as the user id.
 */
@Service
public class CredentialSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialSyncService.class);

    private final CredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CredentialSyncService(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Transactional
    public void handle(UserSyncEvent event) {
        switch (event.eventType()) {
            case "USER_CREATED", "USER_UPDATED" -> upsert(event);
            case "USER_DELETED" -> delete(event.userId());
            default -> LOGGER.warn("Unknown user event type: {}", event.eventType());
        }
    }

    private void upsert(UserSyncEvent event) {
        // Username must stay unique: refuse if another account already uses it
        Optional<Credential> sameUsername = credentialRepository.findByUsername(event.username());
        if (sameUsername.isPresent() && !sameUsername.get().getId().equals(event.userId())) {
            LOGGER.error("Username {} already belongs to another credential, event ignored", event.username());
            return;
        }

        Optional<Credential> existing = credentialRepository.findById(event.userId());
        Credential credential = existing.orElseGet(() -> new Credential(event.userId(), null, null, null));

        credential.setUsername(event.username());
        credential.setRole(event.role());

        if (event.password() != null && !event.password().isBlank()) {
            credential.setPasswordHash(passwordEncoder.encode(event.password()));
        } else if (credential.getPasswordHash() == null) {
            LOGGER.error("New credential {} has no password, event ignored", event.userId());
            return;
        }

        credentialRepository.save(credential);
        LOGGER.info("Credential {} {} (username={}, role={})",
                event.userId(), existing.isPresent() ? "updated" : "created",
                credential.getUsername(), credential.getRole());
    }

    private void delete(UUID userId) {
        credentialRepository.findById(userId).ifPresentOrElse(
                c -> {
                    credentialRepository.delete(c);
                    LOGGER.info("Credential {} deleted", userId);
                },
                () -> LOGGER.info("Credential {} already absent, nothing to delete", userId));
    }
}
