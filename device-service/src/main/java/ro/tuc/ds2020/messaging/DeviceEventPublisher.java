package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.DeviceSyncEvent;
import ro.tuc.ds2020.entities.Device;

import java.util.UUID;

/**
 * Publishes device changes on sync.exchange so other services (monitoring)
 * can keep their local copy of the devices up to date.
 */
@Component
public class DeviceEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public DeviceEventPublisher(RabbitTemplate rabbitTemplate,
                                @Value("${device.sync.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publishCreated(Device device) {
        send("device.created", toEvent("DEVICE_CREATED", device));
    }

    public void publishUpdated(Device device) {
        send("device.updated", toEvent("DEVICE_UPDATED", device));
    }

    public void publishDeleted(UUID deviceId) {
        send("device.deleted", new DeviceSyncEvent("DEVICE_DELETED", deviceId, null, 0));
    }

    private DeviceSyncEvent toEvent(String type, Device device) {
        return new DeviceSyncEvent(type, device.getId(), device.getUserId(), device.getMaxConsumption());
    }

    private void send(String routingKey, DeviceSyncEvent event) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        LOGGER.info("Published {} for device {}", routingKey, event.deviceId());
    }
}
