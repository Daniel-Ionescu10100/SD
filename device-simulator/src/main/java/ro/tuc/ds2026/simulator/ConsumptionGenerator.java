package ro.tuc.ds2026.simulator;
import java.time.LocalDateTime;
import java.util.Random;

public class ConsumptionGenerator {
    private final Random random = new Random();
    private final double baseLoad;

    public ConsumptionGenerator() {
        this.baseLoad = 0.05 + random.nextDouble() * 0.10;   // în [0.05, 0.15)
    }
    public double next(LocalDateTime ts) {
        double noise = random.nextGaussian() * 0.1;
        double value = baseLoad * hourFactor(ts.getHour()) * (1 + noise);
        value = Math.max(0, value);
        return Math.round(value * 1000.0) / 1000.0;
    }
    private double hourFactor(int hour) {
        return switch (hour) {
            case 0, 1, 2, 3, 4, 5, 6          -> 0.4;
            case 7, 8, 9                      -> 1.2;
            case 18, 19, 20, 21, 22           -> 1.6;
            case 23                           -> 0.7;
            default                           -> 0.8;   // 10–17
        };
    }
}
