package com.clinic.client.ui.view;

import com.clinic.client.ui.theme.AppTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ErrorView {
    private final String serviceUrl;
    private final String message;

    public ErrorView(String serviceUrl, String message) {
        this.serviceUrl = serviceUrl;
        this.message = message;
    }

    public Scene createScene() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle(AppTheme.rootStyle());

        Label title = new Label("Không thể kết nối Backend RMI");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.ERROR + ";");

        Label detail = new Label(message == null || message.isBlank() ? serviceUrl : message);
        detail.setWrapText(true);
        detail.setMaxWidth(620);
        detail.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-size: 14px;");

        Label hint = new Label("Hãy chạy server trước, sau đó mở lại client JavaFX.");
        hint.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        root.getChildren().addAll(title, detail, hint);
        return new Scene(root, 900, 520);
    }
}
