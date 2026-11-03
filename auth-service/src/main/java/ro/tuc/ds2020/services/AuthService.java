package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.DuplicateResourceException;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.InvalidCredentialsException;
import ro.tuc.ds2020.dtos.LoginRequestDTO;
import ro.tuc.ds2020.dtos.LoginResponseDTO;
import ro.tuc.ds2020.dtos.RegisterRequestDTO;
import ro.tuc.ds2020.entities.Credential;
import ro.tuc.ds2020.repositories.CredentialRepository;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final CredentialRepository credentialRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Autowired
    public AuthService(CredentialRepository credentialRepository, JwtService jwtService, EntityManager entityManager) {
        this.credentialRepository = credentialRepository;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @org.springframework.transaction.annotation.Transactional
    public UUID register(RegisterRequestDTO dto) {
        if (credentialRepository.existsByUsername(dto.getUsername())) {
            LOGGER.error("Username {} is already taken", dto.getUsername());
            throw new DuplicateResourceException("Credential with username: " + dto.getUsername());
        }
        String hash = passwordEncoder.encode(dto.getPassword());
        Credential credential = new Credential(dto.getId(), dto.getUsername(), hash, dto.getRole());
        credential = credentialRepository.save(credential);
        LOGGER.debug("Credential created with id {}", credential.getId());
        return credential.getId();
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Optional<Credential> credentialOptional = credentialRepository.findByUsername(dto.getUsername());
        if (credentialOptional.isEmpty()) {
            LOGGER.warn("Login attempt for unknown username {}", dto.getUsername());
            throw new InvalidCredentialsException();
        }
        Credential credential = credentialOptional.get();
        if (!passwordEncoder.matches(dto.getPassword(), credential.getPasswordHash())) {
            LOGGER.warn("Bad password for username {}", dto.getUsername());
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generateToken(
                credential.getUsername(),
                credential.getRole(),
                credential.getId()
        );
        LOGGER.debug("Token issued for user {}", credential.getUsername());
        return new LoginResponseDTO(token, credential.getUsername(), credential.getRole(), credential.getId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteByUsername(String username) {
        credentialRepository.findByUsername(username).ifPresent(credentialRepository::delete);
        LOGGER.debug("Deleted credential for username {}", username);
    }

    @org.springframework.transaction.annotation.Transactional
    public void update(String oldUsername, String newUsername, String newPassword, String newRole) {
        Optional<Credential> opt = credentialRepository.findByUsername(oldUsername);
        Credential credential;
        if (opt.isPresent()) {
            credential = opt.get();
        } else {
            credential = new Credential();
        }
        credential.setUsername(newUsername != null && !newUsername.isBlank() ? newUsername : oldUsername);
        if (newPassword != null && !newPassword.isBlank()) {
            credential.setPasswordHash(passwordEncoder.encode(newPassword));
        } else if (credential.getPasswordHash() == null) {
            credential.setPasswordHash(passwordEncoder.encode("default123"));
        }
        if (newRole != null && !newRole.isBlank()) {
            credential.setRole(newRole);
        }
        credentialRepository.save(credential);
        LOGGER.info("Updated credential for user {}", credential.getUsername());
    }
}
