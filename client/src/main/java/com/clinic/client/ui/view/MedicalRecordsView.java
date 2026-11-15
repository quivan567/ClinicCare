package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.model.MedicalRecord;
import com.clinic.shared.model.Role;
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

public class MedicalRecordsView implements RefreshableView {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext appContext;
    private final ObservableList<MedicalRecord> allRecords = FXCollections.observableArrayList();
    private final ObservableList<MedicalRecord> filteredRecords = FXCollections.observableArrayList();

    private TableView<MedicalRecord> table;
    private TextField searchField;
    private Label selectedRecordLabel;
    private Label patientInfoLabel;
    private Label doctorInfoLabel;
    private Label createdAtLabel;
    private TextArea diagnosisArea;
    private TextArea notesArea;
    private Label statusLabel;

    public MedicalRecordsView(AppContext appContext) {
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
        Label title = new Label("Hồ sơ bệnh án");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label subtitle = new Label("Xem lịch sử khám, chẩn đoán và ghi chú điều trị.");
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
        body.getChildren().addAll(createRecordListCard(), createDetailCard());
        HBox.setHgrow(body.getChildren().get(0), Priority.ALWAYS);
        return body;
    }

    private Node createRecordListCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle(AppTheme.cardStyle());
        card.setMinWidth(700);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Tìm theo mã/tên bệnh nhân, bác sĩ, chẩn đoán");
        searchField.setPrefHeight(38);
        searchField.setStyle(AppTheme.inputStyle());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("Tìm kiếm");
        searchButton.setPrefHeight(38);
        searchButton.setStyle(AppTheme.primaryButtonStyle());
        searchButton.setOnAction(event -> applyFilter());

        Button clearButton = new Button("Xóa lọc");
        clearButton.setPrefHeight(38);
        clearButton.setStyle(AppTheme.secondaryButtonStyle());
        clearButton.setOnAction(event -> {
            searchField.clear();
            applyFilter();
        });

        searchField.setOnAction(event -> applyFilter());
        searchBar.getChildren().addAll(searchField, searchButton, clearButton);

        table = new TableView<>();
        table.setItems(filteredRecords);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(520);

        TableColumn<MedicalRecord, String> patientColumn = new TableColumn<>("Bệnh nhân");
        patientColumn.setCellValueFactory(data -> new SimpleStringProperty(patientLabel(data.getValue())));

        TableColumn<MedicalRecord, String> doctorColumn = new TableColumn<>("Bác sĩ");
        doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(doctorName(data.getValue())));

        TableColumn<MedicalRecord, String> diagnosisColumn = new TableColumn<>("Chẩn đoán");
        diagnosisColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getDiagnosis())));

        TableColumn<MedicalRecord, String> createdAtColumn = new TableColumn<>("Ngày tạo");
        createdAtColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDateTime(data.getValue())));

        table.getColumns().addAll(patientColumn, doctorColumn, diagnosisColumn, createdAtColumn);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, record) -> selectRecord(record));

        card.getChildren().addAll(searchBar, table);
        return card;
    }

    private Node createDetailCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(AppTheme.cardStyle());
        card.setPrefWidth(430);

        Label title = new Label("Chi tiết hồ sơ");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        selectedRecordLabel = new Label("Chưa chọn hồ sơ");
        selectedRecordLabel.setWrapText(true);
        selectedRecordLabel.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(12);
        infoGrid.setVgap(8);

        patientInfoLabel = createValueLabel();
        doctorInfoLabel = createValueLabel();
        createdAtLabel = createValueLabel();

        addInfoRow(infoGrid, 0, "Bệnh nhân", patientInfoLabel);
        addInfoRow(infoGrid, 1, "Bác sĩ", doctorInfoLabel);
        addInfoRow(infoGrid, 2, "Ngày tạo", createdAtLabel);

        diagnosisArea = createReadOnlyArea("Chẩn đoán");
        notesArea = createReadOnlyArea("Ghi chú");

        Label diagnosisTitle = sectionLabel("Chẩn đoán");
        Label notesTitle = sectionLabel("Ghi chú");

        statusLabel = new Label("Chọn một hồ sơ để xem chi tiết.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, selectedRecordLabel, infoGrid, diagnosisTitle, diagnosisArea, notesTitle, notesArea, statusLabel);
        return card;
    }

    private TextArea createReadOnlyArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(5);
        area.setStyle("-fx-background-color: white; -fx-border-color: " + AppTheme.BORDER + "; -fx-border-radius: 10; -fx-background-radius: 10;");
        return area;
    }

    private Label createValueLabel() {
        Label label = new Label("");
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: " + AppTheme.TEXT + ";");
        return label;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-weight: bold;");
        return label;
    }

    private void addInfoRow(GridPane grid, int row, String label, Label value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: " + AppTheme.MUTED + "; -fx-font-weight: bold;");
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
            List<MedicalRecord> records = isCurrentUserDoctor()
                    ? appContext.getService().getMedicalRecordsByDoctor(appContext.getCurrentUser().getId())
                    : appContext.getService().getAllMedicalRecords();
            allRecords.setAll(records);
            applyFilter();
            setStatus("Đã tải " + records.size() + " hồ sơ bệnh án.", false);
        } catch (RemoteException e) {
            showError("Không thể tải hồ sơ bệnh án", e.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (keyword.isBlank()) {
            filteredRecords.setAll(allRecords);
            return;
        }

        filteredRecords.setAll(allRecords.stream()
                .filter(record -> contains(patientLabel(record), keyword)
                        || contains(doctorName(record), keyword)
                        || contains(record.getDiagnosis(), keyword)
                        || contains(record.getNotes(), keyword))
                .toList());
        setStatus("Tìm thấy " + filteredRecords.size() + " hồ sơ phù hợp.", false);
    }

    private void selectRecord(MedicalRecord record) {
        if (record == null) {
            clearDetail();
            return;
        }
        selectedRecordLabel.setText("Hồ sơ " + safe(record.getId()));
        patientInfoLabel.setText(patientLabel(record));
        doctorInfoLabel.setText(doctorName(record));
        createdAtLabel.setText(formatDateTime(record));
        diagnosisArea.setText(safe(record.getDiagnosis()));
        notesArea.setText(safe(record.getNotes()));
        setStatus("Đang xem hồ sơ của " + patientLabel(record) + ".", false);
    }

    private void clearDetail() {
        if (selectedRecordLabel != null) {
            selectedRecordLabel.setText("Chưa chọn hồ sơ");
        }
        if (patientInfoLabel != null) {
            patientInfoLabel.setText("");
        }
        if (doctorInfoLabel != null) {
            doctorInfoLabel.setText("");
        }
        if (createdAtLabel != null) {
            createdAtLabel.setText("");
        }
        if (diagnosisArea != null) {
            diagnosisArea.clear();
        }
        if (notesArea != null) {
            notesArea.clear();
        }
    }

    private boolean isCurrentUserDoctor() {
        return appContext.getCurrentUser() != null && appContext.getCurrentUser().getRole() == Role.DOCTOR;
    }

    private String patientLabel(MedicalRecord record) {
        if (record == null) {
            return "";
        }
        if (isBlank(record.getPatientName())) {
            return safe(record.getPatientCode());
        }
        return safe(record.getPatientCode()) + " · " + safe(record.getPatientName());
    }

    private String doctorName(MedicalRecord record) {
        return record == null ? "" : safe(record.getDoctorName());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String formatDateTime(MedicalRecord record) {
        if (record == null || record.getCreatedAt() == null) {
            return "";
        }
        return record.getCreatedAt().format(DATE_TIME_FORMATTER);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
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
