package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class OrdersController implements ClientPage {

    @FXML
    private TextField searchField;

    @FXML
    private VBox ordersContainer;

    @FXML
    private VBox emptyState;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("pt", "PT"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ClientPortalDataService.OrderItem> allOrders;
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        allOrders = dataService.mockOrders();
        refreshOrders();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSearchChanged() {
        refreshOrders();
    }

    @FXML
    private void onExploreCatalog() {
        if (navigator != null) {
            navigator.navigateTo("catalog");
        }
    }

    private void refreshOrders() {
        ordersContainer.getChildren().clear();

        List<ClientPortalDataService.OrderItem> filtered = dataService.filterOrders(allOrders, searchField.getText());
        boolean empty = filtered.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);

        for (ClientPortalDataService.OrderItem order : filtered) {
            ordersContainer.getChildren().add(createOrderCard(order));
        }
    }

    private VBox createOrderCard(ClientPortalDataService.OrderItem order) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox top = new HBox(8);
        Label id = new Label(order.id());
        id.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label date = new Label(dateFormatter.format(order.date()));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        VBox idBox = new VBox(2, id, date);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(dataService.orderStatusLabel(order.status()));
        status.setStyle(statusStyle(order.status()));
        top.getChildren().addAll(idBox, spacer, status);

        VBox lines = new VBox(4);
        for (ClientPortalDataService.OrderLine line : order.items()) {
            HBox row = new HBox(8);
            Label left = new Label(line.productName() + " x " + line.quantity());
            left.setStyle("-fx-text-fill: #374151;");
            Region lineSpacer = new Region();
            HBox.setHgrow(lineSpacer, Priority.ALWAYS);
            Label right = new Label(currencyFormat.format(line.lineTotal()));
            right.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            row.getChildren().addAll(left, lineSpacer, right);
            lines.getChildren().add(row);
        }

        HBox footer = new HBox(8);
        Label total = new Label("Total: " + currencyFormat.format(order.total()));
        total.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button repeatButton = new Button("Repetir Encomenda");
        repeatButton.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        repeatButton.setOnAction(event -> repeatOrder(order));

        Button detailsButton = new Button("Ver Detalhes");
        detailsButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;");
        detailsButton.setOnAction(event -> openOrderDetail(order.id()));

        footer.getChildren().addAll(total, footerSpacer, repeatButton, detailsButton);

        card.getChildren().addAll(top, lines, footer);
        return card;
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "pending" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "paid" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered" -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private void repeatOrder(ClientPortalDataService.OrderItem order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Repetir Encomenda");
        alert.setHeaderText("Pedido preparado");
        alert.setContentText("A encomenda " + order.id() + " foi adicionada ao carrinho.");
        alert.showAndWait();
    }

    private void openOrderDetail(String orderId) {
        if (navigator != null) {
            navigator.navigateTo("order-detail:" + orderId);
        }
    }
}
