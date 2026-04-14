package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import com.smartfactory.mes.simulation.config.SimulationProperties;
import com.smartfactory.mes.simulation.domain.AlarmHistory;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.dto.DashboardResponseModels;
import com.smartfactory.mes.simulation.dto.EquipmentResponseModels;
import com.smartfactory.mes.simulation.dto.LineResponseModels;
import com.smartfactory.mes.simulation.mapper.AlarmHistoryMapper;
import com.smartfactory.mes.simulation.mapper.EquipmentMapper;
import com.smartfactory.mes.simulation.mapper.EquipmentStatusHistoryMapper;
import com.smartfactory.mes.simulation.mapper.ProductionLineMapper;
import com.smartfactory.mes.simulation.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationQueryService {

    private final ProductionLineMapper productionLineMapper;
    private final EquipmentMapper equipmentMapper;
    private final ProductionRecordMapper productionRecordMapper;
    private final EquipmentStatusHistoryMapper equipmentStatusHistoryMapper;
    private final AlarmHistoryMapper alarmHistoryMapper;
    private final SimulationProperties simulationProperties;

    public DashboardResponseModels.DashboardSnapshotResponse getDashboardSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        long trendWindow = Math.max(0L, simulationProperties.getDashboardTrendHours() - 1L);
        LocalDateTime trendStart = now.minusHours(trendWindow).truncatedTo(ChronoUnit.HOURS);

        List<ProductionLine> lines = productionLineMapper.selectActiveLines();
        List<Equipment> equipments = equipmentMapper.selectActiveEquipments();
        List<ProductionRecord> productionRecords = productionRecordMapper.selectSince(dayStart);
        List<EquipmentStatusHistory> histories = equipmentStatusHistoryMapper.selectIntersecting(dayStart);

        Map<Long, List<ProductionRecord>> recordsByLine = productionRecords.stream()
                .collect(Collectors.groupingBy(ProductionRecord::getLineId));
        Map<Long, List<EquipmentStatusHistory>> historiesByEquipment = histories.stream()
                .collect(Collectors.groupingBy(EquipmentStatusHistory::getEquipmentId));
        Map<Long, List<Equipment>> equipmentsByLine = equipments.stream()
                .collect(Collectors.groupingBy(Equipment::getLineId));

        Map<Long, Double> equipmentUptimeMap = calculateEquipmentUptime(equipments, historiesByEquipment, dayStart, now);
        DashboardResponseModels.DashboardKpiResponse kpiResponse = buildDashboardKpis(
                lines,
                equipments,
                productionRecords,
                equipmentUptimeMap
        );

        List<DashboardResponseModels.LineStatusResponse> lineResponses = new ArrayList<>();
        for (ProductionLine line : lines) {
            List<ProductionRecord> lineRecords = recordsByLine.getOrDefault(line.getLineId(), List.of());
            List<Equipment> lineEquipments = equipmentsByLine.getOrDefault(line.getLineId(), List.of());
            double uptime = lineEquipments.isEmpty()
                    ? 0.0
                    : round1(lineEquipments.stream()
                    .mapToDouble(equipment -> equipmentUptimeMap.getOrDefault(equipment.getEquipmentId(), 0.0))
                    .average()
                    .orElse(0.0));
            int production = lineRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
            int defects = lineRecords.stream().mapToInt(ProductionRecord::getDefectCount).sum();
            double achievementRate = line.getTargetProduction() == null || line.getTargetProduction() == 0
                    ? 0.0
                    : round1((double) production / line.getTargetProduction() * 100.0);
            double defectRate = production == 0 ? 0.0 : round2((double) defects / production * 100.0);

            lineResponses.add(new DashboardResponseModels.LineStatusResponse(
                    line.getLineId(),
                    line.getLineCode(),
                    line.getLineName(),
                    line.getProductName(),
                    line.getCurrentStatus(),
                    production,
                    line.getTargetProduction(),
                    achievementRate,
                    uptime,
                    defectRate
            ));
        }

        List<DashboardResponseModels.ProductionTrendPointResponse> trend = buildTrend(
                productionRecordMapper.selectSince(trendStart),
                lines,
                trendStart,
                now
        );

        DashboardResponseModels.EquipmentSummaryResponse equipmentSummary = new DashboardResponseModels.EquipmentSummaryResponse(
                equipments.size(),
                countByStatus(equipments, "RUN"),
                countByStatus(equipments, "STOP"),
                countByStatus(equipments, "IDLE"),
                countByStatus(equipments, "ERROR"),
                countByStatus(equipments, "MAINTENANCE")
        );

        return new DashboardResponseModels.DashboardSnapshotResponse(
                kpiResponse,
                trend,
                lineResponses,
                equipmentSummary,
                mapAlarms(alarmHistoryMapper.selectRecentDashboardAlarms(), lines, equipments),
                now
        );
    }

    public LineResponseModels.LineDetailResponse getLineDetail(Long lineId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();

        ProductionLine line = productionLineMapper.selectById(lineId);
        if (line == null) {
            throw new BusinessException(ErrorCode.LINE_NOT_FOUND);
        }

        List<Equipment> equipments = equipmentMapper.selectByLineId(lineId);
        List<ProductionRecord> lineRecords = productionRecordMapper.selectByLineSince(lineId, dayStart);
        List<EquipmentStatusHistory> histories = equipmentStatusHistoryMapper.selectIntersecting(dayStart);
        Map<Long, List<EquipmentStatusHistory>> historiesByEquipment = histories.stream()
                .collect(Collectors.groupingBy(EquipmentStatusHistory::getEquipmentId));
        Map<Long, List<ProductionRecord>> recordsByEquipment = lineRecords.stream()
                .collect(Collectors.groupingBy(ProductionRecord::getEquipmentId));
        Map<Long, Double> equipmentUptimeMap = calculateEquipmentUptime(equipments, historiesByEquipment, dayStart, now);

        int production = lineRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
        int defects = lineRecords.stream().mapToInt(ProductionRecord::getDefectCount).sum();
        double uptime = equipments.isEmpty()
                ? 0.0
                : round1(equipmentUptimeMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        double achievementRate = line.getTargetProduction() == null || line.getTargetProduction() == 0
                ? 0.0
                : round1((double) production / line.getTargetProduction() * 100.0);
        double defectRate = production == 0 ? 0.0 : round2((double) defects / production * 100.0);

        List<LineResponseModels.LineEquipmentResponse> equipmentResponses = equipments.stream()
                .map(equipment -> {
                    List<ProductionRecord> equipmentRecords = recordsByEquipment.getOrDefault(equipment.getEquipmentId(), List.of());
                    int equipmentProduction = equipmentRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
                    int latestUph = equipmentRecords.stream()
                            .max(Comparator.comparing(ProductionRecord::getRecordTime))
                            .map(ProductionRecord::getUph)
                            .orElse(0);

                    return new LineResponseModels.LineEquipmentResponse(
                            equipment.getEquipmentId(),
                            equipment.getEquipmentCode(),
                            equipment.getEquipmentName(),
                            equipment.getEquipmentType(),
                            equipment.getCurrentStatus(),
                            equipmentProduction,
                            latestUph,
                            equipmentUptimeMap.getOrDefault(equipment.getEquipmentId(), 0.0),
                            equipment.getLastStatusChangedAt(),
                            equipment.getUpdatedAt()
                    );
                })
                .toList();

        return new LineResponseModels.LineDetailResponse(
                line.getLineId(),
                line.getLineCode(),
                line.getLineName(),
                line.getProductName(),
                line.getCurrentStatus(),
                line.getLocation(),
                line.getTargetProduction(),
                new LineResponseModels.LineKpiResponse(
                        production,
                        line.getTargetProduction(),
                        achievementRate,
                        uptime,
                        defectRate
                ),
                equipmentResponses,
                mapAlarms(alarmHistoryMapper.selectRecentByLineId(lineId), List.of(line), equipments),
                now
        );
    }

    public EquipmentResponseModels.EquipmentDetailResponse getEquipmentDetail(Long equipmentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();

        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BusinessException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        List<ProductionRecord> equipmentRecords = productionRecordMapper.selectByEquipmentSince(equipmentId, dayStart);
        List<EquipmentStatusHistory> histories = equipmentStatusHistoryMapper.selectIntersecting(dayStart).stream()
                .filter(history -> equipmentId.equals(history.getEquipmentId()))
                .toList();
        double uptime = calculateSingleEquipmentUptime(histories, dayStart, now);
        int production = equipmentRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
        int latestUph = equipmentRecords.stream()
                .max(Comparator.comparing(ProductionRecord::getRecordTime))
                .map(ProductionRecord::getUph)
                .orElse(0);

        ProductionLine line = productionLineMapper.selectById(equipment.getLineId());

        return new EquipmentResponseModels.EquipmentDetailResponse(
                equipment.getEquipmentId(),
                equipment.getLineId(),
                line == null ? null : line.getLineName(),
                equipment.getEquipmentCode(),
                equipment.getEquipmentName(),
                equipment.getEquipmentType(),
                equipment.getCurrentStatus(),
                equipment.getProcessOrder(),
                equipment.getLastStatusChangedAt(),
                equipment.getLastInspectionAt(),
                new EquipmentResponseModels.EquipmentKpiResponse(
                        production,
                        latestUph,
                        uptime
                ),
                mapAlarms(
                        alarmHistoryMapper.selectRecentByEquipmentId(equipmentId),
                        line == null ? List.of() : List.of(line),
                        List.of(equipment)
                ),
                now
        );
    }

    private DashboardResponseModels.DashboardKpiResponse buildDashboardKpis(
            List<ProductionLine> lines,
            List<Equipment> equipments,
            List<ProductionRecord> records,
            Map<Long, Double> equipmentUptimeMap
    ) {
        int totalProduction = records.stream().mapToInt(ProductionRecord::getProductionCount).sum();
        int totalTarget = lines.stream()
                .map(ProductionLine::getTargetProduction)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int totalDefects = records.stream().mapToInt(ProductionRecord::getDefectCount).sum();

        return new DashboardResponseModels.DashboardKpiResponse(
                totalProduction,
                totalTarget,
                totalTarget == 0 ? 0.0 : round1((double) totalProduction / totalTarget * 100.0),
                round1(equipments.stream()
                        .mapToDouble(equipment -> equipmentUptimeMap.getOrDefault(equipment.getEquipmentId(), 0.0))
                        .average()
                        .orElse(0.0)),
                totalProduction == 0 ? 0.0 : round2((double) totalDefects / totalProduction * 100.0)
        );
    }

    private List<DashboardResponseModels.ProductionTrendPointResponse> buildTrend(
            List<ProductionRecord> records,
            List<ProductionLine> lines,
            LocalDateTime trendStart,
            LocalDateTime now
    ) {
        LocalDateTime bucket = trendStart.truncatedTo(ChronoUnit.HOURS);
        Map<LocalDateTime, Integer> productionByHour = records.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getRecordTime().truncatedTo(ChronoUnit.HOURS),
                        LinkedHashMap::new,
                        Collectors.summingInt(ProductionRecord::getProductionCount)
                ));

        int hourlyTarget = lines.stream()
                .map(ProductionLine::getTargetProduction)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum() / 24;

        List<DashboardResponseModels.ProductionTrendPointResponse> trend = new ArrayList<>();
        while (!bucket.isAfter(now.truncatedTo(ChronoUnit.HOURS))) {
            trend.add(new DashboardResponseModels.ProductionTrendPointResponse(
                    bucket.toLocalTime().truncatedTo(ChronoUnit.HOURS).toString(),
                    productionByHour.getOrDefault(bucket, 0),
                    hourlyTarget
            ));
            bucket = bucket.plusHours(1);
        }

        return trend;
    }

    private Map<Long, Double> calculateEquipmentUptime(
            List<Equipment> equipments,
            Map<Long, List<EquipmentStatusHistory>> historiesByEquipment,
            LocalDateTime windowStart,
            LocalDateTime now
    ) {
        Map<Long, Double> uptimeMap = new HashMap<>();
        for (Equipment equipment : equipments) {
            List<EquipmentStatusHistory> equipmentHistories = historiesByEquipment.getOrDefault(equipment.getEquipmentId(), List.of());
            uptimeMap.put(equipment.getEquipmentId(), calculateSingleEquipmentUptime(equipmentHistories, windowStart, now));
        }
        return uptimeMap;
    }

    private double calculateSingleEquipmentUptime(
            List<EquipmentStatusHistory> histories,
            LocalDateTime windowStart,
            LocalDateTime now
    ) {
        long totalWindowSeconds = Math.max(1L, Duration.between(windowStart, now).getSeconds());
        long runSeconds = 0L;

        for (EquipmentStatusHistory history : histories) {
            LocalDateTime effectiveStart = history.getStartedAt().isBefore(windowStart) ? windowStart : history.getStartedAt();
            LocalDateTime effectiveEnd = history.getEndedAt() == null || history.getEndedAt().isAfter(now)
                    ? now
                    : history.getEndedAt();

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            if ("RUN".equals(history.getStatus())) {
                runSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
            }
        }

        return round1((double) runSeconds / totalWindowSeconds * 100.0);
    }

    private List<DashboardResponseModels.AlarmResponse> mapAlarms(
            List<AlarmHistory> alarms,
            List<ProductionLine> lines,
            List<Equipment> equipments
    ) {
        Map<Long, String> lineNames = lines.stream()
                .collect(Collectors.toMap(ProductionLine::getLineId, ProductionLine::getLineName, (left, right) -> left));
        Map<Long, String> equipmentNames = equipments.stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, Equipment::getEquipmentName, (left, right) -> left));

        return alarms.stream()
                .map(alarm -> new DashboardResponseModels.AlarmResponse(
                        alarm.getAlarmId(),
                        alarm.getLineId(),
                        lineNames.getOrDefault(alarm.getLineId(), null),
                        alarm.getEquipmentId(),
                        alarm.getEquipmentId() == null ? null : equipmentNames.getOrDefault(alarm.getEquipmentId(), null),
                        alarm.getAlarmType(),
                        alarm.getSeverity(),
                        alarm.getMessage(),
                        Boolean.TRUE.equals(alarm.getAcknowledged()),
                        alarm.getCreatedAt()
                ))
                .toList();
    }

    private int countByStatus(List<Equipment> equipments, String status) {
        return (int) equipments.stream()
                .filter(equipment -> status.equals(equipment.getCurrentStatus()))
                .count();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
