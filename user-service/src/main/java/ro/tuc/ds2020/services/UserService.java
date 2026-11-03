package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.ds2020.dtos.UserDTO;
import ro.tuc.ds2020.dtos.UserDetailsDTO;
import ro.tuc.ds2020.dtos.builders.UserBuilder;
import ro.tuc.ds2020.entities.User;
import ro.tuc.ds2020.repositories.UserRepository;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RestClient restClient;

    @Value("${device.service.url:http://device-service:8080}")
    private String deviceServiceUrl;

    @Value("${auth.service.url:http://auth-service:8080}")
    private String authServiceUrl;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restClient = RestClient.create();
    }

    public List<UserDTO> findUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserBuilder::toUserDTO)
                .collect(Collectors.toList());
    }

    public UserDTO findUserById(UUID id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        return UserBuilder.toUserDTO(userOptional.get());
    }

    @Transactional
    public UUID insert(UserDetailsDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            LOGGER.error("Password is required to create a new user");
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            LOGGER.error("User with username {} already exists", dto.getUsername());
            throw new DuplicateResourceException(User.class.getSimpleName() + " with username: " + dto.getUsername());
        }
        User user = UserBuilder.toEntity(dto);
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was inserted in db", user.getId());

        syncRegisterWithAuth(user.getId(), dto);
        syncCreatePersonInDevice(user.getId(), user.getName(), user.getUsername());

        return user.getId();
    }

    @Transactional
    public UserDTO update(UUID id, UserDetailsDTO dto) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        User user = userOptional.get();
        String oldUsername = user.getUsername();

        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(dto.getPassword());
        }
        user.setRole(dto.getRole());
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was updated in db", user.getId());

        syncUpdatePersonInDevice(user.getId(), user.getName(), user.getUsername());
        syncUpdateWithAuth(oldUsername, user.getUsername(), dto.getPassword(), user.getRole());

        return UserBuilder.toUserDTO(user);
    }

    @Transactional
    public void delete(UUID id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        User user = userOptional.get();

        syncDeletePersonInDevice(id);
        syncDeleteWithAuth(user.getUsername());

        userRepository.deleteById(id);
        LOGGER.debug("User with id {} was deleted from db", id);
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void syncAllUsersOnStartup() {
        try {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                try {
                    syncCreatePersonInDevice(u.getId(), u.getName(), u.getUsername());
                } catch (Exception ex) {
                    LOGGER.debug("Startup sync notice for {}: {}", u.getUsername(), ex.getMessage());
                }
            }
            LOGGER.info("Startup sync checked {} users to device-service", allUsers.size());
        } catch (Exception e) {
            LOGGER.warn("Startup user sync notice: {}", e.getMessage());
        }
    }

    private void syncRegisterWithAuth(UUID userId, UserDetailsDTO dto) {
        Map<String, Object> body = Map.of(
                "id",       userId.toString(),
                "username", dto.getUsername(),
                "password", dto.getPassword(),
                "role",     dto.getRole()
        );
        restClient.post()
                .uri(authServiceUrl + "/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Synced user {} ({}) to auth-service", dto.getUsername(), userId);
    }

    private void syncUpdateWithAuth(String oldUsername, String newUsername, String password, String role) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", newUsername);
            if (password != null && !password.trim().isEmpty()) {
                body.put("password", password);
            }
            body.put("role", role);

            restClient.put()
                    .uri(authServiceUrl + "/auth/user/" + oldUsername)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Synced user update for {} to auth-service", oldUsername);
        } catch (Exception e) {
            LOGGER.warn("Notice: could not update credential in auth-service for {}: {}", oldUsername, e.getMessage());
        }
    }

    private void syncDeleteWithAuth(String username) {
        try {
            restClient.delete()
                    .uri(authServiceUrl + "/auth/user/" + username)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Synced deletion of {} to auth-service", username);
        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.warn("User {} not found in auth-service during delete, continuing", username);
        } catch (Exception e) {
            LOGGER.warn("Notice during auth delete of {}: {}", username, e.getMessage());
        }
    }

    private void syncCreatePersonInDevice(UUID userId, String name, String username) {
        Map<String, Object> body = Map.of(
                "id",       userId.toString(),
                "name",     name,
                "username", username
        );
        restClient.post()
                .uri(deviceServiceUrl + "/person")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Synced new person {} to device-service", userId);
    }

    private void syncUpdatePersonInDevice(UUID userId, String name, String username) {
        Map<String, Object> body = Map.of(
                "id",       userId.toString(),
                "name",     name,
                "username", username
        );
        restClient.put()
                .uri(deviceServiceUrl + "/person/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        LOGGER.info("Synced person update {} to device-service", userId);
    }

    private void syncDeletePersonInDevice(UUID userId) {
        try {
            restClient.delete()
                    .uri(deviceServiceUrl + "/person/" + userId)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Synced person deletion {} to device-service", userId);
        } catch (HttpClientErrorException.NotFound e) {
            LOGGER.warn("Person {} not found in device-service during delete, continuing", userId);
        } catch (Exception e) {
            LOGGER.warn("Notice during device person delete of {}: {}", userId, e.getMessage());
        }
    }
}
