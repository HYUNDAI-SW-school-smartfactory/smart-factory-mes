package com.smartfactory.mes.simulation.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class EquipmentResponseModels {

    private EquipmentResponseModels() {
    }

    public record EquipmentDetailResponse(
            Long equipmentId,
            Long lineId,
            String lineName,
            String equipmentCode,
            String equipmentName,
            String equipmentType,
            String currentStatus,
            int processOrder,
            LocalDateTime lastStatusChangedAt,
            LocalDateTime lastInspectionAt,
            EquipmentKpiResponse kpis,
            List<DashboardResponseModels.AlarmResponse> alarms,
            LocalDateTime generatedAt
    ) {
    }

    public record EquipmentKpiResponse(
            int production,
            int uph,
            double uptime
    ) {
    }
}
