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
                : LocalDateTime.now().minusMinutes(5L + equipment.getEquipmentId());

        return EquipmentRuntimeState.builder()
                .equipmentId(equipment.getEquipmentId())
                .lineId(equipment.getLineId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .equipmentType(equipmentType)
                .currentStatus(currentStatus)
                .baseUph(resolveBaseUph(equipmentType, equipment.getProcessOrder()))
                .failureBias(resolveFailureBias(equipmentType))
                .defectBias(resolveDefectBias(equipmentType))
                .minRunTicks(6 + (equipment.getProcessOrder() % 4))
                .minStopTicks(2 + (equipment.getProcessOrder() % 3))
                .minIdleTicks(2)
                .minMaintenanceTicks(4)
                .ticksInCurrentStatus(resolveTicksInCurrentStatus(statusStartedAt))
                .productionCarry(0.0)
                .statusStartedAt(statusStartedAt)
                .lastStatusChangedAt(statusStartedAt)
                .lastInspectionAt(equipment.getLastInspectionAt())
                .build();
    }

    private int resolveBaseUph(EquipmentType equipmentType, Integer processOrder) {
        int step = processOrder == null ? 0 : processOrder * 8;

        return switch (equipmentType) {
            case COIL -> 340 + step;
            case PRESS -> 420 + step;
            case ROBOT -> 380 + step;
            case CONVEYOR -> 360 + step;
            case PACKER -> 300 + step;
            case LABELER -> 280 + step;
            case PALLETIZER -> 250 + step;
            case INSPECTOR -> 220 + step;
        };
    }

    private double resolveFailureBias(EquipmentType equipmentType) {
        return switch (equipmentType) {
            case COIL -> 0.035;
            case PRESS -> 0.030;
            case ROBOT -> 0.028;
            case CONVEYOR -> 0.040;
            case PACKER -> 0.033;
            case LABELER -> 0.025;
            case PALLETIZER -> 0.022;
            case INSPECTOR -> 0.020;
        };
    }

    private double resolveDefectBias(EquipmentType equipmentType) {
        return switch (equipmentType) {
            case COIL, PRESS -> 0.012;
            case ROBOT, CONVEYOR -> 0.010;
            case PACKER, LABELER -> 0.008;
            case PALLETIZER, INSPECTOR -> 0.006;
        };
    }

    private int resolveTicksInCurrentStatus(LocalDateTime statusStartedAt) {
        long seconds = Math.max(0L, Duration.between(statusStartedAt, LocalDateTime.now()).getSeconds());
        return (int) (seconds / 5L);
    }
}
