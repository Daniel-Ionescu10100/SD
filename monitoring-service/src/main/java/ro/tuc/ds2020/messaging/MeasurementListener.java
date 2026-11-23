package ro.tuc.ds2020.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ro.tuc.ds2020.dtos.MeasurementDTO;
import ro.tuc.ds2020.services.ConsumptionService;

@Component
public class MeasurementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementListener.class);

    private final ConsumptionService consumptionService;

    public MeasurementListener(ConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }

    @RabbitListener(queues = "${monitoring.queue.data}")
    public void onMeasurement(MeasurementDTO measurement) {
        LOGGER.info("Received measurement: device={} ts={} value={} kWh",
                measurement.deviceId(), measurement.timestamp(), measurement.measurementValue());
        consumptionService.processMeasurement(measurement);
    }
}
