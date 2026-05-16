package com.clinic.server.websocket;

import com.clinic.shared.dto.RealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RealtimeEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String type, String entityId, String message) {
        messagingTemplate.convertAndSend("/topic/realtime", new RealtimeEvent(type, entityId, message, LocalDateTime.now()));
    }
}
