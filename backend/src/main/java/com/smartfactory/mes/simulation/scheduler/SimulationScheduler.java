package com.smartfactory.mes.simulation.scheduler;

import com.smartfactory.mes.simulation.service.SimulationEngine;
import com.smartfactory.mes.simulation.service.SimulationPersistenceService;
import com.smartfactory.mes.simulation.service.SimulationStateStore;
import com.smartfactory.mes.simulation.websocket.SimulationWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnProperty(prefix = "mes.simulation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SimulationScheduler {

    private final SimulationEngine simulationEngine;
    private final SimulationPersistenceService simulationPersistenceService;
    private final SimulationStateStore simulationStateStore;
    private final SimulationWebSocketBroadcaster simulationWebSocketBroadcaster;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(
            fixedDelayString = "${mes.simulation.fixed-delay-ms:5000}",
            initialDelayString = "${mes.simulation.initial-delay-ms:5000}"
    )
    public void runSimulationTick() {
        if (simulationStateStore.isEmpty() || !running.compareAndSet(false, true)) {
            return;
        }

        try {
            LocalDateTime tickTime = LocalDateTime.now().withNano(0);
            var tickResult = simulationEngine.advance(simulationStateStore.snapshot(), tickTime);
            simulationPersistenceService.persistTick(tickResult);
            simulationStateStore.replace(tickResult.nextStates());
            simulationWebSocketBroadcaster.broadcastCurrentState();
        } finally {
            running.set(false);
        }
    }
}
