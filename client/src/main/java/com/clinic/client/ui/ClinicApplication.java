package com.clinic.client.ui;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RealtimeClient;
import com.clinic.client.ui.core.RmiServiceClient;
import com.clinic.client.ui.view.ErrorView;
import com.clinic.client.ui.view.LoginView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ClinicApplication extends Application {
    private static final String SERVICE_URL = "rmi://localhost:1099/ClinicService";
    private static final String WEBSOCKET_URL = "ws://localhost:8080/ws";

    private Stage primaryStage;
    private AppContext appContext;
    private RealtimeClient realtimeClient;
    private MainLayout mainLayout;
    private double windowedX;
    private double windowedY;
    private double windowedWidth = 1280;
    private double windowedHeight = 720;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Hệ thống quản lý phòng khám");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(650);
        placeInitialWindowAtCenter();
        rememberWindowedBounds();
        primaryStage.xProperty().addListener((observable, oldValue, newValue) -> rememberWindowedBounds());
        primaryStage.yProperty().addListener((observable, oldValue, newValue) -> rememberWindowedBounds());
        primaryStage.widthProperty().addListener((observable, oldValue, newValue) -> rememberWindowedBounds());
        primaryStage.heightProperty().addListener((observable, oldValue, newValue) -> rememberWindowedBounds());

        try {
            appContext = new AppContext(RmiServiceClient.connect(SERVICE_URL), this::showLogin);
            realtimeClient = new RealtimeClient(WEBSOCKET_URL);
            realtimeClient.connect(event -> {
                if (mainLayout != null) {
                    mainLayout.refreshActiveContent();
                }
            });
            showLogin();
        } catch (Exception e) {
            setScene(new ErrorView(SERVICE_URL, e.getMessage()).createScene());
        }

        primaryStage.show();
    }

    private void showLogin() {
        mainLayout = null;
        appContext.clearCurrentUser();
        setScene(new LoginView(appContext, this::showMainLayout).createScene());
    }

    private void showMainLayout() {
        mainLayout = new MainLayout(appContext);
        setScene(mainLayout.createScene());
    }

    private void setScene(Scene scene) {
        boolean maximized = primaryStage.isMaximized();
        boolean fullScreen = primaryStage.isFullScreen();

        primaryStage.setScene(scene);

        if (fullScreen) {
            primaryStage.setFullScreen(true);
            return;
        }
        if (maximized) {
            primaryStage.setMaximized(true);
            return;
        }

        primaryStage.setX(windowedX);
        primaryStage.setY(windowedY);
        primaryStage.setWidth(windowedWidth);
        primaryStage.setHeight(windowedHeight);
    }

    private void placeInitialWindowAtCenter() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        windowedWidth = Math.min(windowedWidth, bounds.getWidth());
        windowedHeight = Math.min(windowedHeight, bounds.getHeight());
        windowedX = bounds.getMinX() + (bounds.getWidth() - windowedWidth) / 2;
        windowedY = bounds.getMinY() + (bounds.getHeight() - windowedHeight) / 2;
        primaryStage.setX(windowedX);
        primaryStage.setY(windowedY);
        primaryStage.setWidth(windowedWidth);
        primaryStage.setHeight(windowedHeight);
    }

    private void rememberWindowedBounds() {
        if (primaryStage == null || primaryStage.isMaximized() || primaryStage.isFullScreen() || primaryStage.isIconified()) {
            return;
        }
        windowedX = primaryStage.getX();
        windowedY = primaryStage.getY();
        windowedWidth = primaryStage.getWidth();
        windowedHeight = primaryStage.getHeight();
    }

    @Override
    public void stop() {
        if (realtimeClient != null) {
            realtimeClient.disconnect();
        }
    }
}
