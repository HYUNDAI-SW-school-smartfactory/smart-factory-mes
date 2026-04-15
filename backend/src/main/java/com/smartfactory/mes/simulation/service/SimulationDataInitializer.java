package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.config.SimulationProperties;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.enums.EquipmentStatus;
import com.smartfactory.mes.simulation.domain.enums.EquipmentType;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentMapper;
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
        LocalDateTime now = LocalDateTime.now().minusMinutes(40);
        List<ProductionLine> lines = new ArrayList<>();

        lines.add(ProductionLine.builder()
                .lineId(1L)
                .lineCode("LINE-PRESS-01")
                .lineName("Roof Panel Line 1")
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
                .lineName("Trunk Panel Line 1")
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
                .lineName("Front Left Door Panel Line 1")
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

    private ProductionLine buildLine(
            Long lineId,
            String lineCode,
            String lineName,
            String productName,
            int targetProduction,
            String location,
            LocalDateTime createdAt
    ) {
        return ProductionLine.builder()
                .lineId(lineId)
                .lineCode(lineCode)
                .lineName(lineName)
                .productName(productName)
                .currentStatus(EquipmentStatus.RUN.name())
                .targetProduction(targetProduction)
                .location(location)
                .isActive(true)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private List<Equipment> buildEquipments() {
        LocalDateTime now = LocalDateTime.now();
        List<Equipment> equipments = new ArrayList<>();

        buildPressLineEquipments(
                equipments,
                1L,
                100L,
                "DOOR",
                now.minusMinutes(32)
        );
        buildPressLineEquipments(
                equipments,
                2L,
                200L,
                "LOOP",
                now.minusMinutes(28)
        );
        buildPressLineEquipments(
                equipments,
                3L,
                300L,
                "TRUNK",
                now.minusMinutes(24)
        );
        buildPressLineEquipments(
                equipments,
                4L,
                400L,
                "HOOD",
                now.minusMinutes(20)
        );

        return equipments;
    }

    private void buildPressLineEquipments(
            List<Equipment> equipments,
            Long lineId,
            Long baseEquipmentId,
            String lineCodePrefix,
            LocalDateTime baseTime
    ) {
        equipments.add(buildEquipment(baseEquipmentId + 1, lineId, "EQ-" + lineCodePrefix + "-01", "Coil Feeder", EquipmentType.COIL, 1, baseTime.plusMinutes(1), baseTime.minusDays(2)));
        equipments.add(buildEquipment(baseEquipmentId + 2, lineId, "EQ-" + lineCodePrefix + "-02", "Press Machine 1", EquipmentType.PRESS, 2, baseTime.plusMinutes(2), baseTime.minusDays(2)));
        equipments.add(buildEquipment(baseEquipmentId + 3, lineId, "EQ-" + lineCodePrefix + "-03", "Press Machine 2", EquipmentType.PRESS, 3, baseTime.plusMinutes(3), baseTime.minusDays(3)));
        equipments.add(buildEquipment(baseEquipmentId + 4, lineId, "EQ-" + lineCodePrefix + "-04", "Press Machine 3", EquipmentType.PRESS, 4, baseTime.plusMinutes(4), baseTime.minusDays(3)));
        equipments.add(buildEquipment(baseEquipmentId + 5, lineId, "EQ-" + lineCodePrefix + "-05", "Trim Machine", EquipmentType.TRIM, 5, baseTime.plusMinutes(5), baseTime.minusDays(4)));
        equipments.add(buildEquipment(baseEquipmentId + 6, lineId, "EQ-" + lineCodePrefix + "-06", "Vision Inspector", EquipmentType.INSPECTOR, 6, baseTime.plusMinutes(6), baseTime.minusDays(1)));
        equipments.add(buildEquipment(baseEquipmentId + 7, lineId, "EQ-" + lineCodePrefix + "-07", "Transfer Robot", EquipmentType.ROBOT, 7, baseTime.plusMinutes(7), baseTime.minusDays(2)));
    }

    private Equipment buildEquipment(
            Long equipmentId,
            Long lineId,
            String equipmentCode,
            String equipmentName,
            EquipmentType equipmentType,
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
                .currentStatus(EquipmentStatus.RUN.name())
                .processOrder(processOrder)
                .lastStatusChangedAt(lastStatusChangedAt)
                .lastInspectionAt(lastInspectionAt)
                .isActive(true)
                .createdAt(lastStatusChangedAt.minusMinutes(50))
                .updatedAt(lastStatusChangedAt)
                .build();
    }
}
