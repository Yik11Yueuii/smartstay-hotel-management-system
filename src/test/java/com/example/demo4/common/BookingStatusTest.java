package com.example.demo4.common;

import com.example.demo4.exception.BusinessException;
import com.example.demo4.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingStatusTest {
    private static final Set<String> ALLOWED = Set.of(
            "PENDING->CONFIRMED", "PENDING->CANCELLED",
            "CONFIRMED->CANCELLED", "CONFIRMED->CHECKED_IN",
            "CHECKED_IN->CHECKED_OUT");

    @Test
    void coversEveryPossibleStateTransition() {
        for (BookingStatus source : BookingStatus.values()) {
            for (BookingStatus target : BookingStatus.values()) {
                String transition = source.name() + "->" + target.name();
                if (ALLOWED.contains(transition)) {
                    assertDoesNotThrow(() -> source.requireTransitionTo(target), transition);
                } else {
                    BusinessException exception = assertThrows(BusinessException.class,
                            () -> source.requireTransitionTo(target), transition);
                    assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
                }
            }
        }
    }

    @Test
    void rejectsUnknownDatabaseStatus() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> BookingStatus.fromCode(99));
        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
    }
}
