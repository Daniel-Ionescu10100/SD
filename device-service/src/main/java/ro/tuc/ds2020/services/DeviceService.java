package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.tuc.ds2020.controllers.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.ds2020.dtos.DeviceDTO;
import ro.tuc.ds2020.dtos.DeviceDetailsDTO;
import ro.tuc.ds2020.dtos.builders.DeviceBuilder;
import ro.tuc.ds2020.entities.Device;
import ro.tuc.ds2020.entities.Person;
import ro.tuc.ds2020.repositories.DeviceRepository;
import ro.tuc.ds2020.repositories.PersonRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final PersonRepository personRepository;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, PersonRepository personRepository) {
        this.deviceRepository = deviceRepository;
        this.personRepository = personRepository;
    }

    public List<DeviceDTO> findDevices() {
        return deviceRepository.findAll()
                .stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDTO findDeviceById(UUID id) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (deviceOptional.isEmpty()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        return DeviceBuilder.toDeviceDTO(deviceOptional.get());
    }

    public List<DeviceDTO> findDevicesByUserId(UUID userId) {
        return deviceRepository.findAllByPersonId(userId)
                .stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public UUID insert(DeviceDetailsDTO dto) {

        Person person = resolvePerson(dto.getUserId());
        Device device = DeviceBuilder.toEntity(dto, person);
        device = deviceRepository.save(device);
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        return device.getId();
    }

    public DeviceDTO update(UUID id, DeviceDetailsDTO dto) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (deviceOptional.isEmpty()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        Device device = deviceOptional.get();
        device.setName(dto.getName());
        device.setDescription(dto.getDescription());
        device.setMaxConsumption(dto.getMaxConsumption());
        device.setPerson(resolvePerson(dto.getUserId()));
        device = deviceRepository.save(device);
        LOGGER.debug("Device with id {} was updated in db", device.getId());
        return DeviceBuilder.toDeviceDTO(device);
    }

    public void delete(UUID id) {
        if (!deviceRepository.existsById(id)) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        deviceRepository.deleteById(id);
        LOGGER.debug("Device with id {} was deleted from db", id);
    }

    public DeviceDTO assignToUser(UUID deviceId, UUID userId) {
        Optional<Device> deviceOptional = deviceRepository.findById(deviceId);
        if (deviceOptional.isEmpty()) {
            LOGGER.error("Device with id {} was not found in db", deviceId);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + deviceId);
        }
        Person person = personRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(Person.class.getSimpleName() + " with id: " + userId));

        Device device = deviceOptional.get();
        device.setPerson(person);
        device = deviceRepository.save(device);
        LOGGER.debug("Device {} assigned to person {}", deviceId, userId);
        return DeviceBuilder.toDeviceDTO(device);
    }

    @org.springframework.transaction.annotation.Transactional
    public void unassignDevicesForUser(UUID userId) {
        List<Device> devices = deviceRepository.findAllByPersonId(userId);
        for (Device device : devices) {
            device.setPerson(null);
        }
        deviceRepository.saveAll(devices);
        LOGGER.info("Unassigned {} devices from person {}", devices.size(), userId);
    }

    private Person resolvePerson(UUID userId) {
        if (userId == null) {
            return null;
        }
        return personRepository.findById(userId)
                .orElseGet(() -> {
                    LOGGER.warn("Person {} not found in device db, auto-provisioning shadow record", userId);
                    Person p = new Person(userId, "User " + userId.toString().substring(0, 8), "user_" + userId.toString().substring(0, 8));
                    return personRepository.save(p);
                });
    }
}
