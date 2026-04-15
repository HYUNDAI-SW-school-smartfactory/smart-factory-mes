package com.smartfactory.mes.simulation.domain;

import com.smartfactory.mes.simulation.domain.enums.EquipmentStatus;
import com.smartfactory.mes.simulation.domain.enums.EquipmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRuntimeState {

    private Long equipmentId;
    private Long lineId;
    private String equipmentCode;
    private String equipmentName;
    private EquipmentType equipmentType;
    private EquipmentStatus currentStatus;
    private int processOrder;
    private int baseUph;
    private double failureBias;
    private double defectBias;
    private int minRunTicks;
    private int minStopTicks;
    private int minIdleTicks;
    private int minMaintenanceTicks;
    private int ticksInCurrentStatus;
    private double productionCarry;
    private boolean blockedByUpstream;
    private LocalDateTime statusStartedAt;
    private LocalDateTime lastStatusChangedAt;
    private LocalDateTime lastInspectionAt;

    public EquipmentRuntimeState copy() {
        return EquipmentRuntimeState.builder()
                .equipmentId(equipmentId)
                .lineId(lineId)
                .equipmentCode(equipmentCode)
                .equipmentName(equipmentName)
                .equipmentType(equipmentType)
                .currentStatus(currentStatus)
                .processOrder(processOrder)
                .baseUph(baseUph)
                .failureBias(failureBias)
                .defectBias(defectBias)
                .minRunTicks(minRunTicks)
                .minStopTicks(minStopTicks)
                .minIdleTicks(minIdleTicks)
                .minMaintenanceTicks(minMaintenanceTicks)
                .ticksInCurrentStatus(ticksInCurrentStatus)
                .productionCarry(productionCarry)
                .blockedByUpstream(blockedByUpstream)
                .statusStartedAt(statusStartedAt)
                .lastStatusChangedAt(lastStatusChangedAt)
                .lastInspectionAt(lastInspectionAt)
                .build();
    }
}
