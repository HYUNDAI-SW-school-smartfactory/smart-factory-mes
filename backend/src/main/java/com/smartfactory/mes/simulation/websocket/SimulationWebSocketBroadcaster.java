package com.smartfactory.mes.simulation.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfactory.mes.global.exception.BusinessException;
import com.smartfactory.mes.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationWebSocketBroadcaster {

    private final SimulationQueryService simulationQueryService;
    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> dashboardSessions = ConcurrentHashMap.newKeySet();
    private final Map<Long, Set<WebSocketSession>> lineSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> equipmentSessions = new ConcurrentHashMap<>();

    public void registerDashboard(WebSocketSession session) {
        dashboardSessions.add(session);
        sendDashboard(session);
    }

    public void registerLine(Long lineId, WebSocketSession session) {
        lineSessions.computeIfAbsent(lineId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sendLine(lineId, session);
    }

    public void registerEquipment(Long equipmentId, WebSocketSession session) {
        equipmentSessions.computeIfAbsent(equipmentId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sendEquipment(equipmentId, session);
    }

    public void unregister(WebSocketSession session) {
        dashboardSessions.remove(session);
        lineSessions.values().forEach(sessions -> sessions.remove(session));
        equipmentSessions.values().forEach(sessions -> sessions.remove(session));
        lineSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        equipmentSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void broadcastCurrentState() {
        broadcastDashboard();
        broadcastLines();
        broadcastEquipments();
    }

    private void broadcastDashboard() {
        String payload = serializeSuccess("dashboard", simulationQueryService.getDashboardSnapshot());
        broadcastToSessions(dashboardSessions, payload);
    }

    private void broadcastLines() {
        for (Map.Entry<Long, Set<WebSocketSession>> entry : lineSessions.entrySet()) {
            Long lineId = entry.getKey();
            try {
                String payload = serializeSuccess("line", simulationQueryService.getLineDetail(lineId));
                broadcastToSessions(entry.getValue(), payload);
            } catch (BusinessException e) {
                String payload = serializeError("line", e.getErrorCode().getCode(), e.getMessage());
                broadcastToSessions(entry.getValue(), payload);
            }
        }
    }

    private void broadcastEquipments() {
        for (Map.Entry<Long, Set<WebSocketSession>> entry : equipmentSessions.entrySet()) {
            Long equipmentId = entry.getKey();
            try {
                String payload = serializeSuccess("equipment", simulationQueryService.getEquipmentDetail(equipmentId));
                broadcastToSessions(entry.getValue(), payload);
            } catch (BusinessException e) {
                String payload = serializeError("equipment", e.getErrorCode().getCode(), e.getMessage());
                broadcastToSessions(entry.getValue(), payload);
            }
        }
    }

    private void sendDashboard(WebSocketSession session) {
        sendToSession(session, serializeSuccess("dashboard", simulationQueryService.getDashboardSnapshot()));
    }

    private void sendLine(Long lineId, WebSocketSession session) {
        try {
            sendToSession(session, serializeSuccess("line", simulationQueryService.getLineDetail(lineId)));
        } catch (BusinessException e) {
            sendToSession(session, serializeError("line", e.getErrorCode().getCode(), e.getMessage()));
            closeQuietly(session, CloseStatus.BAD_DATA);
        }
    }

    private void sendEquipment(Long equipmentId, WebSocketSession session) {
        try {
            sendToSession(session, serializeSuccess("equipment", simulationQueryService.getEquipmentDetail(equipmentId)));
        } catch (BusinessException e) {
            sendToSession(session, serializeError("equipment", e.getErrorCode().getCode(), e.getMessage()));
            closeQuietly(session, CloseStatus.BAD_DATA);
        }
    }

    private void broadcastToSessions(Set<WebSocketSession> sessions, String payload) {
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            sendToSession(session, payload);
        }
    }

    private void sendToSession(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            log.warn("Failed to send simulation websocket payload to session {}", session.getId(), e);
            unregister(session);
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    private String serializeSuccess(String channel, Object data) {
        try {
            return objectMapper.writeValueAsString(SimulationWebSocketMessage.success(channel, data));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize websocket payload", e);
        }
    }

    private String serializeError(String channel, String code, String message) {
        try {
            return objectMapper.writeValueAsString(SimulationWebSocketMessage.error(channel, code, message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize websocket error payload", e);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus closeStatus) {
        try {
            if (session.isOpen()) {
                session.close(closeStatus);
            }
        } catch (IOException ignored) {
        }
    }
}
