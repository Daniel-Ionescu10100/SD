package ro.tuc.ds2020.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hourly_consumption",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "hour_start"}))
public class HourlyConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID deviceId;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(name = "total_kwh", nullable = false)
    private double totalKwh;

    public HourlyConsumption() {
    }

    public HourlyConsumption(UUID deviceId, LocalDateTime hourStart, double totalKwh) {
        this.deviceId = deviceId;
        this.hourStart = hourStart;
        this.totalKwh = totalKwh;
    }

    public void addEnergy(double value) {
        this.totalKwh += value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public LocalDateTime getHourStart() { return hourStart; }
    public void setHourStart(LocalDateTime hourStart) { this.hourStart = hourStart; }

    public double getTotalKwh() { return totalKwh; }
    public void setTotalKwh(double totalKwh) { this.totalKwh = totalKwh; }
}
