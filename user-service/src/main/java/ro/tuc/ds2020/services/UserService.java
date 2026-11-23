package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.ds2020.dtos.UserDTO;
import ro.tuc.ds2020.dtos.UserDetailsDTO;
import ro.tuc.ds2020.dtos.builders.UserBuilder;
import ro.tuc.ds2020.entities.User;
import ro.tuc.ds2020.messaging.UserEventPublisher;
import ro.tuc.ds2020.repositories.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    @Autowired
    public UserService(UserRepository userRepository, UserEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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

        // auth-service creates the credential, device-service creates the person
        eventPublisher.publishCreated(user, dto.getPassword());

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

        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(dto.getPassword());
        }
        user.setRole(dto.getRole());
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was updated in db", user.getId());

        // password is null when it was not changed -> auth-service keeps the old hash
        eventPublisher.publishUpdated(user, dto.getPassword());

        return UserBuilder.toUserDTO(user);
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException(User.class.getSimpleName() + " with id: " + id);
        }
        userRepository.deleteById(id);
        LOGGER.debug("User with id {} was deleted from db", id);

        // auth-service deletes the credential, device-service deletes the person
        // and unassigns its devices
        eventPublisher.publishDeleted(id);
    }
}
