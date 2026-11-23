package ro.tuc.ds2020.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.tuc.ds2020.dtos.HourlyConsumptionDTO;
import ro.tuc.ds2020.entities.MonitoredDevice;
import ro.tuc.ds2020.repositories.MonitoredDeviceRepository;
import ro.tuc.ds2020.services.ConsumptionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/monitoring")
public class MonitoringController {

    private final ConsumptionService consumptionService;
    private final MonitoredDeviceRepository deviceRepository;

    public MonitoringController(ConsumptionService consumptionService,
                                MonitoredDeviceRepository deviceRepository) {
        this.consumptionService = consumptionService;
        this.deviceRepository = deviceRepository;
    }

    // GET /monitoring/consumption?deviceId=...&date=2026-11-18
    @GetMapping("/consumption")
    public ResponseEntity<List<HourlyConsumptionDTO>> getConsumption(
            @RequestParam UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Role") String role) {

        Optional<MonitoredDevice> device = deviceRepository.findById(deviceId);
        if (device.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if ("CLIENT".equals(role)) {
            UUID owner = device.get().getUserId();
            if (owner == null || !owner.toString().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(consumptionService.getDailyConsumption(deviceId, date));
    }
}
