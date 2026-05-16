package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.dto.ApiResponse;
import com.clinic.shared.dto.PrescriptionSearchRequest;
import com.clinic.shared.dto.PrescriptionUpdateRequest;
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

import java.rmi.RemoteException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminPrescriptionManagementView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<Prescription> prescriptions = FXCollections.observableArrayList();
    private final ObservableList<PrescriptionItem> items = FXCollections.observableArrayList();

    private Map<String, Patient> patientsById = Map.of();
    private Map<String, User> usersById = Map.of();
    private TableView<Prescription> prescriptionTable;
    private TableView<PrescriptionItem> itemTable;
    private TextField patientSearchField;
    private TextField doctorSearchField;
    private ComboBox<String> searchStatusComboBox;
    private Label selectedPrescriptionLabel;
    private Label patientInfoLabel;
    private Label doctorInfoLabel;
    private ComboBox<String> statusComboBox;
    private TextField medicineField;
    private TextField dosageField;
    private TextField instructionsField;
    private Label statusLabel;
    private Prescription selectedPrescription;

    public AdminPrescriptionManagementView(AppContext appContext) {
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
        Label title = new Label("Quản lý toa thuốc");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Tra cứu, xem chi tiết, cập nhật thuốc/trạng thái và xóa toa thuốc.");
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
        card.setMinWidth(720);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        patientSearchField = createSearchField("Mã BN");
        doctorSearchField = createSearchField("Bác sĩ");
        searchStatusComboBox = new ComboBox<>(FXCollections.observableArrayList("", PrescriptionStatus.NEW, PrescriptionStatus.DISPENSED));
        searchStatusComboBox.setPromptText("Trạng thái");
        searchStatusComboBox.setPrefHeight(38);
        searchStatusComboBox.setPrefWidth(150);

        Button searchButton = new Button("Tìm kiếm");
        searchButton.setPrefHeight(38);
        searchButton.setStyle(AppTheme.primaryButtonStyle());
        searchButton.setOnAction(event -> searchPrescriptions());

        Button clearButton = new Button("Xóa lọc");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> {
            patientSearchField.clear();
            doctorSearchField.clear();
            searchStatusComboBox.setValue(null);
            loadPrescriptions();
        });

        patientSearchField.setOnAction(event -> searchPrescriptions());
        doctorSearchField.setOnAction(event -> searchPrescriptions());
        searchBar.getChildren().addAll(patientSearchField, doctorSearchField, searchStatusComboBox, searchButton, clearButton);

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

        card.getChildren().addAll(searchBar, prescriptionTable);
        return card;
    }

    private Node createDetailCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(450);

        Label title = new Label("Chi tiết toa thuốc");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedPrescriptionLabel = new Label("Chưa chọn toa thuốc");
        selectedPrescriptionLabel.setWrapText(true);
        selectedPrescriptionLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(12);
        infoGrid.setVgap(8);
        patientInfoLabel = createValueLabel();
        doctorInfoLabel = createValueLabel();
        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(PrescriptionStatus.NEW, PrescriptionStatus.DISPENSED));
        statusComboBox.setMaxWidth(Double.MAX_VALUE);
        statusComboBox.setPrefHeight(38);

        addInfoRow(infoGrid, 0, "Bệnh nhân", patientInfoLabel);
        addInfoRow(infoGrid, 1, "Bác sĩ", doctorInfoLabel);
        addInfoRow(infoGrid, 2, "Trạng thái", statusComboBox);

        itemTable = new TableView<>();
        itemTable.setItems(items);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itemTable.setPrefHeight(210);

        TableColumn<PrescriptionItem, String> medicineColumn = new TableColumn<>("Tên thuốc");
        medicineColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getMedicineName())));

        TableColumn<PrescriptionItem, String> dosageColumn = new TableColumn<>("Liều dùng");
        dosageColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDosage())));

        TableColumn<PrescriptionItem, String> instructionsColumn = new TableColumn<>("Hướng dẫn");
        instructionsColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getInstructions())));

        itemTable.getColumns().addAll(medicineColumn, dosageColumn, instructionsColumn);
        itemTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, item) -> fillItemForm(item));

        GridPane itemForm = new GridPane();
        itemForm.setHgap(12);
        itemForm.setVgap(10);

        medicineField = createInput("Tên thuốc");
        dosageField = createInput("Liều dùng");
        instructionsField = createInput("Hướng dẫn");

        addFormRow(itemForm, 0, "Thuốc", medicineField);
        addFormRow(itemForm, 1, "Liều", dosageField);
        addFormRow(itemForm, 2, "Hướng dẫn", instructionsField);

        HBox itemActions = new HBox(10);
        itemActions.setAlignment(Pos.CENTER_RIGHT);

        Button addItemButton = new Button("Thêm/Cập nhật thuốc");
        addItemButton.setPrefHeight(38);
        addItemButton.setStyle(AppTheme.secondaryButtonStyle());
        addItemButton.setOnAction(event -> addOrUpdateItem());

        Button removeItemButton = new Button("Xóa thuốc");
        removeItemButton.setPrefHeight(38);
        removeItemButton.setStyle(AppTheme.dangerButtonStyle());
        removeItemButton.setOnAction(event -> removeItem());

        itemActions.getChildren().addAll(addItemButton, removeItemButton);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = new Button("Lưu toa");
        saveButton.setPrefHeight(38);
        saveButton.setStyle(AppTheme.primaryButtonStyle());
        saveButton.setOnAction(event -> updatePrescription());

        Button deleteButton = new Button("Xóa toa");
        deleteButton.setPrefHeight(38);
        deleteButton.setStyle(AppTheme.dangerButtonStyle());
        deleteButton.setOnAction(event -> deletePrescription());

        actions.getChildren().addAll(saveButton, deleteButton);

        statusLabel = new Label("Chọn toa thuốc để xem hoặc chỉnh sửa.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedPrescriptionLabel, infoGrid, itemTable, itemForm, itemActions, actions, statusLabel);
        return card;
    }

    private TextField createSearchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setPrefWidth(150);
        field.setStyle(AppTheme.inputStyle());
        return field;
    }

    private TextField createInput(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(38);
        field.setStyle(AppTheme.inputStyle());
        return field;
    }

    private Label createValueLabel() {
        Label label = new Label("");
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + AppTheme.TEXT + ";");
        return label;
    }

    private void addInfoRow(GridPane grid, int row, String label, Node value) {
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
            loadPrescriptions();
        } catch (RemoteException e) {
            showError("Không thể tải dữ liệu toa thuốc", e.getMessage());
        }
    }

    private void loadPrescriptions() {
        try {
            List<Prescription> result = appContext.getService().getPrescriptions();
            prescriptions.setAll(result);
            clearDetail();
            setStatus("Đã tải " + result.size() + " toa thuốc.", false);
        } catch (RemoteException e) {
            showError("Không thể tải danh sách toa thuốc", e.getMessage());
        }
    }

    private void searchPrescriptions() {
        try {
            String status = searchStatusComboBox.getValue();
            List<Prescription> result = appContext.getService().getPrescriptionsByStatus(new PrescriptionSearchRequest(
                    patientSearchField.getText(),
                    doctorSearchField.getText(),
                    status == null ? "" : status
            ));
            prescriptions.setAll(result);
            clearDetail();
            setStatus("Tìm thấy " + result.size() + " toa thuốc.", false);
        } catch (RemoteException e) {
            showError("Không thể tìm kiếm toa thuốc", e.getMessage());
        }
    }

    private void selectPrescription(Prescription prescription) {
        selectedPrescription = prescription;
        if (prescription == null) {
            clearDetail();
            return;
        }
        selectedPrescriptionLabel.setText(safe(prescription.getCode()) + " · " + formatDateTime(prescription));
        patientInfoLabel.setText(patientLabel(prescription));
        doctorInfoLabel.setText(doctorName(prescription.getDoctorId()));
        statusComboBox.setValue(prescription.getStatus());
        items.setAll(prescription.getItems() == null ? List.of() : prescription.getItems());
        clearItemForm();
        setStatus("Đang xem toa " + safe(prescription.getCode()) + ".", false);
    }

    private void addOrUpdateItem() {
        if (isBlank(medicineField.getText()) || isBlank(dosageField.getText())) {
            setStatus("Tên thuốc và liều dùng không được để trống.", true);
            return;
        }
        PrescriptionItem selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            items.add(new PrescriptionItem(medicineField.getText().trim(), dosageField.getText().trim(), instructionsField.getText().trim()));
        } else {
            selectedItem.setMedicineName(medicineField.getText().trim());
            selectedItem.setDosage(dosageField.getText().trim());
            selectedItem.setInstructions(instructionsField.getText().trim());
            itemTable.refresh();
        }
        clearItemForm();
        setStatus("Đã cập nhật danh sách thuốc. Bấm Lưu toa để ghi xuống hệ thống.", false);
    }

    private void removeItem() {
        PrescriptionItem selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            setStatus("Vui lòng chọn thuốc cần xóa.", true);
            return;
        }
        items.remove(selectedItem);
        clearItemForm();
        setStatus("Đã xóa thuốc khỏi toa. Bấm Lưu toa để ghi xuống hệ thống.", false);
    }

    private void updatePrescription() {
        if (selectedPrescription == null) {
            setStatus("Vui lòng chọn toa thuốc cần cập nhật.", true);
            return;
        }
        if (items.isEmpty()) {
            setStatus("Toa thuốc phải có ít nhất một thuốc.", true);
            return;
        }
        try {
            ApiResponse response = appContext.getService().updatePrescription(
                    new PrescriptionUpdateRequest(selectedPrescription.getId(), List.copyOf(items), statusComboBox.getValue())
            );
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                loadData();
            }
        } catch (RemoteException e) {
            showError("Không thể cập nhật toa thuốc", e.getMessage());
        }
    }

    private void deletePrescription() {
        if (selectedPrescription == null) {
            setStatus("Vui lòng chọn toa thuốc cần xóa.", true);
            return;
        }
        try {
            ApiResponse response = appContext.getService().deletePrescription(selectedPrescription.getId());
            setStatus(response.getMessage(), !response.isSuccess());
            if (response.isSuccess()) {
                loadData();
            }
        } catch (RemoteException e) {
            showError("Không thể xóa toa thuốc", e.getMessage());
        }
    }

    private void fillItemForm(PrescriptionItem item) {
        if (item == null) {
            return;
        }
        medicineField.setText(safe(item.getMedicineName()));
        dosageField.setText(safe(item.getDosage()));
        instructionsField.setText(safe(item.getInstructions()));
    }

    private void clearItemForm() {
        if (itemTable != null) {
            itemTable.getSelectionModel().clearSelection();
        }
        medicineField.clear();
        dosageField.clear();
        instructionsField.clear();
    }

    private void clearDetail() {
        selectedPrescription = null;
        items.clear();
        if (selectedPrescriptionLabel != null) {
            selectedPrescriptionLabel.setText("Chưa chọn toa thuốc");
        }
        if (patientInfoLabel != null) {
            patientInfoLabel.setText("");
        }
        if (doctorInfoLabel != null) {
            doctorInfoLabel.setText("");
        }
        if (statusComboBox != null) {
            statusComboBox.setValue(null);
        }
        if (medicineField != null) {
            clearItemForm();
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
        User doctor = usersById.get(doctorId);
        return doctor == null ? "" : safe(doctor.getFullName());
    }

    private int itemCount(Prescription prescription) {
        return prescription.getItems() == null ? 0 : prescription.getItems().size();
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
