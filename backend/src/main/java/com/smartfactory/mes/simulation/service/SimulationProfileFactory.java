package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.enums.EquipmentStatus;
import com.smartfactory.mes.simulation.domain.enums.EquipmentType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class SimulationProfileFactory {

    public EquipmentRuntimeState createRuntimeState(Equipment equipment) {
        EquipmentType equipmentType = EquipmentType.valueOf(equipment.getEquipmentType());
        EquipmentStatus currentStatus = EquipmentStatus.valueOf(equipment.getCurrentStatus());
        LocalDateTime statusStartedAt = equipment.getLastStatusChangedAt() != null
                ? equipment.getLastStatusChangedAt()
                : LocalDateTime.now().minusMinutes(20L + equipment.getEquipmentId());

        return EquipmentRuntimeState.builder()
                .equipmentId(equipment.getEquipmentId())
                .lineId(equipment.getLineId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .equipmentType(equipmentType)
                .currentStatus(currentStatus)
                .processOrder(equipment.getProcessOrder() == null ? 0 : equipment.getProcessOrder())
                .baseUph(resolveBaseUph(equipmentType, equipment.getProcessOrder()))
                .failureBias(resolveFailureBias(equipmentType))
                .defectBias(resolveDefectBias(equipmentType))
                .minRunTicks(16 + ((equipment.getProcessOrder() == null ? 1 : equipment.getProcessOrder()) * 3))
                .minStopTicks(8 + (equipment.getProcessOrder() == null ? 0 : equipment.getProcessOrder() % 3))
                .minIdleTicks(4)
                .minMaintenanceTicks(24)
                .ticksInCurrentStatus(resolveTicksInCurrentStatus(statusStartedAt))
                .productionCarry(0.0)
                .blockedByUpstream(false)
                .statusStartedAt(statusStartedAt)
                .lastStatusChangedAt(statusStartedAt)
                .lastInspectionAt(equipment.getLastInspectionAt())
                .build();
    }

    private int resolveBaseUph(EquipmentType equipmentType, Integer processOrder) {
        int order = processOrder == null ? 1 : processOrder;

        return switch (equipmentType) {
            case COIL -> 540 - (order * 8);
            case PRESS -> 500 - (order * 6);
            case TRIM -> 470;
            case ROBOT -> 455;
            case CONVEYOR -> 420;
            case PACKER -> 300;
            case LABELER -> 280;
            case PALLETIZER -> 250;
            case INSPECTOR -> 390;
        };
    }

    private double resolveFailureBias(EquipmentType equipmentType) {
        return switch (equipmentType) {
            case COIL -> 0.0024;
            case PRESS -> 0.0032;
            case TRIM -> 0.0036;
            case ROBOT -> 0.0026;
            case CONVEYOR -> 0.0024;
            case PACKER -> 0.0020;
            case LABELER -> 0.0020;
            case PALLETIZER -> 0.0020;
            case INSPECTOR -> 0.0018;
        };
    }

    private double resolveDefectBias(EquipmentType equipmentType) {
        return switch (equipmentType) {
            case COIL -> 0.004;
            case PRESS -> 0.012;
            case TRIM -> 0.010;
            case ROBOT -> 0.004;
            case CONVEYOR -> 0.003;
            case PACKER -> 0.003;
            case LABELER -> 0.003;
            case PALLETIZER -> 0.002;
            case INSPECTOR -> 0.006;
        };
    }

    private int resolveTicksInCurrentStatus(LocalDateTime statusStartedAt) {
        long seconds = Math.max(0L, Duration.between(statusStartedAt, LocalDateTime.now()).getSeconds());
        return (int) (seconds / 5L);
    }
}
