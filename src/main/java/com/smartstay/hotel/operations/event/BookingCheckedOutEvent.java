package com.smartstay.hotel.operations.event;

import java.time.LocalDateTime;

public record BookingCheckedOutEvent(Long bookingId, Long roomId, String roomNumber,
                                     LocalDateTime checkedOutTime) {
}
