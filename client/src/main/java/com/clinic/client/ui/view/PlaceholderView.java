package com.clinic.client.ui.view;

import com.clinic.client.ui.theme.AppTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PlaceholderView {
    private final String title;
    private final String description;

    public PlaceholderView(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Node createContent() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(28));
        root.setStyle(AppTheme.rootStyle());

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28));
        card.setStyle(AppTheme.cardStyle());
        card.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT + ";");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.MUTED + ";");

        Label nextStep = new Label("Khung màn hình đã sẵn sàng. Bước tiếp theo sẽ gắn table, form và gọi ClinicService.");
        nextStep.setWrapText(true);
        nextStep.setStyle("-fx-text-fill: " + AppTheme.PRIMARY_DARK + "; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, descriptionLabel, nextStep);
        root.getChildren().add(card);
        return root;
    }
}
