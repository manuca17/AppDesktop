package com.example.appdesktop;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.OrcamentoService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final OrcamentoService orcamentoService = OrcamentoService.getInstance();
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
            applyDashboardState(clientName, List.of(), List.of(), List.of());
            return;
        }

        CompletableFuture<List<ProjetoPersonalizado>> projectsFuture = projetoPersonalizadoService
                .findByUtilizadorId(currentUser.getId())
                .exceptionally(error -> List.of());

        CompletableFuture<List<EncomendaCatalogo>> ordersFuture = encomendaService
                .findByUtilizadorId(currentUser.getId())
                .exceptionally(error -> List.of());

        CompletableFuture<List<Orcamento>> quotesFuture = projectsFuture
                .thenCompose(this::loadQuotesForProjects)
                .exceptionally(error -> List.of());

        CompletableFuture.allOf(projectsFuture, ordersFuture, quotesFuture)
                .whenComplete((done, error) -> Platform.runLater(() -> {
                    List<ClientDashboardService.ProjectItem> mappedProjects = mapApiProjects(projectsFuture.join(), quotesFuture.join());
                    List<ClientDashboardService.OrderItem> mappedOrders = mapApiOrders(ordersFuture.join());
                    List<ClientDashboardService.OrderItem> recentOrders = dashboardService.recentOrders(mappedOrders, 4);
                    applyDashboardState(clientName, mappedProjects, mappedOrders, recentOrders);
                }));
    }

    private CompletableFuture<List<Orcamento>> loadQuotesForProjects(List<ProjetoPersonalizado> projects) {
        if (projects == null || projects.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<List<Orcamento>>> futures = new ArrayList<>();
        for (ProjetoPersonalizado project : projects) {
            if (project == null || project.getId() == null) {
                continue;
            }
            futures.add(orcamentoService.findByProjetoId(project.getId())
                    .exceptionally(error -> List.of()));
        }

        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return allDone.thenApply(done -> {
            List<Orcamento> merged = new ArrayList<>();
            for (CompletableFuture<List<Orcamento>> future : futures) {
                List<Orcamento> list = future.join();
                if (list != null && !list.isEmpty()) {
                    merged.addAll(list);
                }
            }
            return merged;
        });
    }

    private List<ClientDashboardService.OrderItem> mapApiOrders(List<EncomendaCatalogo> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        List<ClientDashboardService.OrderItem> mapped = new ArrayList<>();
        for (EncomendaCatalogo order : orders) {
            if (order == null) {
                continue;
            }

            String status = normalizeOrderStatus(order.getEstado());
            if ("carrinho".equals(status)) {
                continue;
            }

            int itemCount = 0;
            if (order.getLinhas() != null) {
                itemCount = order.getLinhas().stream()
                        .mapToInt(line -> line.getQuantidade() == null ? 0 : line.getQuantidade())
                        .sum();
            }

            mapped.add(new ClientDashboardService.OrderItem(
                    order.getId() == null ? "ENC-?" : "ENC-" + order.getId(),
                    status,
                    order.getDataEncomenda() == null ? LocalDate.now() : order.getDataEncomenda(),
                    order.total(),
                    itemCount
            ));
        }

        return mapped;
    }

    private void applyDashboardState(String clientName,
                                     List<ClientDashboardService.ProjectItem> projects,
                                     List<ClientDashboardService.OrderItem> allOrders,
                                     List<ClientDashboardService.OrderItem> recentOrders) {
        BigDecimal totalInvestment = dashboardService.totalInvestment(allOrders, projects);

        welcomeLabel.setText("Bem-vinda, " + clientName + "!");
        projectsCountLabel.setText(String.valueOf(projects.size()));
        ordersCountLabel.setText(String.valueOf(allOrders.size()));
        investmentLabel.setText(currencyFormat.format(totalInvestment));

        renderProjects(projects);
        renderOrders(recentOrders);
    }

    private String normalizeOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "pendente", "pending" -> "pending";
            case "pago", "paid" -> "paid";
            case "enviado", "shipped" -> "shipped";
            case "entregue", "delivered" -> "delivered";
            case "carrinho", "cart" -> "carrinho";
            default -> status.trim().toLowerCase(Locale.ROOT);
        };
    }

    private List<ClientDashboardService.ProjectItem> mapApiProjects(List<ProjetoPersonalizado> projects, List<Orcamento> quotes) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }

        Map<Integer, BigDecimal> totalByProjectId = new HashMap<>();
        if (quotes != null) {
            for (Orcamento quote : quotes) {
                if (quote == null || quote.getIdProjeto() == null || quote.getIdProjeto().getId() == null) {
                    continue;
                }
                Integer projectId = quote.getIdProjeto().getId();
                BigDecimal value = quote.getValorTotalEstimado() == null ? BigDecimal.ZERO : quote.getValorTotalEstimado();
                totalByProjectId.merge(projectId, value, BigDecimal::add);
            }
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
                        project.getId() == null
                                ? BigDecimal.ZERO
                                : totalByProjectId.getOrDefault(project.getId(), BigDecimal.ZERO)
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
            case "pending" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved", "paid" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
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