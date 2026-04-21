package com.example.appdesktop;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.EncomendaService;
import javafx.application.Platform;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminOrdersController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label totalOrdersLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label paidOrdersLabel;
    @FXML private Label shippedOrdersLabel;
    @FXML private Label deliveredOrdersLabel;
    @FXML private VBox ordersContainer;
    @FXML private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("pt", "PT"));

    private List<EncomendaCatalogo> allOrders = new ArrayList<>();
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll("Todos", "Pendente", "Pago", "Enviado", "Entregue");
        statusCombo.setValue("Todos");
        loadOrders();
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

    private void loadOrders() {
        encomendaService.findAll()
                .whenComplete((orders, error) -> Platform.runLater(() -> {
                    if (error != null || orders == null) {
                        allOrders = List.of();
                    } else {
                        allOrders = orders.stream()
                                .filter(order -> order != null)
                                .filter(order -> !"carrinho".equals(normalizeStatus(order.getEstado())))
                                .toList();
                    }
                    refreshOrders();
                }));
    }

    private void refreshOrders() {
        ordersContainer.getChildren().clear();

        updateStats();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        String statusFilter = statusCombo.getValue();
        String normalizedFilter = normalizeStatusFilter(statusFilter);

        List<EncomendaCatalogo> filtered = allOrders.stream()
                .filter(order -> {
                    String idText = order.getId() == null ? "enc-?" : "enc-" + order.getId();
                    String client = resolveClientName(order).toLowerCase(Locale.ROOT);
                    return search.isBlank()
                            || idText.toLowerCase(Locale.ROOT).contains(search)
                            || client.contains(search);
                })
                .filter(order -> "all".equals(normalizedFilter)
                        || normalizeStatus(order.getEstado()).equals(normalizedFilter))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (EncomendaCatalogo order : filtered) {
            ordersContainer.getChildren().add(createOrderCard(order));
        }
    }

    private void updateStats() {
        if (totalOrdersLabel == null) {
            return;
        }

        totalOrdersLabel.setText(String.valueOf(allOrders.size()));
        pendingOrdersLabel.setText(String.valueOf(allOrders.stream().filter(o -> "pending".equals(normalizeStatus(o.getEstado()))).count()));
        paidOrdersLabel.setText(String.valueOf(allOrders.stream().filter(o -> "paid".equals(normalizeStatus(o.getEstado()))).count()));
        shippedOrdersLabel.setText(String.valueOf(allOrders.stream().filter(o -> "shipped".equals(normalizeStatus(o.getEstado()))).count()));
        deliveredOrdersLabel.setText(String.valueOf(allOrders.stream().filter(o -> "delivered".equals(normalizeStatus(o.getEstado()))).count()));
    }

    private VBox createOrderCard(EncomendaCatalogo order) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(8);
        VBox idBox = new VBox(2);
        String orderId = order.getId() == null ? "ENC-?" : "ENC-" + order.getId();
        Label id = new Label(orderId);
        id.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        LocalDate date = order.getDataEncomenda() == null ? LocalDate.now() : order.getDataEncomenda();
        Label clientDate = new Label(resolveClientName(order) + "  |  " + dateFormatter.format(date));
        clientDate.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        idBox.getChildren().addAll(id, clientDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String status = normalizeStatus(order.getEstado());
        Label statusLabel = new Label(dataService.orderStatusLabel(status));
        statusLabel.setStyle(statusStyle(status));

        header.getChildren().addAll(idBox, spacer, statusLabel);

        VBox itemsBox = new VBox(4);
        if (order.getLinhas() != null && !order.getLinhas().isEmpty()) {
            for (EncomendaCatalogo.LinhaEncomenda line : order.getLinhas()) {
                HBox row = new HBox(8);
                String productName = line.getNomeProduto() == null ? "Produto" : line.getNomeProduto();
                int qty = line.getQuantidade() == null ? 0 : line.getQuantidade();
                Label left = new Label(productName + " x " + qty);
                left.setStyle("-fx-text-fill: #374151;");
                Region lineSpacer = new Region();
                HBox.setHgrow(lineSpacer, Priority.ALWAYS);
                Label right = new Label(currencyFormat.format(line.lineTotal()));
                right.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                row.getChildren().addAll(left, lineSpacer, right);
                itemsBox.getChildren().add(row);
            }
        } else {
            Label emptyItems = new Label("Sem itens disponiveis");
            emptyItems.setStyle("-fx-text-fill: #6b7280;");
            itemsBox.getChildren().add(emptyItems);
        }

        HBox footer = new HBox(8);
        Label total = new Label("Total: " + currencyFormat.format(order.total()));
        total.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        javafx.scene.control.Button updateBtn = new javafx.scene.control.Button("Atualizar Estado");
        updateBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        updateBtn.setOnAction(e -> updateOrderStatus(order));

        footer.getChildren().addAll(total, footerSpacer, updateBtn);

        card.getChildren().addAll(header, itemsBox, footer);
        return card;
    }

    private void updateOrderStatus(EncomendaCatalogo order) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Atualizar Estado da Encomenda");
        dialog.setHeaderText((order.getId() == null ? "ENC-?" : "ENC-" + order.getId()) + " - " + resolveClientName(order));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(220);
        grid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> newStatusCombo = new ComboBox<>();
        newStatusCombo.getItems().setAll("pending", "paid", "shipped", "delivered");
        newStatusCombo.setValue(normalizeStatus(order.getEstado()));

        grid.addRow(0, new Label("Estado:"), newStatusCombo);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String newStatus = newStatusCombo.getValue();
            encomendaService.updateEstado(order.getId(), toBackendOrderStatus(newStatus))
                    .whenComplete((updated, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showInfo("Nao foi possivel atualizar o estado.");
                            return;
                        }
                        loadOrders();
                        showInfo("Estado atualizado com sucesso.");
                    }));
        }
    }

    private String resolveClientName(EncomendaCatalogo order) {
        Utilizador user = order.getIdUtilizador();
        if (user != null && user.getNomeEmpresa() != null && !user.getNomeEmpresa().isBlank()) {
            return user.getNomeEmpresa();
        }
        return "Cliente";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pendente", "pending" -> "pending";
            case "paga", "pago", "paid" -> "paid";
            case "enviada", "enviado", "shipped" -> "shipped";
            case "entregue", "delivered" -> "delivered";
            case "carrinho", "cart" -> "carrinho";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String toBackendOrderStatus(String normalizedStatus) {
        return switch (normalizedStatus) {
            case "paid" -> "paga";
            case "shipped" -> "enviada";
            case "delivered" -> "entregue";
            default -> "pendente";
        };
    }

    private String normalizePaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pago", "paid" -> "paid";
            default -> "pending";
        };
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "pending" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "paid" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered" -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String normalizeStatusFilter(String label) {
        if (label == null || label.isBlank()) {
            return "all";
        }
        if ("Todos".equalsIgnoreCase(label.trim())) {
            return "all";
        }
        return normalizeStatus(label);
    }
}