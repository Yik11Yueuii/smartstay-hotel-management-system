package com.smartstay.hotel.operations;

import com.smartstay.hotel.entity.HousekeepingTask;
import com.smartstay.hotel.service.BookingService;
import com.smartstay.hotel.service.HousekeepingService;
import com.smartstay.hotel.service.OperationReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:operations_workflow_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "operations.reminder.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperationsWorkflowIntegrationTest {
    @Autowired private BookingService bookingService;
    @Autowired private HousekeepingService housekeepingService;
    @Autowired private OperationReminderService reminderService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createSchemaInDedicatedInMemoryDatabase() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS operation_reminder");
        jdbcTemplate.execute("DROP TABLE IF EXISTS housekeeping_task");
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
        jdbcTemplate.execute("CREATE TABLE housekeeping_task (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_no VARCHAR(40) NOT NULL UNIQUE, "
                + "booking_id BIGINT NOT NULL, room_id BIGINT NOT NULL, room_number VARCHAR(20), task_type VARCHAR(40) NOT NULL, "
                + "status INT NOT NULL, priority INT NOT NULL, assignee VARCHAR(50), due_time TIMESTAMP NOT NULL, "
                + "started_time TIMESTAMP, completed_time TIMESTAMP, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT uk_housekeeping_booking_type UNIQUE(booking_id, task_type))");
        jdbcTemplate.execute("CREATE TABLE operation_reminder (id BIGINT AUTO_INCREMENT PRIMARY KEY, reminder_key VARCHAR(100) NOT NULL UNIQUE, "
                + "reminder_type VARCHAR(40) NOT NULL, level INT NOT NULL, title VARCHAR(100) NOT NULL, content VARCHAR(500) NOT NULL, "
                + "booking_id BIGINT, room_id BIGINT, task_id BIGINT, status INT NOT NULL, trigger_time TIMESTAMP NOT NULL, "
                + "resolved_time TIMESTAMP, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbcTemplate.update("INSERT INTO room(id, room_name, room_number, price, status, is_promotion) VALUES (20, ?, ?, ?, 3, 0)",
                "运营测试房", "2020", 500);
        jdbcTemplate.update("INSERT INTO booking(id, order_no, user_id, room_id, room_name, room_number, check_in_date, "
                        + "check_out_date, days, price, total_amount, contact_name, contact_phone, guest_name, guest_id_card, "
                        + "deposit, actual_check_in_time, status) VALUES (30, 'OPS-CHECKOUT-001', 2, 20, ?, ?, CURRENT_DATE - 1, "
                        + "CURRENT_DATE + 1, 2, 500, 1000, '测试用户', '13800000000', '测试用户', "
                        + "'110101199001011234', 200, CURRENT_TIMESTAMP - 1, 3)",
                "运营测试房", "2020");
    }

    @Test
    void checkoutCreatesCleaningTaskAndCompletionClosesTheLoop() {
        bookingService.checkOut(30L, new BigDecimal("200.00"), BigDecimal.ZERO, "运营闭环测试");

        HousekeepingTask task = housekeepingService.list().get(0);
        assertNotNull(task.getId());
        assertEquals(0, task.getStatus());
        assertEquals(5, roomStatus());

        jdbcTemplate.update("UPDATE housekeeping_task SET due_time = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(40), task.getId());
        assertEquals(1, reminderService.scanAndCreate(LocalDateTime.now()));
        assertEquals(0, reminderService.scanAndCreate(LocalDateTime.now()));
        assertEquals(1L, reminderService.count());

        housekeepingService.startTask(task.getId(), "保洁员A");
        housekeepingService.completeTask(task.getId());

        assertEquals(2, housekeepingService.getById(task.getId()).getStatus());
        assertEquals("保洁员A", housekeepingService.getById(task.getId()).getAssignee());
        assertEquals(1, roomStatus());
        assertEquals(1, reminderService.list().get(0).getStatus());
    }

    @Test
    void imminentArrivalReminderIsCreatedOnlyOnce() {
        bookingService.checkOut(30L, new BigDecimal("200.00"), BigDecimal.ZERO, "临近入住提醒测试");
        HousekeepingTask task = housekeepingService.list().get(0);
        jdbcTemplate.update("INSERT INTO booking(id, order_no, user_id, room_id, room_name, room_number, check_in_date, "
                        + "check_out_date, days, price, total_amount, contact_name, contact_phone, deposit, status) "
                        + "VALUES (31, 'OPS-ARRIVAL-001', 3, 20, ?, ?, CURRENT_DATE + 1, CURRENT_DATE + 2, 1, 500, 500, "
                        + "'临近入住用户', '13800000001', 0, 1)",
                "运营测试房", "2020");

        assertEquals(1, reminderService.scanAndCreate(LocalDateTime.now()));
        assertEquals(0, reminderService.scanAndCreate(LocalDateTime.now()));
        assertEquals(1L, reminderService.count());
        assertEquals("CHECKIN_CLEANING_RISK", reminderService.list().get(0).getReminderType());
        assertEquals(task.getId(), reminderService.list().get(0).getTaskId());
    }

    private int roomStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM room WHERE id = 20", Integer.class);
    }
}
