package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.TestResultRequest;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TestingView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Patient> waitingPatients = FXCollections.observableArrayList();

    private TableView<Patient> table;
    private Label selectedPatientLabel;
    private Label statusLabel;
    private TextField testNameField;
    private TextArea resultArea;
    private TextArea notesArea;
    private Patient selectedPatient;

    public TestingView(AppContext appContext) {
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
        Label title = new Label("Nhập kết quả xét nghiệm");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Xử lý bệnh nhân đang chờ xét nghiệm và trả kết quả cho bác sĩ.");
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
        body.getChildren().addAll(createWaitingPatientsCard(), createResultFormCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createWaitingPatientsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(700);

        Label title = new Label("Bệnh nhân chờ xét nghiệm");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        table = new TableView<>();
        table.setItems(waitingPatients);
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
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getAssignedDoctorName())));

        TableColumn<Patient, String> visitColumn = new TableColumn<>("Lần khám");
        visitColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        table.getColumns().addAll(codeColumn, nameColumn, infoColumn, phoneColumn, doctorColumn, visitColumn);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, patient) -> selectPatient(patient));

        card.getChildren().addAll(title, table);
        return card;
    }

    private Node createResultFormCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(420);

        Label title = new Label("Phiếu kết quả");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedPatientLabel = new Label("Chưa chọn bệnh nhân");
        selectedPatientLabel.setWrapText(true);
        selectedPatientLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        testNameField = new TextField();
        testNameField.setPromptText("VD: Công thức máu, X-quang...");
        testNameField.setPrefHeight(38);
        testNameField.setStyle(AppTheme.inputStyle());

        resultArea = createTextArea("Nhập kết quả xét nghiệm");
        notesArea = createTextArea("Ghi chú thêm nếu có");

        addRow(form, 0, "Tên xét nghiệm", testNameField);
        addRow(form, 1, "Kết quả", resultArea);
        addRow(form, 2, "Ghi chú", notesArea);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Xóa form");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> clearForm());

        Button saveButton = new Button("Lưu kết quả");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> saveTestResult());

        actions.getChildren().addAll(clearButton, saveButton);

        statusLabel = new Label("Chọn bệnh nhân chờ xét nghiệm để nhập kết quả.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedPatientLabel, form, actions, statusLabel);
        return card;
    }

    private TextArea createTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(6);
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
        try {
            List<Patient> waiting = appContext.getService().getPatientsByStatus(PatientStatus.WAITING_FOR_TEST);
            waitingPatients.setAll(waiting);
            setStatus("Đã tải " + waiting.size() + " bệnh nhân chờ xét nghiệm.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách bệnh nhân chờ xét nghiệm", e.getMessage());
        }
    }

    private void saveTestResult() {
        if (selectedPatient == null) {
            setStatus("Vui lòng chọn bệnh nhân cần nhập kết quả.", true);
            return;
        }
        if (!PatientStatus.WAITING_FOR_TEST.equalsIgnoreCase(safe(selectedPatient.getStatus()))) {
            setStatus("Chỉ nhập kết quả cho bệnh nhân đang chờ xét nghiệm.", true);
            return;
        }
        if (isBlank(testNameField.getText())) {
            setStatus("Tên xét nghiệm không được để trống.", true);
            return;
        }
        if (isBlank(resultArea.getText())) {
            setStatus("Kết quả xét nghiệm không được để trống.", true);
            return;
        }

        TestResultRequest request = new TestResultRequest(
                selectedPatient.getCode(),
                selectedPatient.getAssignedDoctorId(),
                currentTestingStaffId(),
                testNameField.getText().trim(),
                resultArea.getText().trim(),
                notesArea.getText().trim()
        );

        try {
            ApiResponse response = appContext.getService().createTestResult(request);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                clearForm();
                appContext.reloadAfterLocalChange(this::loadData);
            }
        } catch (RemoteException e) {
            showError("Không thể lưu kết quả xét nghiệm", e.getMessage());
        }
    }

    private void selectPatient(Patient patient) {
        selectedPatient = patient;
        if (patient == null) {
            selectedPatientLabel.setText("Chưa chọn bệnh nhân");
            return;
        }
        selectedPatientLabel.setText(safe(patient.getCode()) + " · " + safe(patient.getFullName()) + " · Bác sĩ: " + safe(patient.getAssignedDoctorName()));
        setStatus("Đã chọn bệnh nhân " + safe(patient.getCode()) + ".", false);
    }

    private void clearForm() {
        if (table != null) {
            table.getSelectionModel().clearSelection();
        }
        selectedPatient = null;
        selectedPatientLabel.setText("Chưa chọn bệnh nhân");
        testNameField.clear();
        resultArea.clear();
        notesArea.clear();
        setStatus("Đã xóa form kết quả.", false);
    }

    private String currentTestingStaffId() {
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
