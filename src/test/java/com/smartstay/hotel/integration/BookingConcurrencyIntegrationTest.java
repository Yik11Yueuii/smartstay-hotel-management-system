package com.smartstay.hotel.integration;

import com.smartstay.hotel.entity.Booking;
import com.smartstay.hotel.exception.BusinessException;
import com.smartstay.hotel.exception.ErrorCode;
import com.smartstay.hotel.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class BookingConcurrencyIntegrationTest {
    @Autowired private BookingService bookingService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createIsolatedSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS booking");
        jdbcTemplate.execute("DROP TABLE IF EXISTS room");
        jdbcTemplate.execute("CREATE TABLE room (id BIGINT AUTO_INCREMENT PRIMARY KEY, room_name VARCHAR(100), "
                + "room_type VARCHAR(50), room_number VARCHAR(20), price DECIMAL(10,2), bed_type VARCHAR(50), "
                + "max_people INT, description VARCHAR(500), status INT, is_promotion INT, promotion_price DECIMAL(10,2), "
                + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.execute("CREATE TABLE booking (id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(50) NOT NULL UNIQUE, "
                + "idempotency_key VARCHAR(64), user_id BIGINT NOT NULL, room_id BIGINT NOT NULL, room_name VARCHAR(100), "
                + "room_number VARCHAR(20), check_in_date DATE NOT NULL, check_out_date DATE NOT NULL, days INT NOT NULL, "
                + "base_price DECIMAL(10,2), price DECIMAL(10,2) NOT NULL, total_amount DECIMAL(10,2) NOT NULL, "
                + "pricing_strategy_version VARCHAR(50), pricing_snapshot CLOB, contact_name VARCHAR(50), "
                + "contact_phone VARCHAR(20), guest_name VARCHAR(50), guest_id_card VARCHAR(20), deposit DECIMAL(10,2), "
                + "actual_check_in_time TIMESTAMP, actual_check_out_time TIMESTAMP, deposit_return DECIMAL(10,2), "
                + "additional_charges DECIMAL(10,2), remark VARCHAR(500), status INT NOT NULL, "
                + "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "CONSTRAINT uk_booking_user_idempotency UNIQUE(user_id, idempotency_key))");
        jdbcTemplate.update("INSERT INTO room(id, room_name, room_number, price, status, is_promotion) VALUES (8, ?, ?, ?, 1, 0)",
                "并发测试房", "0808", 388);
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalBooking() {
        Booking first = bookingService.createBooking(request(), 2L, "same-request-key-0001");
        Booking repeated = bookingService.createBooking(request(), 2L, "same-request-key-0001");

        assertNotNull(first.getId());
        assertEquals(first.getId(), repeated.getId());
        assertEquals(1L, bookingService.count());
    }

    @Test
    void reusedIdempotencyKeyRejectsDifferentRequest() {
        bookingService.createBooking(request(), 2L, "reused-request-key-01");
        Booking changed = request();
        changed.setCheckOutDate(changed.getCheckOutDate().plusDays(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookingService.createBooking(changed, 2L, "reused-request-key-01"));

        assertEquals(ErrorCode.STATE_CONFLICT, exception.getErrorCode());
        assertEquals(1L, bookingService.count());
    }

    @Test
    void concurrentRequestsForSameRoomCreateExactlyOneBooking() throws Exception {
        int competitors = 20;
        CountDownLatch ready = new CountDownLatch(competitors);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Booking> successes = new ConcurrentLinkedQueue<>();
        Queue<BusinessException> conflicts = new ConcurrentLinkedQueue<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(competitors)) {
            for (int i = 0; i < competitors; i++) {
                final int index = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        successes.add(bookingService.createBooking(
                                request(), 100L + index, "concurrent-key-" + String.format("%04d", index)));
                    } catch (BusinessException exception) {
                        conflicts.add(exception);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }

        assertEquals(1, successes.size());
        assertEquals(competitors - 1, conflicts.size());
        assertTrue(conflicts.stream().allMatch(e -> e.getErrorCode() == ErrorCode.STATE_CONFLICT));
        assertEquals(1L, bookingService.count());
    }

    private Booking request() {
        Booking booking = new Booking();
        booking.setRoomId(8L);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        booking.setContactName("并发测试用户");
        booking.setContactPhone("13800000000");
        return booking;
    }
}
