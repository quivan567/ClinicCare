package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.CreatePrescriptionRequest;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.PatientStatus;
import com.clinic.shared.model.PrescriptionItem;
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

public class DoctorPrescriptionView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Patient> eligiblePatients = FXCollections.observableArrayList();
    private final ObservableList<PrescriptionItem> prescriptionItems = FXCollections.observableArrayList();

    private TableView<Patient> patientTable;
    private TableView<PrescriptionItem> itemTable;
    private Label selectedPatientLabel;
    private Label statusLabel;
    private TextField medicineNameField;
    private TextField dosageField;
    private TextArea instructionArea;
    private Patient selectedPatient;

    public DoctorPrescriptionView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox content = new VBox(18);
        content.getChildren().addAll(createHeader(), createBody());
        root.setCenter(content);

        loadPatients();
        return root;
    }

    private Node createHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Kê đơn thuốc");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Chọn bệnh nhân đã khám hoặc đã có kết quả xét nghiệm để tạo toa thuốc.");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Làm mới");
        refreshButton.setPrefHeight(38);
        refreshButton.setStyle(AppTheme.secondaryButtonStyle());
        refreshButton.setOnAction(event -> loadPatients());

        header.getChildren().addAll(titleBox, spacer, refreshButton);
        return header;
    }

    private Node createBody() {
        HBox body = new HBox(18);
        body.getChildren().addAll(createEligiblePatientsCard(), createPrescriptionCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createEligiblePatientsCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(650);

        Label title = new Label("Bệnh nhân có thể kê đơn");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        patientTable = new TableView<>();
        patientTable.setItems(eligiblePatients);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        patientTable.setPrefHeight(520);

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

        patientTable.getColumns().addAll(codeColumn, nameColumn, infoColumn, statusColumn, visitColumn);
        patientTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, patient) -> selectPatient(patient));

        card.getChildren().addAll(title, patientTable);
        return card;
    }

    private Node createPrescriptionCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(470);

        Label title = new Label("Chi tiết toa thuốc");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedPatientLabel = new Label("Chưa chọn bệnh nhân");
        selectedPatientLabel.setWrapText(true);
        selectedPatientLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        medicineNameField = createInput("Tên thuốc");
        dosageField = createInput("VD: 2 viên/ngày");
        instructionArea = createTextArea("VD: Uống sau ăn, sáng/tối...");

        addRow(form, 0, "Tên thuốc", medicineNameField);
        addRow(form, 1, "Liều dùng", dosageField);
        addRow(form, 2, "Hướng dẫn", instructionArea);

        HBox itemActions = new HBox(10);
        itemActions.setAlignment(Pos.CENTER_RIGHT);

        Button removeItemButton = new Button("Xóa thuốc");
        removeItemButton.setPrefHeight(36);
        removeItemButton.setStyle(AppTheme.dangerButtonStyle());
        removeItemButton.setOnAction(event -> removeSelectedItem());

        Button addItemButton = new Button("Thêm thuốc");
        addItemButton.setPrefHeight(36);
        addItemButton.setStyle(AppTheme.primaryButtonStyle());
        addItemButton.setOnAction(event -> addPrescriptionItem());

        itemActions.getChildren().addAll(removeItemButton, addItemButton);

        itemTable = new TableView<>();
        itemTable.setItems(prescriptionItems);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemTable.setPrefHeight(180);

        TableColumn<PrescriptionItem, String> medicineColumn = new TableColumn<>("Thuốc");
        medicineColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getMedicineName())));

        TableColumn<PrescriptionItem, String> dosageColumn = new TableColumn<>("Liều");
        dosageColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDosage())));

        TableColumn<PrescriptionItem, String> instructionColumn = new TableColumn<>("Hướng dẫn");
        instructionColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getInstructions())));

        itemTable.getColumns().addAll(medicineColumn, dosageColumn, instructionColumn);
        itemTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> fillItemForm(item));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button clearButton = new Button("Xóa toa");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> clearPrescription());

        Button saveButton = new Button("Tạo toa thuốc");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> createPrescription());

        actions.getChildren().addAll(clearButton, saveButton);

        statusLabel = new Label("Chọn bệnh nhân, thêm thuốc rồi tạo toa.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedPatientLabel, form, itemActions, itemTable, actions, statusLabel);
        return card;
    }

    private TextField createInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle(AppTheme.inputStyle());
        return field;
    }

    private TextArea createTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(3);
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
        loadPatients();
    }

    private void loadPatients() {
        try {
            List<Patient> patients = appContext.getService().getPatientsByDoctor(currentDoctorId()).stream()
                    .filter(this::canCreatePrescription)
                    .toList();
            eligiblePatients.setAll(patients);
            setStatus("Đã tải " + patients.size() + " bệnh nhân có thể kê đơn.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách bệnh nhân", e.getMessage());
        }
    }

    private boolean canCreatePrescription(Patient patient) {
        if (patient == null) {
            return false;
        }
        String status = safe(patient.getStatus());
        return PatientStatus.EXAMINED.equalsIgnoreCase(status)
                || PatientStatus.HAS_TEST_RESULT.equalsIgnoreCase(status)
                || PatientStatus.WAITING_FOR_PRESCRIPTION.equalsIgnoreCase(status);
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

    private void addPrescriptionItem() {
        if (isBlank(medicineNameField.getText()) || isBlank(dosageField.getText())) {
            setStatus("Tên thuốc và liều dùng không được để trống.", true);
            return;
        }
        PrescriptionItem selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.setMedicineName(medicineNameField.getText().trim());
            selectedItem.setDosage(dosageField.getText().trim());
            selectedItem.setInstructions(instructionArea.getText().trim());
            itemTable.refresh();
            itemTable.getSelectionModel().clearSelection();
            clearItemForm();
            setStatus("Đã cập nhật thuốc trong toa.", false);
            return;
        }

        prescriptionItems.add(new PrescriptionItem(
                medicineNameField.getText().trim(),
                dosageField.getText().trim(),
                instructionArea.getText().trim()
        ));
        clearItemForm();
        setStatus("Đã thêm thuốc vào toa.", false);
    }

    private void removeSelectedItem() {
        PrescriptionItem selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            setStatus("Vui lòng chọn thuốc cần xóa.", true);
            return;
        }
        prescriptionItems.remove(selectedItem);
        clearItemForm();
        setStatus("Đã xóa thuốc khỏi toa.", false);
    }

    private void fillItemForm(PrescriptionItem item) {
        if (item == null) {
            return;
        }
        medicineNameField.setText(safe(item.getMedicineName()));
        dosageField.setText(safe(item.getDosage()));
        instructionArea.setText(safe(item.getInstructions()));
    }

    private void createPrescription() {
        if (selectedPatient == null) {
            setStatus("Vui lòng chọn bệnh nhân cần kê đơn.", true);
            return;
        }
        if (!canCreatePrescription(selectedPatient)) {
            setStatus("Bệnh nhân hiện không ở trạng thái có thể kê đơn.", true);
            return;
        }
        if (prescriptionItems.isEmpty()) {
            setStatus("Toa thuốc phải có ít nhất một thuốc.", true);
            return;
        }

        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                selectedPatient.getCode(),
                currentDoctorId(),
                List.copyOf(prescriptionItems)
        );

        try {
            ApiResponse response = appContext.getService().createPrescription(request);
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                clearPrescription();
                loadPatients();
            }
        } catch (RemoteException e) {
            showError("Không thể tạo toa thuốc", e.getMessage());
        }
    }

    private void clearPrescription() {
        if (patientTable != null) {
            patientTable.getSelectionModel().clearSelection();
        }
        selectedPatient = null;
        selectedPatientLabel.setText("Chưa chọn bệnh nhân");
        prescriptionItems.clear();
        clearItemForm();
        setStatus("Đã xóa toa hiện tại.", false);
    }

    private void clearItemForm() {
        medicineNameField.clear();
        dosageField.clear();
        instructionArea.clear();
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
