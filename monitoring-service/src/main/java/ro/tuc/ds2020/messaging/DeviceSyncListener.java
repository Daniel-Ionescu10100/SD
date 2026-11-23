package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.DeviceSyncEvent;
import ro.tuc.ds2020.services.DeviceSyncService;

@Component
public class DeviceSyncListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceSyncListener.class);

    private final DeviceSyncService deviceSyncService;

    public DeviceSyncListener(DeviceSyncService deviceSyncService) {
        this.deviceSyncService = deviceSyncService;
    }

    @RabbitListener(queues = "${monitoring.queue.sync}")
    public void onDeviceEvent(DeviceSyncEvent event) {
        LOGGER.info("Received sync event: {} device={}", event.eventType(), event.deviceId());
        deviceSyncService.handle(event);
    }
}
