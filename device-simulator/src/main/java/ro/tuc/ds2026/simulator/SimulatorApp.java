package ro.tuc.ds2026.simulator;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

public class SimulatorApp {

    private static final int MINUTES_PER_READING = 10;
    private static final int READINGS_PER_DAY = 24 * 60 / MINUTES_PER_READING;   // 144

    public static void main(String[] args) throws Exception {

        // 1. Citim configurația (cale din argument sau implicit config.properties)
        String configPath = args.length > 0 ? args[0] : "config.properties";
        Properties cfg = loadConfig(configPath);

        String deviceId = cfg.getProperty("device.id");
        UUID.fromString(deviceId);   // validare: aruncă excepție dacă id-ul nu e UUID valid

        int days = Integer.parseInt(cfg.getProperty("simulation.days", "1"));
        long delayMs = Long.parseLong(cfg.getProperty("simulation.delay.ms", "0"));

        // 2. Momentul de start: acum `days` zile, la ora 00:00
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        int totalReadings = days * READINGS_PER_DAY;

        System.out.printf("Simulating %d readings for device %s, starting %s%n",
                totalReadings, deviceId, start);

        ConsumptionGenerator generator = new ConsumptionGenerator();

        // 3. Deschidem conexiunea o singură dată; try-with-resources o închide automat la final
        try (MeasurementPublisher publisher = new MeasurementPublisher(
                cfg.getProperty("rabbitmq.host", "localhost"),
                Integer.parseInt(cfg.getProperty("rabbitmq.port", "5672")),
                cfg.getProperty("rabbitmq.username"),
                cfg.getProperty("rabbitmq.password"),
                cfg.getProperty("rabbitmq.queue", "device.data.queue"))) {

            // 4. Bucla: timp simulat, nu timp real
            for (int i = 0; i < totalReadings; i++) {
                LocalDateTime ts = start.plusMinutes((long) i * MINUTES_PER_READING);
                double value = generator.next(ts);

                Measurement measurement = new Measurement(ts.toString(), deviceId, value);
                publisher.publish(measurement);

                System.out.printf("[%d/%d] sent %s%n", i + 1, totalReadings, measurement);

                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            }
        }

        System.out.println("Simulation finished.");
    }

    private static Properties loadConfig(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(path)) {
            props.load(in);
        }
        return props;
    }
}
