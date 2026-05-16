package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.ChangePasswordRequest;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;

public class ProfileView {
    private final AppContext appContext;

    private Label usernameLabel;
    private Label fullNameLabel;
    private Label roleLabel;
    private Label activeLabel;
    private Label statusLabel;
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;

    public ProfileView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox content = new VBox(18);
        content.getChildren().addAll(createHeader(), createBody());
        root.setCenter(content);

        loadProfile();
        return root;
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Hồ sơ cá nhân");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Xem thông tin tài khoản và thay đổi mật khẩu đăng nhập.");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Làm mới");
        refreshButton.setPrefHeight(38);
        refreshButton.setStyle(AppTheme.secondaryButtonStyle());
        refreshButton.setOnAction(event -> loadProfile());

        header.getChildren().addAll(titleBox, spacer, refreshButton);
        return header;
    }

    private Node createBody() {
        HBox body = new HBox(18);
        body.getChildren().addAll(createInfoCard(), createPasswordCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createInfoCard() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(22));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(520);

        Label title = new Label("Thông tin tài khoản");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(14);
        infoGrid.setVgap(14);

        usernameLabel = createValueLabel();
        fullNameLabel = createValueLabel();
        roleLabel = createValueLabel();
        activeLabel = createValueLabel();

        addInfoRow(infoGrid, 0, "Tài khoản", usernameLabel);
        addInfoRow(infoGrid, 1, "Họ tên", fullNameLabel);
        addInfoRow(infoGrid, 2, "Vai trò", roleLabel);
        addInfoRow(infoGrid, 3, "Trạng thái", activeLabel);

        Label note = new Label("Nếu thông tin cá nhân chưa đúng, vui lòng liên hệ quản trị viên để cập nhật.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, infoGrid, note);
        return card;
    }

    private Node createPasswordCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(420);

        Label title = new Label("Đổi mật khẩu");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        currentPasswordField = createPasswordField("Mật khẩu hiện tại");
        newPasswordField = createPasswordField("Mật khẩu mới");
        confirmPasswordField = createPasswordField("Nhập lại mật khẩu mới");

        addFormRow(form, 0, "Hiện tại", currentPasswordField);
        addFormRow(form, 1, "Mới", newPasswordField);
        addFormRow(form, 2, "Xác nhận", confirmPasswordField);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Xóa form");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> clearPasswordForm());

        Button saveButton = new Button("Đổi mật khẩu");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> changePassword());

        actions.getChildren().addAll(clearButton, saveButton);

        statusLabel = new Label("Nhập mật khẩu hiện tại và mật khẩu mới để cập nhật.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, form, actions, statusLabel);
        return card;
    }

    private Label createValueLabel() {
        Label label = new Label("");
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + AppTheme.TEXT + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle(AppTheme.inputStyle());
        return field;
    }

    private void addInfoRow(GridPane grid, int row, String label, Label value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-weight: bold;");
        grid.add(labelNode, 0, row);
        grid.add(value, 1, row);
        GridPane.setHgrow(value, Priority.ALWAYS);
    }

    private void addFormRow(GridPane form, int row, String label, Node input) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-weight: bold;");
        form.add(labelNode, 0, row);
        form.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    private void loadProfile() {
        User currentUser = appContext.getCurrentUser();
        if (currentUser == null) {
            setStatus("Không tìm thấy phiên đăng nhập hiện tại.", true);
            return;
        }

        try {
            User latest = appContext.getService().getMyProfile(currentUser.getId());
            if (latest != null) {
                appContext.setCurrentUser(latest);
                fillProfile(latest);
                setStatus("Đã tải hồ sơ mới nhất.", false);
            } else {
                fillProfile(currentUser);
                setStatus("Không tìm thấy hồ sơ mới nhất, đang hiển thị thông tin phiên hiện tại.", true);
            }
        } catch (RemoteException e) {
            fillProfile(currentUser);
            showError("Không thể tải hồ sơ cá nhân", e.getMessage());
        }
    }

    private void fillProfile(User user) {
        usernameLabel.setText(safe(user.getUsername()));
        fullNameLabel.setText(safe(user.getFullName()));
        roleLabel.setText(roleLabel(user.getRole()));
        activeLabel.setText(user.isActive() ? "Đang hoạt động" : "Đã khóa");
        activeLabel.setStyle("-fx-text-fill: " + (user.isActive() ? AppTheme.SUCCESS : AppTheme.ERROR) + "; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private void changePassword() {
        User currentUser = appContext.getCurrentUser();
        if (currentUser == null) {
            setStatus("Không tìm thấy phiên đăng nhập hiện tại.", true);
            return;
        }
        if (isBlank(currentPasswordField.getText()) || isBlank(newPasswordField.getText()) || isBlank(confirmPasswordField.getText())) {
            setStatus("Vui lòng nhập đầy đủ thông tin mật khẩu.", true);
            return;
        }
        if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
            setStatus("Mật khẩu xác nhận không khớp.", true);
            return;
        }
        if (newPasswordField.getText().length() < 3) {
            setStatus("Mật khẩu mới nên có ít nhất 3 ký tự.", true);
            return;
        }
        if (currentPasswordField.getText().equals(newPasswordField.getText())) {
            setStatus("Mật khẩu mới không nên trùng mật khẩu hiện tại.", true);
            return;
        }

        try {
            ApiResponse response = appContext.getService().changePassword(
                    new ChangePasswordRequest(currentUser.getId(), currentPasswordField.getText(), newPasswordField.getText())
            );
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                clearPasswordForm();
                loadProfile();
            }
        } catch (RemoteException e) {
            showError("Không thể đổi mật khẩu", e.getMessage());
        }
    }

    private void clearPasswordForm() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        setStatus("Đã xóa form đổi mật khẩu.", false);
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setStyle("-fx-text-fill: " + (error ? AppTheme.ERROR : AppTheme.MUTED) + ";");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Đã có lỗi xảy ra." : message);
        alert.showAndWait();
    }

    private String roleLabel(Role role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case ADMIN -> "Quản trị";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case PHARMACIST -> "Dược sĩ";
            case TESTING_STAFF -> "Xét nghiệm";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
