package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.MensagemChat;
import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.models.FichaTecnica;
import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.services.MensagemChatService;
import com.example.appdesktop.services.OrcamentoService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import com.example.appdesktop.services.ReuniaoService;
import com.example.appdesktop.services.FichaTecnicaService;
import com.example.appdesktop.services.ArtigoCatalogoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AdminProjectsController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label totalProjectsLabel;
    @FXML private Label briefingProjectsLabel;
    @FXML private Label productionProjectsLabel;
    @FXML private Label completedProjectsLabel;
    @FXML private VBox projectsContainer;
    @FXML private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final MensagemChatService mensagemChatService = MensagemChatService.getInstance();
    private final OrcamentoService orcamentoService = OrcamentoService.getInstance();
    private final ReuniaoService reuniaoService = ReuniaoService.getInstance();
    private final FichaTecnicaService fichaTecnicaService = FichaTecnicaService.getInstance();
    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<ProjetoPersonalizado> allProjects = new ArrayList<>();
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        statusCombo.getItems().setAll(
                "Todos",
                "Briefing",
                "Orçamento enviado",
                "Design",
                "Molde",
                "Produção",
                "Enchimento de moldes",
                "Secagem",
                "Acabamento",
                "Cozedura",
                "Vidragem",
                "Inspeção de qualidade",
                "Completo"
        );
        statusCombo.setValue("Todos");
        loadProjects();
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSearchChanged() {
        refreshProjects();
    }

    @FXML
    private void onStatusChanged() {
        refreshProjects();
    }

    private void loadProjects() {
        projetoService.findAll()
                .whenComplete((projects, error) -> Platform.runLater(() -> {
                    if (error != null || projects == null) {
                        allProjects = List.of();
                    } else {
                        allProjects = projects;
                    }
                    refreshProjects();
                }));
    }

    private void refreshProjects() {
        projectsContainer.getChildren().clear();

        updateStats();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        String statusFilter = statusCombo.getValue();
        String normalizedFilter = normalizeStatusFilter(statusFilter);

        List<ProjetoPersonalizado> filtered = allProjects.stream()
                .filter(project -> {
                    String title = project.getTituloProjeto() == null ? "" : project.getTituloProjeto().toLowerCase(Locale.ROOT);
                    String id = project.getId() == null ? "" : ("prj-" + project.getId()).toLowerCase(Locale.ROOT);
                    return search.isBlank() || title.contains(search) || id.contains(search);
                })
                .filter(project -> "all".equals(normalizedFilter)
                        || normalizeStatus(project.getEstadoAtual()).equals(normalizedFilter))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ProjetoPersonalizado project : filtered) {
            projectsContainer.getChildren().add(createProjectCard(project));
        }
    }

    private void updateStats() {
        if (totalProjectsLabel == null) {
            return;
        }

        totalProjectsLabel.setText(String.valueOf(allProjects.size()));
        briefingProjectsLabel.setText(String.valueOf(allProjects.stream().filter(p -> "briefing".equals(normalizeStatus(p.getEstadoAtual()))).count()));
        productionProjectsLabel.setText(String.valueOf(allProjects.stream().filter(p -> "in_production".equals(normalizeStatus(p.getEstadoAtual()))).count()));
        completedProjectsLabel.setText(String.valueOf(allProjects.stream().filter(p -> "completed".equals(normalizeStatus(p.getEstadoAtual()))).count()));
    }

    private VBox createProjectCard(ProjetoPersonalizado project) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(8);
        String projectId = project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId();
        Label title = new Label(resolveTitle(project) + "  (" + projectId + ")");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String status = normalizeStatus(project.getEstadoAtual());
        Label statusLabel = new Label(dataService.projectStatusLabel(status));
        statusLabel.setStyle(statusStyle(status));
        header.getChildren().addAll(title, spacer, statusLabel);

        Label description = new Label(resolveDescription(project));
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        LocalDate createdAt = project.getDataCriacao() == null
                ? LocalDate.now()
                : project.getDataCriacao().atZone(ZoneId.systemDefault()).toLocalDate();

        HBox stats = new HBox(20,
                stat("Quantidade", (project.getQuantidade() == null ? 0 : project.getQuantidade()) + " pecas"),
                stat("Criado em", dateFormatter.format(createdAt))
        );

        HBox actions = new HBox(8);
        javafx.scene.control.Button updateStatusBtn = new javafx.scene.control.Button("Atualizar Estado");
        updateStatusBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        updateStatusBtn.setOnAction(e -> updateProjectStatus(project));

        javafx.scene.control.Button detailsBtn = new javafx.scene.control.Button("Ver Detalhes");
        detailsBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        detailsBtn.setOnAction(e -> showProjectDetails(project));

        javafx.scene.control.Button chatBtn = new javafx.scene.control.Button("Chat Cliente");
        chatBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        chatBtn.setOnAction(e -> openClientChat(project));

        javafx.scene.control.Button meetingBtn = new javafx.scene.control.Button("Agendar Reuniao");
        meetingBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
        meetingBtn.setOnAction(e -> openMeetingDialog(project));

        javafx.scene.control.Button quoteBtn = new javafx.scene.control.Button("Submeter Orcamento");
        quoteBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white;");
        quoteBtn.setOnAction(e -> openQuoteDialog(project));

        javafx.scene.control.Button fichaBtn = new javafx.scene.control.Button("Ficha Tecnica");
        fichaBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        fichaBtn.setOnAction(e -> openFichaTecnicaDialog(project));

        actions.getChildren().addAll(updateStatusBtn, meetingBtn, quoteBtn, chatBtn, fichaBtn, detailsBtn);

        card.getChildren().addAll(header, description, stats, actions);
        return card;
    }

    private VBox stat(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return new VBox(2, l, v);
    }

    private void updateProjectStatus(ProjetoPersonalizado project) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Atualizar Estado do Projeto");
        dialog.setHeaderText(resolveTitle(project));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(100);
        ColumnConstraints col2 = new ColumnConstraints(220);
        grid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> newStatusCombo = new ComboBox<>();
        newStatusCombo.getItems().setAll(
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
        newStatusCombo.setValue(normalizeStatus(project.getEstadoAtual()));

        grid.addRow(0, new Label("Novo Estado:"), newStatusCombo);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String newStatus = newStatusCombo.getValue();
            projetoService.updateEstado(project.getId(), toBackendProjectStatus(newStatus))
                    .whenComplete((updated, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showInfo("Nao foi possivel atualizar o estado do projeto.");
                            return;
                        }
                        loadProjects();
                        showInfo("Estado atualizado para: " + dataService.projectStatusLabel(newStatus));
                        if ("completo".equals(normalizeStatus(newStatus))) {
                            ensureArtigoForProject(project);
                        }
                    }));
        }
    }

    private void ensureArtigoForProject(ProjetoPersonalizado project) {
        if (project == null || project.getId() == null) {
            return;
        }

        Integer quantidade = project.getQuantidade();
        if (quantidade == null || quantidade <= 0) {
            showInfo("Nao foi possivel criar artigo: quantidade invalida.");
            return;
        }

        String artigoNome = resolveArticleName(project);

        orcamentoService.findByProjetoId(project.getId())
                .thenCompose(orcamentos -> {
                    BigDecimal producaoTotal = resolveProductionQuoteTotal(orcamentos);
                    if (producaoTotal == null || producaoTotal.compareTo(BigDecimal.ZERO) <= 0) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Sem orcamento de producao."));
                    }

                    BigDecimal unitPrice = producaoTotal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
                    return artigoService.findAll()
                            .thenCompose(artigos -> {
                                ArtigoCatalogo existing = artigos == null ? null : artigos.stream()
                                        .filter(a -> a != null && a.getNome() != null
                                                && a.getNome().equalsIgnoreCase(artigoNome))
                                        .findFirst()
                                        .orElse(null);
                                if (existing != null) {
                                    if (existing.getStock() != null) {
                                        ArtigoCatalogo payload = new ArtigoCatalogo();
                                        payload.setNome(existing.getNome());
                                        payload.setPrecoUnitario(existing.getPrecoUnitario());
                                        payload.setStock(null);
                                        payload.setVisivel(existing.getVisivel());
                                        return artigoService.update(existing.getId(), payload)
                                                .thenApply(updated -> updated == null ? existing : updated);
                                    }
                                    return CompletableFuture.completedFuture(existing);
                                }

                                ArtigoCatalogo artigo = new ArtigoCatalogo();
                                artigo.setNome(artigoNome);
                                artigo.setPrecoUnitario(unitPrice);
                                artigo.setStock(null); // null => stock ilimitado
                                artigo.setVisivel(true);
                                return artigoService.create(artigo);
                            });
                })
                .thenCompose(artigo -> associateFichasToArtigo(project.getId(), artigo))
                .whenComplete((artigo, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel criar/associar o artigo do projeto. " + formatError(error));
                        return;
                    }
                    showInfo("Artigo e fichas tecnicas associados ao projeto concluido.");
                }));
    }

    private CompletableFuture<ArtigoCatalogo> associateFichasToArtigo(Integer projetoId, ArtigoCatalogo artigo) {
        if (projetoId == null || artigo == null || artigo.getId() == null) {
            return CompletableFuture.completedFuture(artigo);
        }

        return fichaTecnicaService.findByProjetoId(projetoId)
                .thenCompose(fichas -> {
                    if (fichas == null || fichas.isEmpty()) {
                        return CompletableFuture.completedFuture(artigo);
                    }

                    List<CompletableFuture<?>> tasks = new ArrayList<>();
                    for (FichaTecnica ficha : fichas) {
                        if (ficha == null || ficha.getId() == null) {
                            continue;
                        }
                        tasks.add(fichaTecnicaService.updateForArtigo(artigo.getId(), ficha.getId()));
                    }
                    if (tasks.isEmpty()) {
                        return CompletableFuture.completedFuture(artigo);
                    }
                    return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                            .thenApply(ignore -> artigo);
                });
    }

    private BigDecimal resolveProductionQuoteTotal(List<Orcamento> orcamentos) {
        if (orcamentos == null || orcamentos.isEmpty()) {
            return null;
        }
        return orcamentos.stream()
                .filter(o -> o != null)
                .filter(o -> "producao".equals(normalizeQuoteType(o.getTipo())))
                .map(o -> o.getValorTotalEstimado() == null ? BigDecimal.ZERO : o.getValorTotalEstimado())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeQuoteType(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "design";
        }
        return switch (tipo.trim().toLowerCase(Locale.ROOT)) {
            case "producao", "produção", "production" -> "producao";
            case "molde", "mold" -> "molde";
            default -> "design";
        };
    }

    private String resolveArticleName(ProjetoPersonalizado project) {
        if (project == null) {
            return "Projeto personalizado";
        }
        if (project.getTituloProjeto() != null && !project.getTituloProjeto().isBlank()) {
            return project.getTituloProjeto();
        }
        return project.getId() == null ? "Projeto personalizado" : "Projeto " + project.getId();
    }

    private void showProjectDetails(ProjetoPersonalizado project) {
        LocalDate createdAt = project.getDataCriacao() == null
                ? LocalDate.now()
                : project.getDataCriacao().atZone(ZoneId.systemDefault()).toLocalDate();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalhes - " + (project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId()));
        alert.setHeaderText(resolveTitle(project));
        alert.setContentText(
                "Estado: " + dataService.projectStatusLabel(normalizeStatus(project.getEstadoAtual())) + "\n" +
                        "Descricao: " + resolveDescription(project) + "\n" +
                        "Quantidade: " + (project.getQuantidade() == null ? 0 : project.getQuantidade()) + " pecas\n" +
                        "Criado em: " + dateFormatter.format(createdAt)
        );
        alert.showAndWait();
    }

    private void openClientChat(ProjetoPersonalizado project) {
        if (project.getId() == null) {
            showInfo("Projeto sem ID para carregar chat.");
            return;
        }

        mensagemChatService.findByProjetoId(project.getId())
                .whenComplete((apiMessages, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel carregar mensagens do chat.");
                        return;
                    }
                    showChatDialog(project, toChatEntries(project, apiMessages));
                }));
    }

    private void showChatDialog(ProjetoPersonalizado project, List<ChatEntry> messages) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chat com Cliente");
        dialog.setHeaderText(resolveTitle(project));

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #f9fafb;");

        String clientName = resolveClientName(project);
        Label clientLabel = new Label("Cliente: " + clientName);
        clientLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        if (messages.isEmpty()) {
            messages.add(new ChatEntry("client", clientName, "Ola! Pode partilhar o estado atual do projeto?", LocalTime.now().minusMinutes(25).toString()));
        }

        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(10));
        renderChatMessages(messagesBox, messages);

        ScrollPane chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setPrefViewportHeight(300);
        chatScroll.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("Escreva a sua mensagem...");
        inputArea.setPrefRowCount(3);

        root.getChildren().addAll(clientLabel, chatScroll, inputArea);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Enviar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String newMessage = inputArea.getText() == null ? "" : inputArea.getText().trim();
            if (!newMessage.isBlank()) {
                sendChatMessage(project, clientName, newMessage);
            }
        }
    }

    private void sendChatMessage(ProjetoPersonalizado project, String clientName, String content) {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (project.getId() == null || currentUser == null || currentUser.getId() == null) {
            showInfo("Nao foi possivel identificar a artesa para envio da mensagem.");
            return;
        }

        mensagemChatService.createAsArtesa(project.getId(), currentUser.getId(), content)
                .whenComplete((saved, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel enviar a mensagem.");
                        return;
                    }
                    showInfo("Mensagem enviada para " + clientName + ".");
                }));
    }

    private List<ChatEntry> toChatEntries(ProjetoPersonalizado project, List<MensagemChat> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ChatEntry> entries = new ArrayList<>();
        String fallbackClient = resolveClientName(project);
        for (MensagemChat message : messages) {
            if (message == null) {
                continue;
            }

            boolean fromAdmin = message.getIdRemetenteArtesa() != null;
            String senderName;
            if (fromAdmin) {
                senderName = message.getIdRemetenteArtesa().getNome() == null ? "Artesa" : message.getIdRemetenteArtesa().getNome();
            } else {
                if (message.getIdRemetenteUtilizador() != null && message.getIdRemetenteUtilizador().getNomeEmpresa() != null
                        && !message.getIdRemetenteUtilizador().getNomeEmpresa().isBlank()) {
                    senderName = message.getIdRemetenteUtilizador().getNomeEmpresa();
                } else {
                    senderName = fallbackClient;
                }
            }

            String time = "";
            if (message.getDataEnvio() != null) {
                time = message.getDataEnvio().atZone(ZoneId.systemDefault()).toLocalTime().withNano(0).toString();
            }

            entries.add(new ChatEntry(
                    fromAdmin ? "admin" : "client",
                    senderName,
                    message.getConteudo() == null ? "" : message.getConteudo(),
                    time
            ));
        }
        return entries;
    }

    private void renderChatMessages(VBox messagesBox, List<ChatEntry> messages) {
        messagesBox.getChildren().clear();
        for (ChatEntry message : messages) {
            boolean fromAdmin = "admin".equals(message.senderType());

            HBox row = new HBox();
            row.setFillHeight(true);

            VBox bubble = new VBox(3);
            bubble.setPadding(new Insets(8, 10, 8, 10));
            bubble.setMaxWidth(380);
            bubble.setStyle(fromAdmin
                    ? "-fx-background-color: #fef3c7; -fx-background-radius: 10; -fx-border-color: #fcd34d; -fx-border-radius: 10;"
                    : "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

            Label sender = new Label(message.senderName());
            sender.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

            Label content = new Label(message.message());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill: #111827;");

            Label time = new Label(formatTime(message.time()));
            time.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");

            bubble.getChildren().addAll(sender, content, time);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            if (fromAdmin) {
                row.getChildren().addAll(spacer, bubble);
            } else {
                row.getChildren().addAll(bubble, spacer);
            }

            messagesBox.getChildren().add(row);
        }
    }

    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return LocalTime.parse(raw).format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ex) {
            return raw;
        }
    }

    private void openQuoteDialog(ProjetoPersonalizado project) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Submeter Orcamento");
        dialog.setHeaderText(resolveTitle(project));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ComboBox<String> tipoCombo = new ComboBox<>();
        tipoCombo.getItems().setAll("design", "molde", "producao");
        tipoCombo.setValue("design");

        TextField amountField = new TextField();
        amountField.setPromptText("Ex: 1200.00");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Detalhes do orcamento...");
        notesArea.setPrefRowCount(4);

        grid.add(new Label("Tipo:"), 0, 0);
        grid.add(tipoCombo, 1, 0);
        grid.add(new Label("Valor total (EUR):"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Notas (opcional):"), 0, 2);
        grid.add(notesArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Submeter", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        String amountRaw = amountField.getText() == null ? "" : amountField.getText().trim().replace(",", ".");
        if (amountRaw.isBlank()) {
            showInfo("Indique o valor do orcamento.");
            return;
        }

        try {
            new BigDecimal(amountRaw);
        } catch (NumberFormatException ex) {
            showInfo("Valor de orcamento invalido.");
            return;
        }

        Orcamento novo = new Orcamento();
        novo.setTipo(tipoCombo.getValue());
        novo.setValorTotalEstimado(new BigDecimal(amountRaw));
        String notes = notesArea.getText() == null ? "" : notesArea.getText().trim();
        novo.setObservacoes(notes.isBlank() ? null : notes);

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            com.example.appdesktop.models.Artesa artesa = new com.example.appdesktop.models.Artesa();
            artesa.setId(currentUser.getId());
            novo.setIdArtesa(artesa);
        }

        orcamentoService.createForProjeto(project.getId(), novo)
                .thenCompose(saved -> projetoService.updateEstado(project.getId(), "orcamento_enviado"))
                .whenComplete((updated, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel submeter o orcamento.");
                        return;
                    }
                    loadProjects();
                    showInfo("Orcamento submetido com sucesso.");
                }));
    }

    private void openMeetingDialog(ProjetoPersonalizado project) {
        if (project == null || project.getId() == null) {
            showInfo("Projeto sem ID para agendar reuniao.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agendar Reuniao");
        dialog.setHeaderText(resolveTitle(project));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");

        TextField tipoField = new TextField();
        tipoField.setPromptText("videochamada");

        TextField localField = new TextField();
        localField.setPromptText("Google Meet");

        grid.addRow(0, new Label("Data:"), datePicker);
        grid.addRow(1, new Label("Hora:"), timeField);
        grid.addRow(2, new Label("Tipo:"), tipoField);
        grid.addRow(3, new Label("Local:"), localField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Agendar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showInfo("Nao foi possivel identificar a artesa para agendar.");
            return;
        }

        LocalDate data = datePicker.getValue();
        String hora = timeField.getText() == null ? "" : timeField.getText().trim();
        String tipo = tipoField.getText() == null ? "" : tipoField.getText().trim();
        String local = localField.getText() == null ? "" : localField.getText().trim();

        reuniaoService.createForProjetoArtesa(project.getId(), currentUser.getId(), data, hora, tipo, local)
                .whenComplete((created, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Nao foi possivel agendar a reuniao. " + formatError(error));
                        return;
                    }
                    showInfo("Reuniao agendada com sucesso.");
                }));
    }

    private String formatError(Throwable error) {
        if (error == null) {
            return "";
        }
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "";
        }
        return message;
    }

    private void openFichaTecnicaDialog(ProjetoPersonalizado project) {
        if (project == null || project.getId() == null) {
            showInfo("Projeto sem ID para carregar ficha tecnica.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Fichas Tecnicas");
        dialog.setHeaderText(resolveTitle(project));

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        VBox listBox = new VBox(8);
        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(280);
        scrollPane.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        Button addButton = new Button("Adicionar ficha");
        addButton.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
        addButton.setOnAction(e -> openFichaForm(project.getId(), null, listBox));

        root.getChildren().addAll(scrollPane, addButton);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        loadFichas(project.getId(), listBox);
        dialog.showAndWait();
    }

    private void loadFichas(Integer projetoId, VBox listBox) {
        listBox.getChildren().clear();
        Label loading = new Label("A carregar fichas tecnicas...");
        loading.setStyle("-fx-text-fill: #6b7280;");
        listBox.getChildren().add(loading);

        fichaTecnicaService.findByProjetoId(projetoId)
                .whenComplete((fichas, error) -> Platform.runLater(() -> {
                    listBox.getChildren().clear();
                    if (error != null || fichas == null || fichas.isEmpty()) {
                        Label empty = new Label("Sem fichas tecnicas.");
                        empty.setStyle("-fx-text-fill: #6b7280;");
                        listBox.getChildren().add(empty);
                        return;
                    }
                    for (FichaTecnica ficha : fichas) {
                        listBox.getChildren().add(createFichaCard(projetoId, ficha, listBox));
                    }
                }));
    }

    private VBox createFichaCard(Integer projetoId, FichaTecnica ficha, VBox listBox) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        String title = ficha.getRefMolde() == null || ficha.getRefMolde().isBlank()
                ? "Ficha tecnica"
                : "Ficha " + ficha.getRefMolde();
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Label meta = new Label("Tipo barro: " + valueOrDash(ficha.getTipoBarro())
                + "  |  Cor vidrado: " + valueOrDash(ficha.getCorVidrado())
                + "  |  Cozedura: " + (ficha.getTemperaturaCozedura() == null ? "--" : ficha.getTemperaturaCozedura() + "°C"));
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox actions = new HBox(8);
        Button edit = new Button("Editar");
        edit.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        edit.setOnAction(e -> openFichaForm(projetoId, ficha, listBox));

        Button remove = new Button("Remover");
        remove.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        remove.setOnAction(e -> {
            if (ficha.getId() == null) {
                showInfo("Ficha sem ID para remover.");
                return;
            }
            fichaTecnicaService.delete(ficha.getId())
                    .whenComplete((done, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showInfo("Nao foi possivel remover a ficha tecnica.");
                            return;
                        }
                        loadFichas(projetoId, listBox);
                    }));
        });

        actions.getChildren().addAll(edit, remove);
        card.getChildren().addAll(header, meta, actions);
        return card;
    }

    private void openFichaForm(Integer projetoId, FichaTecnica ficha, VBox listBox) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(ficha == null ? "Adicionar Ficha Tecnica" : "Editar Ficha Tecnica");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField tipoBarroField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getTipoBarro()));
        TextField corVidradoField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getCorVidrado()));
        TextField temperaturaField = new TextField(ficha == null || ficha.getTemperaturaCozedura() == null ? "" : ficha.getTemperaturaCozedura().toString());
        TextField tempoSecagemField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getTempoSecagem()));
        TextField refMoldeField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getRefMolde()));
        TextArea observacoesArea = new TextArea(ficha == null ? "" : nullToEmpty(ficha.getObservacoes()));
        observacoesArea.setPrefRowCount(3);

        grid.addRow(0, new Label("Tipo barro"), tipoBarroField);
        grid.addRow(1, new Label("Cor vidrado"), corVidradoField);
        grid.addRow(2, new Label("Temperatura cozedura"), temperaturaField);
        grid.addRow(3, new Label("Tempo secagem"), tempoSecagemField);
        grid.addRow(4, new Label("Ref. molde"), refMoldeField);
        grid.addRow(5, new Label("Observacoes"), observacoesArea);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        FichaTecnica payload = ficha == null ? new FichaTecnica() : ficha;
        payload.setTipoBarro(nullIfBlank(tipoBarroField.getText()));
        payload.setCorVidrado(nullIfBlank(corVidradoField.getText()));
        payload.setTempoSecagem(nullIfBlank(tempoSecagemField.getText()));
        payload.setRefMolde(nullIfBlank(refMoldeField.getText()));
        payload.setObservacoes(nullIfBlank(observacoesArea.getText()));
        payload.setTemperaturaCozedura(parseInteger(temperaturaField.getText()));

        CompletableFuture<?> request = ficha == null
                ? fichaTecnicaService.create(projetoId, payload)
                : fichaTecnicaService.update(ficha.getId(), payload);

        request.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) {
                showInfo("Nao foi possivel guardar a ficha tecnica.");
                return;
            }
            loadFichas(projetoId, listBox);
        }));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveClientName(ProjetoPersonalizado project) {
        if (project.getIdUtilizador() != null
                && project.getIdUtilizador().getNomeEmpresa() != null
                && !project.getIdUtilizador().getNomeEmpresa().isBlank()) {
            return project.getIdUtilizador().getNomeEmpresa();
        }
        if (project.getIdUtilizador() != null
                && project.getIdUtilizador().getEmail() != null
                && !project.getIdUtilizador().getEmail().isBlank()) {
            return project.getIdUtilizador().getEmail();
        }
        return "Cliente";
    }

    private String resolveTitle(ProjetoPersonalizado project) {
        if (project.getTituloProjeto() == null || project.getTituloProjeto().isBlank()) {
            return "Projeto personalizado";
        }
        return project.getTituloProjeto();
    }

    private String resolveDescription(ProjetoPersonalizado project) {
        if (project.getBriefing() == null || project.getBriefing().isBlank()) {
            return "Sem briefing disponivel.";
        }
        return project.getBriefing();
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

    private String toBackendProjectStatus(String normalizedStatus) {
        return normalizedStatus == null || normalizedStatus.isBlank() ? "briefing" : normalizedStatus;
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "briefing" -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "orcamento_enviado" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "completo" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
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

    private record ChatEntry(String senderType, String senderName, String message, String time) {
    }
}