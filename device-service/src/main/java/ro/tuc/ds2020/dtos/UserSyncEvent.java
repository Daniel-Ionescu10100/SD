package ro.tuc.ds2020.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Event published on sync.exchange (routing key user.*) whenever a user changes.
 * eventType: USER_CREATED | USER_UPDATED | USER_DELETED
 * password is only set when it was created/changed (null otherwise).
 */
public record UserSyncEvent(
        @JsonProperty("eventType") String eventType,
        @JsonProperty("userId")    UUID userId,
        @JsonProperty("name")      String name,
        @JsonProperty("username")  String username,
        @JsonProperty("password")  String password,
        @JsonProperty("role")      String role
) {
}
