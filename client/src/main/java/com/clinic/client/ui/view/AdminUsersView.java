package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.UserRequest;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.util.List;

public class AdminUsersView implements RefreshableView {
    private final AppContext appContext;
    private final ObservableList<User> users = FXCollections.observableArrayList();

    private TableView<User> table;
    private TextField idField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField fullNameField;
    private ComboBox<Role> roleComboBox;
    private CheckBox activeCheckBox;
    private Label statusLabel;

    public AdminUsersView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox content = new VBox(18);
        content.getChildren().addAll(createHeader(), createBody());
        root.setCenter(content);

        loadUsers();
        return root;
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Quản lý người dùng");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Tạo, cập nhật, khóa/mở khóa và xóa tài khoản nhân viên.");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Làm mới");
        refreshButton.setPrefHeight(38);
        refreshButton.setStyle(AppTheme.secondaryButtonStyle());
        refreshButton.setOnAction(event -> loadUsers());

        header.getChildren().addAll(titleBox, spacer, refreshButton);
        return header;
    }

    private Node createBody() {
        HBox body = new HBox(18);
        body.getChildren().addAll(createTableCard(), createFormCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createTableCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(620);

        table = new TableView<>();
        table.setItems(users);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(520);

        TableColumn<User, String> usernameColumn = new TableColumn<>("Tài khoản");
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getUsername())));

        TableColumn<User, String> fullNameColumn = new TableColumn<>("Họ tên");
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getFullName())));

        TableColumn<User, String> roleColumn = new TableColumn<>("Vai trò");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(roleLabel(data.getValue().getRole())));

        TableColumn<User, String> activeColumn = new TableColumn<>("Trạng thái");
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "Đang hoạt động" : "Đã khóa"));

        table.getColumns().addAll(usernameColumn, fullNameColumn, roleColumn, activeColumn);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> fillForm(selected));

        card.getChildren().add(table);
        return card;
    }

    private Node createFormCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(380);

        Label title = new Label("Thông tin tài khoản");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        idField = new TextField();
        idField.setEditable(false);
        idField.setPromptText("Tự động khi tạo mới");
        styleInput(idField);

        usernameField = new TextField();
        usernameField.setPromptText("VD: bacsi2");
        styleInput(usernameField);

        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập khi tạo mới, bỏ trống khi cập nhật để giữ cũ");
        styleInput(passwordField);

        fullNameField = new TextField();
        fullNameField.setPromptText("Họ tên nhân viên");
        styleInput(fullNameField);

        roleComboBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setPromptText("Chọn vai trò");

        activeCheckBox = new CheckBox("Đang hoạt động");
        activeCheckBox.setSelected(true);
        activeCheckBox.setStyle("-fx-text-fill: " + AppTheme.TEXT + ";");

        addRow(form, 0, "ID", idField);
        addRow(form, 1, "Tài khoản", usernameField);
        addRow(form, 2, "Mật khẩu", passwordField);
        addRow(form, 3, "Họ tên", fullNameField);
        addRow(form, 4, "Vai trò", roleComboBox);
        form.add(activeCheckBox, 1, 5);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button newButton = new Button("Tạo mới");
        newButton.setPrefHeight(38);
        newButton.setStyle(AppTheme.secondaryButtonStyle());
        newButton.setOnAction(event -> clearForm());

        Button saveButton = new Button("Lưu");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> saveUser());

        Button deleteButton = new Button("Xóa");
        deleteButton.setPrefHeight(38);
        deleteButton.setStyle(AppTheme.dangerButtonStyle());
        deleteButton.setOnAction(event -> deleteUser());

        actions.getChildren().addAll(newButton, saveButton, deleteButton);

        statusLabel = new Label("Chọn một dòng để sửa hoặc bấm Tạo mới.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, form, actions, statusLabel);
        return card;
    }

    private void addRow(GridPane form, int row, String label, Node input) {
        Label labelNode = new Label(label);
        labelNode.setMinWidth(120);
        labelNode.setStyle(AppTheme.formLabelStyle());
        form.add(labelNode, 0, row);
        form.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    private void styleInput(TextField field) {
        field.setPrefHeight(38);
        field.setStyle(AppTheme.inputStyle());
    }

    @Override
    public void refreshData() {
        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> result = appContext.getService().getAllUsers();
            users.setAll(result);
            setStatus("Đã tải " + result.size() + " người dùng.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách người dùng", e.getMessage());
        }
    }

    private void saveUser() {
        if (isBlank(usernameField.getText()) || roleComboBox.getValue() == null) {
            setStatus("Tài khoản và vai trò không được để trống.", true);
            return;
        }
        if (isBlank(idField.getText()) && isBlank(passwordField.getText())) {
            setStatus("Mật khẩu không được để trống khi tạo mới.", true);
            return;
        }

        UserRequest request = new UserRequest(
                emptyToNull(idField.getText()),
                usernameField.getText().trim(),
                passwordField.getText(),
                roleComboBox.getValue(),
                fullNameField.getText().trim(),
                activeCheckBox.isSelected()
        );

        try {
            ApiResponse response = isBlank(idField.getText())
                    ? appContext.getService().createUser(request)
                    : appContext.getService().updateUser(request);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadUsers);
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể lưu người dùng", e.getMessage());
        }
    }

    private void deleteUser() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || isBlank(selected.getId())) {
            setStatus("Vui lòng chọn người dùng cần xóa.", true);
            return;
        }
        if (appContext.getCurrentUser() != null && selected.getId().equals(appContext.getCurrentUser().getId())) {
            setStatus("Không thể tự xóa tài khoản đang đăng nhập.", true);
            return;
        }

        try {
            ApiResponse response = appContext.getService().deleteUser(selected.getId());
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadUsers);
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể xóa người dùng", e.getMessage());
        }
    }

    private void fillForm(User user) {
        if (user == null) {
            return;
        }
        idField.setText(safe(user.getId()));
        usernameField.setText(safe(user.getUsername()));
        passwordField.clear();
        fullNameField.setText(safe(user.getFullName()));
        roleComboBox.setValue(user.getRole());
        activeCheckBox.setSelected(user.isActive());
        setStatus("Đang sửa tài khoản: " + safe(user.getUsername()), false);
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        idField.clear();
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        roleComboBox.setValue(null);
        activeCheckBox.setSelected(true);
        setStatus("Sẵn sàng tạo tài khoản mới.", false);
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

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
