package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds2020.dtos.DeviceSyncEvent;
import ro.tuc.ds2020.entities.MonitoredDevice;
import ro.tuc.ds2020.repositories.HourlyConsumptionRepository;
import ro.tuc.ds2020.repositories.MonitoredDeviceRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSyncService.class);

    private final MonitoredDeviceRepository deviceRepository;
    private final HourlyConsumptionRepository hourlyRepository;

    public DeviceSyncService(MonitoredDeviceRepository deviceRepository,
                             HourlyConsumptionRepository hourlyRepository) {
        this.deviceRepository = deviceRepository;
        this.hourlyRepository = hourlyRepository;
    }

    @Transactional
    public void handle(DeviceSyncEvent event) {
        switch (event.eventType()) {
            case "DEVICE_CREATED", "DEVICE_UPDATED" -> upsert(event);
            case "DEVICE_DELETED" -> delete(event.deviceId());
            default -> LOGGER.warn("Unknown device event type: {}", event.eventType());
        }
    }

    private void upsert(DeviceSyncEvent event) {
        MonitoredDevice device = deviceRepository.findById(event.deviceId())
                .orElseGet(() -> new MonitoredDevice(event.deviceId()));
        device.setUserId(event.userId());
        device.setMaxConsumption(event.maxConsumption());
        deviceRepository.save(device);
        LOGGER.info("Device {} saved (owner={})", device.getId(), device.getUserId());
    }

    private void delete(UUID deviceId) {
        Optional<MonitoredDevice> device = deviceRepository.findById(deviceId);
        if (device.isEmpty()) {
            LOGGER.info("Device {} already absent, nothing to delete", deviceId);
            return;
        }
        hourlyRepository.deleteByDeviceId(deviceId);
        deviceRepository.delete(device.get());
        LOGGER.info("Device {} and its consumption history deleted", deviceId);
    }
}
