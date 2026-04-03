package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ClientDashboardController implements ClientPage {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label projectsCountLabel;

    @FXML
    private Label ordersCountLabel;

    @FXML
    private Label investmentLabel;

    @FXML
    private VBox projectsContainer;

    @FXML
    private VBox ordersContainer;

    private final ClientDashboardService dashboardService = new ClientDashboardService();
    private final ProjetoPersonalizadoService projetoPersonalizadoService = ProjetoPersonalizadoService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        loadDashboard("Maria");
    }

    public void setClientName(String clientName) {
        loadDashboard(clientName);
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    private void loadDashboard(String clientName) {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            applyDashboardState(clientName, List.of(), List.of());
            return;
        }

        List<ClientDashboardService.OrderItem> orders = List.of();
        List<ClientDashboardService.OrderItem> recentOrders = List.of();

        projetoPersonalizadoService.findByUtilizadorId(currentUser.getId())
                .whenComplete((apiProjects, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        applyDashboardState(clientName, List.of(), List.of());
                        return;
                    }

                    List<ClientDashboardService.ProjectItem> mappedProjects = mapApiProjects(apiProjects);
                    applyDashboardState(clientName, mappedProjects, recentOrders);
                }));
    }

    private void applyDashboardState(String clientName,
                                     List<ClientDashboardService.ProjectItem> projects,
                                     List<ClientDashboardService.OrderItem> orders) {
        BigDecimal totalInvestment = dashboardService.totalInvestment(orders, projects);

        welcomeLabel.setText("Bem-vinda, " + clientName + "!");
        projectsCountLabel.setText(String.valueOf(projects.size()));
        ordersCountLabel.setText(String.valueOf(orders.size()));
        investmentLabel.setText(currencyFormat.format(totalInvestment));

        renderProjects(projects);
        renderOrders(orders);
    }

    private List<ClientDashboardService.ProjectItem> mapApiProjects(List<ProjetoPersonalizado> projects) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }

        return projects.stream()
                .map(project -> new ClientDashboardService.ProjectItem(
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
                        BigDecimal.ZERO
                ))
                .toList();
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

    private void renderProjects(List<ClientDashboardService.ProjectItem> projects) {
        projectsContainer.getChildren().clear();
        for (ClientDashboardService.ProjectItem project : projects) {
            VBox card = new VBox(8);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Label title = new Label(project.title());
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            Label badge = new Label(dashboardService.statusLabel(project.status()));
            badge.setStyle(styleForStatus(project.status()));

            header.getChildren().addAll(title, badge);

            Label description = new Label(project.description());
            description.setWrapText(true);
            description.setStyle("-fx-text-fill: #4b5563;");

            Label meta = new Label("Criado em " + dateFormatter.format(project.createdAt()) + "   |   Valor: "
                    + currencyFormat.format(project.quoteTotal()));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            card.getChildren().addAll(header, description, meta);
            projectsContainer.getChildren().add(card);
        }
    }

    private void renderOrders(List<ClientDashboardService.OrderItem> orders) {
        ordersContainer.getChildren().clear();
        for (ClientDashboardService.OrderItem order : orders) {
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-background-radius: 8;");

            VBox left = new VBox(4);
            Label id = new Label(order.id());
            id.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");

            String itemText = order.itemCount() == 1 ? "item" : "itens";
            Label details = new Label(order.itemCount() + " " + itemText + "   |   " + dateFormatter.format(order.date()));
            details.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            left.getChildren().addAll(id, details);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox right = new VBox(4);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label badge = new Label(dashboardService.statusLabel(order.status()));
            badge.setStyle(styleForStatus(order.status()));

            Label total = new Label(currencyFormat.format(order.total()));
            total.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            right.getChildren().addAll(badge, total);

            card.getChildren().addAll(left, spacer, right);
            ordersContainer.getChildren().add(card);
        }
    }

    private String styleForStatus(String status) {
        return switch (status) {
            case "briefing" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "quote_sent" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved", "paid" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered", "completed" -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    @FXML
    private void onViewAllProjects() {
        if (navigator != null) {
            navigator.navigateTo("projects");
            return;
        }
        showInfo("Projetos", "Abrir lista completa de projetos.");
    }

    @FXML
    private void onViewAllOrders() {
        if (navigator != null) {
            navigator.navigateTo("orders");
            return;
        }
        showInfo("Encomendas", "Abrir lista completa de encomendas.");
    }

    @FXML
    private void onNewBriefing() {
        if (navigator != null) {
            navigator.navigateTo("briefing");
            return;
        }
        showInfo("Novo Briefing", "Abrir formulario de briefing personalizado.");
    }

    @FXML
    private void onOpenCatalog() {
        if (navigator != null) {
            navigator.navigateTo("catalog");
            return;
        }
        showInfo("Catalogo", "Abrir catalogo de pecas artesanais.");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
