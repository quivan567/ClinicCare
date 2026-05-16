package com.clinic.client.ui.theme;

public final class AppTheme {
    public static final String WHITE = "#ffffff";
    public static final String BACKGROUND = "#f4f9ff";
    public static final String SURFACE = "#ffffff";
    public static final String PRIMARY = "#0284c7";
    public static final String PRIMARY_DARK = "#0369a1";
    public static final String PRIMARY_SOFT = "#e0f2fe";
    public static final String TEXT = "#0f172a";
    public static final String MUTED = "#64748b";
    public static final String BORDER = "#dbeafe";
    public static final String ERROR = "#dc2626";
    public static final String SUCCESS = "#059669";

    private AppTheme() {
    }

    public static String rootStyle() {
        return "-fx-background-color: " + BACKGROUND + "; -fx-font-family: 'Segoe UI', Arial, sans-serif;";
    }

    public static String cardStyle() {
        return "-fx-background-color: " + SURFACE + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(2,132,199,0.10), 24, 0, 0, 8);";
    }

    public static String primaryButtonStyle() {
        return "-fx-background-color: " + PRIMARY + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0 16;";
    }

    public static String secondaryButtonStyle() {
        return "-fx-background-color: " + PRIMARY_SOFT + ";"
                + "-fx-text-fill: " + PRIMARY_DARK + ";"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0 16;";
    }

    public static String dangerButtonStyle() {
        return "-fx-background-color: #fee2e2;"
                + "-fx-text-fill: " + ERROR + ";"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 0 16;";
    }

    public static String sidebarButtonStyle(boolean active) {
        if (active) {
            return "-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-alignment: center-left; -fx-cursor: hand;";
        }
        return "-fx-background-color: transparent; -fx-text-fill: " + TEXT + "; -fx-background-radius: 10; -fx-alignment: center-left; -fx-cursor: hand;";
    }

    public static String inputStyle() {
        return "-fx-background-color: white;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 0 12;";
    }

    public static String formLabelStyle() {
        return "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;";
    }

    public static String textAreaStyle() {
        return "-fx-background-color: white;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 10;"
                + "-fx-background-radius: 10;";
    }
}
