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

public class ClientLayoutController implements ClientPageNavigator {

    private static final String PAGE_DASHBOARD = "dashboard";
    private static final String PAGE_CATALOG = "catalog";
    private static final String PAGE_BRIEFING = "briefing";
    private static final String PAGE_PROJECTS = "projects";
    private static final String PAGE_ORDERS = "orders";
    private static final String PAGE_PROJECT_DETAIL = "project-detail:";
    private static final String PAGE_ORDER_DETAIL = "order-detail:";
    private static final String PAGE_CART = "cart";
    private static final String PAGE_CHECKOUT = "checkout";
    private static final String PAGE_PROJECT_PAYMENT = "project-payment:";

    @FXML
    private Label userNameLabel;

    @FXML
    private Label companyLabel;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button catalogButton;

    @FXML
    private Button briefingButton;

    @FXML
    private Button projectsButton;

    @FXML
    private Button ordersButton;

    @FXML
    private StackPane contentPane;

    private final Map<String, Button> navButtons = new HashMap<>();

    @FXML
    private void initialize() {
        navButtons.put(PAGE_DASHBOARD, dashboardButton);
        navButtons.put(PAGE_CATALOG, catalogButton);
        navButtons.put(PAGE_BRIEFING, briefingButton);
        navButtons.put(PAGE_PROJECTS, projectsButton);
        navButtons.put(PAGE_ORDERS, ordersButton);
        openPage(PAGE_DASHBOARD);
    }

    public void setClientIdentity(String name, String company) {
        userNameLabel.setText(name == null || name.isBlank() ? "Cliente" : name);
        companyLabel.setText(company == null || company.isBlank() ? "Conta Cliente" : company);
    }

    @FXML
    private void onDashboard() {
        openPage(PAGE_DASHBOARD);
    }

    @FXML
    private void onCatalog() {
        openPage(PAGE_CATALOG);
    }

    @FXML
    private void onBriefing() {
        openPage(PAGE_BRIEFING);
    }

    @FXML
    private void onProjects() {
        openPage(PAGE_PROJECTS);
    }

    @FXML
    private void onOrders() {
        openPage(PAGE_ORDERS);
    }

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

            if (controller instanceof ClientPage clientPage) {
                clientPage.setNavigator(this);
            }

            if (pageKey.startsWith(PAGE_PROJECT_DETAIL) && controller instanceof ProjectDetailController projectDetailController) {
                projectDetailController.setProjectId(pageKey.substring(PAGE_PROJECT_DETAIL.length()));
            }
            if (pageKey.startsWith(PAGE_ORDER_DETAIL) && controller instanceof OrderDetailController orderDetailController) {
                orderDetailController.setOrderId(pageKey.substring(PAGE_ORDER_DETAIL.length()));
            }
            if (pageKey.startsWith(PAGE_PROJECT_PAYMENT) && controller instanceof ProjectPaymentController paymentController) {
                String ids = pageKey.substring(PAGE_PROJECT_PAYMENT.length());
                String[] parts = ids.split("/");
                if (parts.length == 2) {
                    paymentController.setPaymentIds(parts[0], parts[1]);
                }
            }
            if (controller instanceof ClientDashboardController dashboardController) {
                dashboardController.setClientName(userNameLabel.getText());
            }

            contentPane.getChildren().setAll(page);
            updateActiveButton(normalizePageKey(pageKey));
        } catch (IOException ex) {
            showError("Nao foi possivel abrir a pagina.", ex.getMessage());
        }
    }

    private String resolveFxml(String pageKey) {
        return switch (pageKey) {
            case PAGE_CATALOG -> "catalog-view.fxml";
            case PAGE_BRIEFING -> "briefing-view.fxml";
            case PAGE_PROJECTS -> "projects-view.fxml";
            case PAGE_ORDERS -> "orders-view.fxml";
            case PAGE_CART -> "cart-view.fxml";
            case PAGE_CHECKOUT -> "checkout-view.fxml";
            case PAGE_DASHBOARD -> "client-dashboard-view.fxml";
            default -> {
                if (pageKey.startsWith(PAGE_PROJECT_DETAIL)) {
                    yield "project-detail-view.fxml";
                } else if (pageKey.startsWith(PAGE_ORDER_DETAIL)) {
                    yield "order-detail-view.fxml";
                } else if (pageKey.startsWith(PAGE_PROJECT_PAYMENT)) {
                    yield "project-payment-view.fxml";
                } else {
                    yield "client-dashboard-view.fxml";
                }
            }
        };
    }

    private String normalizePageKey(String pageKey) {
        if (pageKey.startsWith(PAGE_PROJECT_DETAIL)) {
            return PAGE_PROJECTS;
        }
        if (pageKey.startsWith(PAGE_ORDER_DETAIL)) {
            return PAGE_ORDERS;
        }
        if (pageKey.startsWith(PAGE_PROJECT_PAYMENT)) {
            return PAGE_PROJECTS;
        }
        return pageKey;
    }

    private void updateActiveButton(String pageKey) {
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(pageKey)) {
                entry.getValue().setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #78350f; -fx-font-weight: bold;");
            } else {
                entry.getValue().setStyle("-fx-background-color: transparent; -fx-text-fill: #374151;");
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