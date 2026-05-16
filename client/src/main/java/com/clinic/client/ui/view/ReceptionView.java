package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.ReceptionIntakeRequest;
import com.clinic.shared.dto.SearchPatientRequest;
import com.clinic.shared.model.Patient;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceptionView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Patient> patients = FXCollections.observableArrayList();
    private final ObservableList<User> doctors = FXCollections.observableArrayList();

    private TableView<Patient> table;
    private TextField searchField;
    private TextField codeField;
    private TextField fullNameField;
    private TextField ageField;
    private ComboBox<String> genderComboBox;
    private TextField phoneField;
    private TextField addressField;
    private ComboBox<User> doctorComboBox;
    private Label statusLabel;

    public ReceptionView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox content = new VBox(18);
        content.getChildren().addAll(createHeader(), createBody());
        root.setCenter(content);

        loadDoctors();
        loadPatients();
        return root;
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Tiếp nhận bệnh nhân");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Tạo lượt tiếp nhận, gán bác sĩ và theo dõi danh sách bệnh nhân.");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Làm mới");
        refreshButton.setPrefHeight(38);
        refreshButton.setStyle(AppTheme.secondaryButtonStyle());
        refreshButton.setOnAction(event -> {
            loadDoctors();
            loadPatients();
        });

        header.getChildren().addAll(titleBox, spacer, refreshButton);
        return header;
    }

    private Node createBody() {
        HBox body = new HBox(18);
        body.getChildren().addAll(createPatientListCard(), createIntakeFormCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createPatientListCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(680);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Tìm theo mã, tên, SĐT hoặc trạng thái");
        searchField.setPrefHeight(38);
        searchField.setStyle(AppTheme.inputStyle());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Tìm kiếm");
        searchButton.setPrefHeight(38);
        searchButton.setStyle(AppTheme.primaryButtonStyle());
        searchButton.setOnAction(event -> searchPatients());

        Button clearButton = new Button("Xóa lọc");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> {
            searchField.clear();
            loadPatients();
        });

        searchField.setOnAction(event -> searchPatients());
        searchBar.getChildren().addAll(searchField, searchButton, clearButton);

        table = new TableView<>();
        table.setItems(patients);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(500);

        TableColumn<Patient, String> codeColumn = new TableColumn<>("Mã BN");
        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getCode())));

        TableColumn<Patient, String> nameColumn = new TableColumn<>("Họ tên");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getFullName())));

        TableColumn<Patient, String> phoneColumn = new TableColumn<>("SĐT");
        phoneColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getPhone())));

        TableColumn<Patient, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(doctorName(data.getValue().getAssignedDoctorId())));

        TableColumn<Patient, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatus())));

        TableColumn<Patient, String> visitColumn = new TableColumn<>("Lần khám gần nhất");
        visitColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        table.getColumns().addAll(codeColumn, nameColumn, phoneColumn, doctorColumn, statusColumn, visitColumn);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selected) -> fillForm(selected));

        card.getChildren().addAll(searchBar, table);
        return card;
    }

    private Node createIntakeFormCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(390);

        Label title = new Label("Phiếu tiếp nhận");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        codeField = createInput("VD: BN005");
        fullNameField = createInput("Họ tên bệnh nhân");
        ageField = createInput("Tuổi");
        phoneField = createInput("Số điện thoại");
        addressField = createInput("Địa chỉ");

        genderComboBox = new ComboBox<>(FXCollections.observableArrayList("Nam", "Nữ", "Khác"));
        genderComboBox.setMaxWidth(Double.MAX_VALUE);
        genderComboBox.setPrefHeight(38);
        genderComboBox.setPromptText("Chọn giới tính");

        doctorComboBox = new ComboBox<>(doctors);
        doctorComboBox.setMaxWidth(Double.MAX_VALUE);
        doctorComboBox.setPrefHeight(38);
        doctorComboBox.setPromptText("Chọn bác sĩ phụ trách");
        configureDoctorComboBox();

        addRow(form, 0, "Mã BN", codeField);
        addRow(form, 1, "Họ tên", fullNameField);
        addRow(form, 2, "Tuổi", ageField);
        addRow(form, 3, "Giới tính", genderComboBox);
        addRow(form, 4, "SĐT", phoneField);
        addRow(form, 5, "Địa chỉ", addressField);
        addRow(form, 6, "Bác sĩ", doctorComboBox);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Tạo phiếu mới");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> clearForm());

        Button submitButton = new Button("Tiếp nhận");
        submitButton.setPrefHeight(38);
        submitButton.setStyle(AppTheme.primaryButtonStyle());
        submitButton.setOnAction(event -> receivePatient());

        actions.getChildren().addAll(clearButton, submitButton);

        statusLabel = new Label("Nhập thông tin bệnh nhân rồi bấm Tiếp nhận.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, form, actions, statusLabel);
        return card;
    }

    private TextField createInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle(AppTheme.inputStyle());
        return field;
    }

    private void addRow(GridPane form, int row, String label, Node input) {
        Label labelNode = new Label(label);
        labelNode.setMinWidth(120);
        labelNode.setStyle(AppTheme.formLabelStyle());
        form.add(labelNode, 0, row);
        form.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    private void configureDoctorComboBox() {
        doctorComboBox = new ComboBox<>(doctors);
        doctorComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                if (user == null) {
                    return "";
                }
                return safe(user.getFullName()) + " (" + safe(user.getUsername()) + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }

    private void loadDoctors() {
        try {
            doctors.setAll(appContext.getService().getUsersByRole(Role.DOCTOR));
        } catch (RemoteException e) {
            showError("Không thể tải danh sách bác sĩ", e.getMessage());
        }
    }

    @Override
    public void refreshData() {
        loadDoctors();
        loadPatients();
    }

    private void loadPatients() {
        try {
            List<Patient> result = appContext.getService().getAllPatients();
            patients.setAll(result);
            setStatus("Đã tải " + result.size() + " bệnh nhân.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách bệnh nhân", e.getMessage());
        }
    }

    private void searchPatients() {
        try {
            List<Patient> result = appContext.getService().searchPatients(new SearchPatientRequest(searchField.getText()));
            patients.setAll(result);
            setStatus("Tìm thấy " + result.size() + " bệnh nhân.", false);
        } catch (RemoteException e) {
            showError("Không thể tìm kiếm bệnh nhân", e.getMessage());
        }
    }

    private void receivePatient() {
        if (isBlank(codeField.getText()) || isBlank(fullNameField.getText())) {
            setStatus("Mã bệnh nhân và họ tên không được để trống.", true);
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageField.getText().trim());
        } catch (Exception e) {
            setStatus("Tuổi phải là số hợp lệ.", true);
            return;
        }

        User selectedDoctor = doctorComboBox.getValue();
        ReceptionIntakeRequest request = new ReceptionIntakeRequest(
                codeField.getText().trim(),
                fullNameField.getText().trim(),
                age,
                genderComboBox.getValue(),
                phoneField.getText().trim(),
                addressField.getText().trim(),
                selectedDoctor == null ? null : selectedDoctor.getId()
        );

        try {
            ApiResponse response = appContext.getService().receivePatient(request);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                loadPatients();
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể tiếp nhận bệnh nhân", e.getMessage());
        }
    }

    private void fillForm(Patient patient) {
        if (patient == null) {
            return;
        }
        codeField.setText(safe(patient.getCode()));
        fullNameField.setText(safe(patient.getFullName()));
        ageField.setText(String.valueOf(patient.getAge()));
        genderComboBox.setValue(patient.getGender());
        phoneField.setText(safe(patient.getPhone()));
        addressField.setText(safe(patient.getAddress()));
        doctorComboBox.setValue(findDoctor(patient.getAssignedDoctorId()));
        setStatus("Đang xem bệnh nhân: " + safe(patient.getCode()), false);
    }

    private void clearForm() {
        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
        codeField.clear();
        fullNameField.clear();
        ageField.clear();
        genderComboBox.setValue(null);
        phoneField.clear();
        addressField.clear();
        doctorComboBox.setValue(null);
        setStatus("Sẵn sàng tạo phiếu tiếp nhận mới.", false);
    }

    private User findDoctor(String doctorId) {
        if (isBlank(doctorId)) {
            return null;
        }
        return doctors.stream()
                .filter(doctor -> doctorId.equals(doctor.getId()))
                .findFirst()
                .orElse(null);
    }

    private String doctorName(String doctorId) {
        User doctor = findDoctor(doctorId);
        return doctor == null ? "" : safe(doctor.getFullName());
    }

    private String formatDateTime(Patient patient) {
        if (patient == null || patient.getLastVisitAt() == null) {
            return "";
        }
        return patient.getLastVisitAt().format(DATE_TIME_FORMATTER);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
