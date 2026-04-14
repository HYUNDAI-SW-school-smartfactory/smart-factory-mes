package com.smartfactory.mes.simulation.websocket;

public record SimulationWebSocketMessage(
        String channel,
        Object data,
        SimulationWebSocketError error
) {

    public static SimulationWebSocketMessage success(String channel, Object data) {
        return new SimulationWebSocketMessage(channel, data, null);
    }

    public static SimulationWebSocketMessage error(String channel, String code, String message) {
        return new SimulationWebSocketMessage(channel, null, new SimulationWebSocketError(code, message));
    }

    public record SimulationWebSocketError(
            String code,
            String message
    ) {
    }
}
