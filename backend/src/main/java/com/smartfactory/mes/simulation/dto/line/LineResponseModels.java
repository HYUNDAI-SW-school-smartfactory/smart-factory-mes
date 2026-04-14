package com.smartfactory.mes.simulation.dto.line;

import com.smartfactory.mes.simulation.dto.dashboard.DashboardResponseModels;

import java.time.LocalDateTime;
import java.util.List;

public final class LineResponseModels {

    private LineResponseModels() {
    }

    public record LineDetailResponse(
            Long lineId,
            String lineCode,
            String lineName,
            String productName,
            String currentStatus,
            String location,
            int targetProduction,
            LineKpiResponse kpis,
            List<LineEquipmentResponse> equipments,
            List<DashboardResponseModels.AlarmResponse> alarms,
            LocalDateTime generatedAt
    ) {
    }

    public record LineKpiResponse(
            int production,
            int targetProduction,
            double achievementRate,
            double uptime,
            double defectRate
    ) {
    }

    public record LineEquipmentResponse(
            Long equipmentId,
            String equipmentCode,
            String equipmentName,
            String equipmentType,
            String currentStatus,
            int production,
            int uph,
            double uptime,
            LocalDateTime lastStatusChangedAt,
            LocalDateTime updatedAt
    ) {
    }
}
