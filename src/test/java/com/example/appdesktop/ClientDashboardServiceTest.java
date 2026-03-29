package com.example.appdesktop;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientDashboardServiceTest {

    private final ClientDashboardService service = new ClientDashboardService();

    @Test
    void shouldFilterOnlyActiveProjects() {
        List<ClientDashboardService.ProjectItem> projects = List.of(
                new ClientDashboardService.ProjectItem("1", "A", "in_production", "", LocalDate.now(), new BigDecimal("10")),
                new ClientDashboardService.ProjectItem("2", "B", "completed", "", LocalDate.now(), new BigDecimal("20")),
                new ClientDashboardService.ProjectItem("3", "C", "approved", "", LocalDate.now(), new BigDecimal("30"))
        );

        List<ClientDashboardService.ProjectItem> activeProjects = service.activeProjects(projects);

        assertEquals(2, activeProjects.size());
    }

    @Test
    void shouldCalculateTotalInvestment() {
        List<ClientDashboardService.OrderItem> orders = List.of(
                new ClientDashboardService.OrderItem("1", "paid", LocalDate.now(), new BigDecimal("50.25"), 1),
                new ClientDashboardService.OrderItem("2", "paid", LocalDate.now(), new BigDecimal("49.75"), 2)
        );

        List<ClientDashboardService.ProjectItem> projects = List.of(
                new ClientDashboardService.ProjectItem("3", "P1", "approved", "", LocalDate.now(), new BigDecimal("100.00")),
                new ClientDashboardService.ProjectItem("4", "P2", "approved", "", LocalDate.now(), new BigDecimal("150.00"))
        );

        BigDecimal total = service.totalInvestment(orders, projects);

        assertEquals(new BigDecimal("350.00"), total);
    }

    @Test
    void shouldLimitRecentOrders() {
        List<ClientDashboardService.OrderItem> orders = List.of(
                new ClientDashboardService.OrderItem("A", "paid", LocalDate.of(2026, 3, 1), new BigDecimal("10"), 1),
                new ClientDashboardService.OrderItem("B", "paid", LocalDate.of(2026, 3, 10), new BigDecimal("10"), 1),
                new ClientDashboardService.OrderItem("C", "paid", LocalDate.of(2026, 3, 5), new BigDecimal("10"), 1)
        );

        List<ClientDashboardService.OrderItem> recent = service.recentOrders(orders, 2);

        assertEquals(2, recent.size());
        assertEquals("B", recent.get(0).id());
        assertEquals("C", recent.get(1).id());
    }
}
