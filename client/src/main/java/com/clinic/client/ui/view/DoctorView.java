package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.MedicalRecordRequest;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.PatientStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DoctorView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Patient> waitingPatients = FXCollections.observableArrayList();
    private final ObservableList<Patient> myPatients = FXCollections.observableArrayList();

    private TableView<Patient> waitingTable;
    private TableView<Patient> myPatientsTable;
    private Label selectedPatientLabel;
    private Label statusLabel;
    private TextArea diagnosisArea;
    private TextArea notesArea;
    private CheckBox requiresTestCheckBox;
    private Patient selectedPatient;

    public DoctorView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox content = new VBox(18);
        content.getChildren().addAll(createHeader(), createBody());
        root.setCenter(content);

        loadData();
        return root;
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Khám bệnh");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Nhận bệnh nhân vào khám và tạo hồ sơ bệnh án.");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Làm mới");
        refreshButton.setPrefHeight(38);
        refreshButton.setStyle(AppTheme.secondaryButtonStyle());
        refreshButton.setOnAction(event -> loadData());

        header.getChildren().addAll(titleBox, spacer, refreshButton);
        return header;
    }

    private Node createBody() {
        HBox body = new HBox(18);
        VBox left = new VBox(18, createWaitingCard(), createMyPatientsCard());
        HBox.setHgrow(left, Priority.ALWAYS);
        body.getChildren().addAll(left, createMedicalRecordCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createWaitingCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Bệnh nhân chờ khám");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button acceptButton = new Button("Nhận khám");
        acceptButton.setPrefHeight(36);
        acceptButton.setStyle(AppTheme.primaryButtonStyle());
        acceptButton.setOnAction(event -> acceptSelectedPatient());
        header.getChildren().addAll(title, spacer, acceptButton);

        waitingTable = createPatientTable();
        waitingTable.setItems(waitingPatients);
        waitingTable.setPrefHeight(220);
        waitingTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, patient) -> selectPatient(patient));

        card.getChildren().addAll(header, waitingTable);
        return card;
    }

    private Node createMyPatientsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());

        Label title = new Label("Bệnh nhân của tôi");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        myPatientsTable = createPatientTable();
        myPatientsTable.setItems(myPatients);
        myPatientsTable.setPrefHeight(240);
        myPatientsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, patient) -> selectPatient(patient));

        card.getChildren().addAll(title, myPatientsTable);
        return card;
    }

    private TableView<Patient> createPatientTable() {
        TableView<Patient> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Patient, String> codeColumn = new TableColumn<>("Mã BN");
        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getCode())));

        TableColumn<Patient, String> nameColumn = new TableColumn<>("Họ tên");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getFullName())));

        TableColumn<Patient, String> infoColumn = new TableColumn<>("Thông tin");
        infoColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAge() + " tuổi · " + safe(data.getValue().getGender())));

        TableColumn<Patient, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatus())));

        TableColumn<Patient, String> visitColumn = new TableColumn<>("Lần khám");
        visitColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        table.getColumns().addAll(codeColumn, nameColumn, infoColumn, statusColumn, visitColumn);
        return table;
    }

    private Node createMedicalRecordCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(420);

        Label title = new Label("Hồ sơ bệnh án");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedPatientLabel = new Label("Chưa chọn bệnh nhân");
        selectedPatientLabel.setWrapText(true);
        selectedPatientLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        diagnosisArea = createTextArea("Nhập chẩn đoán");
        notesArea = createTextArea("Ghi chú, chỉ định, triệu chứng...");
        requiresTestCheckBox = new CheckBox("Cần xét nghiệm");
        requiresTestCheckBox.setStyle("-fx-text-fill: " + AppTheme.TEXT + ";");

        addRow(form, 0, "Chẩn đoán", diagnosisArea);
        addRow(form, 1, "Ghi chú", notesArea);
        form.add(requiresTestCheckBox, 1, 2);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Xóa form");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> clearRecordForm());

        Button saveButton = new Button("Lưu hồ sơ");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> createMedicalRecord());

        actions.getChildren().addAll(clearButton, saveButton);

        statusLabel = new Label("Chọn bệnh nhân đang khám để tạo hồ sơ.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedPatientLabel, form, actions, statusLabel);
        return card;
    }

    private TextArea createTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(5);
        area.setWrapText(true);
        area.setStyle(AppTheme.textAreaStyle());
        return area;
    }

    private void addRow(GridPane form, int row, String label, Node input) {
        Label labelNode = new Label(label);
        labelNode.setMinWidth(120);
        labelNode.setStyle(AppTheme.formLabelStyle());
        form.add(labelNode, 0, row);
        form.add(input, 1, row);
        GridPane.setHgrow(input, Priority.ALWAYS);
    }

    @Override
    public void refreshData() {
        loadData();
    }

    private void loadData() {
        String doctorId = currentDoctorId();
        if (isBlank(doctorId)) {
            setStatus("Không xác định được bác sĩ đang đăng nhập.", true);
            return;
        }
        try {
            List<Patient> assigned = appContext.getService().getPatientsByDoctor(doctorId);
            List<Patient> waiting = assigned.stream()
                    .filter(patient -> {
                        String status = patient.getStatus();
                        return status != null && (status.equalsIgnoreCase(PatientStatus.WAITING)
                                || status.equalsIgnoreCase(PatientStatus.ASSIGNED));
                    })
                    .toList();
            waitingPatients.setAll(waiting);
            myPatients.setAll(assigned);
            setStatus("Đã tải " + waiting.size() + " bệnh nhân chờ khám.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách bệnh nhân", e.getMessage());
        }
    }

    private void acceptSelectedPatient() {
        Patient patient = waitingTable.getSelectionModel().getSelectedItem();
        if (patient == null) {
            setStatus("Vui lòng chọn bệnh nhân chờ khám.", true);
            return;
        }
        try {
            ApiResponse response = appContext.getService().acceptPatient(patient.getCode(), currentDoctorId());
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                appContext.reloadAfterLocalChange(this::loadData);
                selectPatient(patient);
            }
        } catch (RemoteException e) {
            showError("Không thể nhận bệnh nhân", e.getMessage());
        }
    }

    private void createMedicalRecord() {
        if (selectedPatient == null) {
            setStatus("Vui lòng chọn bệnh nhân cần tạo hồ sơ.", true);
            return;
        }
        if (!PatientStatus.EXAMINING.equalsIgnoreCase(safe(selectedPatient.getStatus()))
                && !PatientStatus.HAS_TEST_RESULT.equalsIgnoreCase(safe(selectedPatient.getStatus()))) {
            setStatus("Chỉ tạo hồ sơ cho bệnh nhân đang khám hoặc đã có kết quả xét nghiệm.", true);
            return;
        }
        if (isBlank(diagnosisArea.getText())) {
            setStatus("Chẩn đoán không được để trống.", true);
            return;
        }

        MedicalRecordRequest request = new MedicalRecordRequest(
                selectedPatient.getCode(),
                currentDoctorId(),
                diagnosisArea.getText().trim(),
                notesArea.getText().trim(),
                requiresTestCheckBox.isSelected()
        );

        try {
            ApiResponse response = appContext.getService().createMedicalRecord(request);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                clearRecordForm();
                appContext.reloadAfterLocalChange(this::loadData);
            }
        } catch (RemoteException e) {
            showError("Không thể lưu hồ sơ bệnh án", e.getMessage());
        }
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        if (patient == null) {
            selectedPatientLabel.setText("Chưa chọn bệnh nhân");
            return;
        }
        selectedPatientLabel.setText(safe(patient.getCode()) + " · " + safe(patient.getFullName()) + " · " + safe(patient.getStatus()));
        setStatus("Đã chọn bệnh nhân " + safe(patient.getCode()) + ".", false);
    }

    private void clearRecordForm() {
        diagnosisArea.clear();
        notesArea.clear();
        requiresTestCheckBox.setSelected(false);
        setStatus("Đã xóa form hồ sơ.", false);
    }

    private String currentDoctorId() {
        return appContext.getCurrentUser() == null ? null : appContext.getCurrentUser().getId();
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
