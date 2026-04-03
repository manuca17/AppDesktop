package com.example.appdesktop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ClientDashboardService {

    public List<ProjectItem> mockProjects() {
        return List.of(
                new ProjectItem("PRJ-001", "Servico de Jantar Terracota", "in_production",
                        "Conjunto exclusivo para restaurante boutique.", LocalDate.now().minusDays(10), new BigDecimal("780.00")),
                new ProjectItem("PRJ-002", "Linha Ceramica Hotelaria", "quote_sent",
                        "Desenvolvimento de colecao personalizada para hotel.", LocalDate.now().minusDays(18), new BigDecimal("1250.00")),
                new ProjectItem("PRJ-003", "Colecao Primavera", "approved",
                        "Pecas para edicao sazonal de loja conceito.", LocalDate.now().minusDays(27), new BigDecimal("560.00")),
                new ProjectItem("PRJ-004", "Pecas comemorativas", "completed",
                        "Projeto fechado e entregue ao cliente.", LocalDate.now().minusDays(40), new BigDecimal("430.00"))
        );
    }

    public List<OrderItem> mockOrders() {
        return List.of(
                new OrderItem("ENC-1042", "delivered", LocalDate.now().minusDays(2), new BigDecimal("84.90"), 3),
                new OrderItem("ENC-1037", "paid", LocalDate.now().minusDays(5), new BigDecimal("59.50"), 2),
                new OrderItem("ENC-1029", "in_production", LocalDate.now().minusDays(9), new BigDecimal("134.20"), 5),
                new OrderItem("ENC-1014", "approved", LocalDate.now().minusDays(16), new BigDecimal("92.00"), 1)
        );
    }

    public List<ProjectItem> activeProjects(List<ProjectItem> projects) {
        return projects.stream()
                .filter(project -> !"completed".equals(project.status()))
                .toList();
    }

    public List<OrderItem> recentOrders(List<OrderItem> orders, int limit) {
        return orders.stream()
                .sorted(Comparator.comparing(OrderItem::date).reversed())
                .limit(limit)
                .toList();
    }

    public BigDecimal totalInvestment(List<OrderItem> orders, List<ProjectItem> projects) {
        BigDecimal ordersTotal = orders.stream()
                .map(OrderItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectsTotal = projects.stream()
                .map(ProjectItem::quoteTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ordersTotal.add(projectsTotal);
    }

    public String statusLabel(String status) {
        return switch (status) {
            case "briefing" -> "Em Analise";
            case "in_production" -> "Em Producao";
            case "quote_sent" -> "Orcamento Enviado";
            case "approved" -> "Aprovado";
            case "delivered" -> "Entregue";
            case "paid" -> "Pago";
            case "completed" -> "Concluido";
            default -> status.toLowerCase(Locale.ROOT);
        };
    }

    public record ProjectItem(
            String id,
            String title,
            String status,
            String description,
            LocalDate createdAt,
            BigDecimal quoteTotal
    ) {
    }

    public record OrderItem(
            String id,
            String status,
            LocalDate date,
            BigDecimal total,
            int itemCount
    ) {
    }
}
