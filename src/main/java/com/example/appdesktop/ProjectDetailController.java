package com.example.appdesktop;

import com.example.appdesktop.models.MensagemChat;
import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Reuniao;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.services.MensagemChatService;
import com.example.appdesktop.services.OrcamentoService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import com.example.appdesktop.services.ReuniaoService;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.ItemEncomendaService;
import com.example.appdesktop.services.ArtigoCatalogoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private Button reorderButton;

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
    private final ReuniaoService reuniaoService = ReuniaoService.getInstance();
    private final OrcamentoService orcamentoService = OrcamentoService.getInstance();
    private final MensagemChatService mensagemChatService = MensagemChatService.getInstance();
    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final ItemEncomendaService itemEncomendaService = ItemEncomendaService.getInstance();
    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private ClientPageNavigator navigator;
    private String projectId;
    private boolean initialized;
    private List<Orcamento> projectQuotes = new ArrayList<>();
    private List<MensagemChat> projectMessages = new ArrayList<>();
    private Integer projectQuantity;
    private BigDecimal projectQuoteTotal = BigDecimal.ZERO;
    private String projectTitle;

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
        if (newMessageField == null) {
            return;
        }

        String content = newMessageField.getText() == null ? "" : newMessageField.getText().trim();
        if (content.isBlank()) {
            showInfo("Chat", "Escreva uma mensagem antes de enviar.");
            return;
        }

        Integer projectNumericId = extractProjectNumericId(projectId);
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (projectNumericId == null || currentUser == null || currentUser.getId() == null) {
            showInfo("Chat", "Nao foi possivel identificar o projeto ou utilizador.");
            return;
        }

        mensagemChatService.createAsUtilizador(projectNumericId, currentUser.getId(), content)
                .whenComplete((saved, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Chat", "Nao foi possivel enviar a mensagem.");
                        return;
                    }
                    newMessageField.clear();
                    loadMessages(projectNumericId);
                }));
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
                    loadMeetings(project.getId());
                    loadQuotes(project.getId());
                    loadMessages(project.getId());
                }));
    }

    private void populateProject(ProjetoPersonalizado project) {
        showContent();

        String status = project.getEstadoAtual();
        String normalizedStatus = normalizeStatus(status);
        projectTitleLabel.setText(nonBlank(project.getTituloProjeto(), "Projeto personalizado"));
        projectTitle = nonBlank(project.getTituloProjeto(), "Projeto " + (project.getId() == null ? "" : project.getId()));
        projectIdLabel.setText(project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId());
        projectStatusBadge.setText(dataService.projectStatusLabel(normalizedStatus));
        projectStatusBadge.setStyle(statusStyle(normalizedStatus));

        if (reorderButton != null) {
            boolean isComplete = "completo".equals(normalizedStatus) || "completed".equals(normalizedStatus);
            reorderButton.setVisible(isComplete);
            reorderButton.setManaged(isComplete);
        }

        createdAtLabel.setText(formatDate(project.getDataCriacao()));
        createdAtLabel.setVisible(project.getDataCriacao() != null);
        createdAtLabel.setManaged(project.getDataCriacao() != null);
        quantityLabel.setText(project.getQuantidade() != null ? project.getQuantidade() + " pecas" : "Nao disponivel");
        projectQuantity = project.getQuantidade();
        deadlineLabel.setText("A definir");
        deadlineLabel.setVisible(false);
        deadlineLabel.setManaged(false);

        quoteDesignLabel.setText("A carregar...");
        quoteMoldLabel.setText("A carregar...");
        quoteProductionLabel.setText("A carregar...");
        quoteTotalLabel.setText(currencyFormat.format(BigDecimal.ZERO));
        quoteDetailsLabel.setText("A carregar orcamentos...");

        if (briefingTypeLabel != null) {
            briefingTypeLabel.setText("Projeto personalizado");
        }
        if (briefingBudgetLabel != null) {
            briefingBudgetLabel.setText("Nao definido");
        }
        if (briefingDescriptionLabel != null) {
            briefingDescriptionLabel.setText(nonBlank(project.getBriefing(), "Sem briefing"));
        }

        renderTracking(status);
        renderMeetingsLoading();
        renderMessagesLoading();
        renderPaymentsLoading();
    }

    private void loadQuotes(Integer projetoId) {
        if (projetoId == null) {
            projectQuotes = List.of();
            applyQuotesToOverview();
            renderPaymentsFromQuotes();
            return;
        }

        orcamentoService.findByProjetoId(projetoId)
                .whenComplete((quotes, error) -> Platform.runLater(() -> {
                    if (error != null || quotes == null) {
                        projectQuotes = List.of();
                        quoteDesignLabel.setText(currencyFormat.format(BigDecimal.ZERO));
                        quoteMoldLabel.setText(currencyFormat.format(BigDecimal.ZERO));
                        quoteProductionLabel.setText(currencyFormat.format(BigDecimal.ZERO));
                        quoteTotalLabel.setText(currencyFormat.format(BigDecimal.ZERO));
                        quoteDetailsLabel.setText("Sem orcamentos disponiveis.");
                        renderPaymentsUnavailable();
                        return;
                    }
                    projectQuotes = quotes;
                    applyQuotesToOverview();
                    renderPaymentsFromQuotes();
                }));
    }

    private void applyQuotesToOverview() {
        BigDecimal design = totalByType("design");
        BigDecimal mold = totalByType("molde");
        BigDecimal production = totalByType("producao");
        BigDecimal total = design.add(mold).add(production);
        projectQuoteTotal = total;

        quoteDesignLabel.setText(currencyFormat.format(design));
        quoteMoldLabel.setText(currencyFormat.format(mold));
        quoteProductionLabel.setText(currencyFormat.format(production));
        quoteTotalLabel.setText(currencyFormat.format(total));

        if (projectQuotes == null || projectQuotes.isEmpty()) {
            quoteDetailsLabel.setText("Sem orcamentos enviados.");
        } else {
            quoteDetailsLabel.setText(projectQuotes.size() + " orcamento(s) enviado(s)");
        }
    }

    private void renderPaymentsFromQuotes() {
        paymentsContainer.getChildren().clear();
        if (projectQuotes == null || projectQuotes.isEmpty()) {
            renderPaymentsUnavailable();
            return;
        }

        for (Orcamento quote : projectQuotes) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: " + paymentBackground(normalizeQuoteStatus(quote.getEstado()))
                    + "; -fx-border-color: " + paymentBorder(normalizeQuoteStatus(quote.getEstado()))
                    + "; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox top = new HBox(8);
            Label phase = new Label(phaseLabel(quote.getTipo()));
            phase.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            String status = normalizeQuoteStatus(quote.getEstado());
            Label statusBadge = new Label("paid".equals(status) ? "Pago" : "Pendente");
            statusBadge.setStyle(paymentStatusStyle(status));
            top.getChildren().addAll(phase, spacer, statusBadge);

            Label amount = new Label(currencyFormat.format(quote.getValorTotalEstimado() == null ? BigDecimal.ZERO : quote.getValorTotalEstimado()));
            amount.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label details = new Label(nonBlank(quote.getObservacoes(), "Sem descricao"));
            details.setWrapText(true);
            details.setStyle("-fx-text-fill: #4b5563;");

            HBox actions = new HBox(8);
            if (!"paid".equals(status) && quote.getId() != null && projectId != null) {
                Button payButton = new Button("Pagar");
                payButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;");
                payButton.setOnAction(e -> {
                    if (navigator != null) {
                        navigator.navigateTo("project-payment:" + projectId + "/" + quote.getId());
                    }
                });
                actions.getChildren().add(payButton);
            }

            card.getChildren().addAll(top, amount, details, actions);
            paymentsContainer.getChildren().add(card);
        }
    }

    private BigDecimal totalByType(String tipo) {
        if (projectQuotes == null || projectQuotes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return projectQuotes.stream()
                .filter(q -> q != null)
                .filter(q -> normalizeQuoteType(q.getTipo()).equals(tipo))
                .map(q -> q.getValorTotalEstimado() == null ? BigDecimal.ZERO : q.getValorTotalEstimado())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void loadMeetings(Integer projetoId) {
        if (projetoId == null) {
            renderMeetingsUnavailable();
            return;
        }
        reuniaoService.findByProjetoId(projetoId)
                .whenComplete((reunioes, error) -> Platform.runLater(() -> {
                    if (error != null || reunioes == null) {
                        renderMeetingsUnavailable();
                        return;
                    }
                    renderMeetings(reunioes);
                }));
    }

    private void loadMessages(Integer projetoId) {
        if (projetoId == null) {
            projectMessages = List.of();
            renderMessagesEmpty();
            return;
        }

        mensagemChatService.findByProjetoId(projetoId)
                .whenComplete((messages, error) -> Platform.runLater(() -> {
                    if (error != null || messages == null) {
                        renderMessagesError();
                        return;
                    }
                    projectMessages = messages;
                    renderMessages(projectMessages);
                }));
    }

    private void renderMeetings(List<Reuniao> reunioes) {
        meetingsContainer.getChildren().clear();
        if (reunioes.isEmpty()) {
            Label empty = new Label("Nao existem reunioes agendadas para este projeto.");
            empty.setStyle("-fx-text-fill: #6b7280;");
            meetingsContainer.getChildren().add(empty);
            return;
        }
        for (Reuniao reuniao : reunioes) {
            meetingsContainer.getChildren().add(createMeetingCard(reuniao));
        }
    }

    private VBox createMeetingCard(Reuniao reuniao) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox header = new HBox(8);
        String dataFormatada = reuniao.getData() != null ? dateFormatter.format(reuniao.getData()) : "Data a definir";
        String hora = reuniao.getHora() != null ? reuniao.getHora() : "";
        Label dataLabel = new Label(dataFormatada + (hora.isBlank() ? "" : "  |  " + hora));
        dataLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String estado = reuniao.getEstado() != null ? reuniao.getEstado() : "scheduled";
        Label estadoLabel = new Label(meetingStatusLabel(estado));
        estadoLabel.setStyle(meetingStatusStyle(estado));

        header.getChildren().addAll(dataLabel, spacer, estadoLabel);

        String tipo = reuniao.getTipo() == null || reuniao.getTipo().isBlank() ? "--" : reuniao.getTipo();
        String local = reuniao.getLocal() == null || reuniao.getLocal().isBlank() ? "--" : reuniao.getLocal();
        Label meta = new Label("Tipo: " + tipo + "  |  Local: " + local);
        meta.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        HBox actions = new HBox(8);
        if ("scheduled".equals(normalizeMeetingStatus(estado))) {
            Button confirm = new Button("Confirmar presenca");
            confirm.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
            confirm.setOnAction(e -> confirmMeeting(reuniao));
            actions.getChildren().add(confirm);
        }

        if (reuniao.getNotas() != null && !reuniao.getNotas().isBlank()) {
            Label notasLabel = new Label(reuniao.getNotas());
            notasLabel.setWrapText(true);
            notasLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
            card.getChildren().addAll(header, meta, notasLabel, actions);
        } else {
            card.getChildren().addAll(header, meta, actions);
        }

        return card;
    }

    private void confirmMeeting(Reuniao reuniao) {
        if (reuniao == null || reuniao.getId() == null) {
            showInfo("Reunioes", "ID da reuniao invalido.");
            return;
        }

        reuniaoService.confirmPresence(reuniao.getId())
                .whenComplete((updated, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Reunioes", "Nao foi possivel confirmar a reuniao.");
                        return;
                    }
                    Integer projectNumericId = extractProjectNumericId(projectId);
                    if (projectNumericId != null) {
                        loadMeetings(projectNumericId);
                    }
                }));
    }

    private void renderMeetingsLoading() {
        meetingsContainer.getChildren().clear();
        Label loading = new Label("A carregar reunioes...");
        loading.setStyle("-fx-text-fill: #6b7280;");
        meetingsContainer.getChildren().add(loading);
    }

    private void renderTracking(String status) {
        String normalizedStatus = normalizeStatus(status);
        trackingContainer.getChildren().clear();

        List<String> stages = List.of(
                "briefing",
                "orcamento_enviado",
                "design",
                "molde",
                "producao",
                "enchimento_moldes",
                "secagem",
                "acabamento",
                "cozedura",
                "vidragem",
                "inspecao_qualidade",
                "completo"
        );

        int currentIndex = stages.indexOf(normalizedStatus);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        trackingCountLabel.setText((currentIndex + 1) + " de " + stages.size() + " fases");
        trackingProgress.setProgress(progressForStatus(normalizedStatus));

        for (int i = 0; i < stages.size(); i++) {
            String stage = stages.get(i);
            boolean isCurrent = i == currentIndex;
            boolean isComplete = i < currentIndex;

            HBox row = new HBox(10);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: " + stageBackground(isCurrent ? "in_progress" : (isComplete ? "completed" : "pending"))
                    + "; -fx-background-radius: 8;");

            Label index = new Label(String.valueOf(i + 1));
            index.setStyle("-fx-text-fill: white; -fx-background-color: "
                    + stageColor(isCurrent ? "in_progress" : (isComplete ? "completed" : "pending"))
                    + "; -fx-padding: 2 6; -fx-background-radius: 999;");

            VBox text = new VBox(2);
            Label name = new Label(phaseLabel(stage));
            name.setStyle(isCurrent
                    ? "-fx-font-weight: bold; -fx-text-fill: #111827;"
                    : "-fx-font-weight: normal; -fx-text-fill: #374151;");

            text.getChildren().add(name);
            row.getChildren().addAll(index, text);
            trackingContainer.getChildren().add(row);
        }
    }

    private void renderMeetingsUnavailable() {
        meetingsContainer.getChildren().clear();
        Label empty = new Label("Nao existem reunioes agendadas para este projeto.");
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
        Label empty = new Label("Sem orcamentos pendentes para pagamento.");
        empty.setStyle("-fx-text-fill: #6b7280;");
        paymentsContainer.getChildren().add(empty);
    }

    private void renderPaymentsLoading() {
        paymentsContainer.getChildren().clear();
        Label loading = new Label("A carregar orcamentos...");
        loading.setStyle("-fx-text-fill: #6b7280;");
        paymentsContainer.getChildren().add(loading);
    }

    private void renderMessagesLoading() {
        messagesContainer.getChildren().clear();
        Label loading = new Label("A carregar mensagens...");
        loading.setStyle("-fx-text-fill: #6b7280;");
        messagesContainer.getChildren().add(loading);
        if (newMessageField != null) {
            newMessageField.setDisable(false);
            newMessageField.setPromptText("Escreva a sua mensagem...");
        }
    }

    private void renderMessagesError() {
        messagesContainer.getChildren().clear();
        Label error = new Label("Nao foi possivel carregar o chat.");
        error.setStyle("-fx-text-fill: #6b7280;");
        messagesContainer.getChildren().add(error);
    }

    private void renderMessagesEmpty() {
        messagesContainer.getChildren().clear();
        Label empty = new Label("Sem mensagens disponiveis.");
        empty.setStyle("-fx-text-fill: #6b7280;");
        messagesContainer.getChildren().add(empty);
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

    private void renderMessages(List<MensagemChat> messages) {
        messagesContainer.getChildren().clear();
        if (messages == null || messages.isEmpty()) {
            renderMessagesEmpty();
            return;
        }

        List<ChatEntry> entries = toChatEntries(messages);
        for (ChatEntry entry : entries) {
            boolean fromClient = "client".equals(entry.senderType());

            HBox row = new HBox();
            row.setFillHeight(true);

            VBox bubble = new VBox(3);
            bubble.setPadding(new Insets(8, 10, 8, 10));
            bubble.setMaxWidth(380);
            bubble.setStyle(fromClient
                    ? "-fx-background-color: #dbeafe; -fx-background-radius: 10; -fx-border-color: #93c5fd; -fx-border-radius: 10;"
                    : "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

            Label sender = new Label(entry.senderName());
            sender.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

            Label content = new Label(entry.message());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill: #111827;");

            Label time = new Label(formatTime(entry.time()));
            time.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");

            bubble.getChildren().addAll(sender, content, time);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            if (fromClient) {
                row.getChildren().addAll(spacer, bubble);
            } else {
                row.getChildren().addAll(bubble, spacer);
            }

            messagesContainer.getChildren().add(row);
        }
    }

    private List<ChatEntry> toChatEntries(List<MensagemChat> messages) {
        List<ChatEntry> entries = new ArrayList<>();
        for (MensagemChat message : messages) {
            if (message == null) {
                continue;
            }

            boolean fromArtesa = message.getIdRemetenteArtesa() != null;
            boolean fromClient = message.getIdRemetenteUtilizador() != null;
            if (!fromArtesa && !fromClient) {
                fromClient = true;
            }

            String senderName;
            if (fromClient && message.getIdRemetenteUtilizador() != null) {
                if (message.getIdRemetenteUtilizador().getNomeEmpresa() != null
                        && !message.getIdRemetenteUtilizador().getNomeEmpresa().isBlank()) {
                    senderName = message.getIdRemetenteUtilizador().getNomeEmpresa();
                } else if (message.getIdRemetenteUtilizador().getEmail() != null
                        && !message.getIdRemetenteUtilizador().getEmail().isBlank()) {
                    senderName = message.getIdRemetenteUtilizador().getEmail();
                } else {
                    senderName = "Cliente";
                }
            } else if (!fromClient && message.getIdRemetenteArtesa() != null) {
                if (message.getIdRemetenteArtesa().getNome() != null
                        && !message.getIdRemetenteArtesa().getNome().isBlank()) {
                    senderName = message.getIdRemetenteArtesa().getNome();
                } else {
                    senderName = "Artesa";
                }
            } else {
                senderName = fromClient ? "Cliente" : "Artesa";
            }

            String time = "";
            if (message.getDataEnvio() != null) {
                time = timeFormatter.format(message.getDataEnvio().atZone(ZoneId.systemDefault()).toLocalTime());
            }

            entries.add(new ChatEntry(
                    fromClient ? "client" : "admin",
                    senderName,
                    message.getConteudo() == null ? "" : message.getConteudo(),
                    time
            ));
        }
        return entries;
    }

    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw;
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
            case "scheduled", "agendada" -> "Agendada";
            case "confirmed", "confirmada" -> "Confirmada";
            case "cancelled", "cancelada" -> "Cancelada";
            default -> status;
        };
    }

    private String meetingStatusStyle(String status) {
        return switch (status) {
            case "scheduled", "agendada" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "confirmed", "confirmada" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "cancelled", "cancelada" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private String normalizeMeetingStatus(String status) {
        if (status == null || status.isBlank()) {
            return "scheduled";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "agendada", "scheduled" -> "scheduled";
            case "confirmada", "confirmed" -> "confirmed";
            case "cancelada", "cancelled" -> "cancelled";
            default -> "scheduled";
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
            case "orcamento_enviado", "orçamento enviado", "quote_sent" -> "orcamento_enviado";
            case "design" -> "design";
            case "molde", "mold" -> "molde";
            case "producao", "produção", "production" -> "producao";
            case "enchimento de moldes", "enchimento_moldes" -> "enchimento_moldes";
            case "secagem" -> "secagem";
            case "acabamento" -> "acabamento";
            case "cozedura" -> "cozedura";
            case "vidragem" -> "vidragem";
            case "inspecao de qualidade", "inspecao_qualidade", "inspeção de qualidade" -> "inspecao_qualidade";
            case "concluido", "concluído", "completed", "completo" -> "completo";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private double progressForStatus(String status) {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "briefing" -> 0.08;
            case "orcamento_enviado", "quote_sent" -> 0.16;
            case "design" -> 0.24;
            case "molde", "mold" -> 0.32;
            case "producao", "production" -> 0.40;
            case "enchimento_moldes" -> 0.50;
            case "secagem" -> 0.60;
            case "acabamento" -> 0.70;
            case "cozedura" -> 0.80;
            case "vidragem" -> 0.90;
            case "inspecao_qualidade" -> 0.96;
            case "completo", "completed" -> 1.0;
            default -> 0.08;
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
            case "orcamento_enviado", "quote_sent" -> "Orcamento Enviado";
            case "approved" -> "Aprovado";
            case "in_production" -> "Em Producao";
            case "completed", "completo" -> "Concluido";
            case "design" -> "Design";
            case "mold", "molde" -> "Molde";
            case "production", "producao" -> "Producao";
            case "enchimento_moldes" -> "Enchimento de Moldes";
            case "secagem" -> "Secagem";
            case "acabamento" -> "Acabamento";
            case "cozedura" -> "Cozedura";
            case "vidragem" -> "Vidragem";
            case "inspecao_qualidade" -> "Inspecao de Qualidade";
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

    private String normalizeQuoteType(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "design";
        }
        return switch (tipo.trim().toLowerCase(Locale.ROOT)) {
            case "molde", "mold" -> "molde";
            case "producao", "production" -> "producao";
            default -> "design";
        };
    }

    private String normalizeQuoteStatus(String estado) {
        if (estado == null || estado.isBlank()) {
            return "pending";
        }
        return switch (estado.trim().toLowerCase(Locale.ROOT)) {
            case "paga", "pago", "paid" -> "paid";
            default -> "pending";
        };
    }

    private String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private record ChatEntry(String senderType, String senderName, String message, String time) {
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void onConfirmFirstMeeting() {
        showInfo("Reunioes", "Confirmacao de reuniao ainda nao disponivel na API.");
    }

    @FXML
    private void onReorderProject() {
        Integer projectNumericId = extractProjectNumericId(projectId);
        if (projectNumericId == null) {
            showInfo("Reencomenda", "Projeto invalido.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reencomendar");
        dialog.setHeaderText("Quantidade (opcional)");
        dialog.setContentText("Se vazio, mantem as quantidades originais.");

        Integer quantidade = null;
        String input = dialog.showAndWait().orElse("").trim();
        if (!input.isBlank()) {
            try {
                quantidade = Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                showInfo("Reencomenda", "Quantidade invalida.");
                return;
            }
        }

        int reorderQuantidade = quantidade != null ? quantidade : (projectQuantity == null || projectQuantity <= 0 ? 1 : projectQuantity);
        if (reorderQuantidade <= 0) {
            showInfo("Reencomenda", "Quantidade invalida.");
            return;
        }
        BigDecimal unitPrice = resolveProductionUnitPrice();
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal estimate = unitPrice.multiply(BigDecimal.valueOf(reorderQuantidade));
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Reencomenda");
            confirm.setHeaderText("Estimativa para " + reorderQuantidade + " pecas");
            confirm.setContentText("Estimativa total: " + currencyFormat.format(estimate));
            if (confirm.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
                return;
            }
        }
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showInfo("Reencomenda", "Reencomenda criada, mas sem sessao ativa.");
            return;
        }

        String artigoNome = projectTitle == null || projectTitle.isBlank()
                ? "Projeto " + projectNumericId
                : projectTitle;

        artigoService.findAll()
                .whenComplete((artigos, error) -> Platform.runLater(() -> {
                    if (error != null || artigos == null) {
                        showInfo("Reencomenda", "Nao foi possivel localizar o artigo do projeto.");
                        return;
                    }

                    ArtigoCatalogo artigo = artigos.stream()
                            .filter(a -> a != null && a.getNome() != null && a.getNome().equalsIgnoreCase(artigoNome))
                            .findFirst()
                            .orElse(null);

                    if (artigo == null || artigo.getId() == null) {
                        showInfo("Reencomenda", "Artigo do projeto nao encontrado. Confirme se o projeto esta completo.");
                        return;
                    }

                    encomendaService.addItemAoCarrinho(currentUser.getId(), artigo.getId(), reorderQuantidade)
                            .whenComplete((carrinho, cartError) -> Platform.runLater(() -> {
                                if (cartError != null) {
                                    showInfo("Reencomenda", "Nao foi possivel adicionar o artigo ao carrinho. " + formatError(cartError, ""));
                                    return;
                                }
                                showInfo("Reencomenda", "Artigo adicionado ao carrinho. Prossiga para o checkout.");
                                if (navigator != null) {
                                    navigator.navigateTo("checkout");
                                }
                            }));
                }));
    }

    private BigDecimal resolveProductionUnitPrice() {
        if (projectQuantity == null || projectQuantity <= 0) {
            return null;
        }
        BigDecimal productionTotal = totalByType("producao");
        if (productionTotal == null || productionTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return productionTotal.divide(BigDecimal.valueOf(projectQuantity), 2, RoundingMode.HALF_UP);
    }

    private String formatError(Throwable error, String fallback) {
        if (error == null) {
            return fallback == null ? "" : fallback;
        }
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return message;
    }
}
