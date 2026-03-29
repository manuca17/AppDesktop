package com.example.appdesktop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPortalDataServiceTest {

    private final ClientPortalDataService service = new ClientPortalDataService();

    @Test
    void shouldFilterProductsBySearchAndCategory() {
        List<ClientPortalDataService.ProductItem> products = service.mockProducts();

        List<ClientPortalDataService.ProductItem> filtered = service.filterProducts(products, "caneca", "Canecas");

        assertEquals(1, filtered.size());
        assertEquals("Caneca Ria", filtered.get(0).name());
    }

    @Test
    void shouldFilterOrdersByProductName() {
        List<ClientPortalDataService.OrderItem> orders = service.mockOrders();

        List<ClientPortalDataService.OrderItem> filtered = service.filterOrders(orders, "jarra");

        assertEquals(1, filtered.size());
        assertEquals("ENC-1014", filtered.get(0).id());
    }

    @Test
    void shouldCalculateOrderTotalFromLines() {
        ClientPortalDataService.OrderItem order = service.mockOrders().get(0);

        assertTrue(order.total().doubleValue() > 0);
        assertEquals(3, order.totalItems());
    }

    @Test
    void shouldFindByNormalizedIds() {
        assertTrue(service.findProjectById("prj001").isPresent());
        assertTrue(service.findOrderById("enc_1042").isPresent());
    }
}
