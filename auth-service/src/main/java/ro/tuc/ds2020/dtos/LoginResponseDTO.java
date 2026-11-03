package ro.tuc.ds2020.dtos;

import java.util.UUID;

public class LoginResponseDTO {

    private String token;
    private String username;
    private String role;
    private UUID userId;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, String username, String role, UUID userId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
