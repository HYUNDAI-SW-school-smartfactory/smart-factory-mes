package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.dto.line.LineResponseModels;
import com.smartfactory.mes.simulation.persistence.mapper.AlarmHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentStatusHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionLineMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class LineQueryService {

    private final ProductionLineMapper productionLineMapper;
    private final EquipmentMapper equipmentMapper;
    private final ProductionRecordMapper productionRecordMapper;
    private final EquipmentStatusHistoryMapper equipmentStatusHistoryMapper;
    private final AlarmHistoryMapper alarmHistoryMapper;
    private final SimulationQuerySupport simulationQuerySupport;

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
        Map<Long, Double> equipmentUptimeMap =
                simulationQuerySupport.calculateEquipmentUptime(equipments, historiesByEquipment, dayStart, now);

        int production = lineRecords.stream().mapToInt(ProductionRecord::getProductionCount).sum();
        int defects = lineRecords.stream().mapToInt(ProductionRecord::getDefectCount).sum();
        double uptime = equipments.isEmpty()
                ? 0.0
                : simulationQuerySupport.round1(
                equipmentUptimeMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0)
        );
        double achievementRate = line.getTargetProduction() == null || line.getTargetProduction() == 0
                ? 0.0
                : simulationQuerySupport.round1((double) production / line.getTargetProduction() * 100.0);
        double defectRate = production == 0
                ? 0.0
                : simulationQuerySupport.round2((double) defects / production * 100.0);

        List<LineResponseModels.LineEquipmentResponse> equipmentResponses = equipments.stream()
                .map(equipment -> {
                    List<ProductionRecord> equipmentRecords = recordsByEquipment.getOrDefault(
                            equipment.getEquipmentId(),
                            List.of()
                    );
                    int equipmentProduction = equipmentRecords.stream()
                            .mapToInt(ProductionRecord::getProductionCount)
                            .sum();
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
                simulationQuerySupport.mapAlarms(alarmHistoryMapper.selectRecentByLineId(lineId), List.of(line), equipments),
                now
        );
    }
}
