package com.smartfactory.mes.simulation.service;

import com.smartfactory.mes.simulation.domain.EquipmentRuntimeState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationStateStore {

    private final Map<Long, EquipmentRuntimeState> states = new ConcurrentHashMap<>();
    private boolean ready;

    public synchronized void initialize(List<EquipmentRuntimeState> initialStates) {
        states.clear();
        initialStates.forEach(state -> states.put(state.getEquipmentId(), state));
        ready = false;
    }

    public synchronized List<EquipmentRuntimeState> snapshot() {
        List<EquipmentRuntimeState> copy = new ArrayList<>();
        states.values().stream()
                .sorted((left, right) -> Long.compare(left.getEquipmentId(), right.getEquipmentId()))
                .forEach(state -> copy.add(state.copy()));
        return copy;
    }

    public synchronized void replace(List<EquipmentRuntimeState> nextStates) {
        states.clear();
        nextStates.forEach(state -> states.put(state.getEquipmentId(), state));
    }

    public synchronized boolean isEmpty() {
        return states.isEmpty();
    }

    public synchronized boolean isReady() {
        return ready;
    }

    public synchronized void markReady() {
        ready = true;
    }
}
