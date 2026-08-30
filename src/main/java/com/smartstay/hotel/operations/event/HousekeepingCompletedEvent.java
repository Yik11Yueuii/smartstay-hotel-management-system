package com.smartstay.hotel.operations.event;

public record HousekeepingCompletedEvent(Long taskId, Long roomId) {
}
