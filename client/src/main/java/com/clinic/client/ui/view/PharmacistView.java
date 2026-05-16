package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.DispensePrescriptionRequest;
import com.clinic.shared.model.Patient;
import com.clinic.shared.model.Prescription;
import com.clinic.shared.model.PrescriptionItem;
import com.clinic.shared.model.PrescriptionStatus;
import com.clinic.shared.model.User;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PharmacistView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Prescription> prescriptions = FXCollections.observableArrayList();
    private final ObservableList<PrescriptionItem> prescriptionItems = FXCollections.observableArrayList();

    private Map<String, Patient> patientsById = Map.of();
    private Map<String, User> usersById = Map.of();
    private TableView<Prescription> prescriptionTable;
    private TableView<PrescriptionItem> itemTable;
    private Label selectedPrescriptionLabel;
    private Label patientInfoLabel;
    private Label doctorInfoLabel;
    private Label statusLabel;
    private Prescription selectedPrescription;

    public PharmacistView(AppContext appContext) {
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
        Label title = new Label("Cấp phát thuốc");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Tra cứu toa thuốc mới tạo, xem chi tiết thuốc và xác nhận cấp phát.");
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
        body.getChildren().addAll(createPrescriptionListCard(), createDetailCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createPrescriptionListCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(700);

        Label title = new Label("Toa thuốc chờ cấp phát");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        prescriptionTable = new TableView<>();
        prescriptionTable.setItems(prescriptions);
        prescriptionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        prescriptionTable.setPrefHeight(520);

        TableColumn<Prescription, String> codeColumn = new TableColumn<>("Mã toa");
        codeColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getCode())));

        TableColumn<Prescription, String> patientColumn = new TableColumn<>("Bệnh nhân");
        patientColumn.setCellValueFactory(data -> new SimpleStringProperty(patientLabel(data.getValue())));

        TableColumn<Prescription, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(doctorName(data.getValue().getDoctorId())));

        TableColumn<Prescription, String> itemCountColumn = new TableColumn<>("Số thuốc");
        itemCountColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(itemCount(data.getValue()))));

        TableColumn<Prescription, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatus())));

        TableColumn<Prescription, String> createdAtColumn = new TableColumn<>("Ngày tạo");
        createdAtColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        prescriptionTable.getColumns().addAll(codeColumn, patientColumn, doctorColumn, itemCountColumn, statusColumn, createdAtColumn);
        prescriptionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, prescription) -> selectPrescription(prescription));

        card.getChildren().addAll(title, prescriptionTable);
        return card;
    }

    private Node createDetailCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(430);

        Label title = new Label("Chi tiết toa thuốc");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedPrescriptionLabel = new Label("Chưa chọn toa thuốc");
        selectedPrescriptionLabel.setWrapText(true);
        selectedPrescriptionLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(12);
        infoGrid.setVgap(8);

        patientInfoLabel = new Label("");
        patientInfoLabel.setWrapText(true);
        doctorInfoLabel = new Label("");
        doctorInfoLabel.setWrapText(true);

        addInfoRow(infoGrid, 0, "Bệnh nhân", patientInfoLabel);
        addInfoRow(infoGrid, 1, "Bác sĩ", doctorInfoLabel);

        itemTable = new TableView<>();
        itemTable.setItems(prescriptionItems);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemTable.setPrefHeight(300);

        TableColumn<PrescriptionItem, String> medicineColumn = new TableColumn<>("Tên thuốc");
        medicineColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getMedicineName())));

        TableColumn<PrescriptionItem, String> dosageColumn = new TableColumn<>("Liều dùng");
        dosageColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDosage())));

        TableColumn<PrescriptionItem, String> instructionsColumn = new TableColumn<>("Hướng dẫn");
        instructionsColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getInstructions())));

        itemTable.getColumns().addAll(medicineColumn, dosageColumn, instructionsColumn);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button dispenseButton = new Button("Xác nhận cấp phát");
        dispenseButton.setPrefHeight(38);
        dispenseButton.setStyle(AppTheme.primaryButtonStyle());
        dispenseButton.setOnAction(event -> dispensePrescription());

        actions.getChildren().add(dispenseButton);

        statusLabel = new Label("Chọn toa thuốc mới tạo để cấp phát.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedPrescriptionLabel, infoGrid, itemTable, actions, statusLabel);
        return card;
    }

    private void addInfoRow(GridPane grid, int row, String label, Label value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-weight: bold;");
        value.setStyle("-fx-text-fill: " + AppTheme.TEXT + ";");
        grid.add(labelNode, 0, row);
        grid.add(value, 1, row);
        GridPane.setHgrow(value, Priority.ALWAYS);
    }

    @Override
    public void refreshData() {
        loadData();
    }

    private void loadData() {
        try {
            patientsById = appContext.getService().getAllPatients().stream()
                    .collect(Collectors.toMap(Patient::getId, Function.identity(), (a, b) -> a));
            usersById = appContext.getService().getAllUsers().stream()
                    .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

            List<Prescription> waitingPrescriptions = appContext.getService().getPrescriptions().stream()
                    .filter(prescription -> PrescriptionStatus.NEW.equalsIgnoreCase(safe(prescription.getStatus())))
                    .toList();
            prescriptions.setAll(waitingPrescriptions);
            clearSelection();
            setStatus("Đã tải " + waitingPrescriptions.size() + " toa thuốc chờ cấp phát.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách toa thuốc", e.getMessage());
        }
    }

    private void selectPrescription(Prescription prescription) {
        selectedPrescription = prescription;
        if (prescription == null) {
            clearSelection();
            return;
        }

        selectedPrescriptionLabel.setText(safe(prescription.getCode()) + " · " + safe(prescription.getStatus()));
        patientInfoLabel.setText(patientLabel(prescription));
        doctorInfoLabel.setText(doctorName(prescription.getDoctorId()));
        prescriptionItems.setAll(prescription.getItems() == null ? List.of() : prescription.getItems());
        setStatus("Đã chọn toa " + safe(prescription.getCode()) + ".", false);
    }

    private void dispensePrescription() {
        if (selectedPrescription == null) {
            setStatus("Vui lòng chọn toa thuốc cần cấp phát.", true);
            return;
        }
        if (!PrescriptionStatus.NEW.equalsIgnoreCase(safe(selectedPrescription.getStatus()))) {
            setStatus("Chỉ cấp phát toa thuốc đang ở trạng thái Mới tạo.", true);
            return;
        }

        try {
            ApiResponse response = appContext.getService().dispensePrescription(
                    new DispensePrescriptionRequest(selectedPrescription.getId(), currentPharmacistId())
            );
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                loadData();
            }
        } catch (RemoteException e) {
            showError("Không thể cấp phát thuốc", e.getMessage());
        }
    }

    private void clearSelection() {
        selectedPrescription = null;
        prescriptionItems.clear();
        if (selectedPrescriptionLabel != null) {
            selectedPrescriptionLabel.setText("Chưa chọn toa thuốc");
        }
        if (patientInfoLabel != null) {
            patientInfoLabel.setText("");
        }
        if (doctorInfoLabel != null) {
            doctorInfoLabel.setText("");
        }
    }

    private String patientLabel(Prescription prescription) {
        Patient patient = patientsById.get(prescription.getPatientId());
        if (patient == null) {
            return safe(prescription.getPatientCode());
        }
        return safe(patient.getCode()) + " · " + safe(patient.getFullName());
    }

    private String doctorName(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            return "";
        }
        User doctor = usersById.get(doctorId);
        return doctor == null ? "" : safe(doctor.getFullName());
    }

    private int itemCount(Prescription prescription) {
        return prescription.getItems() == null ? 0 : prescription.getItems().size();
    }

    private String currentPharmacistId() {
        return appContext.getCurrentUser() == null ? null : appContext.getCurrentUser().getId();
    }

    private String formatDateTime(Prescription prescription) {
        if (prescription == null || prescription.getCreatedAt() == null) {
            return "";
        }
        return prescription.getCreatedAt().format(DATE_TIME_FORMATTER);
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
}
