package com.example.demo4.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo4.common.BookingStatus;
import com.example.demo4.common.BusinessMetricType;
import com.example.demo4.common.HousekeepingStatus;
import com.example.demo4.common.RoomStatus;
import com.example.demo4.entity.Booking;
import com.example.demo4.entity.BusinessMetricEvent;
import com.example.demo4.entity.HousekeepingTask;
import com.example.demo4.entity.OperationReminder;
import com.example.demo4.entity.Room;
import com.example.demo4.service.BookingService;
import com.example.demo4.service.BusinessMetricService;
import com.example.demo4.service.HousekeepingService;
import com.example.demo4.service.OperationReminderService;
import com.example.demo4.service.RoomService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DecisionDashboardService {
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final BookingService bookingService;
    private final RoomService roomService;
    private final BusinessMetricService businessMetricService;
    private final HousekeepingService housekeepingService;
    private final OperationReminderService reminderService;
    private final ApiPerformanceMonitor performanceMonitor;

    public DecisionDashboardService(BookingService bookingService,
                                    RoomService roomService,
                                    BusinessMetricService businessMetricService,
                                    HousekeepingService housekeepingService,
                                    OperationReminderService reminderService,
                                    ApiPerformanceMonitor performanceMonitor) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.businessMetricService = businessMetricService;
        this.housekeepingService = housekeepingService;
        this.reminderService = reminderService;
        this.performanceMonitor = performanceMonitor;
    }

    public Map<String, Object> snapshot() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> occupancy = occupancyForecast(today);
        Map<String, Object> pricing = pricingImpact(now);
        Map<String, Object> protection = bookingProtection(today);
        Map<String, Object> operations = operations(now);
        Map<String, Object> api = performanceMonitor.snapshot();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", now);
        result.put("occupancy", occupancy);
        result.put("pricing", pricing);
        result.put("protection", protection);
        result.put("operations", operations);
        result.put("api", api);
        result.put("decisions", decisions(occupancy, pricing, protection, operations, api));
        return result;
    }

    private Map<String, Object> occupancyForecast(LocalDate today) {
        LocalDate end = today.plusDays(14);
        List<Room> rooms = roomService.list();
        Set<Long> capacityRoomIds = new HashSet<>();
        long readyRooms = 0;
        long cleaningRooms = 0;
        for (Room room : rooms) {
            if (!Integer.valueOf(RoomStatus.MAINTENANCE.getCode()).equals(room.getStatus())) {
                capacityRoomIds.add(room.getId());
            }
            if (Integer.valueOf(RoomStatus.AVAILABLE.getCode()).equals(room.getStatus())) readyRooms++;
            if (Integer.valueOf(RoomStatus.CLEANING.getCode()).equals(room.getStatus())) cleaningRooms++;
        }

        List<Booking> bookings = bookingService.list(new LambdaQueryWrapper<Booking>()
                .in(Booking::getStatus, BookingStatus.PENDING.getCode(),
                        BookingStatus.CONFIRMED.getCode(), BookingStatus.CHECKED_IN.getCode())
                .lt(Booking::getCheckInDate, end)
                .gt(Booking::getCheckOutDate, today));

        List<Map<String, Object>> days = new ArrayList<>();
        double sumRate = 0;
        double peakRate = -1;
        LocalDate peakDate = today;
        double lowestNextWeekRate = 101;
        LocalDate lowestNextWeekDate = today;
        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = today.plusDays(offset);
            Set<Long> occupiedRooms = new HashSet<>();
            for (Booking booking : bookings) {
                if (capacityRoomIds.contains(booking.getRoomId())
                        && !date.isBefore(booking.getCheckInDate())
                        && date.isBefore(booking.getCheckOutDate())) {
                    occupiedRooms.add(booking.getRoomId());
                }
            }
            double rate = percent(occupiedRooms.size(), capacityRoomIds.size());
            sumRate += rate;
            if (rate > peakRate) {
                peakRate = rate;
                peakDate = date;
            }
            if (offset < 7 && rate < lowestNextWeekRate) {
                lowestNextWeekRate = rate;
                lowestNextWeekDate = date;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("label", DATE_LABEL.format(date));
            point.put("weekday", weekday(date.getDayOfWeek()));
            point.put("occupiedRooms", occupiedRooms.size());
            point.put("availableInventory", Math.max(0, capacityRoomIds.size() - occupiedRooms.size()));
            point.put("occupancyRate", round(rate));
            days.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("capacityRooms", capacityRoomIds.size());
        result.put("readyRooms", readyRooms);
        result.put("cleaningRooms", cleaningRooms);
        result.put("todayRate", days.isEmpty() ? 0 : days.get(0).get("occupancyRate"));
        result.put("average14DayRate", round(sumRate / 14));
        result.put("peakRate", round(Math.max(0, peakRate)));
        result.put("peakDate", peakDate);
        result.put("lowestNextWeekRate", round(Math.min(100, lowestNextWeekRate)));
        result.put("lowestNextWeekDate", lowestNextWeekDate);
        result.put("days", days);
        return result;
    }

    private Map<String, Object> pricingImpact(LocalDateTime now) {
        LocalDateTime start = now.minusDays(30);
        List<Booking> eligible = bookingService.list(new LambdaQueryWrapper<Booking>()
                .ne(Booking::getStatus, BookingStatus.CANCELLED.getCode())
                .ge(Booking::getCreateTime, start));
        List<Booking> smart = eligible.stream()
                .filter(booking -> booking.getPricingStrategyVersion() != null && booking.getBasePrice() != null)
                .toList();

        BigDecimal baseline = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        int increased = 0;
        int discounted = 0;
        int unchanged = 0;
        Map<LocalDate, BigDecimal[]> daily = new HashMap<>();
        for (Booking booking : smart) {
            BigDecimal bookingBaseline = value(booking.getBasePrice())
                    .multiply(BigDecimal.valueOf(booking.getDays() == null ? 0 : booking.getDays()));
            BigDecimal bookingActual = value(booking.getTotalAmount());
            baseline = baseline.add(bookingBaseline);
            actual = actual.add(bookingActual);
            int comparison = bookingActual.compareTo(bookingBaseline);
            if (comparison > 0) increased++;
            else if (comparison < 0) discounted++;
            else unchanged++;
            LocalDate created = booking.getCreateTime() == null ? now.toLocalDate() : booking.getCreateTime().toLocalDate();
            BigDecimal[] totals = daily.computeIfAbsent(created,
                    key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[0] = totals[0].add(bookingBaseline);
            totals[1] = totals[1].add(bookingActual);
        }

        List<Map<String, Object>> dailyPoints = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = now.toLocalDate().minusDays(offset);
            BigDecimal[] totals = daily.getOrDefault(date,
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("label", DATE_LABEL.format(date));
            point.put("baselineAmount", money(totals[0]));
            point.put("bookedAmount", money(totals[1]));
            point.put("impact", money(totals[1].subtract(totals[0])));
            dailyPoints.add(point);
        }

        BigDecimal impact = actual.subtract(baseline);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowDays", 30);
        result.put("eligibleBookings", eligible.size());
        result.put("smartBookings", smart.size());
        result.put("coverageRate", round(percent(smart.size(), eligible.size())));
        result.put("baselineAmount", money(baseline));
        result.put("bookedAmount", money(actual));
        result.put("netImpact", money(impact));
        result.put("impactRate", baseline.signum() == 0 ? 0
                : impact.multiply(BigDecimal.valueOf(100)).divide(baseline, 2, RoundingMode.HALF_UP));
        result.put("increasedBookings", increased);
        result.put("discountedBookings", discounted);
        result.put("unchangedBookings", unchanged);
        result.put("daily", dailyPoints);
        return result;
    }

    private Map<String, Object> bookingProtection(LocalDate today) {
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        List<BusinessMetricEvent> events = businessMetricService.recentSince(start);
        long inventoryConflicts = countType(events, BusinessMetricType.INVENTORY_CONFLICT);
        long replays = countType(events, BusinessMetricType.IDEMPOTENT_REPLAY);
        long keyConflicts = countType(events, BusinessMetricType.IDEMPOTENCY_KEY_CONFLICT);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            long dailyConflicts = events.stream().filter(event -> event.getCreateTime() != null
                    && event.getCreateTime().toLocalDate().equals(date)
                    && (BusinessMetricType.INVENTORY_CONFLICT.equals(event.getEventType())
                    || BusinessMetricType.IDEMPOTENCY_KEY_CONFLICT.equals(event.getEventType()))).count();
            long dailyReplays = events.stream().filter(event -> event.getCreateTime() != null
                    && event.getCreateTime().toLocalDate().equals(date)
                    && BusinessMetricType.IDEMPOTENT_REPLAY.equals(event.getEventType())).count();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("label", DATE_LABEL.format(date));
            point.put("conflicts", dailyConflicts);
            point.put("replays", dailyReplays);
            trend.add(point);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inventoryConflicts7d", inventoryConflicts);
        result.put("idempotentReplays7d", replays);
        result.put("keyConflicts7d", keyConflicts);
        result.put("protectedRequests7d", inventoryConflicts + replays + keyConflicts);
        result.put("trend", trend);
        return result;
    }

    private Map<String, Object> operations(LocalDateTime now) {
        long pending = housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getStatus, HousekeepingStatus.PENDING.getCode()));
        long inProgress = housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .eq(HousekeepingTask::getStatus, HousekeepingStatus.IN_PROGRESS.getCode()));
        long overdue = housekeepingService.count(new LambdaQueryWrapper<HousekeepingTask>()
                .in(HousekeepingTask::getStatus,
                        HousekeepingStatus.PENDING.getCode(), HousekeepingStatus.IN_PROGRESS.getCode())
                .le(HousekeepingTask::getDueTime, now));
        long reminders = reminderService.count(new LambdaQueryWrapper<OperationReminder>()
                .eq(OperationReminder::getStatus, 0));
        return Map.of("pendingTasks", pending, "inProgressTasks", inProgress,
                "overdueTasks", overdue, "openReminders", reminders);
    }

    private List<Map<String, Object>> decisions(Map<String, Object> occupancy,
                                                Map<String, Object> pricing,
                                                Map<String, Object> protection,
                                                Map<String, Object> operations,
                                                Map<String, Object> api) {
        List<Map<String, Object>> decisions = new ArrayList<>();
        long overdue = ((Number) operations.get("overdueTasks")).longValue();
        if (overdue > 0) {
            decisions.add(decision("danger", "清洁任务已影响可售库存",
                    overdue + " 间客房清洁逾期，优先处理可直接恢复库存。",
                    "/pages/admin/admin-operations.html", "处理清洁任务"));
        }
        double peakRate = ((Number) occupancy.get("peakRate")).doubleValue();
        if (peakRate >= 80) {
            decisions.add(decision("warning", "高入住率日期需要收益管理",
                    occupancy.get("peakDate") + " 预测入住率 " + peakRate + "%：检查促销并评估提价。",
                    "/pages/admin/admin-room.html", "检查价格"));
        } else {
            double lowRate = ((Number) occupancy.get("lowestNextWeekRate")).doubleValue();
            if (lowRate <= 30) {
                decisions.add(decision("opportunity", "未来一周存在低入住率窗口",
                        occupancy.get("lowestNextWeekDate") + " 预测入住率仅 " + lowRate
                                + "%：适合定向促销或长住优惠。",
                        "/pages/admin/admin-room.html", "配置促销"));
            }
        }
        double coverage = ((Number) pricing.get("coverageRate")).doubleValue();
        if (((Number) pricing.get("eligibleBookings")).intValue() > 0 && coverage < 80) {
            decisions.add(decision("info", "智能定价覆盖率仍可提升",
                    "近 30 天仅 " + coverage + "% 的有效订单保留了定价决策快照，旧订单不会计入效果评估。",
                    "/pages/admin/admin-booking.html", "核对订单"));
        }
        long conflicts = ((Number) protection.get("inventoryConflicts7d")).longValue();
        if (conflicts > 0) {
            decisions.add(decision("success", "库存保护正在生效",
                    "近 7 天拦截 " + conflicts + " 次库存冲突，并发请求没有转化为重复订单。",
                    "/pages/admin/admin-booking.html", "查看订单"));
        }
        if ("WARNING".equals(api.get("health"))) {
            decisions.add(decision("danger", "接口性能超出监控阈值",
                    "最近滚动窗口 P95 为 " + api.get("p95Ms") + " ms，错误率 "
                            + round(((Number) api.get("errorRate")).doubleValue() * 100)
                            + "%：优先查看最慢接口。",
                    "#api-performance", "定位慢接口"));
        }
        if (decisions.isEmpty()) {
            decisions.add(decision("success", "当前没有需要立即处理的异常",
                    "入住率、运营任务与接口性能均在设定阈值内。",
                    "#occupancy-forecast", "查看趋势"));
        }
        return decisions;
    }

    private static Map<String, Object> decision(String level, String title, String message,
                                                String actionUrl, String actionLabel) {
        return Map.of("level", level, "title", title, "message", message,
                "actionUrl", actionUrl, "actionLabel", actionLabel);
    }

    private static long countType(List<BusinessMetricEvent> events, String type) {
        return events.stream().filter(event -> type.equals(event.getEventType())).count();
    }

    private static String weekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private static BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static double percent(long value, long total) {
        return total == 0 ? 0 : (double) value * 100 / total;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
