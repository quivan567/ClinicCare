package com.clinic.client.ui.core;

import com.clinic.shared.dto.RealtimeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RealtimeClient {
    private static final String REALTIME_TOPIC = "/topic/realtime";

    private final String websocketUrl;
    private StompSession session;

    public RealtimeClient(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public void connect(Consumer<RealtimeEvent> eventHandler) {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(createMessageConverter());

        CompletableFuture<StompSession> connectFuture = stompClient.connectAsync(websocketUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                RealtimeClient.this.session = session;
                session.subscribe(REALTIME_TOPIC, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return RealtimeEvent.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload instanceof RealtimeEvent event) {
                            Platform.runLater(() -> eventHandler.accept(event));
                        }
                    }
                });
            }
        });

        connectFuture.exceptionally(error -> null);
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private MappingJackson2MessageConverter createMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
