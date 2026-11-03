package ro.tuc.ds2020.dtos.builders;

import ro.tuc.ds2020.dtos.DeviceDTO;
import ro.tuc.ds2020.dtos.DeviceDetailsDTO;
import ro.tuc.ds2020.entities.Device;
import ro.tuc.ds2020.entities.Person;

public class DeviceBuilder {

    private DeviceBuilder() {
    }

    public static DeviceDTO toDeviceDTO(Device device) {
        return new DeviceDTO(
                device.getId(),
                device.getName(),
                device.getDescription(),
                device.getMaxConsumption(),
                device.getUserId()
        );
    }

    public static Device toEntity(DeviceDetailsDTO dto, Person person) {
        return new Device(
                dto.getName(),
                dto.getDescription(),
                dto.getMaxConsumption(),
                person
        );
    }
}
