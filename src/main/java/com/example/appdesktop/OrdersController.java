package com.example.appdesktop;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.EncomendaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import java.util.stream.Collectors;

public class OrdersController implements ClientPage {

    @FXML private TextField searchField;
    @FXML private VBox ordersContainer;
    @FXML private VBox emptyState;

    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("pt", "PT"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<EncomendaCatalogo> allOrders = List.of();
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        loadOrders();
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
        if (navigator != null) navigator.navigateTo("catalog");
    }

    private void loadOrders() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            renderEmpty();
            return;
        }

        encomendaService.findByUtilizadorId(currentUser.getId())
                .whenComplete((orders, error) -> Platform.runLater(() -> {
                    if (error != null || orders == null) {
                        allOrders = List.of();
                    } else {
                        allOrders = orders.stream()
                                .filter(order -> order != null)
                                .filter(order -> {
                                    String status = order.getEstado();
                                    return status == null || !"carrinho".equalsIgnoreCase(status);
                                })
                                .toList();
                    }
                    refreshOrders();
                }));
    }

    private void refreshOrders() {
        ordersContainer.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        List<EncomendaCatalogo> filtered = allOrders.stream()
                .filter(o -> search.isBlank()
                        || ("ENC-" + o.getId()).toLowerCase(Locale.ROOT).contains(search)
                        || (o.getLinhas() != null && o.getLinhas().stream()
                                .anyMatch(l -> l.getNomeProduto() != null
                                        && l.getNomeProduto().toLowerCase(Locale.ROOT).contains(search))))
                .collect(Collectors.toList());

        boolean empty = filtered.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);

        for (EncomendaCatalogo order : filtered) {
            ordersContainer.getChildren().add(createOrderCard(order));
        }
    }

    private void renderEmpty() {
        ordersContainer.getChildren().clear();
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private VBox createOrderCard(EncomendaCatalogo order) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Header
        HBox top = new HBox(8);
        String encId = "ENC-" + order.getId();
        Label id = new Label(encId);
        id.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        String dataStr = order.getDataEncomenda() != null ? dateFormatter.format(order.getDataEncomenda()) : "Data desconhecida";
        Label date = new Label(dataStr);
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        VBox idBox = new VBox(2, id, date);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String estado = normalizeStatus(order.getEstado());
        Label status = new Label(statusLabel(estado));
        status.setStyle(statusStyle(estado));
        top.getChildren().addAll(idBox, spacer, status);

        // Linhas
        VBox lines = new VBox(4);
        if (order.getLinhas() != null) {
            for (EncomendaCatalogo.LinhaEncomenda linha : order.getLinhas()) {
                HBox row = new HBox(8);
                int qty = linha.getQuantidade() != null ? linha.getQuantidade() : 0;
                Label left = new Label(linha.getNomeProduto() + " x " + qty);
                left.setStyle("-fx-text-fill: #374151;");
                Region lineSpacer = new Region();
                HBox.setHgrow(lineSpacer, Priority.ALWAYS);
                Label right = new Label(currencyFormat.format(linha.lineTotal()));
                right.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                row.getChildren().addAll(left, lineSpacer, right);
                lines.getChildren().add(row);
            }
        }

        // Footer
        HBox footer = new HBox(8);
        Label total = new Label("Total: " + currencyFormat.format(order.total()));
        total.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button detailsButton = new Button("Ver Detalhes");
        detailsButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;");
        detailsButton.setOnAction(event -> openOrderDetail(String.valueOf(order.getId())));

        footer.getChildren().addAll(total, footerSpacer, detailsButton);
        card.getChildren().addAll(top, lines, footer);
        return card;
    }

    private void openOrderDetail(String orderId) {
        if (navigator != null) navigator.navigateTo("order-detail:" + orderId);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "pending";
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pendente", "pending" -> "pending";
            case "pago", "paid" -> "paid";
            case "enviado", "shipped" -> "shipped";
            case "entregue", "delivered" -> "delivered";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending" -> "Pendente";
            case "paid" -> "Pago";
            case "shipped" -> "Enviado";
            case "delivered" -> "Entregue";
            default -> formatStatusLabel(status);
        };
    }

    private String formatStatusLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Pendente";
        }
        String normalized = raw.replace('_', ' ').trim().toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.length() > 1 ? part.substring(1) : "");
        }
        return builder.length() == 0 ? "Pendente" : builder.toString();
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "pending"       -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "paid"          -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped"       -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered"     -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default              -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }
}