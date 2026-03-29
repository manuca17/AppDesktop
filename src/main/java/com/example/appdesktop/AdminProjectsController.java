package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminProjectsController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private VBox projectsContainer;
    @FXML private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<ClientPortalDataService.ProjectItem> allProjects;
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        allProjects = new ArrayList<>(dataService.mockProjects());
        statusCombo.getItems().setAll("all", "briefing", "quote_sent", "approved", "in_production", "completed");
        statusCombo.setValue("all");
        refreshProjects();
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

    private void refreshProjects() {
        projectsContainer.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        String statusFilter = statusCombo.getValue();

        List<ClientPortalDataService.ProjectItem> filtered = allProjects.stream()
                .filter(p -> search.isBlank()
                        || p.title().toLowerCase(Locale.ROOT).contains(search)
                        || p.id().toLowerCase(Locale.ROOT).contains(search))
                .filter(p -> "all".equals(statusFilter) || p.status().equals(statusFilter))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ClientPortalDataService.ProjectItem project : filtered) {
            projectsContainer.getChildren().add(createProjectCard(project));
        }
    }

    private VBox createProjectCard(ClientPortalDataService.ProjectItem project) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Header
        HBox header = new HBox(8);
        Label title = new Label(project.title() + "  (" + project.id() + ")");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusLabel = new Label(dataService.projectStatusLabel(project.status()));
        statusLabel.setStyle(statusStyle(project.status()));
        header.getChildren().addAll(title, spacer, statusLabel);

        Label description = new Label(project.description());
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        // Stats row
        HBox stats = new HBox(20,
                stat("Quantidade", project.quantity() + " pecas"),
                stat("Orcamento", currencyFormat.format(project.quote().total())),
                stat("Mensagens", String.valueOf(project.messagesCount())),
                stat("Criado em", dateFormatter.format(project.createdAt()))
        );

        ProgressBar progressBar = new ProgressBar(completion(project.stages()));
        progressBar.setPrefWidth(Double.MAX_VALUE);

        // Actions
        HBox actions = new HBox(8);
        javafx.scene.control.Button updateStatusBtn = new javafx.scene.control.Button("Atualizar Estado");
        updateStatusBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white;");
        updateStatusBtn.setOnAction(e -> updateProjectStatus(project));

        javafx.scene.control.Button detailsBtn = new javafx.scene.control.Button("Ver Detalhes");
        detailsBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        detailsBtn.setOnAction(e -> showProjectDetails(project));

        actions.getChildren().addAll(updateStatusBtn, detailsBtn);

        card.getChildren().addAll(header, description, stats, progressBar, actions);
        return card;
    }

    private VBox stat(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return new VBox(2, l, v);
    }

    private void updateProjectStatus(ClientPortalDataService.ProjectItem project) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Atualizar Estado do Projeto");
        dialog.setHeaderText(project.title());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(100);
        ColumnConstraints col2 = new ColumnConstraints(220);
        grid.getColumnConstraints().addAll(col1, col2);

        ComboBox<String> newStatusCombo = new ComboBox<>();
        newStatusCombo.getItems().setAll("briefing", "quote_sent", "approved", "in_production", "completed");
        newStatusCombo.setValue(project.status());

        grid.addRow(0, new Label("Novo Estado:"), newStatusCombo);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String newStatus = newStatusCombo.getValue();
            ClientPortalDataService.ProjectItem updated = new ClientPortalDataService.ProjectItem(
                    project.id(), project.title(), newStatus, project.description(),
                    project.quantity(), project.quote(), project.messagesCount(),
                    project.createdAt(), project.currentStage(), project.stages());
            allProjects.replaceAll(p -> p.id().equals(project.id()) ? updated : p);
            refreshProjects();
            showInfo("Estado atualizado para: " + dataService.projectStatusLabel(newStatus));
        }
    }

    private void showProjectDetails(ClientPortalDataService.ProjectItem project) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalhes - " + project.id());
        alert.setHeaderText(project.title());
        alert.setContentText(
                "Estado: " + dataService.projectStatusLabel(project.status()) + "\n" +
                "Descricao: " + project.description() + "\n" +
                "Quantidade: " + project.quantity() + " pecas\n" +
                "Orcamento total: " + currencyFormat.format(project.quote().total()) + "\n" +
                "  - Design: " + currencyFormat.format(project.quote().designPhase()) + "\n" +
                "  - Moldagem: " + currencyFormat.format(project.quote().moldPhase()) + "\n" +
                "  - Producao: " + currencyFormat.format(project.quote().productionPhase()) + "\n" +
                "Mensagens: " + project.messagesCount() + "\n" +
                "Criado em: " + dateFormatter.format(project.createdAt()));
        alert.showAndWait();
    }

    private double completion(List<String> stages) {
        if (stages == null || stages.isEmpty()) return 0;
        long done = stages.stream().filter("completed"::equals).count();
        long inProgress = stages.stream().filter("in_progress"::equals).count();
        return (done + (inProgress > 0 ? 0.5 : 0.0)) / stages.size();
    }

    private String statusStyle(String status) {
        return switch (status) {
            case "quote_sent"    -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved"      -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "completed"     -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            default              -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
