package com.kwcapstone.Config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    public void register(String sessionId, String memberId, String projectId) {
        sessionMap.put(sessionId, new SessionInfo(memberId, projectId));
    }

    public SessionInfo remove(String sessionId) {
        return sessionMap.remove(sessionId);
    }

    @Getter
    @AllArgsConstructor
    public static class SessionInfo {
        private String memberId;
        private String projectId;
    }
}
