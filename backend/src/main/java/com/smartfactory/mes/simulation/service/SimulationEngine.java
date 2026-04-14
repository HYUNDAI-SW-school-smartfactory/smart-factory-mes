package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.AlarmHistory;
import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import com.smartfactory.mes.simulation.domain.ProductionRecord;
import com.smartfactory.mes.simulation.domain.enums.AlarmSeverity;
import com.smartfactory.mes.simulation.domain.enums.AlarmType;
import com.smartfactory.mes.simulation.domain.enums.EquipmentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.smartfactory.mes.simulation.service.SimulationTickModels.CurrentEquipmentStateUpdate;
import static com.smartfactory.mes.simulation.service.SimulationTickModels.CurrentLineStateUpdate;
import static com.smartfactory.mes.simulation.service.SimulationTickModels.SimulationTickResult;
import static com.smartfactory.mes.simulation.service.SimulationTickModels.StatusTransition;

@Component
public class SimulationEngine {

    public SimulationTickResult bootstrap(List<EquipmentRuntimeState> currentStates, LocalDateTime tickTime) {
        return advanceInternal(currentStates, tickTime, false);
    }

    public SimulationTickResult advance(List<EquipmentRuntimeState> currentStates, LocalDateTime tickTime) {
        return advanceInternal(currentStates, tickTime, true);
    }

    private SimulationTickResult advanceInternal(
            List<EquipmentRuntimeState> currentStates,
            LocalDateTime tickTime,
            boolean allowTransitions
    ) {
        List<EquipmentRuntimeState> nextStates = new ArrayList<>();
        List<CurrentEquipmentStateUpdate> equipmentUpdates = new ArrayList<>();
        List<ProductionRecord> productionRecords = new ArrayList<>();
        List<StatusTransition> transitions = new ArrayList<>();
        List<AlarmHistory> alarms = new ArrayList<>();

        for (EquipmentRuntimeState state : currentStates) {
            EquipmentRuntimeState nextState = state.copy();
            EquipmentStatus previousStatus = state.getCurrentStatus();
            EquipmentStatus nextStatus = allowTransitions ? resolveNextStatus(state, tickTime) : previousStatus;
            boolean changed = nextStatus != previousStatus;

            if (changed) {
                nextState.setCurrentStatus(nextStatus);
                nextState.setTicksInCurrentStatus(0);
                nextState.setStatusStartedAt(tickTime);
                nextState.setLastStatusChangedAt(tickTime);
                transitions.add(new StatusTransition(
                        state.getEquipmentId(),
                        previousStatus,
                        nextStatus,
                        state.getStatusStartedAt(),
                        tickTime
                ));

                AlarmHistory alarm = createAlarm(nextState, tickTime);
                if (alarm != null) {
                    alarms.add(alarm);
                }
            } else {
                nextState.setTicksInCurrentStatus(state.getTicksInCurrentStatus() + 1);
            }

            ProductionMetrics metrics = resolveMetrics(nextState, tickTime);
            productionRecords.add(metrics.toRecord(nextState, tickTime));

            if (metrics.defectCount() >= 2) {
                alarms.add(AlarmHistory.builder()
                        .lineId(nextState.getLineId())
                        .equipmentId(nextState.getEquipmentId())
                        .alarmType(AlarmType.DEFECT.name())
                        .severity(AlarmSeverity.MEDIUM.name())
                        .message(nextState.getEquipmentName() + " defect count is higher than the normal range.")
                        .acknowledged(false)
                        .createdAt(tickTime)
                        .build());
            }

            nextStates.add(nextState);
            equipmentUpdates.add(new CurrentEquipmentStateUpdate(
                    nextState.getEquipmentId(),
                    nextState.getCurrentStatus(),
                    nextState.getLastStatusChangedAt(),
                    tickTime
            ));
        }

        List<CurrentLineStateUpdate> lineUpdates = deriveLineUpdates(nextStates, tickTime);

        return new SimulationTickResult(
                nextStates,
                equipmentUpdates,
                lineUpdates,
                productionRecords,
                transitions,
                alarms
        );
    }

