package com.example.appdesktop;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.EncomendaService;
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

    @FXML private Label orderIdLabel;
    @FXML private Label orderDateLabel;
    @FXML private Label orderStatusBadge;
    @FXML private ProgressBar trackingProgress;
    @FXML private Label trackingLabel;
    @FXML private VBox trackingStepsContainer;
    @FXML private VBox itemsContainer;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;
    @FXML private Label paymentBadge;
    @FXML private Label clientNameLabel;
    @FXML private Label companyLabel;
    @FXML private Label streetLabel;
    @FXML private Label cityLabel;
    @FXML private Label countryLabel;
    @FXML private VBox notFoundBox;
    @FXML private VBox contentBox;

    private final EncomendaService encomendaService = EncomendaService.getInstance();
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
        if (initialized) Platform.runLater(this::refresh);
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.navigateTo("orders");
    }

    private void refresh() {
        if (!initialized || orderId == null || orderId.isBlank()) {
            showNotFound();
            return;
        }

        Integer numericId = extractNumericId(orderId);
        if (numericId == null) {
            showNotFound();
            return;
        }

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showNotFound();
            return;
        }

        encomendaService.findByUtilizadorId(currentUser.getId())
                .whenComplete((orders, error) -> Platform.runLater(() -> {
                    if (error != null || orders == null) {
                        showNotFound();
                        return;
                    }
                    EncomendaCatalogo found = orders.stream()
                            .filter(o -> numericId.equals(o.getId()))
                            .findFirst()
                            .orElse(null);
                    if (found == null) showNotFound();
                    else renderOrder(found);
                }));
    }

    private void renderOrder(EncomendaCatalogo order) {
        notFoundBox.setVisible(false);
        notFoundBox.setManaged(false);
        contentBox.setVisible(true);
        contentBox.setManaged(true);

        String encId = "ENC-" + order.getId();
        orderIdLabel.setText(encId);
        String dataStr = order.getDataEncomenda() != null ? "Encomendada a " + dateFormatter.format(order.getDataEncomenda()) : "";
        orderDateLabel.setText(dataStr);

        String estado = normalizeStatus(order.getEstado());
        orderStatusBadge.setText(statusLabel(estado));
        orderStatusBadge.setStyle(statusStyle(estado));

        renderTracking(estado, order.getEstadoPagamento());
        renderItems(order);

        subtotalLabel.setText(currencyFormat.format(order.total()));
        totalLabel.setText(currencyFormat.format(order.total()));

        boolean pago = isPaymentConfirmed(estado, order.getEstadoPagamento());
        paymentBadge.setText(pago ? "Pago" : "Pendente");
        paymentBadge.setStyle(pago
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;"
                : "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;");

        // dados do cliente (via utilizador associado)
        Utilizador u = order.getIdUtilizador();
        clientNameLabel.setText(u != null && u.getNomeEmpresa() != null ? u.getNomeEmpresa() : "--");
        companyLabel.setText(u != null && u.getNomeEmpresa() != null ? u.getNomeEmpresa() : "--");
        streetLabel.setText("--");
        cityLabel.setText("--");
        countryLabel.setText("--");
    }

    private void renderTracking(String estado, String estadoPagamento) {
        trackingStepsContainer.getChildren().clear();
        boolean pago = isPaymentConfirmed(estado, estadoPagamento);

        List<TrackingStep> steps = new ArrayList<>();
        steps.add(new TrackingStep("Pedido Recebido", true));
        steps.add(new TrackingStep("Pagamento Confirmado", pago));
        steps.add(new TrackingStep("Enviado", List.of("shipped", "delivered").contains(estado)));
        steps.add(new TrackingStep("Entregue", "delivered".equals(estado)));

        long completed = steps.stream().filter(TrackingStep::completed).count();
        trackingProgress.setProgress(steps.isEmpty() ? 0 : (double) completed / steps.size());
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

    private void renderItems(EncomendaCatalogo order) {
        itemsContainer.getChildren().clear();
        if (order.getLinhas() == null) return;
        for (EncomendaCatalogo.LinhaEncomenda linha : order.getLinhas()) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

            VBox left = new VBox(2);
            Label name = new Label(linha.getNomeProduto());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            int qty = linha.getQuantidade() != null ? linha.getQuantidade() : 0;
            String precoStr = linha.getPrecoUnitario() != null ? currencyFormat.format(linha.getPrecoUnitario()) + " cada" : "";
            Label qty2 = new Label("Quantidade: " + qty + (precoStr.isBlank() ? "" : " | " + precoStr));
            qty2.setStyle("-fx-text-fill: #6b7280;");
            left.getChildren().addAll(name, qty2);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label total = new Label(currencyFormat.format(linha.lineTotal()));
            total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            row.getChildren().addAll(left, spacer, total);
            itemsContainer.getChildren().add(row);
        }
    }

    private void showNotFound() {
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        notFoundBox.setVisible(true);
        notFoundBox.setManaged(true);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "pending";
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pendente", "pending" -> "pending";
            case "pago", "paid"        -> "paid";
            case "enviado", "shipped"  -> "shipped";
            case "entregue", "delivered" -> "delivered";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "pending"       -> "Pendente";
            case "paid"          -> "Pago";
            case "shipped"       -> "Enviado";
            case "delivered"     -> "Entregue";
            default -> status;
        };
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

    private boolean isPaymentConfirmed(String normalizedEstado, String estadoPagamentoRaw) {
        String pagamento = normalizePaymentStatus(estadoPagamentoRaw);
        if (List.of("paid", "confirmed", "completed").contains(pagamento)) {
            return true;
        }

        // Fallback: quando a API nao envia estadoPagamento, inferimos pelo estado da encomenda.
        return List.of("paid", "shipped", "delivered").contains(normalizedEstado);
    }

    private String normalizePaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pago", "paid" -> "paid";
            case "confirmado", "confirmed" -> "confirmed";
            case "concluido", "completed", "liquidado" -> "completed";
            case "pendente", "pending" -> "pending";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private record TrackingStep(String label, boolean completed) {}

    private String normalizeOrderId(String rawId) {
        if (rawId == null) return null;
        String cleaned = rawId.trim().toUpperCase(Locale.ROOT);
        if (cleaned.isBlank()) return null;
        cleaned = cleaned.replace("/", "").replace("_", "-");
        if (cleaned.startsWith("ORDER-DETAIL:")) cleaned = cleaned.substring("ORDER-DETAIL:".length());
        if (cleaned.matches("ENC\\d+")) cleaned = "ENC-" + cleaned.substring(3);
        return cleaned;
    }

    private Integer extractNumericId(String normalizedId) {
        if (normalizedId == null) return null;
        if (normalizedId.matches("ENC-\\d+")) return Integer.parseInt(normalizedId.substring(4));
        if (normalizedId.matches("\\d+")) return Integer.parseInt(normalizedId);
        return null;
    }
}