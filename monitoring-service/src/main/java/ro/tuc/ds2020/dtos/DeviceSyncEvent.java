package ro.tuc.ds2020.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DeviceSyncEvent(
        @JsonProperty("eventType")      String eventType,
        @JsonProperty("deviceId")       UUID deviceId,
        @JsonProperty("userId")         UUID userId,
        @JsonProperty("maxConsumption") double maxConsumption
) {
}
