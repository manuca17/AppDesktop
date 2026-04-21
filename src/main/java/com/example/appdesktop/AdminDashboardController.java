package com.example.appdesktop;

import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.services.ArtigoCatalogoService;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class AdminDashboardController implements AdminPage {

    @FXML private Label activeProjectsLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label revenueLabel;
    @FXML private VBox recentOrdersContainer;
    @FXML private VBox activeProjectsContainer;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        loadDashboard();
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    private void loadDashboard() {
        CompletableFuture<List<ArtigoCatalogo>> productsFuture = artigoService.findAll().exceptionally(error -> List.of());
        CompletableFuture<List<ProjetoPersonalizado>> projectsFuture = projetoService.findAll().exceptionally(error -> List.of());
        CompletableFuture<List<EncomendaCatalogo>> ordersFuture = encomendaService.findAll().exceptionally(error -> List.of());

        CompletableFuture.allOf(productsFuture, projectsFuture, ordersFuture)
                .whenComplete((done, error) -> Platform.runLater(() -> {
                    List<ArtigoCatalogo> products = productsFuture.join();
                    List<ProjetoPersonalizado> projects = projectsFuture.join();
                    List<EncomendaCatalogo> orders = ordersFuture.join();

                    applyKpis(products, projects, orders);
                    renderRecentOrders(mapRecentOrders(orders));
                    renderActiveProjects(mapActiveProjects(projects));
                }));
    }

    private void applyKpis(List<ArtigoCatalogo> products,
                           List<ProjetoPersonalizado> projects,
                           List<EncomendaCatalogo> orders) {
        long activeProjects = projects.stream()
                .filter(project -> project != null)
                .filter(project -> {
                    String status = normalizeProjectStatus(project.getEstadoAtual());
                    return !"completed".equals(status);
                })
                .count();

        long pendingOrders = orders.stream()
                .filter(order -> order != null)
                .filter(order -> {
                    String status = normalizeOrderStatus(order.getEstado());
                    return !"delivered".equals(status) && !"carrinho".equals(status);
                })
                .count();

        long lowStock = products.stream()
                .filter(product -> product != null)
                .filter(product -> {
                    Integer stock = product.getStock();
                    return stock != null && stock < 5;
                })
                .count();

        BigDecimal orderRevenue = orders.stream()
                .filter(order -> order != null)
                .filter(order -> !"carrinho".equals(normalizeOrderStatus(order.getEstado())))
                .map(EncomendaCatalogo::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        activeProjectsLabel.setText(String.valueOf(activeProjects));
        pendingOrdersLabel.setText(String.valueOf(pendingOrders));
        lowStockLabel.setText(String.valueOf(lowStock));
        revenueLabel.setText(currencyFormat.format(orderRevenue));
    }

    private List<OrderRow> mapRecentOrders(List<EncomendaCatalogo> orders) {
        return orders.stream()
                .filter(order -> order != null)
                .filter(order -> !"carrinho".equals(normalizeOrderStatus(order.getEstado())))
                .sorted(Comparator.comparing(order -> order.getDataEncomenda() == null ? LocalDate.MIN : order.getDataEncomenda(), Comparator.reverseOrder()))
                .limit(3)
                .map(order -> new OrderRow(
                        order.getId() == null ? "ENC-?" : "ENC-" + order.getId(),
                        order.getIdUtilizador() != null && order.getIdUtilizador().getNomeEmpresa() != null
                                ? order.getIdUtilizador().getNomeEmpresa()
                                : "Cliente",
                        order.getDataEncomenda() == null ? LocalDate.now() : order.getDataEncomenda(),
                        normalizeOrderStatus(order.getEstado()),
                        order.total()
                ))
                .toList();
    }

    private List<ProjectRow> mapActiveProjects(List<ProjetoPersonalizado> projects) {
        return projects.stream()
                .filter(project -> project != null)
                .map(project -> new ProjectRow(
                        project.getId() == null ? "PRJ-?" : "PRJ-" + project.getId(),
                        project.getTituloProjeto() == null || project.getTituloProjeto().isBlank()
                                ? "Projeto personalizado"
                                : project.getTituloProjeto(),
                        normalizeProjectStatus(project.getEstadoAtual()),
                        project.getQuantidade() == null ? 0 : project.getQuantidade(),
                        project.getDataCriacao() == null
                                ? LocalDate.now()
                                : project.getDataCriacao().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
                .filter(project -> !"completed".equals(project.status()))
                .sorted(Comparator.comparing(ProjectRow::createdAt, Comparator.reverseOrder()))
                .limit(4)
                .toList();
    }

    private void renderRecentOrders(List<OrderRow> orders) {
        recentOrdersContainer.getChildren().clear();
        if (orders.isEmpty()) {
            Label empty = new Label("Sem encomendas recentes.");
            empty.setStyle("-fx-text-fill: #6b7280;");
            recentOrdersContainer.getChildren().add(empty);
            return;
        }

        for (OrderRow order : orders) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            VBox left = new VBox(2);
            Label id = new Label(order.id());
            id.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Label client = new Label(order.clientName() + "  |  " + dateFormatter.format(order.date()));
            client.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            left.getChildren().addAll(id, client);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(dataService.orderStatusLabel(order.status()));
            status.setStyle(orderStatusStyle(order.status()));

            Label total = new Label(currencyFormat.format(order.total()));
            total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

            row.getChildren().addAll(left, spacer, status, total);
            recentOrdersContainer.getChildren().add(row);
        }
    }

    private void renderActiveProjects(List<ProjectRow> projects) {
        activeProjectsContainer.getChildren().clear();
        if (projects.isEmpty()) {
            Label empty = new Label("Sem projetos ativos.");
            empty.setStyle("-fx-text-fill: #6b7280;");
            activeProjectsContainer.getChildren().add(empty);
            return;
        }

        for (ProjectRow project : projects) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

            VBox left = new VBox(2);
            Label title = new Label(project.title());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
            Label meta = new Label(project.quantity() + " pecas  |  " + dateFormatter.format(project.createdAt()));
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            left.getChildren().addAll(title, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(dataService.projectStatusLabel(project.status()));
            status.setStyle(projectStatusStyle(project.status()));

            row.getChildren().addAll(left, spacer, status);
            activeProjectsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void onViewAllOrders() {
        if (navigator != null) {
            navigator.navigateTo("admin-orders");
        }
    }

    @FXML
    private void onViewAllProjects() {
        if (navigator != null) {
            navigator.navigateTo("admin-projects");
        }
    }

    @FXML
    private void onManageCatalog() {
        if (navigator != null) {
            navigator.navigateTo("admin-catalog");
        }
    }

    private String normalizeProjectStatus(String status) {
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

    private String orderStatusStyle(String status) {
        return switch (status) {
            case "paid" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "shipped" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "delivered" -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private String projectStatusStyle(String status) {
        return switch (status) {
            case "quote_sent" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "approved" -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "in_production" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 4 8; -fx-background-radius: 999;";
            case "completed" -> "-fx-background-color: #ede9fe; -fx-text-fill: #5b21b6; -fx-padding: 4 8; -fx-background-radius: 999;";
            default -> "-fx-background-color: #e5e7eb; -fx-text-fill: #374151; -fx-padding: 4 8; -fx-background-radius: 999;";
        };
    }

    private record OrderRow(String id, String clientName, LocalDate date, String status, BigDecimal total) {
    }

    private record ProjectRow(String id, String title, String status, int quantity, LocalDate createdAt) {
    }
}