package com.example.appdesktop;

import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.MensagemChatService;
import com.example.appdesktop.services.OrcamentoService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProjectsController implements ClientPage {

    @FXML
    private VBox projectsContainer;

    @FXML
    private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final ProjetoPersonalizadoService projetoPersonalizadoService = ProjetoPersonalizadoService.getInstance();
    private final MensagemChatService mensagemChatService = MensagemChatService.getInstance();
    private final OrcamentoService orcamentoService = OrcamentoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        loadProjectsForCurrentUser();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onNewProject() {
        if (navigator != null) {
            navigator.navigateTo("briefing");
        }
    }

    private void loadProjectsForCurrentUser() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            renderProjects(List.of());
            return;
        }

        projetoPersonalizadoService.findByUtilizadorId(currentUser.getId())
                .thenCompose(projects -> {
                    CompletableFuture<Map<Integer, BigDecimal>> quoteTotalsFuture = loadQuoteTotals(projects)
                            .exceptionally(error -> Map.of());
                    CompletableFuture<Map<Integer, Integer>> messageTotalsFuture = loadMessageTotals(projects)
                            .exceptionally(error -> Map.of());
                    return quoteTotalsFuture.thenCombine(messageTotalsFuture,
                            (quoteTotals, messageTotals) -> new Object[]{projects, quoteTotals, messageTotals});
                })
                .whenComplete((result, error) -> Platform.runLater(() -> {
                    if (error != null || result == null) {
                        renderProjects(List.of());
                        return;
                    }
                    List<ProjetoPersonalizado> projects = (List<ProjetoPersonalizado>) result[0];
                    Map<Integer, BigDecimal> quoteTotals = (Map<Integer, BigDecimal>) result[1];
                    Map<Integer, Integer> messageTotals = (Map<Integer, Integer>) result[2];
                    renderProjects(mapProjects(projects, quoteTotals, messageTotals));
                }));
    }

    private CompletableFuture<Map<Integer, BigDecimal>> loadQuoteTotals(List<ProjetoPersonalizado> projects) {
        if (projects == null || projects.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        Map<Integer, BigDecimal> totals = new HashMap<>();
        List<CompletableFuture<Void>> requests = new ArrayList<>();
        for (ProjetoPersonalizado project : projects) {
            if (project == null || project.getId() == null) {
                continue;
            }
            CompletableFuture<Void> request = orcamentoService.findByProjetoId(project.getId())
                    .thenAccept(quotes -> totals.put(project.getId(), sumQuotes(quotes)))
                    .exceptionally(error -> {
                        totals.put(project.getId(), BigDecimal.ZERO);
                        return null;
                    });
            requests.add(request);
        }

        if (requests.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        return CompletableFuture.allOf(requests.toArray(new CompletableFuture[0]))
                .thenApply(done -> totals);
    }

    private BigDecimal sumQuotes(List<Orcamento> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return quotes.stream()
                .filter(q -> q != null && q.getValorTotalEstimado() != null)
                .map(Orcamento::getValorTotalEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CompletableFuture<Map<Integer, Integer>> loadMessageTotals(List<ProjetoPersonalizado> projects) {
        if (projects == null || projects.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        Map<Integer, Integer> totals = new HashMap<>();
        List<CompletableFuture<Void>> requests = new ArrayList<>();
        for (ProjetoPersonalizado project : projects) {
            if (project == null || project.getId() == null) {
                continue;
            }
            CompletableFuture<Void> request = mensagemChatService.findTotalByProjetoId(project.getId())
                    .thenAccept(total -> totals.put(project.getId(), total == null ? 0 : total))
                    .exceptionally(error -> {
                        totals.put(project.getId(), 0);
                        return null;
                    });
            requests.add(request);
        }

        if (requests.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        return CompletableFuture.allOf(requests.toArray(new CompletableFuture[0]))
                .thenApply(done -> totals);
    }

    private List<ProjectCardData> mapProjects(List<ProjetoPersonalizado> projects,
                                              Map<Integer, BigDecimal> quoteTotals,
                                              Map<Integer, Integer> messageTotals) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }

        return projects.stream()
                .map(project -> new ProjectCardData(
                        project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId(),
                        project.getTituloProjeto() == null || project.getTituloProjeto().isBlank()
                                ? "Projeto personalizado"
                                : project.getTituloProjeto(),
                        normalizeStatus(project.getEstadoAtual()),
                        project.getBriefing() == null || project.getBriefing().isBlank()
                                ? "Sem briefing disponivel."
                                : project.getBriefing(),
                        project.getDataCriacao() == null
                                ? LocalDate.now()
                                : project.getDataCriacao().atZone(ZoneId.systemDefault()).toLocalDate(),
                        project.getQuantidade(),
                        project.getId() == null ? BigDecimal.ZERO : quoteTotals.getOrDefault(project.getId(), BigDecimal.ZERO),
                        project.getId() == null ? 0 : messageTotals.getOrDefault(project.getId(), 0)
                ))
                .toList();
    }

    private void renderProjects(List<ProjectCardData> projects) {
        projectsContainer.getChildren().clear();

        emptyLabel.setVisible(projects.isEmpty());
        emptyLabel.setManaged(projects.isEmpty());

        for (ProjectCardData project : projects) {
            projectsContainer.getChildren().add(createCard(project));
        }
    }

    private VBox createCard(ProjectCardData project) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(8);
        Label title = new Label(project.title() + " (" + project.id() + ")");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label status = new Label(dataService.projectStatusLabel(project.status()));
        status.setStyle(statusStyle(project.status()));
        header.getChildren().addAll(title, spacer, status);

        Label description = new Label(project.description());
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        HBox stats = new HBox(16,
                stat("Quantidade", project.quantidade() != null ? project.quantidade() + " pecas" : "--"),
                stat("Valor", currencyFormat.format(project.quoteTotal() == null ? BigDecimal.ZERO : project.quoteTotal())),
                stat("Mensagens", String.valueOf(project.messageCount()))
        );

        Label timeline = new Label("Criado em " + dateFormatter.format(project.createdAt())
                + "   |   Fase atual: " + stageLabel(project.status()));
        timeline.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        ProgressBar progressBar = new ProgressBar(completion(project.status()));
        progressBar.setPrefWidth(Double.MAX_VALUE);

        Button detailsButton = new Button("Ver Detalhes do Projeto");
        detailsButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
        detailsButton.setOnAction(event -> openProjectDetail(project.id()));

        card.getChildren().addAll(header, description, stats, timeline, progressBar, detailsButton);
        return card;
    }

    private VBox stat(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return new VBox(2, l, v);
    }

    private double completion(String status) {
        String normalized = normalizeStatus(status);
        return switch (normalized) {
            case "briefing" -> 0.08;
            case "orcamento_enviado" -> 0.16;
            case "design" -> 0.24;
            case "molde" -> 0.32;
            case "producao" -> 0.40;
            case "enchimento_moldes" -> 0.50;
            case "secagem" -> 0.60;
            case "acabamento" -> 0.70;
            case "cozedura" -> 0.80;
            case "vidragem" -> 0.90;
            case "inspecao_qualidade" -> 0.96;
            case "completo" -> 1.0;
            default -> 0.08;
        };
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "briefing" -> "Briefing";
            case "orcamento_enviado" -> "Orcamento Enviado";
            case "design" -> "Design";
            case "molde" -> "Molde";
            case "producao" -> "Producao";
            case "enchimento_moldes" -> "Enchimento de Moldes";
            case "secagem" -> "Secagem";
            case "acabamento" -> "Acabamento";
            case "cozedura" -> "Cozedura";
            case "vidragem" -> "Vidragem";
            case "inspecao_qualidade" -> "Inspecao de Qualidade";
            case "completo" -> "Concluido";
            default -> "Acabamento";
        };
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

    private void openProjectDetail(String projectId) {
        if (navigator != null) {
            navigator.navigateTo("project-detail:" + projectId);
        }
    }

    private record ProjectCardData(
            String id,
            String title,
            String status,
            String description,
            LocalDate createdAt,
            Integer quantidade,
            BigDecimal quoteTotal,
            int messageCount
    ) {
    }
}