package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import com.smartfactory.mes.simulation.dto.dashboard.DashboardResponseModels;
import com.smartfactory.mes.simulation.dto.equipment.EquipmentResponseModels;
import com.smartfactory.mes.simulation.dto.line.LineResponseModels;
import com.smartfactory.mes.simulation.service.SimulationTickModels.SimulationTickResult;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationRealtimeSnapshotService {

    private final DashboardQueryService dashboardQueryService;
    private final LineQueryService lineQueryService;
    private final EquipmentQueryService equipmentQueryService;

    private DashboardCache dashboardCache;
    private final Map<Long, LineCache> lineCaches = new HashMap<>();
    private final Map<Long, EquipmentCache> equipmentCaches = new HashMap<>();

    public synchronized void initializeFromDatabase() {
        DashboardResponseModels.DashboardSnapshotResponse dashboard = dashboardQueryService.getDashboardSnapshot();

        long totalSecondsToday = Math.max(1L, Duration.between(
                dashboard.generatedAt().toLocalDate().atStartOfDay(),
                dashboard.generatedAt()
        ).getSeconds());

        DashboardCache nextDashboardCache = new DashboardCache();
        nextDashboardCache.generatedAt = dashboard.generatedAt();
        nextDashboardCache.totalTarget = dashboard.kpis().totalTarget();
        nextDashboardCache.productionTrend = new ArrayList<>(dashboard.productionTrend());
        nextDashboardCache.dashboardAlarms = new ArrayDeque<>(dashboard.recentAlarms());

        lineCaches.clear();
        equipmentCaches.clear();

        for (DashboardResponseModels.LineStatusResponse lineSummary : dashboard.lines()) {
            LineResponseModels.LineDetailResponse lineDetail = lineQueryService.getLineDetail(lineSummary.lineId());

            LineCache lineCache = new LineCache();
            lineCache.lineId = lineDetail.lineId();
            lineCache.lineCode = lineDetail.lineCode();
            lineCache.lineName = lineDetail.lineName();
            lineCache.productName = lineDetail.productName();
            lineCache.currentStatus = lineDetail.currentStatus();
            lineCache.location = lineDetail.location();
            lineCache.targetProduction = lineDetail.targetProduction();
            lineCache.production = lineDetail.kpis().production();
            lineCache.defectCount = estimateDefectCount(lineDetail.kpis().production(), lineDetail.kpis().defectRate());
            lineCache.alarms = new ArrayDeque<>(lineDetail.alarms());

            lineCaches.put(lineCache.lineId, lineCache);
            nextDashboardCache.totalProduction += lineCache.production;
            nextDashboardCache.totalDefects += lineCache.defectCount;

            for (LineResponseModels.LineEquipmentResponse equipmentSummary : lineDetail.equipments()) {
                EquipmentResponseModels.EquipmentDetailResponse equipmentDetail =
                        equipmentQueryService.getEquipmentDetail(equipmentSummary.equipmentId());

                EquipmentCache equipmentCache = new EquipmentCache();
                equipmentCache.equipmentId = equipmentDetail.equipmentId();
                equipmentCache.lineId = equipmentDetail.lineId();
                equipmentCache.lineName = equipmentDetail.lineName();
                equipmentCache.equipmentCode = equipmentDetail.equipmentCode();
                equipmentCache.equipmentName = equipmentDetail.equipmentName();
                equipmentCache.equipmentType = equipmentDetail.equipmentType();
                equipmentCache.currentStatus = equipmentDetail.currentStatus();
                equipmentCache.processOrder = equipmentDetail.processOrder();
                equipmentCache.lastStatusChangedAt = equipmentDetail.lastStatusChangedAt();
                equipmentCache.lastInspectionAt = equipmentDetail.lastInspectionAt();
                equipmentCache.production = equipmentDetail.kpis().production();
                equipmentCache.uph = equipmentDetail.kpis().uph();
                equipmentCache.totalObservedSeconds = totalSecondsToday;
                equipmentCache.runObservedSeconds = Math.round(totalSecondsToday * (equipmentDetail.kpis().uptime() / 100.0));
                equipmentCache.alarms = new ArrayDeque<>(equipmentDetail.alarms());

                equipmentCaches.put(equipmentCache.equipmentId, equipmentCache);
            }
        }

        nextDashboardCache.totalProduction = equipmentCaches.values().stream()
                .mapToInt(cache -> cache.production)
                .sum();
        nextDashboardCache.equipmentTotal = equipmentCaches.size();
        nextDashboardCache.runningCount = (int) equipmentCaches.values().stream().filter(cache -> "RUN".equals(cache.currentStatus)).count();
        nextDashboardCache.stoppedCount = (int) equipmentCaches.values().stream().filter(cache -> "STOP".equals(cache.currentStatus)).count();
        nextDashboardCache.idleCount = (int) equipmentCaches.values().stream().filter(cache -> "IDLE".equals(cache.currentStatus)).count();
        nextDashboardCache.errorCount = (int) equipmentCaches.values().stream().filter(cache -> "ERROR".equals(cache.currentStatus)).count();
        nextDashboardCache.maintenanceCount = (int) equipmentCaches.values().stream().filter(cache -> "MAINTENANCE".equals(cache.currentStatus)).count();

        dashboardCache = nextDashboardCache;
    }

    public synchronized void applyTick(SimulationTickResult tickResult) {
        if (dashboardCache == null) {
            return;
        }

        LocalDateTime generatedAt = tickResult.productionRecords().isEmpty()
                ? LocalDateTime.now()
                : tickResult.productionRecords().get(0).getRecordTime();

        Map<Long, Integer> productionByLine = new HashMap<>();
        Map<Long, Integer> defectsByLine = new HashMap<>();
        int tickProductionTotal = 0;
        int tickDefectTotal = 0;

        for (var record : tickResult.productionRecords()) {
            EquipmentCache equipmentCache = equipmentCaches.get(record.getEquipmentId());
            if (equipmentCache == null) {
                continue;
            }

            equipmentCache.production += record.getProductionCount();
            equipmentCache.uph = record.getUph();
            equipmentCache.totalObservedSeconds += 5L;
            if ("RUN".equals(equipmentCache.currentStatus)) {
                equipmentCache.runObservedSeconds += 5L;
            }

            productionByLine.merge(record.getLineId(), record.getProductionCount(), Integer::sum);
            defectsByLine.merge(record.getLineId(), record.getDefectCount(), Integer::sum);
            tickProductionTotal += record.getProductionCount();
            tickDefectTotal += record.getDefectCount();
        }

        tickResult.equipmentUpdates().forEach(update -> {
            EquipmentCache equipmentCache = equipmentCaches.get(update.equipmentId());
            if (equipmentCache != null) {
                equipmentCache.currentStatus = update.status().name();
                equipmentCache.lastStatusChangedAt = update.lastStatusChangedAt();
            }
        });

        tickResult.lineUpdates().forEach(update -> {
            LineCache lineCache = lineCaches.get(update.lineId());
            if (lineCache != null) {
                lineCache.currentStatus = update.status().name();
            }
        });

        for (Map.Entry<Long, LineCache> entry : lineCaches.entrySet()) {
            Long lineId = entry.getKey();
            LineCache lineCache = entry.getValue();
            lineCache.production += productionByLine.getOrDefault(lineId, 0);
            lineCache.defectCount += defectsByLine.getOrDefault(lineId, 0);
        }

        tickResult.alarms().forEach(alarm -> {
            DashboardResponseModels.AlarmResponse response = mapAlarm(alarm);
            dashboardCache.dashboardAlarms.addFirst(response);
            trimDeque(dashboardCache.dashboardAlarms, 10);

            LineCache lineCache = lineCaches.get(alarm.getLineId());
            if (lineCache != null) {
                lineCache.alarms.addFirst(response);
                trimDeque(lineCache.alarms, 20);
            }

            if (alarm.getEquipmentId() != null) {
                EquipmentCache equipmentCache = equipmentCaches.get(alarm.getEquipmentId());
                if (equipmentCache != null) {
                    equipmentCache.alarms.addFirst(response);
                    trimDeque(equipmentCache.alarms, 20);
                }
            }
        });

        dashboardCache.totalProduction += tickProductionTotal;
        dashboardCache.totalDefects += tickDefectTotal;
        dashboardCache.generatedAt = generatedAt;
        dashboardCache.runningCount = (int) equipmentCaches.values().stream().filter(cache -> "RUN".equals(cache.currentStatus)).count();
        dashboardCache.stoppedCount = (int) equipmentCaches.values().stream().filter(cache -> "STOP".equals(cache.currentStatus)).count();
        dashboardCache.idleCount = (int) equipmentCaches.values().stream().filter(cache -> "IDLE".equals(cache.currentStatus)).count();
        dashboardCache.errorCount = (int) equipmentCaches.values().stream().filter(cache -> "ERROR".equals(cache.currentStatus)).count();
        dashboardCache.maintenanceCount = (int) equipmentCaches.values().stream().filter(cache -> "MAINTENANCE".equals(cache.currentStatus)).count();

        updateTrend(generatedAt, tickProductionTotal);
    }

    public synchronized DashboardResponseModels.DashboardSnapshotResponse getDashboardSnapshot() {
        if (dashboardCache == null) {
            return dashboardQueryService.getDashboardSnapshot();
        }

        List<DashboardResponseModels.LineStatusResponse> lineResponses = lineCaches.values().stream()
                .sorted(Comparator.comparing(cache -> cache.lineId))
                .map(this::toDashboardLineResponse)
                .toList();

        double avgUptime = equipmentCaches.isEmpty()
                ? 0.0
                : round1(equipmentCaches.values().stream().mapToDouble(this::equipmentUptime).average().orElse(0.0));
        double avgDefectRate = dashboardCache.totalProduction == 0
                ? 0.0
                : round2((double) dashboardCache.totalDefects / dashboardCache.totalProduction * 100.0);

        return new DashboardResponseModels.DashboardSnapshotResponse(
                new DashboardResponseModels.DashboardKpiResponse(
                        dashboardCache.totalProduction,
                        dashboardCache.totalTarget,
                        dashboardCache.totalTarget == 0 ? 0.0 : round1((double) dashboardCache.totalProduction / dashboardCache.totalTarget * 100.0),
                        avgUptime,
                        avgDefectRate
                ),
                new ArrayList<>(dashboardCache.productionTrend),
                lineResponses,
                new DashboardResponseModels.EquipmentSummaryResponse(
                        dashboardCache.equipmentTotal,
                        dashboardCache.runningCount,
                        dashboardCache.stoppedCount,
                        dashboardCache.idleCount,
                        dashboardCache.errorCount,
                        dashboardCache.maintenanceCount
                ),
                new ArrayList<>(dashboardCache.dashboardAlarms),
                dashboardCache.generatedAt
        );
    }

    public synchronized LineResponseModels.LineDetailResponse getLineDetail(Long lineId) {
        if (dashboardCache == null) {
            return lineQueryService.getLineDetail(lineId);
        }

        LineCache lineCache = lineCaches.get(lineId);
        if (lineCache == null) {
            throw new BusinessException(ErrorCode.LINE_NOT_FOUND);
        }

        List<LineResponseModels.LineEquipmentResponse> equipments = equipmentCaches.values().stream()
                .filter(cache -> lineId.equals(cache.lineId))
                .sorted(Comparator.comparing(cache -> cache.processOrder))
                .map(this::toLineEquipmentResponse)
                .toList();

        double uptime = equipments.isEmpty()
                ? 0.0
                : round1(equipmentCaches.values().stream()
                .filter(cache -> lineId.equals(cache.lineId))
                .mapToDouble(this::equipmentUptime)
                .average()
                .orElse(0.0));

        double achievementRate = lineCache.targetProduction == 0
                ? 0.0
                : round1((double) lineCache.production / lineCache.targetProduction * 100.0);
        double defectRate = lineCache.production == 0
                ? 0.0
                : round2((double) lineCache.defectCount / lineCache.production * 100.0);

        return new LineResponseModels.LineDetailResponse(
                lineCache.lineId,
                lineCache.lineCode,
                lineCache.lineName,
                lineCache.productName,
                lineCache.currentStatus,
                lineCache.location,
                lineCache.targetProduction,
                new LineResponseModels.LineKpiResponse(
                        lineCache.production,
                        lineCache.targetProduction,
                        achievementRate,
                        uptime,
                        defectRate
                ),
                equipments,
                new ArrayList<>(lineCache.alarms),
                dashboardCache.generatedAt
        );
    }

    public synchronized EquipmentResponseModels.EquipmentDetailResponse getEquipmentDetail(Long equipmentId) {
        if (dashboardCache == null) {
            return equipmentQueryService.getEquipmentDetail(equipmentId);
        }

        EquipmentCache equipmentCache = equipmentCaches.get(equipmentId);
        if (equipmentCache == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        return new EquipmentResponseModels.EquipmentDetailResponse(
                equipmentCache.equipmentId,
                equipmentCache.lineId,
                equipmentCache.lineName,
                equipmentCache.equipmentCode,
                equipmentCache.equipmentName,
                equipmentCache.equipmentType,
                equipmentCache.currentStatus,
                equipmentCache.processOrder,
                equipmentCache.lastStatusChangedAt,
                equipmentCache.lastInspectionAt,
                new EquipmentResponseModels.EquipmentKpiResponse(
                        equipmentCache.production,
                        equipmentCache.uph,
                        round1(equipmentUptime(equipmentCache))
                ),
                new ArrayList<>(equipmentCache.alarms),
                dashboardCache.generatedAt
        );
    }

    private DashboardResponseModels.LineStatusResponse toDashboardLineResponse(LineCache lineCache) {
        double achievementRate = lineCache.targetProduction == 0
                ? 0.0
                : round1((double) lineCache.production / lineCache.targetProduction * 100.0);
        double uptime = round1(equipmentCaches.values().stream()
                .filter(cache -> lineCache.lineId.equals(cache.lineId))
                .mapToDouble(this::equipmentUptime)
                .average()
                .orElse(0.0));
        double defectRate = lineCache.production == 0
                ? 0.0
                : round2((double) lineCache.defectCount / lineCache.production * 100.0);

        return new DashboardResponseModels.LineStatusResponse(
                lineCache.lineId,
                lineCache.lineCode,
                lineCache.lineName,
                lineCache.productName,
                lineCache.currentStatus,
                lineCache.production,
                lineCache.targetProduction,
                achievementRate,
                uptime,
                defectRate
        );
    }

    private LineResponseModels.LineEquipmentResponse toLineEquipmentResponse(EquipmentCache equipmentCache) {
        return new LineResponseModels.LineEquipmentResponse(
                equipmentCache.equipmentId,
                equipmentCache.equipmentCode,
                equipmentCache.equipmentName,
                equipmentCache.equipmentType,
                equipmentCache.currentStatus,
                equipmentCache.production,
                equipmentCache.uph,
                round1(equipmentUptime(equipmentCache)),
                equipmentCache.lastStatusChangedAt,
                dashboardCache.generatedAt
        );
    }

    private DashboardResponseModels.AlarmResponse mapAlarm(com.smartfactory.mes.simulation.domain.AlarmHistory alarm) {
        LineCache lineCache = lineCaches.get(alarm.getLineId());
        EquipmentCache equipmentCache = alarm.getEquipmentId() == null ? null : equipmentCaches.get(alarm.getEquipmentId());

        return new DashboardResponseModels.AlarmResponse(
                alarm.getAlarmId(),
                alarm.getLineId(),
                lineCache == null ? null : lineCache.lineName,
                alarm.getEquipmentId(),
                equipmentCache == null ? null : equipmentCache.equipmentName,
                alarm.getAlarmType(),
                alarm.getSeverity(),
                alarm.getMessage(),
                Boolean.TRUE.equals(alarm.getAcknowledged()),
                alarm.getCreatedAt()
        );
    }

    private void updateTrend(LocalDateTime generatedAt, int tickProductionTotal) {
        LocalDateTime currentHour = generatedAt.truncatedTo(ChronoUnit.HOURS);

        if (dashboardCache.productionTrend.isEmpty()) {
            dashboardCache.productionTrend = new ArrayList<>();
        }

        List<DashboardResponseModels.ProductionTrendPointResponse> trend = new ArrayList<>(dashboardCache.productionTrend);
        if (trend.isEmpty()) {
            for (int i = 7; i >= 0; i--) {
                LocalDateTime hour = currentHour.minusHours(i);
                trend.add(new DashboardResponseModels.ProductionTrendPointResponse(
                        formatHour(hour),
                        0,
                        dashboardCache.totalTarget / 24
                ));
            }
        }

        DashboardResponseModels.ProductionTrendPointResponse last = trend.get(trend.size() - 1);
        if (!last.time().equals(formatHour(currentHour))) {
            trend.add(new DashboardResponseModels.ProductionTrendPointResponse(
                    formatHour(currentHour),
                    tickProductionTotal,
                    dashboardCache.totalTarget / 24
            ));
            while (trend.size() > 8) {
                trend.remove(0);
            }
        } else {
            trend.set(trend.size() - 1, new DashboardResponseModels.ProductionTrendPointResponse(
                    last.time(),
                    last.production() + tickProductionTotal,
                    last.target()
            ));
        }

        dashboardCache.productionTrend = trend;
    }

    private int estimateDefectCount(int production, double defectRate) {
        return (int) Math.round(production * (defectRate / 100.0));
    }

    private double equipmentUptime(EquipmentCache equipmentCache) {
        if (equipmentCache.totalObservedSeconds == 0) {
            return 0.0;
        }
        return (double) equipmentCache.runObservedSeconds / equipmentCache.totalObservedSeconds * 100.0;
    }

    private void trimDeque(Deque<?> deque, int limit) {
        while (deque.size() > limit) {
            deque.removeLast();
        }
    }

    private String formatHour(LocalDateTime dateTime) {
        return dateTime.toLocalTime().truncatedTo(ChronoUnit.HOURS).toString();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class DashboardCache {
        private int totalProduction;
        private int totalTarget;
        private int totalDefects;
        private int equipmentTotal;
        private int runningCount;
        private int stoppedCount;
        private int idleCount;
        private int errorCount;
        private int maintenanceCount;
        private LocalDateTime generatedAt;
        private List<DashboardResponseModels.ProductionTrendPointResponse> productionTrend = new ArrayList<>();
        private Deque<DashboardResponseModels.AlarmResponse> dashboardAlarms = new ArrayDeque<>();
    }

    private static class LineCache {
        private Long lineId;
        private String lineCode;
        private String lineName;
        private String productName;
        private String currentStatus;
        private String location;
        private int targetProduction;
        private int production;
        private int defectCount;
        private Deque<DashboardResponseModels.AlarmResponse> alarms = new ArrayDeque<>();
    }

    private static class EquipmentCache {
        private Long equipmentId;
        private Long lineId;
        private String lineName;
        private String equipmentCode;
        private String equipmentName;
        private String equipmentType;
        private String currentStatus;
        private int processOrder;
        private LocalDateTime lastStatusChangedAt;
        private LocalDateTime lastInspectionAt;
        private int production;
        private int uph;
        private long totalObservedSeconds;
        private long runObservedSeconds;
        private Deque<DashboardResponseModels.AlarmResponse> alarms = new ArrayDeque<>();
    }
}
