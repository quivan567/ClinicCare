package com.clinic.client.ui.core;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class UiBackground {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "clinic-background");
        thread.setDaemon(true);
        return thread;
    });

    private UiBackground() {
    }

    public static <T> void call(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        EXECUTOR.execute(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                Platform.runLater(() -> onFailure.accept(ex));
            }
        });
    }
}
