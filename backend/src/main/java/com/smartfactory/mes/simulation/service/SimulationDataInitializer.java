package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.config.SimulationProperties;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.element.EquipmentStatus;
import com.smartfactory.mes.simulation.domain.element.EquipmentType;
import com.smartfactory.mes.simulation.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnProperty(prefix = "mes.simulation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SimulationDataInitializer implements ApplicationRunner {

    private final EquipmentMapper equipmentMapper;
    private final SimulationPersistenceService simulationPersistenceService;
    private final SimulationProfileFactory simulationProfileFactory;
    private final SimulationStateStore simulationStateStore;
    private final SimulationEngine simulationEngine;
    private final SimulationRealtimeSnapshotService simulationRealtimeSnapshotService;
    private final SimulationProperties simulationProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (simulationPersistenceService.countLines() == 0) {
            simulationPersistenceService.seedLines(buildLines());
        }

        if (simulationPersistenceService.countEquipments() == 0) {
            simulationPersistenceService.seedEquipments(buildEquipments());
        }

        List<Equipment> equipments = equipmentMapper.selectActiveEquipments();
        simulationPersistenceService.ensureOpenStatusHistories(equipments);

        List<EquipmentRuntimeState> runtimeStates = equipments.stream()
                .map(simulationProfileFactory::createRuntimeState)
                .toList();

        simulationStateStore.initialize(runtimeStates);

        if (simulationPersistenceService.countProductionRecords() == 0) {
            warmUpRecords();
            simulationRealtimeSnapshotService.initializeFromDatabase();
            simulationStateStore.markReady();
            return;
        }

        simulationRealtimeSnapshotService.initializeFromDatabase();
        simulationStateStore.markReady();
    }

    private void warmUpRecords() {
        LocalDateTime tickTime = LocalDateTime.now().minusSeconds((long) simulationProperties.getBootstrapTicks() * 5L);

        for (int i = 0; i < simulationProperties.getBootstrapTicks(); i++) {
            tickTime = tickTime.plusSeconds(5);
            var result = simulationEngine.bootstrap(simulationStateStore.snapshot(), tickTime);
            simulationPersistenceService.persistBootstrapRecords(result.productionRecords());
            simulationStateStore.replace(result.nextStates());
        }
    }

    private List<ProductionLine> buildLines() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(20);
        List<ProductionLine> lines = new ArrayList<>();

        lines.add(ProductionLine.builder()
                .lineId(1L)
                .lineCode("LINE-PRESS-01")
                .lineName("Press Line 1")
                .productName("Brake Pad Plate")
                .currentStatus(EquipmentStatus.RUN.name())
                .targetProduction(6400)
                .location("Plant A - Zone 1")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        lines.add(ProductionLine.builder()
                .lineId(2L)
                .lineCode("LINE-ASM-01")
                .lineName("Assembly Line 1")
                .productName("Control Module")
                .currentStatus(EquipmentStatus.RUN.name())
                .targetProduction(5200)
                .location("Plant A - Zone 2")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
        lines.add(ProductionLine.builder()
                .lineId(3L)
                .lineCode("LINE-PACK-01")
                .lineName("Packaging Line 1")
                .productName("Finished Goods Pack")
                .currentStatus(EquipmentStatus.RUN.name())
                .targetProduction(4800)
                .location("Plant A - Zone 3")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return lines;
    }

    private List<Equipment> buildEquipments() {
        LocalDateTime now = LocalDateTime.now();
        List<Equipment> equipments = new ArrayList<>();

        equipments.add(buildEquipment(1L, 1L, "EQ-101", "Coil Feeder 1", EquipmentType.COIL, EquipmentStatus.RUN, 1, now.minusMinutes(19), now.minusDays(3)));
        equipments.add(buildEquipment(2L, 1L, "EQ-102", "Press Machine 1", EquipmentType.PRESS, EquipmentStatus.RUN, 2, now.minusMinutes(18), now.minusDays(2)));
        equipments.add(buildEquipment(3L, 1L, "EQ-103", "Press Machine 2", EquipmentType.PRESS, EquipmentStatus.RUN, 3, now.minusMinutes(17), now.minusDays(4)));
        equipments.add(buildEquipment(4L, 1L, "EQ-104", "Transfer Robot 1", EquipmentType.ROBOT, EquipmentStatus.RUN, 4, now.minusMinutes(16), now.minusDays(2)));
        equipments.add(buildEquipment(5L, 1L, "EQ-105", "Vision Inspector 1", EquipmentType.INSPECTOR, EquipmentStatus.IDLE, 5, now.minusMinutes(7), now.minusDays(1)));

        equipments.add(buildEquipment(6L, 2L, "EQ-201", "Loader Robot 1", EquipmentType.ROBOT, EquipmentStatus.RUN, 1, now.minusMinutes(15), now.minusDays(2)));
        equipments.add(buildEquipment(7L, 2L, "EQ-202", "Assembly Robot 1", EquipmentType.ROBOT, EquipmentStatus.RUN, 2, now.minusMinutes(14), now.minusDays(3)));
        equipments.add(buildEquipment(8L, 2L, "EQ-203", "Assembly Robot 2", EquipmentType.ROBOT, EquipmentStatus.RUN, 3, now.minusMinutes(13), now.minusDays(4)));
        equipments.add(buildEquipment(9L, 2L, "EQ-204", "Torque Conveyor 1", EquipmentType.CONVEYOR, EquipmentStatus.RUN, 4, now.minusMinutes(12), now.minusDays(5)));
        equipments.add(buildEquipment(10L, 2L, "EQ-205", "Final Inspector 1", EquipmentType.INSPECTOR, EquipmentStatus.RUN, 5, now.minusMinutes(11), now.minusDays(1)));

        equipments.add(buildEquipment(11L, 3L, "EQ-301", "Case Packer 1", EquipmentType.PACKER, EquipmentStatus.RUN, 1, now.minusMinutes(10), now.minusDays(2)));
        equipments.add(buildEquipment(12L, 3L, "EQ-302", "Labeler 1", EquipmentType.LABELER, EquipmentStatus.RUN, 2, now.minusMinutes(9), now.minusDays(2)));
        equipments.add(buildEquipment(13L, 3L, "EQ-303", "Seal Conveyor 1", EquipmentType.CONVEYOR, EquipmentStatus.RUN, 3, now.minusMinutes(8), now.minusDays(3)));
        equipments.add(buildEquipment(14L, 3L, "EQ-304", "Palletizer 1", EquipmentType.PALLETIZER, EquipmentStatus.IDLE, 4, now.minusMinutes(6), now.minusDays(4)));
        equipments.add(buildEquipment(15L, 3L, "EQ-305", "Final Inspector 2", EquipmentType.INSPECTOR, EquipmentStatus.RUN, 5, now.minusMinutes(5), now.minusDays(1)));

        return equipments;
    }

    private Equipment buildEquipment(
            Long equipmentId,
            Long lineId,
            String equipmentCode,
            String equipmentName,
            EquipmentType equipmentType,
            EquipmentStatus currentStatus,
            int processOrder,
            LocalDateTime lastStatusChangedAt,
            LocalDateTime lastInspectionAt
    ) {
        return Equipment.builder()
                .equipmentId(equipmentId)
                .lineId(lineId)
                .equipmentCode(equipmentCode)
                .equipmentName(equipmentName)
                .equipmentType(equipmentType.name())
                .currentStatus(currentStatus.name())
                .processOrder(processOrder)
                .lastStatusChangedAt(lastStatusChangedAt)
                .lastInspectionAt(lastInspectionAt)
                .isActive(true)
                .createdAt(lastStatusChangedAt.minusMinutes(30))
                .updatedAt(lastStatusChangedAt)
                .build();
    }
}
