package com.example.appdesktop;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailController implements ClientPage {

    @FXML
    private Label orderIdLabel;

    @FXML
    private Label orderDateLabel;

    @FXML
    private Label orderStatusBadge;

    @FXML
    private ProgressBar trackingProgress;

    @FXML
    private Label trackingLabel;

    @FXML
    private VBox trackingStepsContainer;

    @FXML
    private VBox itemsContainer;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Label paymentBadge;

    @FXML
    private Label clientNameLabel;

    @FXML
    private Label companyLabel;

    @FXML
    private Label streetLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private Label countryLabel;

    @FXML
    private VBox notFoundBox;

    @FXML
    private VBox contentBox;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("pt", "PT"));
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private ClientPageNavigator navigator;
    private String orderId;
    private boolean initialized;

    @FXML
    private void initialize() {
        initialized = true;
        if (orderId != null && !orderId.isBlank()) {
            refresh();
        }
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    public void setOrderId(String orderId) {
        this.orderId = normalizeOrderId(orderId);
        if (initialized) {
            Platform.runLater(this::refresh);
        }
    }

    @FXML
    private void onBack() {
        if (navigator != null) {
            navigator.navigateTo("orders");
        }
    }

    private void refresh() {
        if (!initialized) {
            return;
        }

        if (orderId == null || orderId.isBlank()) {
            showNotFound();
            return;
        }

        dataService.findOrderById(orderId).ifPresentOrElse(this::renderOrder, this::showNotFound);
    }

    private void renderOrder(ClientPortalDataService.OrderItem order) {
        notFoundBox.setVisible(false);
        notFoundBox.setManaged(false);
        contentBox.setVisible(true);
        contentBox.setManaged(true);

        orderIdLabel.setText(order.id());
        orderDateLabel.setText("Encomendada a " + dateFormatter.format(order.date()));
        orderStatusBadge.setText(dataService.orderStatusLabel(order.status()));
        orderStatusBadge.setStyle(statusStyle(order.status()));

        renderTracking(order);
        renderItems(order);

        subtotalLabel.setText(currencyFormat.format(order.total()));
        totalLabel.setText(currencyFormat.format(order.total()));
        paymentBadge.setText("paid".equals(order.paymentStatus()) ? "Pago" : "Pendente");
        paymentBadge.setStyle("paid".equals(order.paymentStatus())
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;"
                : "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;");

        clientNameLabel.setText(order.clientName());
        companyLabel.setText(order.address().company());
        streetLabel.setText(order.address().street());
        cityLabel.setText(order.address().postalCode() + " " + order.address().city());
        countryLabel.setText(order.address().country());
    }

    private void renderTracking(ClientPortalDataService.OrderItem order) {
        trackingStepsContainer.getChildren().clear();

        List<TrackingStep> steps = buildSteps(order);
        long completed = steps.stream().filter(TrackingStep::completed).count();
        double progress = steps.isEmpty() ? 0 : (double) completed / steps.size();

        trackingProgress.setProgress(progress);
        trackingLabel.setText(completed + " de " + steps.size());

        for (TrackingStep step : steps) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8, 0, 8, 0));

            Label bullet = new Label(step.completed() ? "✓" : "- ");
            bullet.setStyle(step.completed()
                    ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                    : "-fx-text-fill: #9ca3af;");

            Label text = new Label(step.label());
            text.setStyle(step.completed()
                    ? "-fx-text-fill: #111827; -fx-font-weight: bold;"
                    : "-fx-text-fill: #6b7280;");

            row.getChildren().addAll(bullet, text);
            trackingStepsContainer.getChildren().add(row);
        }
    }

    private void renderItems(ClientPortalDataService.OrderItem order) {
        itemsContainer.getChildren().clear();
        for (ClientPortalDataService.OrderLine item : order.items()) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

            VBox left = new VBox(2);
            Label name = new Label(item.productName());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Label qty = new Label("Quantidade: " + item.quantity() + " | " + currencyFormat.format(item.unitPrice()) + " cada");
            qty.setStyle("-fx-text-fill: #6b7280;");
            left.getChildren().addAll(name, qty);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label total = new Label(currencyFormat.format(item.lineTotal()));
            total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            row.getChildren().addAll(left, spacer, total);
            itemsContainer.getChildren().add(row);
        }
    }

    private List<TrackingStep> buildSteps(ClientPortalDataService.OrderItem order) {
        List<TrackingStep> steps = new ArrayList<>();
        steps.add(new TrackingStep("Pedido Recebido", true));
        steps.add(new TrackingStep("Pagamento Confirmado", "paid".equals(order.paymentStatus())));
        steps.add(new TrackingStep("Em Producao", List.of("in_production", "shipped", "delivered").contains(order.status())));
        steps.add(new TrackingStep("Enviado", List.of("shipped", "delivered").contains(order.status())));
        steps.add(new TrackingStep("Entregue", "delivered".equals(order.status())));
        return steps;
    }

    private void showNotFound() {
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        notFoundBox.setVisible(true);
        notFoundBox.setManaged(true);
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

    private record TrackingStep(String label, boolean completed) {
    }

    private String normalizeOrderId(String rawId) {
        if (rawId == null) {
            return null;
        }

        String cleaned = rawId.trim().toUpperCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return null;
        }

        cleaned = cleaned.replace("/", "").replace("_", "-");
        if (cleaned.startsWith("ORDER-DETAIL:")) {
            cleaned = cleaned.substring("ORDER-DETAIL:".length());
        }
        if (cleaned.matches("ENC\\d+")) {
            cleaned = "ENC-" + cleaned.substring(3);
        }
        return cleaned;
    }
}
