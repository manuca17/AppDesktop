package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProjectDetailController implements ClientPage {

    @FXML
    private VBox notFoundBox;

    @FXML
    private VBox contentBox;

    @FXML
    private Label projectTitleLabel;

    @FXML
    private Label projectIdLabel;

    @FXML
    private Label projectStatusBadge;

    @FXML
    private Label createdAtLabel;

    @FXML
    private Label quantityLabel;

    @FXML
    private Label deadlineLabel;

    @FXML
    private Label quoteDesignLabel;

    @FXML
    private Label quoteMoldLabel;

    @FXML
    private Label quoteProductionLabel;

    @FXML
    private Label quoteTotalLabel;

    @FXML
    private Label quoteDetailsLabel;

    @FXML
    private Label briefingTypeLabel;

    @FXML
    private Label briefingBudgetLabel;

    @FXML
    private Label briefingDescriptionLabel;

    @FXML
    private ProgressBar trackingProgress;

    @FXML
    private Label trackingCountLabel;

    @FXML
    private VBox trackingContainer;

    @FXML
    private VBox meetingsContainer;

    @FXML
    private VBox messagesContainer;

    @FXML
    private VBox paymentsContainer;

    @FXML
    private TextField newMessageField;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private ClientPageNavigator navigator;
    private String projectId;
    private boolean initialized;

    @FXML
    private void initialize() {
        initialized = true;
        if (projectId != null && !projectId.isBlank()) {
            refresh();
        }
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    public void setProjectId(String projectId) {
        this.projectId = normalizeProjectId(projectId);
        if (initialized) {
            Platform.runLater(this::refresh);
        }
    }

    @FXML
    private void onBack() {
        if (navigator != null) {
            navigator.navigateTo("projects");
        }
    }

    @FXML
    private void onApproveQuote() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Orcamento");
        alert.setHeaderText("Funcionalidade indisponivel");
        alert.setContentText("A aprovacao de orcamento ainda nao esta disponivel na API.");
        alert.showAndWait();
    }

    @FXML
    private void onSendMessage() {
        showInfo("Chat", "Mensagens ainda nao disponiveis na API.");
    }

    @FXML
    private void onConfirmFirstMeeting() {
        showInfo("Reunioes", "Confirmacao de reuniao ainda nao disponivel na API.");
    }

    private void refresh() {
        if (!initialized) {
            return;
        }

        if (projectId == null || projectId.isBlank()) {
            showNotFound();
            return;
        }

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showNotFound();
            return;
        }

        Integer requestedId = extractProjectNumericId(projectId);
        if (requestedId == null) {
            showNotFound();
            return;
        }

        projetoService.findByUtilizadorId(currentUser.getId())
                .whenComplete((projects, error) -> Platform.runLater(() -> {
                    if (error != null || projects == null) {
                        showNotFound();
                        return;
                    }

                    ProjetoPersonalizado project = projects.stream()
                            .filter(item -> item != null && requestedId.equals(item.getId()))
                            .findFirst()
                            .orElse(null);

                    if (project == null) {
                        showNotFound();
                        return;
                    }

                    populateProject(project);
                }));
    }

    private void populateProject(ProjetoPersonalizado project) {
        showContent();

        String status = normalizeStatus(project.getEstadoAtual());
        projectTitleLabel.setText(nonBlank(project.getTituloProjeto(), "Projeto personalizado"));
        projectIdLabel.setText(project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId());
        projectStatusBadge.setText(dataService.projectStatusLabel(status));
        projectStatusBadge.setStyle(statusStyle(status));

        createdAtLabel.setText(formatDate(project.getDataCriacao()));
        quantityLabel.setText("Nao disponivel");
        deadlineLabel.setText("A definir");

        quoteDesignLabel.setText(currencyFormat.format(java.math.BigDecimal.ZERO));
        quoteMoldLabel.setText(currencyFormat.format(java.math.BigDecimal.ZERO));
        quoteProductionLabel.setText(currencyFormat.format(java.math.BigDecimal.ZERO));
        quoteTotalLabel.setText(currencyFormat.format(java.math.BigDecimal.ZERO));
        quoteDetailsLabel.setText("Dados de orcamento ainda nao disponiveis na API.");

        briefingTypeLabel.setText("Projeto personalizado");
        briefingBudgetLabel.setText("Nao definido");
        briefingDescriptionLabel.setText(nonBlank(project.getBriefing(), "Sem briefing"));

        renderTracking(status);
        renderMeetingsUnavailable();
        renderMessagesUnavailable();
        renderPaymentsUnavailable();
    }

    private void renderTracking(String status) {
        trackingContainer.getChildren().clear();
        trackingCountLabel.setText("1 de 1 fases");
        trackingProgress.setProgress(progressForStatus(status));

        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: " + stageBackground("in_progress") + "; -fx-background-radius: 8;");

        Label index = new Label("1");
        index.setStyle("-fx-text-fill: white; -fx-background-color: " + stageColor("in_progress") + "; -fx-padding: 2 6; -fx-background-radius: 999;");

        VBox text = new VBox(2);
        Label name = new Label(phaseLabel(status));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Label period = new Label("Estado atual do projeto segundo a API");
        period.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        text.getChildren().addAll(name, period);
        row.getChildren().addAll(index, text);
        trackingContainer.getChildren().add(row);
    }

    private void renderMeetingsUnavailable() {
        meetingsContainer.getChildren().clear();
        Label empty = new Label("Reunioes ainda nao disponiveis na API.");
        empty.setStyle("-fx-text-fill: #6b7280;");
        meetingsContainer.getChildren().add(empty);
    }

    private void renderMessagesUnavailable() {
        messagesContainer.getChildren().clear();
        Label empty = new Label("Chat ainda nao disponivel na API.");
        empty.setStyle("-fx-text-fill: #6b7280;");
        messagesContainer.getChildren().add(empty);
        if (newMessageField != null) {
            newMessageField.setDisable(true);
            newMessageField.setPromptText("Chat indisponivel");
        }
    }

    private void renderPaymentsUnavailable() {
        if (paymentsContainer == null) {
            return;
        }

        paymentsContainer.getChildren().clear();
        Label empty = new Label("Pagamentos ainda nao disponiveis na API.");
        empty.setStyle("-fx-text-fill: #6b7280;");
        paymentsContainer.getChildren().add(empty);
    }

    private void showNotFound() {
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        notFoundBox.setVisible(true);
        notFoundBox.setManaged(true);
    }

    private void showContent() {
        notFoundBox.setVisible(false);
        notFoundBox.setManaged(false);
        contentBox.setVisible(true);
        contentBox.setManaged(true);
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "briefing" -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "quote_sent" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "completed" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private String stageBackground(String status) {
        return switch (status) {
            case "completed" -> "#dcfce7";
            case "in_progress" -> "#dbeafe";
            default -> "#f3f4f6";
        };
    }

    private String stageColor(String status) {
        return switch (status) {
            case "completed" -> "#16a34a";
            case "in_progress" -> "#2563eb";
            default -> "#9ca3af";
        };
    }

    private String meetingStatusLabel(String status) {
        return switch (status) {
            case "scheduled" -> "Agendada";
            case "confirmed" -> "Confirmada";
            case "cancelled" -> "Cancelada";
            default -> status;
        };
    }

    private String meetingStatusStyle(String status) {
        return switch (status) {
            case "scheduled" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "confirmed" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "cancelled" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private String normalizeProjectId(String rawId) {
        if (rawId == null) {
            return null;
        }

        String cleaned = rawId.trim().toUpperCase(Locale.ROOT);
        if (cleaned.isBlank()) {
            return null;
        }

        cleaned = cleaned.replace("/", "").replace("_", "-");
        if (cleaned.startsWith("PROJECT-DETAIL:")) {
            cleaned = cleaned.substring("PROJECT-DETAIL:".length());
        }
        if (cleaned.matches("PRJ\\d+")) {
            cleaned = "PRJ-" + cleaned.substring(3);
        }
        return cleaned;
    }

    private Integer extractProjectNumericId(String normalizedId) {
        if (normalizedId == null || normalizedId.isBlank()) {
            return null;
        }
        if (normalizedId.matches("PRJ-\\d+")) {
            return Integer.parseInt(normalizedId.substring(4));
        }
        if (normalizedId.matches("\\d+")) {
            return Integer.parseInt(normalizedId);
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "briefing";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "em_analise", "analise", "briefing" -> "briefing";
            case "orcamento_enviado", "quote_sent" -> "quote_sent";
            case "aprovado", "approved" -> "approved";
            case "em_producao", "in_production" -> "in_production";
            case "concluido", "completed" -> "completed";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private double progressForStatus(String status) {
        return switch (status) {
            case "briefing" -> 0.1;
            case "quote_sent" -> 0.3;
            case "approved" -> 0.5;
            case "in_production" -> 0.75;
            case "completed" -> 1.0;
            default -> 0.0;
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "A definir" : dateFormatter.format(date);
    }

    private String formatDate(Instant date) {
        if (date == null) {
            return "A definir";
        }
        return dateFormatter.format(date.atZone(ZoneId.systemDefault()).toLocalDate());
    }

    private String phaseLabel(String phase) {
        return switch (phase) {
            case "briefing" -> "Briefing";
            case "quote_sent" -> "Orcamento";
            case "approved" -> "Aprovado";
            case "in_production" -> "Em Producao";
            case "completed" -> "Concluido";
            case "design" -> "Fase 1: Design";
            case "mold" -> "Fase 2: Molde";
            case "production" -> "Fase 3: Producao";
            default -> phase;
        };
    }

    private String paymentBackground(String status) {
        return status.equals("paid") ? "#f0fdf4" : "#fffbeb";
    }

    private String paymentBorder(String status) {
        return status.equals("paid") ? "#bbf7d0" : "#fcd34d";
    }

    private String paymentStatusStyle(String status) {
        return status.equals("paid")
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;"
                : "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
    }

    private String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
