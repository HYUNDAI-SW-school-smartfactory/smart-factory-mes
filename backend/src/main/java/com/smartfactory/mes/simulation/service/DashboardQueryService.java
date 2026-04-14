package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.config.SimulationProperties;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.dto.dashboard.DashboardResponseModels;
import com.smartfactory.mes.simulation.persistence.mapper.AlarmHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentStatusHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionLineMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class DashboardQueryService {

    private final ProductionLineMapper productionLineMapper;
    private final EquipmentMapper equipmentMapper;
    private final ProductionRecordMapper productionRecordMapper;
    private final EquipmentStatusHistoryMapper equipmentStatusHistoryMapper;
    private final AlarmHistoryMapper alarmHistoryMapper;
    private final SimulationProperties simulationProperties;
    private final SimulationQuerySupport simulationQuerySupport;

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

        Map<Long, Double> equipmentUptimeMap =
                simulationQuerySupport.calculateEquipmentUptime(equipments, historiesByEquipment, dayStart, now);

        List<DashboardResponseModels.LineStatusResponse> lineResponses = new ArrayList<>();
        for (ProductionLine line : lines) {
            List<ProductionRecord> lineRecords = recordsByLine.getOrDefault(line.getLineId(), List.of());
            List<Equipment> lineEquipments = equipmentsByLine.getOrDefault(line.getLineId(), List.of());
            double uptime = lineEquipments.isEmpty()
                    ? 0.0
                    : simulationQuerySupport.round1(lineEquipments.stream()
                    .mapToDouble(equipment -> equipmentUptimeMap.getOrDefault(equipment.getEquipmentId(), 0.0))
                    .average()
                    .orElse(0.0));
            int production = lineRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
            int defects = lineRecords.stream().mapToInt(ProductionRecord::getDefectCount).sum();
            double achievementRate = line.getTargetProduction() == null || line.getTargetProduction() == 0
                    ? 0.0
                    : simulationQuerySupport.round1((double) production / line.getTargetProduction() * 100.0);
            double defectRate = production == 0
                    ? 0.0
                    : simulationQuerySupport.round2((double) defects / production * 100.0);

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

        return new DashboardResponseModels.DashboardSnapshotResponse(
                buildDashboardKpis(lines, equipments, productionRecords, equipmentUptimeMap),
                buildTrend(productionRecordMapper.selectSince(trendStart), lines, trendStart, now),
                lineResponses,
                new DashboardResponseModels.EquipmentSummaryResponse(
                        equipments.size(),
                        simulationQuerySupport.countByStatus(equipments, "RUN"),
                        simulationQuerySupport.countByStatus(equipments, "STOP"),
                        simulationQuerySupport.countByStatus(equipments, "IDLE"),
                        simulationQuerySupport.countByStatus(equipments, "ERROR"),
                        simulationQuerySupport.countByStatus(equipments, "MAINTENANCE")
                ),
                simulationQuerySupport.mapAlarms(alarmHistoryMapper.selectRecentDashboardAlarms(), lines, equipments),
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
                totalTarget == 0 ? 0.0 : simulationQuerySupport.round1((double) totalProduction / totalTarget * 100.0),
                simulationQuerySupport.round1(equipments.stream()
                        .mapToDouble(equipment -> equipmentUptimeMap.getOrDefault(equipment.getEquipmentId(), 0.0))
                        .average()
                        .orElse(0.0)),
                totalProduction == 0 ? 0.0 : simulationQuerySupport.round2((double) totalDefects / totalProduction * 100.0)
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
}
