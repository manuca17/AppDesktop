package com.example.appdesktop.services;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ItemEncomenda;
import com.example.appdesktop.models.Utilizador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EncomendaService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.encomenda-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_ENCOMENDA_BASE_URL";
    private static final String REORDER_BASE_URL_PROPERTY = "appdesktop.api.encomenda-projeto-base-url";
    private static final String REORDER_BASE_URL_ENV = "APPDESKTOP_API_ENCOMENDA_PROJETO_BASE_URL";
    private static final EncomendaService INSTANCE = new EncomendaService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String reorderBaseUrl;
    private final ItemEncomendaService itemEncomendaService;

    private EncomendaService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
        this.reorderBaseUrl = resolveReorderBaseUrl();
        this.itemEncomendaService = ItemEncomendaService.getInstance();
    }

    public static EncomendaService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<EncomendaCatalogo> addItemAoCarrinho(Integer utilizadorId, Integer artigoId, int quantidade) {
        if (utilizadorId == null || artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Utilizador e artigo sao obrigatorios."));
        }

        Map<String, Object> payloadSimples = new LinkedHashMap<>();
        payloadSimples.put("utilizadorId", utilizadorId);
        payloadSimples.put("artigoId", artigoId);
        payloadSimples.put("quantidade", Math.max(1, quantidade));

        Map<String, Object> payloadRelacional = new LinkedHashMap<>();
        payloadRelacional.put("idUtilizador", Map.of("id", utilizadorId));
        payloadRelacional.put("idArtigo", Map.of("id", artigoId));
        payloadRelacional.put("quantidade", Math.max(1, quantidade));

        // Tenta primeiro formato simples (mais comum em endpoints DTO) e faz fallback para formato relacional.
        return postForObject(baseUrl + "/carrinho", payloadSimples)
                .handle((result, error) -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return postForObject(baseUrl + "/carrinho", payloadRelacional);
                })
                .thenCompose(future -> future);
    }

    public CompletableFuture<EncomendaCatalogo> getCarrinhoAtual(Integer utilizadorId) {
        if (utilizadorId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do utilizador e obrigatorio."));
        }

        String url = baseUrl + "/carrinho/" + utilizadorId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() == 404) {
                        return CompletableFuture.completedFuture(null);
                    }
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        JsonNode node = parseNode(response.body());
                        if (node == null || node.isNull()) {
                            return CompletableFuture.completedFuture(null);
                        }
                        if (node.isArray()) {
                            if (node.isEmpty()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return CompletableFuture.completedFuture(mapEncomenda(node.get(0)));
                        }
                        return CompletableFuture.completedFuture(mapEncomenda(node));
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<EncomendaCatalogo> checkout(Integer utilizadorId) {
        return checkout(utilizadorId, null);
    }

    public CompletableFuture<EncomendaCatalogo> checkout(Integer utilizadorId, Integer projetoId) {
        if (utilizadorId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do utilizador e obrigatorio."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("utilizadorId", utilizadorId);
        if (projetoId != null) {
            payload.put("projetoId", projetoId);
        }
        return postForObject(baseUrl + "/checkout", payload);
    }

    public CompletableFuture<EncomendaCatalogo> reencomendarProjeto(Integer projetoId, Integer quantidade) {
        if (projetoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do projeto e obrigatorio."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (quantidade != null) {
            payload.put("quantidade", quantidade);
        }

        String url = resolveReorderBaseUrl() + "/projeto/" + projetoId + "/reencomendar";
        return postForObject(url, payload)
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    throw new IllegalStateException(cause.getMessage() == null ? defaultErrorMessage(500) : cause.getMessage());
                });
    }

    public CompletableFuture<Void> clearCarrinho(Integer utilizadorId) {
        if (utilizadorId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do utilizador e obrigatorio."));
        }

        String deleteUrl = baseUrl + "/carrinho/" + utilizadorId;
        HttpRequest deleteRequest = HttpRequest.newBuilder(URI.create(deleteUrl))
                .header("Accept", "application/json")
                .DELETE()
                .build();

        return httpClient.sendAsync(deleteRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(null);
                    }

                    // Fallback para API que expoe limpar por POST.
                    return clearCarrinhoByPost(utilizadorId);
                });
    }

    public CompletableFuture<List<EncomendaCatalogo>> findByUtilizadorId(Integer utilizadorId) {
        if (utilizadorId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do utilizador e obrigatorio."));
        }

        String url = baseUrl + "/utilizador/" + utilizadorId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        List<EncomendaCatalogo> encomendas = parseList(response.body());
                        return enrichOrdersWithItens(encomendas);
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<List<EncomendaCatalogo>> findAll() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        List<EncomendaCatalogo> encomendas = parseList(response.body());
                        return enrichOrdersWithItens(encomendas);
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<EncomendaCatalogo> updateEstado(Integer encomendaId, String estado) {
        if (encomendaId == null || estado == null || estado.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID e estado da encomenda sao obrigatorios."));
        }

        Map<String, Object> payload = Map.of("estado", estado);
        return patchEstado(baseUrl + "/" + encomendaId + "/estado", payload)
                .handle((result, error) -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return putEstado(baseUrl + "/" + encomendaId + "/estado", payload);
                })
                .thenCompose(future -> future);
    }

    private CompletableFuture<EncomendaCatalogo> postForObject(String url, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            JsonNode node = parseNode(response.body());
                            if (node == null || node.isNull()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return CompletableFuture.completedFuture(mapEncomenda(node));
                        }
                        String body = response.body();
                        String message = (body == null || body.isBlank())
                                ? defaultErrorMessage(response.statusCode())
                                : body;
                        return CompletableFuture.failedFuture(new IllegalStateException(message));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido para a API de encomendas.", ex));
        }
    }

    private CompletableFuture<EncomendaCatalogo> patchEstado(String url, Map<String, Object> payload) {
        return sendEstadoUpdate("PATCH", url, payload);
    }

    private CompletableFuture<EncomendaCatalogo> putEstado(String url, Map<String, Object> payload) {
        return sendEstadoUpdate("PUT", url, payload);
    }

    private CompletableFuture<EncomendaCatalogo> sendEstadoUpdate(String method, String url, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            JsonNode node = parseNode(response.body());
                            if (node == null || node.isNull()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return CompletableFuture.completedFuture(mapEncomenda(node));
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido de atualizacao do estado da encomenda.", ex));
        }
    }

    private CompletableFuture<Void> clearCarrinhoByPost(Integer utilizadorId) {
        try {
            String url = baseUrl + "/carrinho/limpar";
            String json = objectMapper.writeValueAsString(Map.of("utilizadorId", utilizadorId));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido de limpar carrinho.", ex));
        }
    }

    private CompletableFuture<List<EncomendaCatalogo>> enrichOrdersWithItens(List<EncomendaCatalogo> encomendas) {
        if (encomendas == null || encomendas.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for (EncomendaCatalogo encomenda : encomendas) {
            if (encomenda == null || encomenda.getId() == null) {
                continue;
            }
            CompletableFuture<Void> task = itemEncomendaService.findByEncomendaId(encomenda.getId())
                    .exceptionally(error -> List.<ItemEncomenda>of())
                    .thenAccept(itens -> encomenda.setLinhas(mapItensToLinhas(itens)));
            tasks.add(task);
        }

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
                .thenApply(ignore -> encomendas);
    }

    private List<EncomendaCatalogo.LinhaEncomenda> mapItensToLinhas(List<ItemEncomenda> itens) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }

        List<EncomendaCatalogo.LinhaEncomenda> linhas = new ArrayList<>();
        for (ItemEncomenda item : itens) {
            if (item == null) {
                continue;
            }
            EncomendaCatalogo.LinhaEncomenda linha = new EncomendaCatalogo.LinhaEncomenda();
            linha.setId(item.getId());
            linha.setQuantidade(item.getQuantidade());
            linha.setNomeProduto(item.resolvedNomeProduto());
            linha.setPrecoUnitario(item.resolvedPrecoUnitario());
            linhas.add(linha);
        }
        return linhas;
    }

    private List<EncomendaCatalogo> parseList(String rawBody) {
        JsonNode root = parseNode(rawBody);
        if (root == null || !root.isArray()) {
            return List.of();
        }

        List<EncomendaCatalogo> result = new ArrayList<>();
        for (JsonNode node : root) {
            result.add(mapEncomenda(node));
        }
        return result;
    }

    private JsonNode parseNode(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (IOException ex) {
            return null;
        }
    }

    private EncomendaCatalogo mapEncomenda(JsonNode node) {
        EncomendaCatalogo enc = new EncomendaCatalogo();
        enc.setId(readInteger(node, "id", "idEncomenda", "id_encomenda"));
        enc.setEstado(readText(node, "estado", "status"));

        String estadoPagamento = readText(node, "estadoPagamento", "estado_pagamento", "paymentStatus");
        enc.setEstadoPagamento(estadoPagamento);
        enc.setDataEncomenda(readLocalDate(node, "dataEncomenda", "data_encomenda", "dataCriacao", "createdAt"));

        JsonNode utilizadorNode = node.get("idUtilizador");
        if (utilizadorNode != null && !utilizadorNode.isNull()) {
            Utilizador u = new Utilizador();
            if (utilizadorNode.isObject()) {
                u.setId(readInteger(utilizadorNode, "id"));
                u.setNomeEmpresa(readText(utilizadorNode, "nomeEmpresa"));
                u.setEmail(readText(utilizadorNode, "email"));
            } else if (utilizadorNode.isNumber()) {
                u.setId(utilizadorNode.asInt());
            }
            enc.setIdUtilizador(u);
        }

        JsonNode projetoNode = node.get("idProjeto");
        if (projetoNode != null && !projetoNode.isNull()) {
            com.example.appdesktop.models.ProjetoPersonalizado projeto = new com.example.appdesktop.models.ProjetoPersonalizado();
            if (projetoNode.isObject()) {
                projeto.setId(readInteger(projetoNode, "id"));
                projeto.setTituloProjeto(readText(projetoNode, "tituloProjeto"));
            } else if (projetoNode.isNumber()) {
                projeto.setId(projetoNode.asInt());
            }
            enc.setIdProjeto(projeto);
        }

        JsonNode linhasNode = node.get("linhas");
        if (linhasNode == null) linhasNode = node.get("itens");
        if (linhasNode != null && linhasNode.isArray()) {
            List<EncomendaCatalogo.LinhaEncomenda> linhas = new ArrayList<>();
            for (JsonNode l : linhasNode) {
                linhas.add(mapLinha(l));
            }
            enc.setLinhas(linhas);
        }

        BigDecimal total = readBigDecimal(node, "total", "valorTotal");
        enc.setTotal(total);

        return enc;
    }

    private EncomendaCatalogo.LinhaEncomenda mapLinha(JsonNode node) {
        EncomendaCatalogo.LinhaEncomenda linha = new EncomendaCatalogo.LinhaEncomenda();
        linha.setId(readInteger(node, "id", "id_item", "idItem"));
        Integer qtd = readInteger(node, "quantidade");
        linha.setQuantidade(qtd == null ? 1 : qtd);

        BigDecimal preco = readBigDecimal(node, "precoUnitario", "preco", "precoUnitarioNoMomento");
        linha.setPrecoUnitario(preco == null ? BigDecimal.ZERO : preco);

        String nomeProduto = readText(node, "nomeProduto", "nome_produto");
        if (nomeProduto == null) {
            JsonNode produtoNode = node.get("idArtigo");
            if (produtoNode == null) produtoNode = node.get("idProduto");
            if (produtoNode == null) produtoNode = node.get("artigo");
            if (produtoNode != null && produtoNode.isObject()) {
                nomeProduto = readText(produtoNode, "nome", "nomeArtigo", "titulo");
            }
        }
        linha.setNomeProduto(nomeProduto != null ? nomeProduto : "Produto");

        return linha;
    }

    private Integer readInteger(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            if (field.isNumber()) {
                return field.asInt();
            }
            if (field.isTextual()) {
                try {
                    return Integer.parseInt(field.asText());
                } catch (NumberFormatException ignored) {
                    // try next
                }
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException ignored) {
                // try next
            }
        }
        return null;
    }

    private String readText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            String value = field.asText();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDate readLocalDate(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            if (field.isArray() && field.size() == 3) {
                return LocalDate.of(field.get(0).asInt(), field.get(1).asInt(), field.get(2).asInt());
            }
            String raw = field.asText();
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) return "Encomendas nao encontradas.";
        if (statusCode == 401 || statusCode == 403) return "Sem permissao para consultar encomendas.";
        return "Nao foi possivel carregar as encomendas.";
    }

    private String resolveBaseUrl() {
        String fromProperty = System.getProperty(BASE_URL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) return fromProperty.trim();
        String fromEnv = System.getenv(BASE_URL_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        return "http://localhost:8080/api/encomendas-catalogo";
    }

    private String resolveReorderBaseUrl() {
        String fromProperty = System.getProperty(REORDER_BASE_URL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) return fromProperty.trim();
        String fromEnv = System.getenv(REORDER_BASE_URL_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        return "http://localhost:8080/api/encomendas";
    }
}
