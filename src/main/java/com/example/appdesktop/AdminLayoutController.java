package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AdminLayoutController implements AdminPageNavigator {

    private static final String PAGE_DASHBOARD = "admin-dashboard";
    private static final String PAGE_CATALOG   = "admin-catalog";
    private static final String PAGE_PROJECTS  = "admin-projects";
    private static final String PAGE_ORDERS    = "admin-orders";

    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Button dashboardButton;
    @FXML private Button catalogButton;
    @FXML private Button projectsButton;
    @FXML private Button ordersButton;
    @FXML private StackPane contentPane;

    private final Map<String, Button> navButtons = new HashMap<>();

    @FXML
    private void initialize() {
        navButtons.put(PAGE_DASHBOARD, dashboardButton);
        navButtons.put(PAGE_CATALOG,   catalogButton);
        navButtons.put(PAGE_PROJECTS,  projectsButton);
        navButtons.put(PAGE_ORDERS,    ordersButton);
        openPage(PAGE_DASHBOARD);
    }

    public void setAdminIdentity(String name, String role) {
        adminNameLabel.setText(name == null || name.isBlank() ? "Administrador" : name);
        adminRoleLabel.setText(role == null || role.isBlank() ? "Administrador" : role);
    }

    @FXML private void onDashboard()  { openPage(PAGE_DASHBOARD); }
    @FXML private void onCatalog()    { openPage(PAGE_CATALOG); }
    @FXML private void onProjects()   { openPage(PAGE_PROJECTS); }
    @FXML private void onOrders()     { openPage(PAGE_ORDERS); }

    @FXML
    private void onLogout() {
        try {
            Parent loginRoot = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml")).load();
            Stage stage = (Stage) contentPane.getScene().getWindow();
            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Taca Lab - Login");
        } catch (IOException ex) {
            showError("Nao foi possivel terminar sessao.", ex.getMessage());
        }
    }

    @Override
    public void navigateTo(String pageKey) {
        openPage(pageKey);
    }

    private void openPage(String pageKey) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(resolveFxml(pageKey)));
            Parent page = loader.load();
            Object controller = loader.getController();

            if (controller instanceof AdminPage adminPage) {
                adminPage.setNavigator(this);
            }

            contentPane.getChildren().setAll(page);
            updateActiveButton(pageKey);
        } catch (IOException ex) {
            showError("Nao foi possivel abrir a pagina.", ex.getMessage());
        }
    }

    private String resolveFxml(String pageKey) {
        return switch (pageKey) {
            case PAGE_CATALOG  -> "admin-catalog-view.fxml";
            case PAGE_PROJECTS -> "admin-projects-view.fxml";
            case PAGE_ORDERS   -> "admin-orders-view.fxml";
            default            -> "admin-dashboard-view.fxml";
        };
    }

    private void updateActiveButton(String pageKey) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(pageKey)) {
                entry.getValue().setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #78350f; -fx-font-weight: bold; -fx-alignment: center-left; -fx-padding: 10 12;");
            } else {
                entry.getValue().setStyle("-fx-background-color: transparent; -fx-text-fill: #374151; -fx-alignment: center-left; -fx-padding: 10 12;");
            }
        }
    }

    private void showError(String header, String details) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }
}
