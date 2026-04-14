package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.AlarmHistory;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.domain.element.EquipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class SimulationTickModels {

    private SimulationTickModels() {
    }

    public record SimulationTickResult(
            List<EquipmentRuntimeState> nextStates,
            List<CurrentEquipmentStateUpdate> equipmentUpdates,
            List<CurrentLineStateUpdate> lineUpdates,
            List<ProductionRecord> productionRecords,
            List<StatusTransition> statusTransitions,
            List<AlarmHistory> alarms
    ) {
    }

    public record CurrentEquipmentStateUpdate(
            Long equipmentId,
            EquipmentStatus status,
            LocalDateTime lastStatusChangedAt,
            LocalDateTime updatedAt
    ) {
    }

    public record CurrentLineStateUpdate(
            Long lineId,
            EquipmentStatus status,
            LocalDateTime updatedAt
    ) {
    }

    public record StatusTransition(
            Long equipmentId,
            EquipmentStatus previousStatus,
            EquipmentStatus nextStatus,
            LocalDateTime previousStartedAt,
            LocalDateTime transitionAt
    ) {
    }
}
