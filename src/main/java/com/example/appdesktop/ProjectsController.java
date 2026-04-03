package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
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
import java.util.List;
import java.util.Locale;

public class ProjectsController implements ClientPage {

    @FXML
    private VBox projectsContainer;

    @FXML
    private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final ProjetoPersonalizadoService projetoPersonalizadoService = ProjetoPersonalizadoService.getInstance();
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
                .whenComplete((projects, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        renderProjects(List.of());
                        return;
                    }
                    renderProjects(mapProjects(projects));
                }));
    }

    private List<ProjectCardData> mapProjects(List<ProjetoPersonalizado> projects) {
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
                                : project.getDataCriacao().atZone(ZoneId.systemDefault()).toLocalDate()
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
                stat("Quantidade", "--"),
                stat("Valor", currencyFormat.format(BigDecimal.ZERO)),
                stat("Mensagens", "--")
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
        return switch (status) {
            case "briefing" -> 0.1;
            case "quote_sent" -> 0.3;
            case "approved" -> 0.5;
            case "in_production" -> 0.75;
            case "completed" -> 1.0;
            default -> 0.0;
        };
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "briefing" -> "Briefing";
            case "quote_sent" -> "Orcamento";
            case "approved" -> "Aprovado";
            case "in_production" -> "Em Producao";
            case "completed" -> "Concluido";
            case "molding" -> "Moldagem";
            case "drying" -> "Secagem";
            case "first_firing" -> "Primeira Cozedura";
            case "glazing" -> "Vidragem";
            case "second_firing" -> "Segunda Cozedura";
            default -> "Acabamento";
        };
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
            LocalDate createdAt
    ) {
    }
}