    private EquipmentStatus resolveNextStatus(EquipmentRuntimeState state, LocalDateTime tickTime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return switch (state.getCurrentStatus()) {
            case RUN -> resolveFromRun(state, random, tickTime);
            case STOP -> state.getTicksInCurrentStatus() >= state.getMinStopTicks() && random.nextDouble() < 0.60
                    ? EquipmentStatus.RUN
                    : EquipmentStatus.STOP;
            case IDLE -> {
                if (state.getTicksInCurrentStatus() < state.getMinIdleTicks()) {
                    yield EquipmentStatus.IDLE;
                }

                double roll = random.nextDouble();
                if (roll < 0.60) {
                    yield EquipmentStatus.RUN;
                }
                if (roll < 0.80) {
                    yield EquipmentStatus.STOP;
                }
                yield EquipmentStatus.IDLE;
            }
            case ERROR -> {
                if (state.getTicksInCurrentStatus() < state.getMinStopTicks() + 1) {
                    yield EquipmentStatus.ERROR;
                }
                yield random.nextDouble() < 0.55 ? EquipmentStatus.STOP : EquipmentStatus.IDLE;
            }
            case MAINTENANCE -> {
                if (state.getTicksInCurrentStatus() < state.getMinMaintenanceTicks()) {
                    yield EquipmentStatus.MAINTENANCE;
                }
                yield random.nextDouble() < 0.70 ? EquipmentStatus.IDLE : EquipmentStatus.RUN;
            }
        };
    }

    private EquipmentStatus resolveFromRun(EquipmentRuntimeState state, ThreadLocalRandom random, LocalDateTime tickTime) {
        if (state.getTicksInCurrentStatus() < state.getMinRunTicks()) {
            return EquipmentStatus.RUN;
        }

        double roll = random.nextDouble();
        double maintenanceBias = shouldScheduleMaintenance(state, tickTime) ? 0.012 : 0.0;

        if (roll < state.getFailureBias() * 0.25) {
            return EquipmentStatus.ERROR;
        }
        if (roll < state.getFailureBias() + maintenanceBias) {
            return maintenanceBias > 0.0 ? EquipmentStatus.MAINTENANCE : EquipmentStatus.STOP;
        }
        if (roll < state.getFailureBias() + 0.06) {
            return EquipmentStatus.IDLE;
        }
        return EquipmentStatus.RUN;
    }

    private boolean shouldScheduleMaintenance(EquipmentRuntimeState state, LocalDateTime tickTime) {
        if (state.getLastInspectionAt() == null) {
            return false;
        }

        return state.getLastInspectionAt().plusDays(7).isBefore(tickTime);
    }

    private ProductionMetrics resolveMetrics(EquipmentRuntimeState state, LocalDateTime tickTime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (state.getCurrentStatus() == EquipmentStatus.RUN) {
            int uph = Math.max(80, (int) Math.round(state.getBaseUph() * (0.92 + random.nextDouble() * 0.16)));
            double expectedUnits = uph / 720.0;
            int productionCount = resolveProductionCountForTick(expectedUnits, random);

            int defectCount = 0;
            if (productionCount > 0 && random.nextDouble() < state.getDefectBias()) {
                defectCount = 1 + (random.nextDouble() < state.getDefectBias() * 0.40 ? 1 : 0);
            }

            BigDecimal operationRate = decimal(88 + random.nextDouble() * 10);
            return new ProductionMetrics(productionCount, defectCount, operationRate, uph);
        }

        if (state.getCurrentStatus() == EquipmentStatus.IDLE) {
            int uph = Math.max(20, (int) Math.round(state.getBaseUph() * 0.18));
            int productionCount = resolveProductionCountForTick(uph / 720.0, random);
            return new ProductionMetrics(productionCount, 0, decimal(25 + random.nextDouble() * 20), uph);
        }

        if (state.getCurrentStatus() == EquipmentStatus.MAINTENANCE) {
            state.setLastInspectionAt(tickTime);
        }

        return new ProductionMetrics(0, 0, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0);
    }

