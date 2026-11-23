package ro.tuc.ds2020.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds2020.entities.HourlyConsumption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, Long> {

    Optional<HourlyConsumption> findByDeviceIdAndHourStart(UUID deviceId, LocalDateTime hourStart);

    List<HourlyConsumption> findByDeviceIdAndHourStartBetweenOrderByHourStartAsc(
            UUID deviceId, LocalDateTime from, LocalDateTime to);

    void deleteByDeviceId(UUID deviceId);
}
