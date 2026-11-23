package ro.tuc.ds2026.simulator;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Measurement (
        @JsonProperty("timestamp")         String timestamp,
        @JsonProperty("device_id")         String deviceId,
        @JsonProperty("measurement_value") double measurementValue
)
{

}
