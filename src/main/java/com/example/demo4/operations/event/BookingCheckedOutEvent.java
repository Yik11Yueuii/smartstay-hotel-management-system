package com.example.demo4.operations.event;

import java.time.LocalDateTime;

public record BookingCheckedOutEvent(Long bookingId, Long roomId, String roomNumber,
                                     LocalDateTime checkedOutTime) {
}
