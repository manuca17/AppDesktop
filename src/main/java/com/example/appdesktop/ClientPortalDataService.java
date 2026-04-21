package com.example.appdesktop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ClientPortalDataService {

    public List<ProductItem> mockProducts() {
        return List.of(
                new ProductItem("P-100", "Caneca Ria", "Canecas", "Caneca artesanal com vidrado azul oceano.",
                        new BigDecimal("19.90"), 12, "https://picsum.photos/seed/caneca1/300/300",
                        new TechnicalSheet("Gres", "Azul fosco", 1220, "9x9 cm", "320 g", "Pode ir a maquina de lavar.")),
                new ProductItem("P-101", "Prato Serra", "Pratos", "Prato de sobremesa com acabamento rustico.",
                        new BigDecimal("24.50"), 4, "https://picsum.photos/seed/prato1/300/300",
                        new TechnicalSheet("Faiança", "Branco brilhante", 1080, "21 cm", "410 g", "Ideal para empratamento.")),
                new ProductItem("P-102", "Jarra Duna", "Decoracao", "Jarra alta para flores secas ou frescas.",
                        new BigDecimal("38.00"), 0, "https://picsum.photos/seed/jarra1/300/300",
                        new TechnicalSheet("Terracota", "Natural", 980, "26x12 cm", "820 g", "Uso decorativo interno.")),
                new ProductItem("P-103", "Tigela Alfama", "Tigelas", "Tigela media para sopas e bowls.",
                        new BigDecimal("17.75"), 9, "https://picsum.photos/seed/tigela1/300/300",
                        new TechnicalSheet("Gres", "Verde acetinado", 1210, "16x7 cm", "360 g", "Resistente ao micro-ondas.")),
                new ProductItem("P-104", "Travessa Sol", "Servir", "Travessa oval para servir entradas.",
                        new BigDecimal("29.90"), 6, "https://picsum.photos/seed/travessa1/300/300",
                        new TechnicalSheet("Faiança", "Ambar", 1060, "32x18 cm", "760 g", "Recomenda-se lavagem manual."))
        );
    }

    public List<ProjectItem> mockProjects() {
        return List.of(
                new ProjectItem("PRJ-001", "Servico de Jantar Terracota", "in_production",
                        "Conjunto exclusivo para restaurante boutique.",
                        60,
                        new ProjectQuote(new BigDecimal("180.00"), new BigDecimal("220.00"), new BigDecimal("380.00")),
                        5,
                        LocalDate.now().minusDays(10),
                        "glazing",
                        List.of("completed", "completed", "completed", "in_progress", "pending", "pending")),
                new ProjectItem("PRJ-002", "Linha Ceramica Hotelaria", "quote_sent",
                        "Colecao personalizada para unidades hoteleiras.",
                        120,
                        new ProjectQuote(new BigDecimal("250.00"), new BigDecimal("300.00"), new BigDecimal("700.00")),
                        3,
                        LocalDate.now().minusDays(18),
                        "molding",
                        List.of("completed", "in_progress", "pending", "pending", "pending", "pending")),
                new ProjectItem("PRJ-003", "Colecao Primavera", "approved",
                        "Pecas para edicao sazonal de loja conceito.",
                        40,
                        new ProjectQuote(new BigDecimal("120.00"), new BigDecimal("140.00"), new BigDecimal("300.00")),
                        2,
                        LocalDate.now().minusDays(27),
                        "drying",
                        List.of("completed", "completed", "in_progress", "pending", "pending", "pending"))
        );
    }

    public List<OrderItem> mockOrders() {
        return List.of(
                new OrderItem("ENC-1042", "delivered", "paid", "Maria Silva",
                        new DeliveryAddress("Restaurante Terra", "Rua Principal, 123", "1000-000", "Lisboa", "Portugal"),
                        LocalDate.now().minusDays(2),
                        List.of(
                                new OrderLine("Caneca Ria", 2, new BigDecimal("19.90"), "https://picsum.photos/seed/caneca1/120/120"),
                                new OrderLine("Travessa Sol", 1, new BigDecimal("29.90"), "https://picsum.photos/seed/travessa1/120/120")
                        )),
                new OrderItem("ENC-1037", "paid", "paid", "Maria Silva",
                        new DeliveryAddress("Restaurante Terra", "Rua Principal, 123", "1000-000", "Lisboa", "Portugal"),
                        LocalDate.now().minusDays(5),
                        List.of(
                                new OrderLine("Prato Serra", 2, new BigDecimal("24.50"), "https://picsum.photos/seed/prato1/120/120")
                        )),
                new OrderItem("ENC-1029", "in_production", "paid", "Maria Silva",
                        new DeliveryAddress("Restaurante Terra", "Rua Principal, 123", "1000-000", "Lisboa", "Portugal"),
                        LocalDate.now().minusDays(9),
                        List.of(
                                new OrderLine("Tigela Alfama", 4, new BigDecimal("17.75"), "https://picsum.photos/seed/tigela1/120/120"),
                                new OrderLine("Caneca Ria", 3, new BigDecimal("19.90"), "https://picsum.photos/seed/caneca1/120/120")
                        )),
                new OrderItem("ENC-1014", "shipped", "pending", "Maria Silva",
                        new DeliveryAddress("Restaurante Terra", "Rua Principal, 123", "1000-000", "Lisboa", "Portugal"),
                        LocalDate.now().minusDays(16),
                        List.of(
                                new OrderLine("Jarra Duna", 1, new BigDecimal("38.00"), "https://picsum.photos/seed/jarra1/120/120")
                        ))
        );
    }

    public Optional<ProjectItem> findProjectById(String projectId) {
        String normalized = normalizeId(projectId);
        return mockProjects().stream()
                .filter(project -> normalizeId(project.id()).equals(normalized))
                .findFirst();
    }

    public Optional<OrderItem> findOrderById(String orderId) {
        String normalized = normalizeId(orderId);
        return mockOrders().stream()
                .filter(order -> normalizeId(order.id()).equals(normalized))
                .findFirst();
    }

    public ProjectBriefing projectBriefing(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> new ProjectBriefing("Loiça de Mesa", new BigDecimal("1200.00"), 60,
                    LocalDate.now().plusDays(22), "Conjunto premium com acabamentos organicos para degustacao.");
            case "PRJ-002" -> new ProjectBriefing("Hotelaria", new BigDecimal("1800.00"), 120,
                    LocalDate.now().plusDays(35), "Colecao para pequeno-almoco e room-service de boutique hotel.");
            default -> new ProjectBriefing("Retalho", new BigDecimal("900.00"), 40,
                    LocalDate.now().plusDays(16), "Colecao sazonal para loja fisica e online.");
        };
    }

    public String quoteDetails(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> "Inclui prototipagem, testes de vidrado e embalagem personalizada.";
            case "PRJ-002" -> "Inclui desenvolvimento de molde e lote piloto de validacao.";
            default -> "Inclui desenho da colecao e producao em duas fornadas.";
        };
    }

    public List<ProjectStage> trackingStages(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> List.of(
                    new ProjectStage("Briefing", "completed", LocalDate.now().minusDays(10), LocalDate.now().minusDays(9)),
                    new ProjectStage("Design", "completed", LocalDate.now().minusDays(9), LocalDate.now().minusDays(7)),
                    new ProjectStage("Moldagem", "completed", LocalDate.now().minusDays(7), LocalDate.now().minusDays(5)),
                    new ProjectStage("Secagem", "in_progress", LocalDate.now().minusDays(4), null),
                    new ProjectStage("Vidragem", "pending", null, null),
                    new ProjectStage("Segunda cozedura", "pending", null, null)
            );
            case "PRJ-002" -> List.of(
                    new ProjectStage("Briefing", "completed", LocalDate.now().minusDays(18), LocalDate.now().minusDays(16)),
                    new ProjectStage("Design", "in_progress", LocalDate.now().minusDays(15), null),
                    new ProjectStage("Moldagem", "pending", null, null),
                    new ProjectStage("Secagem", "pending", null, null),
                    new ProjectStage("Vidragem", "pending", null, null)
            );
            default -> List.of(
                    new ProjectStage("Briefing", "completed", LocalDate.now().minusDays(27), LocalDate.now().minusDays(24)),
                    new ProjectStage("Design", "completed", LocalDate.now().minusDays(24), LocalDate.now().minusDays(21)),
                    new ProjectStage("Aprovacao", "in_progress", LocalDate.now().minusDays(20), null),
                    new ProjectStage("Producao", "pending", null, null)
            );
        };
    }

    public List<ProjectMeeting> projectMeetings(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> List.of(
                    new ProjectMeeting("M-001", projectId, LocalDate.now().plusDays(2), "11:00",
                            "Revisao final do vidrado e validacao da paleta de cores.", "scheduled"),
                    new ProjectMeeting("M-002", projectId, LocalDate.now().minusDays(6), "14:30",
                            "Alinhamento de proporcoes e medidas das pecas.", "confirmed")
            );
            case "PRJ-002" -> List.of(
                    new ProjectMeeting("M-003", projectId, LocalDate.now().plusDays(4), "10:00",
                            "Avaliacao de prototipos para hotelaria.", "scheduled")
            );
            default -> List.of();
        };
    }

    public List<ProjectMessage> projectMessages(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> List.of(
                    new ProjectMessage("MSG-01", projectId, "artisan", "Ines (Artesa)",
                            "Ja finalizamos as primeiras amostras. Envio fotos hoje a tarde.", "10:42"),
                    new ProjectMessage("MSG-02", projectId, "client", "Maria Silva",
                            "Perfeito, obrigada. Quero validar o tom de azul antes da fornalha final.", "11:10")
            );
            case "PRJ-002" -> List.of(
                    new ProjectMessage("MSG-03", projectId, "artisan", "Ines (Artesa)",
                            "Precisamos confirmar o numero final de unidades por modelo.", "09:20")
            );
            default -> List.of();
        };
    }

    public List<ProductItem> filterProducts(List<ProductItem> products, String searchTerm, String selectedCategory) {
        String normalizedSearch = searchTerm == null ? "" : searchTerm.toLowerCase(Locale.ROOT);
        String normalizedCategory = selectedCategory == null ? "all" : selectedCategory;

        return products.stream()
                .filter(product -> normalizedSearch.isBlank()
                        || product.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || product.description().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .filter(product -> "all".equals(normalizedCategory) || product.category().equals(normalizedCategory))
                .toList();
    }

    public List<OrderItem> filterOrders(List<OrderItem> orders, String searchTerm) {
        String normalizedSearch = searchTerm == null ? "" : searchTerm.toLowerCase(Locale.ROOT);
        return orders.stream()
                .filter(order -> normalizedSearch.isBlank()
                        || order.id().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || order.items().stream().anyMatch(line -> line.productName().toLowerCase(Locale.ROOT).contains(normalizedSearch)))
                .sorted(Comparator.comparing(OrderItem::date).reversed())
                .toList();
    }

    public String projectStatusLabel(String status) {
        return switch (status) {
            case "briefing" -> "Em Analise";
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
            case "quote_sent" -> "Orcamento Enviado";
            case "approved" -> "Aprovado";
            case "in_production" -> "Em Producao";
            case "completed" -> "Concluido";
            default -> status;
        };
    }

    public String orderStatusLabel(String status) {
        return switch (status) {
            case "pending" -> "Pendente";
            case "paid" -> "Pago";
            case "in_production" -> "Em Producao";
            case "shipped" -> "Enviado";
            case "delivered" -> "Entregue";
            default -> status;
        };
    }

    public record TechnicalSheet(
            String clay,
            String glaze,
            int firingTemp,
            String dimensions,
            String weight,
            String notes
    ) {
    }

    public record ProductItem(
            String id,
            String name,
            String category,
            String description,
            BigDecimal price,
            int stock,
            String imageUrl,
            TechnicalSheet technicalSheet
    ) {
    }

    public record ProjectQuote(
            BigDecimal designPhase,
            BigDecimal moldPhase,
            BigDecimal productionPhase
    ) {
        public BigDecimal total() {
            return designPhase.add(moldPhase).add(productionPhase);
        }
    }

    public record ProjectItem(
            String id,
            String title,
            String status,
            String description,
            int quantity,
            ProjectQuote quote,
            int messagesCount,
            LocalDate createdAt,
            String currentStage,
            List<String> stages
    ) {
    }

    public record OrderLine(
            String productName,
            int quantity,
            BigDecimal unitPrice,
            String imageUrl
    ) {
        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record OrderItem(
            String id,
            String status,
            String paymentStatus,
            String clientName,
            DeliveryAddress address,
            LocalDate date,
            List<OrderLine> items
    ) {
        public BigDecimal total() {
            return items.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public int totalItems() {
            return items.stream().mapToInt(OrderLine::quantity).sum();
        }
    }

    public record DeliveryAddress(
            String company,
            String street,
            String postalCode,
            String city,
            String country
    ) {
    }

    public record ProjectBriefing(
            String projectType,
            BigDecimal budget,
            int quantity,
            LocalDate deadline,
            String description
    ) {
    }

    public record ProjectStage(
            String name,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record ProjectMeeting(
            String id,
            String projectId,
            LocalDate date,
            String time,
            String notes,
            String status
    ) {
    }

    public record ProjectMessage(
            String id,
            String projectId,
            String senderId,
            String senderName,
            String message,
            String timestamp
    ) {
    }

    public record CartItem(
            String productId,
            String productName,
            BigDecimal price,
            int quantity,
            String imageUrl
    ) {
        public BigDecimal lineTotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record Payment(
            String id,
            String projectId,
            String phase,
            BigDecimal amount,
            String status,
            LocalDate dueDate,
            LocalDate paidDate,
            String method
    ) {
    }

    public List<Payment> projectPayments(String projectId) {
        return switch (projectId) {
            case "PRJ-001" -> List.of(
                    new Payment("PAY-001", projectId, "design", new BigDecimal("180.00"), "paid",
                            null, LocalDate.now().minusDays(15), "card"),
                    new Payment("PAY-002", projectId, "mold", new BigDecimal("220.00"), "pending",
                            LocalDate.now().plusDays(3), null, null),
                    new Payment("PAY-003", projectId, "production", new BigDecimal("380.00"), "pending",
                            LocalDate.now().plusDays(20), null, null)
            );
            case "PRJ-002" -> List.of(
                    new Payment("PAY-004", projectId, "design", new BigDecimal("250.00"), "pending",
                            LocalDate.now().plusDays(7), null, null),
                    new Payment("PAY-005", projectId, "mold", new BigDecimal("300.00"), "pending",
                            LocalDate.now().plusDays(14), null, null),
                    new Payment("PAY-006", projectId, "production", new BigDecimal("700.00"), "pending",
                            LocalDate.now().plusDays(28), null, null)
            );
            default -> List.of();
        };
    }

    private String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        normalized = normalized.replace("_", "-").replace("/", "");
        if (normalized.matches("[A-Z]+\\d+")) {
            int split = 0;
            while (split < normalized.length() && Character.isLetter(normalized.charAt(split))) {
                split++;
            }
            normalized = normalized.substring(0, split) + "-" + normalized.substring(split);
        }
        return normalized;
    }
}