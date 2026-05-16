package com.clinic.client.ui.view;

import com.clinic.client.ui.core.AppContext;
import com.clinic.client.ui.theme.AppTheme;
import com.clinic.shared.model.Role;
import com.clinic.shared.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardView {
    private final AppContext appContext;

    public DashboardView(AppContext appContext) {
        this.appContext = appContext;
    }

    public Node createContent() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        User user = appContext.getCurrentUser();

        Label greeting = new Label("Xin chào, " + user.getFullName());
        greeting.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label description = new Label("Chọn chức năng ở menu bên trái để bắt đầu làm việc.");
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.MUTED + ";");

        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);

        String[] features = featuresByRole(user.getRole());
        for (int i = 0; i < features.length; i++) {
            VBox card = createFeatureCard(features[i]);
            cards.add(card, i % 3, i / 3);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }

        root.getChildren().addAll(greeting, description, cards);
        return root;
    }

    private VBox createFeatureCard(String text) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22));
        card.setMinHeight(110);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(AppTheme.cardStyle());

        Label title = new Label(text);
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY_DARK + ";");

        Label subtitle = new Label("Đang chờ dựng màn hình chi tiết");
        subtitle.setStyle("-fx-text-fill: " + AppTheme.MUTED + ";");

        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private String[] featuresByRole(Role role) {
        return switch (role) {
            case ADMIN -> new String[]{"Quản lý người dùng", "Quản lý bệnh nhân", "Theo dõi toa thuốc", "Hồ sơ bệnh án"};
            case RECEPTIONIST -> new String[]{"Tiếp nhận bệnh nhân", "Tra cứu bệnh nhân", "Phân công bác sĩ"};
            case DOCTOR -> new String[]{"Danh sách chờ khám", "Tạo hồ sơ bệnh án", "Yêu cầu xét nghiệm", "Kê đơn thuốc"};
            case PHARMACIST -> new String[]{"Tra cứu toa thuốc", "Cấp phát thuốc"};
            case TESTING_STAFF -> new String[]{"Danh sách chờ xét nghiệm", "Nhập kết quả xét nghiệm"};
        };
    }
}
