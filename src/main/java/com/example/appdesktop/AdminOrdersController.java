package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminOrdersController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private VBox ordersContainer;
    @FXML private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("pt", "PT"));

    private List<ClientPortalDataService.OrderItem> allOrders;
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        allOrders = new ArrayList<>(dataService.mockOrders());
        statusCombo.getItems().setAll("all", "pending", "paid", "in_production", "shipped", "delivered");
        statusCombo.setValue("all");
        refreshOrders();
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSearchChanged() {
        refreshOrders();
    }

    @FXML
    private void onStatusChanged() {
        refreshOrders();
    }

    private void refreshOrders() {
        ordersContainer.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        String statusFilter = statusCombo.getValue();

        List<ClientPortalDataService.OrderItem> filtered = allOrders.stream()
                .filter(o -> search.isBlank()
                        || o.id().toLowerCase(Locale.ROOT).contains(search)
                        || o.clientName().toLowerCase(Locale.ROOT).contains(search))
                .filter(o -> "all".equals(statusFilter) || o.status().equals(statusFilter))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ClientPortalDataService.OrderItem order : filtered) {
            ordersContainer.getChildren().add(createOrderCard(order));
        }
    }

    private VBox createOrderCard(ClientPortalDataService.OrderItem order) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Header
        HBox header = new HBox(8);
        VBox idBox = new VBox(2);
        Label id = new Label(order.id());
        id.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label clientDate = new Label(order.clientName() + "  |  " + dateFormatter.format(order.date()));
        clientDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        idBox.getChildren().addAll(id, clientDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(dataService.orderStatusLabel(order.status()));
        statusLabel.setStyle(statusStyle(order.status()));

        Label paymentLabel = new Label(order.paymentStatus().equals("paid") ? "Pago" : "Pendente");
        paymentLabel.setStyle(order.paymentStatus().equals("paid")
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;"
                : "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;");

        header.getChildren().addAll(idBox, spacer, statusLabel, paymentLabel);

        // Delivery address
        Label address = new Label(order.address().company() + ", " + order.address().street()
                + ", " + order.address().postalCode() + " " + order.address().city());
        address.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        // Items
        VBox itemsBox = new VBox(4);
        for (ClientPortalDataService.OrderLine line : order.items()) {
            HBox row = new HBox(8);
            Label left = new Label(line.productName() + " x " + line.quantity());
            left.setStyle("-fx-text-fill: #374151;");
            Region lineSpacer = new Region();
            HBox.setHgrow(lineSpacer, Priority.ALWAYS);
            Label right = new Label(currencyFormat.format(line.lineTotal()));
            right.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            row.getChildren().addAll(left, lineSpacer, right);
            itemsBox.getChildren().add(row);
        }

        // Footer
        HBox footer = new HBox(8);
        Label total = new Label("Total: " + currencyFormat.format(order.total()));
        total.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        javafx.scene.control.Button updateBtn = new javafx.scene.control.Button("Atualizar Estado");
        updateBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        updateBtn.setOnAction(e -> updateOrderStatus(order));

        footer.getChildren().addAll(total, footerSpacer, updateBtn);

        card.getChildren().addAll(header, address, itemsBox, footer);
        return card;
    }

    private void updateOrderStatus(ClientPortalDataService.OrderItem order) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Atualizar Estado da Encomenda");
        dialog.setHeaderText(order.id() + " - " + order.clientName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(220);
        grid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> newStatusCombo = new ComboBox<>();
        newStatusCombo.getItems().setAll("pending", "paid", "in_production", "shipped", "delivered");
        newStatusCombo.setValue(order.status());

        ComboBox<String> newPaymentCombo = new ComboBox<>();
        newPaymentCombo.getItems().setAll("pending", "paid");
        newPaymentCombo.setValue(order.paymentStatus());

        grid.addRow(0, new Label("Estado:"),   newStatusCombo);
        grid.addRow(1, new Label("Pagamento:"), newPaymentCombo);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            ClientPortalDataService.OrderItem updated = new ClientPortalDataService.OrderItem(
                    order.id(), newStatusCombo.getValue(), newPaymentCombo.getValue(),
                    order.clientName(), order.address(), order.date(), order.items());
            allOrders.replaceAll(o -> o.id().equals(order.id()) ? updated : o);
            refreshOrders();
            showInfo("Estado atualizado com sucesso.");
        }
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "pending"       -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "paid"          -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped"       -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered"     -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default              -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
