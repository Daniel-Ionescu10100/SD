package ro.tuc.ds2020.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.ds2020.entities.MonitoredDevice;

import java.util.UUID;

public interface MonitoredDeviceRepository extends JpaRepository<MonitoredDevice, UUID> {
}
