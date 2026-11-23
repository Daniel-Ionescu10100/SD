package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds2020.dtos.HourlyConsumptionDTO;
import ro.tuc.ds2020.dtos.MeasurementDTO;
import ro.tuc.ds2020.entities.HourlyConsumption;
import ro.tuc.ds2020.repositories.HourlyConsumptionRepository;
import ro.tuc.ds2020.repositories.MonitoredDeviceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConsumptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);

    private final HourlyConsumptionRepository hourlyRepository;
    private final MonitoredDeviceRepository deviceRepository;

    public ConsumptionService(HourlyConsumptionRepository hourlyRepository,
                              MonitoredDeviceRepository deviceRepository) {
        this.hourlyRepository = hourlyRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void processMeasurement(MeasurementDTO m) {
        UUID deviceId = m.deviceId();

        if (!deviceRepository.existsById(deviceId)) {
            LOGGER.warn("Measurement for unknown device {} ignored", deviceId);
            return;
        }

        LocalDateTime timestamp = LocalDateTime.parse(m.timestamp());
        LocalDateTime hourStart = timestamp.truncatedTo(ChronoUnit.HOURS);

        HourlyConsumption hour = hourlyRepository
                .findByDeviceIdAndHourStart(deviceId, hourStart)
                .orElseGet(() -> new HourlyConsumption(deviceId, hourStart, 0));

        hour.addEnergy(m.measurementValue());
        hourlyRepository.save(hour);

        LOGGER.debug("Device {} hour {} -> total {} kWh", deviceId, hourStart, hour.getTotalKwh());
    }

    @Transactional(readOnly = true)
    public List<HourlyConsumptionDTO> getDailyConsumption(UUID deviceId, LocalDate date) {
        double[] perHour = new double[24];

        hourlyRepository.findByDeviceIdAndHourStartBetweenOrderByHourStartAsc(
                        deviceId, date.atStartOfDay(), date.atTime(23, 0))
                .forEach(h -> perHour[h.getHourStart().getHour()] = h.getTotalKwh());

        List<HourlyConsumptionDTO> result = new ArrayList<>(24);
        for (int hour = 0; hour < 24; hour++) {
            double rounded = Math.round(perHour[hour] * 1000.0) / 1000.0;
            result.add(new HourlyConsumptionDTO(hour, rounded));
        }
        return result;
    }
}
