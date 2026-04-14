package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.AlarmHistory;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentStatusHistory;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.dto.dashboard.DashboardResponseModels;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class SimulationQuerySupport {

    Map<Long, Double> calculateEquipmentUptime(
            List<Equipment> equipments,
            Map<Long, List<EquipmentStatusHistory>> historiesByEquipment,
            LocalDateTime windowStart,
            LocalDateTime now
    ) {
        Map<Long, Double> uptimeMap = new HashMap<>();
        for (Equipment equipment : equipments) {
            List<EquipmentStatusHistory> equipmentHistories =
                    historiesByEquipment.getOrDefault(equipment.getEquipmentId(), List.of());
            uptimeMap.put(
                    equipment.getEquipmentId(),
                    calculateSingleEquipmentUptime(equipmentHistories, windowStart, now)
            );
        }
        return uptimeMap;
    }

    double calculateSingleEquipmentUptime(
            List<EquipmentStatusHistory> histories,
            LocalDateTime windowStart,
            LocalDateTime now
    ) {
        long totalWindowSeconds = Math.max(1L, Duration.between(windowStart, now).getSeconds());
        long runSeconds = 0L;

        for (EquipmentStatusHistory history : histories) {
            LocalDateTime effectiveStart = history.getStartedAt().isBefore(windowStart)
                    ? windowStart
                    : history.getStartedAt();
            LocalDateTime effectiveEnd = history.getEndedAt() == null || history.getEndedAt().isAfter(now)
                    ? now
                    : history.getEndedAt();

            if (!effectiveEnd.isAfter(effectiveStart)) {
                continue;
            }

            if ("RUN".equals(history.getStatus())) {
                runSeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
            }
        }

        return round1((double) runSeconds / totalWindowSeconds * 100.0);
    }

    List<DashboardResponseModels.AlarmResponse> mapAlarms(
            List<AlarmHistory> alarms,
            List<ProductionLine> lines,
            List<Equipment> equipments
    ) {
        Map<Long, String> lineNames = lines.stream()
                .collect(Collectors.toMap(ProductionLine::getLineId, ProductionLine::getLineName, (left, right) -> left));
        Map<Long, String> equipmentNames = equipments.stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, Equipment::getEquipmentName, (left, right) -> left));

        return alarms.stream()
                .map(alarm -> new DashboardResponseModels.AlarmResponse(
                        alarm.getAlarmId(),
                        alarm.getLineId(),
                        lineNames.getOrDefault(alarm.getLineId(), null),
                        alarm.getEquipmentId(),
                        alarm.getEquipmentId() == null ? null : equipmentNames.getOrDefault(alarm.getEquipmentId(), null),
                        alarm.getAlarmType(),
                        alarm.getSeverity(),
                        alarm.getMessage(),
                        Boolean.TRUE.equals(alarm.getAcknowledged()),
                        alarm.getCreatedAt()
                ))
                .toList();
    }

    int countByStatus(List<Equipment> equipments, String status) {
        return (int) equipments.stream()
                .filter(equipment -> status.equals(equipment.getCurrentStatus()))
                .count();
    }

    double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
