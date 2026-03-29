package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AdminDashboardController implements AdminPage {

    @FXML private Label activeProjectsLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label revenueLabel;
    @FXML private VBox recentOrdersContainer;
    @FXML private VBox activeProjectsContainer;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        List<ClientPortalDataService.ProductItem> products = dataService.mockProducts();
        List<ClientPortalDataService.ProjectItem> projects = dataService.mockProjects();
        List<ClientPortalDataService.OrderItem> orders = dataService.mockOrders();

        long activeProjects = projects.stream().filter(p -> !"completed".equals(p.status())).count();
        long pendingOrders  = orders.stream().filter(o -> "in_production".equals(o.status())).count();
        long lowStock       = products.stream().filter(p -> p.stock() < 5).count();

        BigDecimal orderRevenue   = orders.stream().map(ClientPortalDataService.OrderItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal projectRevenue = projects.stream().map(p -> p.quote().total())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRevenue   = orderRevenue.add(projectRevenue);

        activeProjectsLabel.setText(String.valueOf(activeProjects));
        pendingOrdersLabel.setText(String.valueOf(pendingOrders));
        lowStockLabel.setText(String.valueOf(lowStock));
        revenueLabel.setText(currencyFormat.format(totalRevenue));

        renderRecentOrders(orders.stream().limit(3).toList());
        renderActiveProjects(projects.stream().filter(p -> !"completed".equals(p.status())).toList());
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    private void renderRecentOrders(List<ClientPortalDataService.OrderItem> orders) {
        recentOrdersContainer.getChildren().clear();
        for (ClientPortalDataService.OrderItem order : orders) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            VBox left = new VBox(2);
            Label id = new Label(order.id());
            id.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Label client = new Label(order.clientName() + "  |  " + dateFormatter.format(order.date()));
            client.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            left.getChildren().addAll(id, client);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(dataService.orderStatusLabel(order.status()));
            status.setStyle(orderStatusStyle(order.status()));

            Label total = new Label(currencyFormat.format(order.total()));
            total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            row.getChildren().addAll(left, spacer, status, total);
            recentOrdersContainer.getChildren().add(row);
        }
    }

    private void renderActiveProjects(List<ClientPortalDataService.ProjectItem> projects) {
        activeProjectsContainer.getChildren().clear();
        for (ClientPortalDataService.ProjectItem project : projects) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            VBox left = new VBox(2);
            Label title = new Label(project.title());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Label meta = new Label(project.quantity() + " pecas  |  " + currencyFormat.format(project.quote().total()));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            left.getChildren().addAll(title, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(dataService.projectStatusLabel(project.status()));
            status.setStyle(projectStatusStyle(project.status()));

            row.getChildren().addAll(left, spacer, status);
            activeProjectsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void onViewAllOrders() {
        if (navigator != null) {
            navigator.navigateTo("admin-orders");
        }
    }

    @FXML
    private void onViewAllProjects() {
        if (navigator != null) {
            navigator.navigateTo("admin-projects");
        }
    }

    @FXML
    private void onManageCatalog() {
        if (navigator != null) {
            navigator.navigateTo("admin-catalog");
        }
    }

    private String orderStatusStyle(String status) {
        return switch (status) {
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "paid"          -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped"       -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered"     -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default              -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private String projectStatusStyle(String status) {
        return switch (status) {
            case "quote_sent"    -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved"      -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "completed"     -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            default              -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }
}
