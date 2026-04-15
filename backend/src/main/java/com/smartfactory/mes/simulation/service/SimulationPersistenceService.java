package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.persistence.mapper.AlarmHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentMapper;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentStatusHistoryMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionLineMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static com.smartfactory.mes.simulation.service.SimulationTickModels.SimulationTickResult;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationPersistenceService {

    private final ProductionLineMapper productionLineMapper;
    private final EquipmentMapper equipmentMapper;
    private final ProductionRecordMapper productionRecordMapper;
    private final EquipmentStatusHistoryMapper equipmentStatusHistoryMapper;
    private final AlarmHistoryMapper alarmHistoryMapper;

    @Transactional
    public void seedLines(List<ProductionLine> lines) {
        lines.forEach(productionLineMapper::insert);
    }

    @Transactional
    public void seedEquipments(List<Equipment> equipments) {
        equipments.forEach(equipmentMapper::insert);
    }

    @Transactional
    public void ensureOpenStatusHistories(List<Equipment> equipments) {
        for (Equipment equipment : equipments) {
            if (equipmentStatusHistoryMapper.countOpenHistory(equipment.getEquipmentId()) == 0) {
                equipmentStatusHistoryMapper.insert(EquipmentStatusHistory.builder()
                        .equipmentId(equipment.getEquipmentId())
                        .status(equipment.getCurrentStatus())
                        .startedAt(equipment.getLastStatusChangedAt())
                        .endedAt(null)
                        .durationSeconds(null)
                        .createdAt(equipment.getCreatedAt())
                        .build());
            }
        }
    }

    @Transactional
    public void persistBootstrapRecords(List<ProductionRecord> records) {
        records.forEach(productionRecordMapper::insert);
    }

    @Transactional
    public void persistTick(SimulationTickResult tickResult) {
        tickResult.equipmentUpdates().forEach(update ->
                equipmentMapper.updateCurrentState(
                        update.equipmentId(),
                        update.status().name(),
                        update.lastStatusChangedAt(),
                        update.updatedAt()
                )
        );

        tickResult.lineUpdates().forEach(update ->
                productionLineMapper.updateCurrentStatus(
                        update.lineId(),
                        update.status().name(),
                        update.updatedAt()
                )
        );

        tickResult.productionRecords().forEach(productionRecordMapper::insert);
        closeAndOpenStatusHistories(tickResult);
        tickResult.alarms().forEach(alarmHistoryMapper::insert);
    }

    @Transactional
    public void resetSimulationData() {
        alarmHistoryMapper.deleteAll();
        equipmentStatusHistoryMapper.deleteAll();
        productionRecordMapper.deleteAll();
        equipmentMapper.deleteAll();
        productionLineMapper.deleteAll();
    }

    public long countLines() {
        return productionLineMapper.countAll();
    }

    public long countEquipments() {
        return equipmentMapper.countAll();
    }

    public long countProductionRecords() {
        return productionRecordMapper.countAll();
    }

    private void closeAndOpenStatusHistories(SimulationTickResult tickResult) {
        tickResult.statusTransitions().forEach(transition -> {
            int durationSeconds = (int) Math.max(
                    0L,
                    Duration.between(transition.previousStartedAt(), transition.transitionAt()).getSeconds()
            );

            equipmentStatusHistoryMapper.closeOpenHistory(
                    transition.equipmentId(),
                    transition.transitionAt(),
                    durationSeconds
            );

            equipmentStatusHistoryMapper.insert(EquipmentStatusHistory.builder()
                    .equipmentId(transition.equipmentId())
                    .status(transition.nextStatus().name())
                    .startedAt(transition.transitionAt())
                    .endedAt(null)
                    .durationSeconds(null)
                    .createdAt(transition.transitionAt())
                    .build());
        });
    }
}
