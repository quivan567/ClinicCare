package com.clinic.client.ui;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.core.RefreshableView;
import com.clinic.client.ui.core.ScreenKey;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.client.ui.view.AdminPrescriptionManagementView;
import com.clinic.client.ui.view.AdminUsersView;
import com.clinic.client.ui.view.DashboardView;
import com.clinic.client.ui.view.DoctorPrescriptionView;
import com.clinic.client.ui.view.DoctorView;
import com.clinic.client.ui.view.MedicalRecordsView;
import com.clinic.client.ui.view.PatientManagementView;
import com.clinic.client.ui.view.PharmacistView;
import com.clinic.client.ui.view.ProfileView;
import com.clinic.client.ui.view.ReceptionView;
import com.clinic.client.ui.view.TestingView;
import com.clinic.shared.dto.RealtimeEventType;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.User;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainLayout {
    private final AppContext appContext;
    private final BorderPane root = new BorderPane();
    private final VBox menuBox = new VBox(8);
    private final Map<ScreenKey, Button> menuButtons = new LinkedHashMap<>();
    private final Map<ScreenKey, Node> screenCache = new EnumMap<>(ScreenKey.class);
    private final Map<ScreenKey, RefreshableView> refreshableByScreen = new EnumMap<>(ScreenKey.class);
    private final Set<ScreenKey> dirtyScreens = EnumSet.noneOf(ScreenKey.class);
    private final PauseTransition refreshDelay = new PauseTransition(Duration.millis(200));
    private ScreenKey activeScreen = ScreenKey.DASHBOARD;
    private RefreshableView activeRefreshableView;

    public MainLayout(AppContext appContext) {
        this.appContext = appContext;
        refreshDelay.setOnFinished(event -> refreshActiveContent());
    }

    public Scene createScene() {
        root.setStyle(AppTheme.rootStyle());
        root.setTop(createTopBar());
        root.setLeft(createSidebar());
        showScreen(ScreenKey.DASHBOARD);
        return new Scene(root, 1280, 720);
    }

    private Node createTopBar() {
        User user = appContext.getCurrentUser();
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(18, 24, 18, 24));
        topBar.setStyle("-fx-background-color: " + AppTheme.WHITE + "; -fx-border-color: transparent transparent " + AppTheme.BORDER + " transparent;");

        Label title = new Label("ClinicCare");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Label userInfo = new Label(user.getFullName() + " · " + roleLabel(user.getRole()));
        userInfo.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Đăng xuất");
        logoutButton.setPrefHeight(36);
        logoutButton.setStyle("-fx-background-color: " + AppTheme.PRIMARY_SOFT + "; -fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        logoutButton.setOnAction(event -> appContext.logout());

        topBar.getChildren().addAll(title, spacer, userInfo, logoutButton);
        return topBar;
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(18);
        sidebar.setPadding(new Insets(24, 16, 24, 16));
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: " + AppTheme.WHITE + "; -fx-border-color: transparent " + AppTheme.BORDER + " transparent transparent;");

        menuBox.setFillWidth(true);

        addMenu(ScreenKey.DASHBOARD, "Tổng quan");
        addRoleMenus(appContext.getCurrentUser().getRole());
        addMenu(ScreenKey.PROFILE, "Hồ sơ cá nhân");

        sidebar.getChildren().add(menuBox);
        return sidebar;
    }

    private void addRoleMenus(Role role) {
        if (role == Role.ADMIN) {
            addMenu(ScreenKey.USERS, "Quản lý người dùng");
            addMenu(ScreenKey.PATIENTS, "Quản lý bệnh nhân");
            addMenu(ScreenKey.PRESCRIPTIONS, "Quản lý toa thuốc");
            addMenu(ScreenKey.MEDICAL_RECORDS, "Hồ sơ bệnh án");
            return;
        }
        if (role == Role.RECEPTIONIST) {
            addMenu(ScreenKey.RECEPTION, "Tiếp nhận bệnh nhân");
            addMenu(ScreenKey.PATIENTS, "Tra cứu bệnh nhân");
            return;
        }
        if (role == Role.DOCTOR) {
            addMenu(ScreenKey.DOCTOR, "Khám bệnh");
            addMenu(ScreenKey.MEDICAL_RECORDS, "Hồ sơ bệnh án");
            addMenu(ScreenKey.PRESCRIPTIONS, "Kê đơn thuốc");
            return;
        }
        if (role == Role.TESTING_STAFF) {
            addMenu(ScreenKey.TESTING, "Nhập kết quả xét nghiệm");
            return;
        }
        if (role == Role.PHARMACIST) {
            addMenu(ScreenKey.PHARMACY, "Cấp phát thuốc");
        }
    }

    private void addMenu(ScreenKey key, String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(42);
        button.setPadding(new Insets(0, 14, 0, 14));
        button.setStyle(AppTheme.sidebarButtonStyle(key == activeScreen));
        button.setOnAction(event -> showScreen(key));
        menuButtons.put(key, button);
        menuBox.getChildren().add(button);
    }

    private void showScreen(ScreenKey key) {
        activeScreen = key;
        refreshMenuStyles();
        Node content = screenCache.get(key);
        if (content == null) {
            content = createContent(key);
            screenCache.put(key, content);
        } else {
            RefreshableView cachedView = refreshableByScreen.get(key);
            if (cachedView != null && dirtyScreens.remove(key)) {
                cachedView.refreshData();
            }
        }
        activeRefreshableView = refreshableByScreen.get(key);
        root.setCenter(content);
    }

    private void refreshMenuStyles() {
        menuButtons.forEach((key, button) -> button.setStyle(AppTheme.sidebarButtonStyle(key == activeScreen)));
    }

    private Node createContent(ScreenKey key) {
        activeRefreshableView = null;
        return switch (key) {
            case DASHBOARD -> new DashboardView(appContext).createContent();
            case RECEPTION -> createRefreshableContent(new ReceptionView(appContext));
            case DOCTOR -> createRefreshableContent(new DoctorView(appContext));
            case TESTING -> createRefreshableContent(new TestingView(appContext));
            case PHARMACY -> createRefreshableContent(new PharmacistView(appContext));
            case USERS -> createRefreshableContent(new AdminUsersView(appContext));
            case PATIENTS -> createRefreshableContent(new PatientManagementView(appContext));
            case PRESCRIPTIONS -> appContext.getCurrentUser().getRole() == Role.DOCTOR
                    ? createRefreshableContent(new DoctorPrescriptionView(appContext))
                    : createRefreshableContent(new AdminPrescriptionManagementView(appContext));
            case MEDICAL_RECORDS -> createRefreshableContent(new MedicalRecordsView(appContext));
            case PROFILE -> new ProfileView(appContext).createContent();
        };
    }

    private Node createRefreshableContent(RefreshableView view) {
        refreshableByScreen.put(activeScreen, view);
        activeRefreshableView = view;
        if (view instanceof ReceptionView receptionView) {
            return receptionView.createContent();
        }
        if (view instanceof DoctorView doctorView) {
            return doctorView.createContent();
        }
        if (view instanceof TestingView testingView) {
            return testingView.createContent();
        }
        if (view instanceof PharmacistView pharmacistView) {
            return pharmacistView.createContent();
        }
        if (view instanceof PatientManagementView patientManagementView) {
            return patientManagementView.createContent();
        }
        if (view instanceof DoctorPrescriptionView doctorPrescriptionView) {
            return doctorPrescriptionView.createContent();
        }
        if (view instanceof AdminPrescriptionManagementView adminPrescriptionManagementView) {
            return adminPrescriptionManagementView.createContent();
        }
        if (view instanceof MedicalRecordsView medicalRecordsView) {
            return medicalRecordsView.createContent();
        }
        if (view instanceof AdminUsersView adminUsersView) {
            return adminUsersView.createContent();
        }
        throw new IllegalArgumentException("Unsupported refreshable view: " + view.getClass().getName());
    }

    public void onRealtimeEvent(String type) {
        dirtyScreens.addAll(affectedScreens(type));
        refreshDelay.playFromStart();
    }

    public void refreshActiveContent() {
        if (activeRefreshableView == null || !dirtyScreens.contains(activeScreen)) {
            return;
        }
        if (appContext.isRealtimeSuppressed()) {
            dirtyScreens.remove(activeScreen);
            return;
        }
        dirtyScreens.remove(activeScreen);
        activeRefreshableView.refreshData();
    }

    private Set<ScreenKey> affectedScreens(String type) {
        if (type == null) {
            return EnumSet.allOf(ScreenKey.class);
        }
        return switch (type) {
            case RealtimeEventType.PATIENT_CHANGED -> EnumSet.of(
                    ScreenKey.RECEPTION, ScreenKey.DOCTOR, ScreenKey.TESTING, ScreenKey.PATIENTS, ScreenKey.PRESCRIPTIONS);
            case RealtimeEventType.PRESCRIPTION_CHANGED -> EnumSet.of(ScreenKey.PRESCRIPTIONS, ScreenKey.PHARMACY);
            case RealtimeEventType.MEDICAL_RECORD_CHANGED -> EnumSet.of(ScreenKey.MEDICAL_RECORDS, ScreenKey.DOCTOR);
            case RealtimeEventType.TEST_RESULT_CHANGED -> EnumSet.of(ScreenKey.TESTING, ScreenKey.DOCTOR);
            case RealtimeEventType.USER_CHANGED -> EnumSet.of(ScreenKey.USERS, ScreenKey.RECEPTION, ScreenKey.PATIENTS);
            default -> EnumSet.noneOf(ScreenKey.class);
        };
    }

    private String roleLabel(Role role) {
        return switch (role) {
            case ADMIN -> "Quản trị";
            case RECEPTIONIST -> "Lễ tân";
            case DOCTOR -> "Bác sĩ";
            case PHARMACIST -> "Dược sĩ";
            case TESTING_STAFF -> "Xét nghiệm";
        };
    }
}
