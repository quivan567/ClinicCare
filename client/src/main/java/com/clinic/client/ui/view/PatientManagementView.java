package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.AssignDoctorRequest;
import com.clinic.shared.dto.SearchPatientRequest;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.PatientStatus;
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

public class PatientManagementView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Patient> patients = FXCollections.observableArrayList();
    private final ObservableList<User> doctors = FXCollections.observableArrayList();

    private TableView<Patient> table;
    private TextField searchField;
    private TextField idField;
    private TextField codeField;
    private TextField fullNameField;
    private TextField ageField;
    private ComboBox<String> genderComboBox;
    private TextField phoneField;
    private TextField addressField;
    private ComboBox<User> doctorComboBox;
    private ComboBox<String> statusComboBox;
    private Label statusLabel;

    public PatientManagementView(AppContext appContext) {
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
        Label title = new Label("Quản lý bệnh nhân");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Tra cứu, thêm, cập nhật, phân công bác sĩ và xóa bệnh nhân đủ điều kiện.");
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
        body.getChildren().addAll(createListCard(), createFormCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createListCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(720);

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

        Button clearFilterButton = new Button("Xóa lọc");
        clearFilterButton.setPrefHeight(38);
        clearFilterButton.setStyle(AppTheme.secondaryButtonStyle());
        clearFilterButton.setOnAction(event -> {
            searchField.clear();
            loadPatients();
        });

        searchField.setOnAction(event -> searchPatients());
        searchBar.getChildren().addAll(searchField, searchButton, clearFilterButton);

        table = new TableView<>();
        table.setItems(patients);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(520);

        TableColumn<Patient, String> codeColumn = new TableColumn<>("Mã BN");
        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getCode())));

        TableColumn<Patient, String> nameColumn = new TableColumn<>("Họ tên");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getFullName())));

        TableColumn<Patient, String> infoColumn = new TableColumn<>("Thông tin");
        infoColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAge() + " tuổi · " + safe(data.getValue().getGender())));

        TableColumn<Patient, String> phoneColumn = new TableColumn<>("SĐT");
        phoneColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getPhone())));

        TableColumn<Patient, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(displayDoctor(data.getValue())));

        TableColumn<Patient, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatus())));

        TableColumn<Patient, String> visitColumn = new TableColumn<>("Lần khám");
        visitColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        table.getColumns().addAll(codeColumn, nameColumn, infoColumn, phoneColumn, doctorColumn, statusColumn, visitColumn);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, patient) -> fillForm(patient));

        card.getChildren().addAll(searchBar, table);
        return card;
    }

    private Node createFormCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(410);

        Label title = new Label("Thông tin bệnh nhân");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        idField = createInput("ID hệ thống");
        idField.setEditable(false);
        codeField = createInput("VD: BN005");
        fullNameField = createInput("Họ tên bệnh nhân");
        ageField = createInput("Tuổi");
        phoneField = createInput("Số điện thoại");
        addressField = createInput("Địa chỉ");

        genderComboBox = new ComboBox<>(FXCollections.observableArrayList("Nam", "Nữ", "Khác"));
        genderComboBox.setPromptText("Giới tính");
        genderComboBox.setMaxWidth(Double.MAX_VALUE);
        genderComboBox.setPrefHeight(38);

        doctorComboBox = new ComboBox<>(doctors);
        doctorComboBox.setPromptText("Chọn bác sĩ");
        doctorComboBox.setMaxWidth(Double.MAX_VALUE);
        doctorComboBox.setPrefHeight(38);
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

        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(
                PatientStatus.WAITING,
                PatientStatus.ASSIGNED,
                PatientStatus.EXAMINING,
                PatientStatus.WAITING_FOR_TEST,
                PatientStatus.HAS_TEST_RESULT,
                PatientStatus.EXAMINED,
                PatientStatus.WAITING_FOR_PRESCRIPTION,
                PatientStatus.PRESCRIBED,
                PatientStatus.COMPLETED
        ));
        statusComboBox.setPromptText("Trạng thái");
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        statusComboBox.setPrefHeight(38);

        addRow(form, 0, "ID", idField);
        addRow(form, 1, "Mã BN", codeField);
        addRow(form, 2, "Họ tên", fullNameField);
        addRow(form, 3, "Tuổi", ageField);
        addRow(form, 4, "Giới tính", genderComboBox);
        addRow(form, 5, "SĐT", phoneField);
        addRow(form, 6, "Địa chỉ", addressField);
        addRow(form, 7, "Bác sĩ", doctorComboBox);
        addRow(form, 8, "Trạng thái", statusComboBox);

        HBox actionsTop = new HBox(10);
        actionsTop.setAlignment(Pos.CENTER_RIGHT);

        Button newButton = new Button("Tạo mới");
        newButton.setPrefHeight(38);
        newButton.setStyle(AppTheme.secondaryButtonStyle());
        newButton.setOnAction(event -> clearForm());

        Button saveButton = new Button("Lưu");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> savePatient());

        actionsTop.getChildren().addAll(newButton, saveButton);

        HBox actionsBottom = new HBox(10);
        actionsBottom.setAlignment(Pos.CENTER_RIGHT);

        Button assignButton = new Button("Phân công");
        assignButton.setPrefHeight(38);
        assignButton.setStyle(AppTheme.primaryButtonStyle());
        assignButton.setOnAction(event -> assignDoctor());

        Button deleteButton = new Button("Xóa");
        deleteButton.setPrefHeight(38);
        deleteButton.setStyle(AppTheme.dangerButtonStyle());
        deleteButton.setOnAction(event -> deletePatient());

        actionsBottom.getChildren().addAll(assignButton, deleteButton);

        statusLabel = new Label("Chọn bệnh nhân để sửa hoặc bấm Tạo mới.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, form, actionsTop, actionsBottom, statusLabel);
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

    private void savePatient() {
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

        Patient patient = new Patient();
        patient.setId(emptyToNull(idField.getText()));
        patient.setCode(codeField.getText().trim());
        patient.setFullName(fullNameField.getText().trim());
        patient.setAge(age);
        patient.setGender(genderComboBox.getValue());
        patient.setPhone(phoneField.getText().trim());
        patient.setAddress(addressField.getText().trim());
        patient.setAssignedDoctorId(doctorComboBox.getValue() == null ? null : doctorComboBox.getValue().getId());
        patient.setStatus(statusComboBox.getValue() == null ? PatientStatus.WAITING : statusComboBox.getValue());

        try {
            ApiResponse response = isBlank(patient.getId())
                    ? appContext.getService().addPatient(patient)
                    : appContext.getService().updatePatient(patient);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadPatients);
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể lưu bệnh nhân", e.getMessage());
        }
    }

    private void assignDoctor() {
        Patient selected = table.getSelectionModel().getSelectedItem();
        User doctor = doctorComboBox.getValue();
        if (selected == null) {
            setStatus("Vui lòng chọn bệnh nhân cần phân công.", true);
            return;
        }
        if (doctor == null) {
            setStatus("Vui lòng chọn bác sĩ phụ trách.", true);
            return;
        }

        try {
            ApiResponse response = appContext.getService().assignDoctor(new AssignDoctorRequest(selected.getCode(), doctor.getId()));
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadPatients);
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể phân công bác sĩ", e.getMessage());
        }
    }

    private void deletePatient() {
        Patient selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Vui lòng chọn bệnh nhân cần xóa.", true);
            return;
        }

        try {
            ApiResponse response = appContext.getService().deletePatient(selected.getCode());
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadPatients);
                clearForm();
            }
        } catch (RemoteException e) {
            showError("Không thể xóa bệnh nhân", e.getMessage());
        }
    }

    private void fillForm(Patient patient) {
        if (patient == null) {
            return;
        }
        idField.setText(safe(patient.getId()));
        codeField.setText(safe(patient.getCode()));
        fullNameField.setText(safe(patient.getFullName()));
        ageField.setText(String.valueOf(patient.getAge()));
        genderComboBox.setValue(patient.getGender());
        phoneField.setText(safe(patient.getPhone()));
        addressField.setText(safe(patient.getAddress()));
        doctorComboBox.setValue(findDoctor(patient.getAssignedDoctorId()));
        statusComboBox.setValue(patient.getStatus());
        setStatus("Đang sửa bệnh nhân " + safe(patient.getCode()) + ".", false);
    }

    private void clearForm() {
        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
        idField.clear();
        codeField.clear();
        fullNameField.clear();
        ageField.clear();
        genderComboBox.setValue(null);
        phoneField.clear();
        addressField.clear();
        doctorComboBox.setValue(null);
        statusComboBox.setValue(PatientStatus.WAITING);
        setStatus("Sẵn sàng tạo bệnh nhân mới.", false);
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

    private String displayDoctor(Patient patient) {
        if (patient == null) {
            return "";
        }
        if (!isBlank(patient.getAssignedDoctorName())) {
            return patient.getAssignedDoctorName();
        }
        return doctorName(patient.getAssignedDoctorId());
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

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
