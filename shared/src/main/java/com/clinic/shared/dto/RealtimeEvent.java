package com.clinic.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RealtimeEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String entityId;
    private String message;
    private LocalDateTime occurredAt;

    public RealtimeEvent() {
    }

    public RealtimeEvent(String type, String entityId, String message, LocalDateTime occurredAt) {
        this.type = type;
        this.entityId = entityId;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
