package ro.tuc.ds2020.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.ds2020.dtos.PersonDTO;
import ro.tuc.ds2020.entities.Device;
import ro.tuc.ds2020.entities.Person;
import ro.tuc.ds2020.messaging.DeviceEventPublisher;
import ro.tuc.ds2020.repositories.DeviceRepository;
import ro.tuc.ds2020.repositories.PersonRepository;

import java.util.List;
import java.util.UUID;

@Service
public class PersonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonService.class);

    private final PersonRepository personRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceEventPublisher deviceEventPublisher;

    @Autowired
    public PersonService(PersonRepository personRepository,
                         DeviceRepository deviceRepository,
                         DeviceEventPublisher deviceEventPublisher) {
        this.personRepository = personRepository;
        this.deviceRepository = deviceRepository;
        this.deviceEventPublisher = deviceEventPublisher;
    }

    public UUID insert(PersonDTO dto) {
        Person person = personRepository.findById(dto.getId())
                .orElseGet(() -> new Person(dto.getId(), dto.getName(), dto.getUsername()));
        person.setName(dto.getName());
        person.setUsername(dto.getUsername());
        person = personRepository.save(person);
        LOGGER.debug("Person with id {} was inserted/upserted in device db", person.getId());
        return person.getId();
    }

    public PersonDTO update(UUID id, PersonDTO dto) {
        Person person = personRepository.findById(id)
                .orElseGet(() -> new Person(id, dto.getName(), dto.getUsername()));
        person.setName(dto.getName());
        person.setUsername(dto.getUsername());
        person = personRepository.save(person);
        LOGGER.debug("Person with id {} was updated in device db", person.getId());
        return new PersonDTO(person.getId(), person.getName(), person.getUsername());
    }

    @Transactional
    public void delete(UUID id) {
        // Unassign the person's devices first (works even if the person row is missing)
        List<Device> devices = deviceRepository.findAllByPersonId(id);
        for (Device device : devices) {
            device.setPerson(null);
        }
        deviceRepository.saveAll(devices);

        // Tell monitoring-service that these devices no longer have an owner
        devices.forEach(deviceEventPublisher::publishUpdated);

        personRepository.findById(id).ifPresent(personRepository::delete);
        LOGGER.debug("Person with id {} was cleaned up in device db ({} devices unassigned)", id, devices.size());
    }
}
