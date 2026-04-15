package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.config.SimulationProperties;
import com.smartfactory.mes.simulation.domain.Equipment;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.ProductionLine;
import com.smartfactory.mes.simulation.domain.enums.EquipmentStatus;
import com.smartfactory.mes.simulation.domain.enums.EquipmentType;
import com.smartfactory.mes.simulation.persistence.mapper.EquipmentMapper;
import com.smartfactory.mes.simulation.persistence.mapper.ProductionLineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnProperty(prefix = "mes.simulation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SimulationDataInitializer implements ApplicationRunner {

    private final EquipmentMapper equipmentMapper;
    private final ProductionLineMapper productionLineMapper;
    private final SimulationPersistenceService simulationPersistenceService;
    private final SimulationProfileFactory simulationProfileFactory;
    private final SimulationStateStore simulationStateStore;
    private final SimulationEngine simulationEngine;
    private final SimulationRealtimeSnapshotService simulationRealtimeSnapshotService;
    private final SimulationProperties simulationProperties;

    @Override
    public void run(ApplicationArguments args) {
        List<ProductionLine> expectedLines = buildLines();
        List<Equipment> expectedEquipments = buildEquipments();

        List<ProductionLine> currentLines = productionLineMapper.selectActiveLines();
        List<Equipment> currentEquipments = equipmentMapper.selectActiveEquipments();

        if (requiresReseed(currentLines, currentEquipments, expectedLines, expectedEquipments)) {
            simulationPersistenceService.resetSimulationData();
            simulationPersistenceService.seedLines(expectedLines);
            simulationPersistenceService.seedEquipments(expectedEquipments);
        } else {
            if (currentLines.isEmpty()) {
                simulationPersistenceService.seedLines(expectedLines);
            }

            if (currentEquipments.isEmpty()) {
                simulationPersistenceService.seedEquipments(expectedEquipments);
            }
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

    private boolean requiresReseed(
            List<ProductionLine> currentLines,
            List<Equipment> currentEquipments,
            List<ProductionLine> expectedLines,
            List<Equipment> expectedEquipments
    ) {
        if (currentLines.isEmpty() && currentEquipments.isEmpty()) {
            return false;
        }

        if (currentLines.size() != expectedLines.size() || currentEquipments.size() != expectedEquipments.size()) {
            return true;
        }

        Set<String> currentLineCodes = currentLines.stream()
                .sorted(Comparator.comparing(ProductionLine::getLineId))
                .map(ProductionLine::getLineCode)
                .collect(Collectors.toSet());
        Set<String> expectedLineCodes = expectedLines.stream()
                .map(ProductionLine::getLineCode)
                .collect(Collectors.toSet());

        if (!currentLineCodes.equals(expectedLineCodes)) {
            return true;
        }

        Set<String> currentEquipmentCodes = currentEquipments.stream()
                .map(Equipment::getEquipmentCode)
                .collect(Collectors.toSet());
        Set<String> expectedEquipmentCodes = expectedEquipments.stream()
                .map(Equipment::getEquipmentCode)
                .collect(Collectors.toSet());

        return !currentEquipmentCodes.equals(expectedEquipmentCodes);
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
        return List.of(
                buildLine(1L, "LINE-PRESS-DOOR", "Press - Door Line", "Door Outer Panel", 6400, "Press Shop - Bay 1", now),
                buildLine(2L, "LINE-PRESS-LOOP", "Press - Loop Line", "Loop Reinforcement Panel", 6200, "Press Shop - Bay 2", now.plusMinutes(2)),
                buildLine(3L, "LINE-PRESS-TRUNK", "Press - Trunk Line", "Trunk Lid Panel", 6100, "Press Shop - Bay 3", now.plusMinutes(4)),
                buildLine(4L, "LINE-PRESS-HOOD", "Press - Hood Line", "Hood Outer Panel", 6300, "Press Shop - Bay 4", now.plusMinutes(6))
        );
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