    private List<CurrentLineStateUpdate> deriveLineUpdates(List<EquipmentRuntimeState> nextStates, LocalDateTime tickTime) {
        Map<Long, EnumMap<EquipmentStatus, Integer>> statusCountsByLine = new HashMap<>();

        for (EquipmentRuntimeState state : nextStates) {
            statusCountsByLine
                    .computeIfAbsent(state.getLineId(), key -> new EnumMap<>(EquipmentStatus.class))
                    .merge(state.getCurrentStatus(), 1, Integer::sum);
        }

        List<CurrentLineStateUpdate> lineUpdates = new ArrayList<>();
        for (Map.Entry<Long, EnumMap<EquipmentStatus, Integer>> entry : statusCountsByLine.entrySet()) {
            EquipmentStatus lineStatus = resolveLineStatus(entry.getValue());
            lineUpdates.add(new CurrentLineStateUpdate(entry.getKey(), lineStatus, tickTime));
        }
        return lineUpdates;
    }

    private EquipmentStatus resolveLineStatus(Map<EquipmentStatus, Integer> counts) {
        int run = counts.getOrDefault(EquipmentStatus.RUN, 0);
        int error = counts.getOrDefault(EquipmentStatus.ERROR, 0);
        int maintenance = counts.getOrDefault(EquipmentStatus.MAINTENANCE, 0);
        int idle = counts.getOrDefault(EquipmentStatus.IDLE, 0);

        if (error > 0) {
            return EquipmentStatus.ERROR;
        }
        if (maintenance >= 2) {
            return EquipmentStatus.MAINTENANCE;
        }
        if (run >= 4) {
            return EquipmentStatus.RUN;
        }
        if (run >= 2 || idle > 0) {
            return EquipmentStatus.IDLE;
        }
        return EquipmentStatus.STOP;
    }

    private AlarmHistory createAlarm(EquipmentRuntimeState nextState, LocalDateTime tickTime) {
        return switch (nextState.getCurrentStatus()) {
            case STOP -> AlarmHistory.builder()
                    .lineId(nextState.getLineId())
                    .equipmentId(nextState.getEquipmentId())
                    .alarmType(AlarmType.STOP.name())
                    .severity(AlarmSeverity.MEDIUM.name())
                    .message(nextState.getEquipmentName() + " changed to STOP status.")
                    .acknowledged(false)
                    .createdAt(tickTime)
                    .build();
            case ERROR -> AlarmHistory.builder()
                    .lineId(nextState.getLineId())
                    .equipmentId(nextState.getEquipmentId())
                    .alarmType(AlarmType.ERROR.name())
                    .severity(AlarmSeverity.HIGH.name())
                    .message(nextState.getEquipmentName() + " reported an ERROR state. Immediate check is required.")
                    .acknowledged(false)
                    .createdAt(tickTime)
                    .build();
            case MAINTENANCE -> AlarmHistory.builder()
                    .lineId(nextState.getLineId())
                    .equipmentId(nextState.getEquipmentId())
                    .alarmType(AlarmType.MAINTENANCE.name())
                    .severity(AlarmSeverity.LOW.name())
                    .message(nextState.getEquipmentName() + " reached the planned maintenance interval.")
                    .acknowledged(false)
                    .createdAt(tickTime)
                    .build();
            default -> null;
        };
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private int resolveProductionCountForTick(double expectedUnits, ThreadLocalRandom random) {
        int wholeUnits = (int) Math.floor(expectedUnits);
        double fractionalUnits = expectedUnits - wholeUnits;

        // Stochastic rounding keeps the average production rate similar
        // while making 5-second updates feel more alive on the dashboard.
        if (random.nextDouble() < fractionalUnits) {
            return wholeUnits + 1;
        }

        return wholeUnits;
    }

    private record ProductionMetrics(
            int productionCount,
            int defectCount,
            BigDecimal operationRate,
            int uph
    ) {
        ProductionRecord toRecord(EquipmentRuntimeState state, LocalDateTime tickTime) {
            return ProductionRecord.builder()
                    .lineId(state.getLineId())
                    .equipmentId(state.getEquipmentId())
                    .recordTime(tickTime)
                    .productionCount(productionCount)
                    .defectCount(defectCount)
                    .operationRate(operationRate)
                    .uph(uph)
                    .createdAt(tickTime)
                    .build();
        }
    }
}
