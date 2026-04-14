package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.global.exception.ErrorCode;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.dto.equipment.EquipmentResponseModels;
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

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class EquipmentQueryService {

    private final ProductionLineMapper productionLineMapper;
    private final EquipmentMapper equipmentMapper;
    private final ProductionRecordMapper productionRecordMapper;
    private final EquipmentStatusHistoryMapper equipmentStatusHistoryMapper;
    private final AlarmHistoryMapper alarmHistoryMapper;
    private final SimulationQuerySupport simulationQuerySupport;

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
        double uptime = simulationQuerySupport.calculateSingleEquipmentUptime(histories, dayStart, now);
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
                simulationQuerySupport.mapAlarms(
                        alarmHistoryMapper.selectRecentByEquipmentId(equipmentId),
                        line == null ? List.of() : List.of(line),
                        List.of(equipment)
                ),
                now
        );
    }
}
