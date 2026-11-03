package ro.tuc.ds2020.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public class DeviceDetailsDTO {

    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @Positive(message = "Max consumption must be a positive number")
    private double maxConsumption;

    private UUID userId;

    public DeviceDetailsDTO() {
    }

    public DeviceDetailsDTO(String name, String description, double maxConsumption, UUID userId) {
        this.name = name;
        this.description = description;
        this.maxConsumption = maxConsumption;
        this.userId = userId;
    }

    public DeviceDetailsDTO(UUID id, String name, String description, double maxConsumption, UUID userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxConsumption = maxConsumption;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMaxConsumption() {
        return maxConsumption;
    }

    public void setMaxConsumption(double maxConsumption) {
        this.maxConsumption = maxConsumption;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
