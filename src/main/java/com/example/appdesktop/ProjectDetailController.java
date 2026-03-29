package com.example.appdesktop;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private final List<ClientPortalDataService.ProjectMessage> dynamicMessages = new ArrayList<>();
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
        alert.setHeaderText("Orcamento aprovado");
        alert.setContentText("Entraremos em contacto para os proximos passos.");
        alert.showAndWait();
    }

    @FXML
    private void onSendMessage() {
        String message = newMessageField.getText();
        if (message == null || message.isBlank() || projectId == null) {
            return;
        }

        dynamicMessages.add(new ClientPortalDataService.ProjectMessage(
                "NEW-" + (dynamicMessages.size() + 1),
                projectId,
                "client",
                "Maria Silva",
                message.trim(),
                "Agora"
        ));
        newMessageField.clear();
        renderMessages();
    }

    @FXML
    private void onConfirmFirstMeeting() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reuniao");
        alert.setHeaderText("Presenca confirmada");
        alert.setContentText("A reuniao foi confirmada com sucesso.");
        alert.showAndWait();
    }

    private void refresh() {
        if (!initialized) {
            return;
        }

        if (projectId == null || projectId.isBlank()) {
            showNotFound();
            return;
        }

        dataService.findProjectById(projectId).ifPresentOrElse(project -> {
            showContent();

            projectTitleLabel.setText(project.title());
            projectIdLabel.setText(project.id());
            projectStatusBadge.setText(dataService.projectStatusLabel(project.status()));
            projectStatusBadge.setStyle(statusStyle(project.status()));

            ClientPortalDataService.ProjectBriefing briefing = dataService.projectBriefing(project.id());
            createdAtLabel.setText(formatDate(project.createdAt()));
            quantityLabel.setText(briefing.quantity() + " pecas");
            deadlineLabel.setText(formatDate(briefing.deadline()));

            ClientPortalDataService.ProjectQuote quote = project.quote();
            quoteDesignLabel.setText(currencyFormat.format(quote == null ? java.math.BigDecimal.ZERO : quote.designPhase()));
            quoteMoldLabel.setText(currencyFormat.format(quote == null ? java.math.BigDecimal.ZERO : quote.moldPhase()));
            quoteProductionLabel.setText(currencyFormat.format(quote == null ? java.math.BigDecimal.ZERO : quote.productionPhase()));
            quoteTotalLabel.setText(currencyFormat.format(quote == null ? java.math.BigDecimal.ZERO : quote.total()));
            quoteDetailsLabel.setText(dataService.quoteDetails(project.id()));

            briefingTypeLabel.setText(nonBlank(briefing.projectType(), "Nao definido"));
            briefingBudgetLabel.setText(briefing.budget() == null ? "Nao definido" : currencyFormat.format(briefing.budget()));
            briefingDescriptionLabel.setText(nonBlank(briefing.description(), "Sem descricao"));

            renderTracking(dataService.trackingStages(project.id()));
            renderMeetings(dataService.projectMeetings(project.id()));

            dynamicMessages.clear();
            dynamicMessages.addAll(dataService.projectMessages(project.id()));
            renderMessages();

            renderPayments(dataService.projectPayments(project.id()));
        }, this::showNotFound);
    }

    private void renderTracking(List<ClientPortalDataService.ProjectStage> stages) {
        trackingContainer.getChildren().clear();
        long completed = stages.stream().filter(stage -> "completed".equals(stage.status())).count();
        trackingCountLabel.setText(completed + " de " + stages.size() + " fases");
        trackingProgress.setProgress(stages.isEmpty() ? 0 : ((double) completed / stages.size()));

        for (int i = 0; i < stages.size(); i++) {
            ClientPortalDataService.ProjectStage stage = stages.get(i);
            HBox row = new HBox(10);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: " + stageBackground(stage.status()) + "; -fx-background-radius: 8;");

            Label index = new Label("" + (i + 1));
            index.setStyle("-fx-text-fill: white; -fx-background-color: " + stageColor(stage.status()) + "; -fx-padding: 2 6; -fx-background-radius: 999;");

            VBox text = new VBox(2);
            Label name = new Label(stage.name());
            name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            String dates = stage.startDate() == null
                    ? "Pendente"
                    : "Inicio: " + dateFormatter.format(stage.startDate())
                    + (stage.endDate() != null ? " | Fim: " + dateFormatter.format(stage.endDate()) : "");
            Label period = new Label(dates);
            period.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            text.getChildren().addAll(name, period);
            row.getChildren().addAll(index, text);
            trackingContainer.getChildren().add(row);
        }
    }

    private void renderMeetings(List<ClientPortalDataService.ProjectMeeting> meetings) {
        meetingsContainer.getChildren().clear();

        if (meetings.isEmpty()) {
            Label empty = new Label("Nenhuma reuniao agendada para este projeto");
            empty.setStyle("-fx-text-fill: #6b7280;");
            meetingsContainer.getChildren().add(empty);
            return;
        }

        for (ClientPortalDataService.ProjectMeeting meeting : meetings) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;");

            Label header = new Label(dateFormatter.format(meeting.date()) + " - " + meeting.time());
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            Label notes = new Label(meeting.notes());
            notes.setWrapText(true);
            notes.setStyle("-fx-text-fill: #4b5563;");

            Label status = new Label(meetingStatusLabel(meeting.status()));
            status.setStyle(meetingStatusStyle(meeting.status()));

            card.getChildren().addAll(header, notes, status);
            meetingsContainer.getChildren().add(card);
        }
    }

    private void renderMessages() {
        messagesContainer.getChildren().clear();
        if (dynamicMessages.isEmpty()) {
            Label empty = new Label("Nenhuma mensagem ainda");
            empty.setStyle("-fx-text-fill: #6b7280;");
            messagesContainer.getChildren().add(empty);
            return;
        }

        for (ClientPortalDataService.ProjectMessage msg : dynamicMessages) {
            HBox row = new HBox();
            VBox bubble = new VBox(2);
            bubble.setPadding(new Insets(8));
            bubble.setMaxWidth(460);

            boolean isClient = "client".equals(msg.senderId());
            row.setStyle(isClient ? "-fx-alignment: center-right;" : "-fx-alignment: center-left;");
            bubble.setStyle(isClient
                    ? "-fx-background-color: #d97706; -fx-background-radius: 8;"
                    : "-fx-background-color: #f3f4f6; -fx-background-radius: 8;");

            Label sender = new Label(msg.senderName());
            sender.setStyle(isClient ? "-fx-text-fill: #fffbeb; -fx-font-size: 11px;" : "-fx-text-fill: #6b7280; -fx-font-size: 11px;");

            Label text = new Label(msg.message());
            text.setWrapText(true);
            text.setStyle(isClient ? "-fx-text-fill: white;" : "-fx-text-fill: #111827;");

            Label time = new Label(msg.timestamp());
            time.setStyle(isClient ? "-fx-text-fill: #ffedd5; -fx-font-size: 11px;" : "-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

            bubble.getChildren().addAll(sender, text, time);
            row.getChildren().add(bubble);
            HBox.setHgrow(row, Priority.ALWAYS);
            messagesContainer.getChildren().add(row);
        }
    }

    private void renderPayments(List<ClientPortalDataService.Payment> payments) {
        if (paymentsContainer == null) {
            return;
        }

        paymentsContainer.getChildren().clear();

        if (payments.isEmpty()) {
            Label empty = new Label("Nenhum pagamento registado");
            empty.setStyle("-fx-text-fill: #6b7280;");
            paymentsContainer.getChildren().add(empty);
            return;
        }

        java.math.BigDecimal paidTotal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal pendingTotal = java.math.BigDecimal.ZERO;

        for (ClientPortalDataService.Payment payment : payments) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(14));
            card.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            card.setStyle("-fx-background-color: " + paymentBackground(payment.status())
                    + "; -fx-border-color: " + paymentBorder(payment.status())
                    + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-width: 1;");

            HBox header = new HBox(10);
            VBox left = new VBox(4);
            Label phase = new Label(phaseLabel(payment.phase()));
            phase.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");

            Label amount = new Label(currencyFormat.format(payment.amount()));
            amount.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            left.getChildren().addAll(phase, amount);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox right = new VBox(4);
            right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            Label status = new Label(payment.status().equals("paid") ? "Pago" : "Pendente");
            status.setStyle(paymentStatusStyle(payment.status()));

            if (payment.status().equals("paid") && payment.paidDate() != null) {
                Label date = new Label("Pago em: " + dateFormatter.format(payment.paidDate()));
                date.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
                right.getChildren().add(date);
            }
            if (payment.status().equals("pending") && payment.dueDate() != null) {
                Label dueDate = new Label("Venc.: " + dateFormatter.format(payment.dueDate()));
                dueDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
                right.getChildren().add(dueDate);
            }
            right.getChildren().add(status);

            header.getChildren().addAll(left, spacer, right);

            if (payment.status().equals("paid")) {
                paidTotal = paidTotal.add(payment.amount());
                card.getChildren().add(header);
            } else {
                pendingTotal = pendingTotal.add(payment.amount());
                VBox footer = new VBox(8);
                footer.getChildren().add(header);
                
                Button payButton = new Button("Efetuar Pagamento");
                payButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
                payButton.setPrefWidth(Double.MAX_VALUE);
                String paymentId = payment.id();
                String projId = projectId;
                payButton.setOnAction(event -> {
                    if (navigator != null) {
                        navigator.navigateTo("project-payment:" + projId + "/" + paymentId);
                    }
                });
                footer.getChildren().add(payButton);
                card.getChildren().add(footer);
            }

            paymentsContainer.getChildren().add(card);
        }
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

    private String formatDate(LocalDate date) {
        return date == null ? "A definir" : dateFormatter.format(date);
    }

    private String phaseLabel(String phase) {
        return switch (phase) {
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
}
