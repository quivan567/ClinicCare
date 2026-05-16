package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.LoginRequest;
import com.clinic.shared.dto.LoginResponse;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class LoginView {
    private final AppContext appContext;
    private final Runnable loginSuccessHandler;

    public LoginView(AppContext appContext, Runnable loginSuccessHandler) {
        this.appContext = appContext;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(36));
        root.setStyle(AppTheme.rootStyle());

        VBox shell = new VBox(26);
        shell.setAlignment(Pos.CENTER);

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label brand = new Label("ClinicCare");
        brand.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY_DARK + ";");

        Label subtitle = new Label("Hệ thống quản lý phòng khám");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: " + AppTheme.MUTED + ";");

        header.getChildren().addAll(brand, subtitle);

        GridPane form = new GridPane();
        form.setAlignment(Pos.CENTER);
        form.setHgap(12);
        form.setVgap(14);
        form.setPadding(new Insets(30));
        form.setMaxWidth(430);
        form.setStyle(AppTheme.cardStyle());

        Label title = new Label("Đăng nhập");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        usernameField.setPrefHeight(42);
        usernameField.setStyle(AppTheme.inputStyle());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");
        passwordField.setPrefHeight(42);
        passwordField.setStyle(AppTheme.inputStyle());

        Button loginButton = new Button("Đăng nhập");
        loginButton.setPrefHeight(42);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(AppTheme.primaryButtonStyle());

        Label statusLabel = new Label("Nhập tài khoảng và mật khẩu để đăng nhập");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        loginButton.setOnAction(event -> login(usernameField, passwordField, statusLabel));
        passwordField.setOnAction(event -> login(usernameField, passwordField, statusLabel));

        form.add(title, 0, 0);
        form.add(new Label("Tài khoản"), 0, 1);
        form.add(usernameField, 0, 2);
        form.add(new Label("Mật khẩu"), 0, 3);
        form.add(passwordField, 0, 4);
        form.add(loginButton, 0, 5);
        form.add(statusLabel, 0, 6);

        shell.getChildren().addAll(header, form);
        root.setCenter(shell);
        return new Scene(root, 1280, 720);
    }

    private void login(TextField usernameField, PasswordField passwordField, Label statusLabel) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            statusLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR + ";");
            return;
        }

        try {
            LoginResponse response = appContext.getService().login(new LoginRequest(username, password));
            if (response.isSuccess()) {
                appContext.setCurrentUser(response.getUser());
                loginSuccessHandler.run();
            } else {
                statusLabel.setText(response.getMessage());
                statusLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR + ";");
            }
        } catch (Exception e) {
            statusLabel.setText("Lỗi kết nối Backend: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR + ";");
        }
    }
}
