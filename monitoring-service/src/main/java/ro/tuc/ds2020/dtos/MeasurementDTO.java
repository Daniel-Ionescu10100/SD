package ro.tuc.ds2020.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record MeasurementDTO(
        @JsonProperty("timestamp")         String timestamp,
        @JsonProperty("device_id")         UUID deviceId,
        @JsonProperty("measurement_value") double measurementValue
) {
}
