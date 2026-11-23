package ro.tuc.ds2020.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "monitored_device")
public class MonitoredDevice {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "max_consumption", nullable = false)
    private double maxConsumption;

    public MonitoredDevice() {
    }

    public MonitoredDevice(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public double getMaxConsumption() { return maxConsumption; }
    public void setMaxConsumption(double maxConsumption) { this.maxConsumption = maxConsumption; }
}
