package com.smartfactory.mes.simulation.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class DashboardResponseModels {

    private DashboardResponseModels() {
    }

    public record DashboardSnapshotResponse(
            DashboardKpiResponse kpis,
            List<ProductionTrendPointResponse> productionTrend,
            List<LineStatusResponse> lines,
            EquipmentSummaryResponse equipmentSummary,
            List<AlarmResponse> recentAlarms,
            LocalDateTime generatedAt
    ) {
    }

    public record DashboardKpiResponse(
            int totalProduction,
            int totalTarget,
            double achievementRate,
            double avgUptime,
            double avgDefectRate
    ) {
    }

    public record ProductionTrendPointResponse(
            String time,
            int production,
            int target
    ) {
    }

    public record LineStatusResponse(
            Long lineId,
            String lineCode,
            String lineName,
            String productName,
            String currentStatus,
            int production,
            int targetProduction,
            double achievementRate,
            double uptime,
            double defectRate
    ) {
    }

    public record EquipmentSummaryResponse(
            int total,
            int running,
            int stopped,
            int idle,
            int error,
            int maintenance
    ) {
    }

    public record AlarmResponse(
            Long alarmId,
            Long lineId,
            String lineName,
            Long equipmentId,
            String equipmentName,
            String alarmType,
            String severity,
            String message,
            boolean acknowledged,
            LocalDateTime createdAt
    ) {
    }
}
