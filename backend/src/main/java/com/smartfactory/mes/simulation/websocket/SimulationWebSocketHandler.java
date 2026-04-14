package com.smartfactory.mes.simulation.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class SimulationWebSocketHandler extends TextWebSocketHandler {

    private final SimulationWebSocketBroadcaster broadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String query = session.getUri() != null ? session.getUri().getQuery() : null;

        if (path.endsWith("/dashboard")) {
            broadcaster.registerDashboard(session);
            return;
        }

        Long entityId = parseId(query);
        if (entityId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (path.endsWith("/line")) {
            broadcaster.registerLine(entityId, session);
            return;
        }

        if (path.endsWith("/equipment")) {
            broadcaster.registerEquipment(entityId, session);
            return;
        }

        session.close(CloseStatus.NOT_ACCEPTABLE);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        broadcaster.unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Long parseId(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String token : query.split("&")) {
            String[] parts = token.split("=", 2);
            if (parts.length == 2 && "id".equals(parts[0])) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }
}
