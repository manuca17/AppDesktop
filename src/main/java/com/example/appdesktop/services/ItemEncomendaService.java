package com.example.appdesktop.services;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ItemEncomenda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemEncomendaService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.itens-encomenda-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_ITENS_ENCOMENDA_BASE_URL";
    private static final ItemEncomendaService INSTANCE = new ItemEncomendaService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private ItemEncomendaService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static ItemEncomendaService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<ItemEncomenda>> findAll() {
        return get(baseUrl);
    }

    public CompletableFuture<List<ItemEncomenda>> findByEncomendaId(Integer encomendaId) {
        if (encomendaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da encomenda e obrigatorio."));
        }
        return get(baseUrl + "/encomenda/" + encomendaId);
    }

    private CompletableFuture<List<ItemEncomenda>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseList(response.body()));
                    }
                    if (response.statusCode() == 404) {
                        return CompletableFuture.completedFuture(List.<ItemEncomenda>of());
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                })
                .exceptionallyCompose(ex -> {
                    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Nao foi possivel contactar a API de itens encomenda em " + url + ".", cause));
                });
    }

    private List<ItemEncomenda> parseList(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!root.isArray()) {
                return List.of();
            }

            List<ItemEncomenda> result = new ArrayList<>();
            for (JsonNode node : root) {
                result.add(mapItem(node));
            }
            return result;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private ItemEncomenda mapItem(JsonNode node) {
        ItemEncomenda item = new ItemEncomenda();
        item.setId(readInteger(node, "id", "id_item", "idItem"));
        Integer quantidade = readInteger(node, "quantidade");
        item.setQuantidade(quantidade != null ? quantidade : 1);

        ItemEncomenda.ArtigoRef artigo = readArtigo(node);
        item.setIdArtigo(artigo);

        item.setNomeProduto(readText(node, "nomeProduto", "nome_produto"));
        item.setPrecoUnitario(readBigDecimal(node, "precoUnitario", "preco", "precoUnitarioNoMomento"));

        EncomendaCatalogo encomendaRef = new EncomendaCatalogo();
        Integer idEnc = readNestedId(node, "idEncomenda");
        if (idEnc == null) {
            idEnc = readInteger(node, "idEncomenda", "id_encomenda");
        }
        encomendaRef.setId(idEnc);
        item.setIdEncomenda(encomendaRef);

        return item;
    }

    private ItemEncomenda.ArtigoRef readArtigo(JsonNode node) {
        JsonNode artigoNode = node.get("idArtigo");
        if (artigoNode == null || artigoNode.isNull()) {
            artigoNode = node.get("artigo");
        }

        ItemEncomenda.ArtigoRef artigo = new ItemEncomenda.ArtigoRef();
        if (artigoNode != null && artigoNode.isObject()) {
            artigo.setId(readInteger(artigoNode, "id", "idArtigo", "id_artigo"));
            artigo.setNome(readText(artigoNode, "nome"));
            artigo.setNomeArtigo(readText(artigoNode, "nomeArtigo", "nome_artigo"));
            artigo.setTitulo(readText(artigoNode, "titulo"));
            artigo.setPrecoUnitario(readBigDecimal(artigoNode, "precoUnitario"));
            artigo.setPreco(readBigDecimal(artigoNode, "preco"));
            artigo.setPrecoVenda(readBigDecimal(artigoNode, "precoVenda"));
            return artigo;
        }

        // Mantem objeto vazio para fallbacks de nome/preco resolvidos.
        return artigo;
    }

    private Integer readNestedId(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        if (field.isNumber()) {
            return field.asInt();
        }
        if (field.isObject()) {
            return readInteger(field, "id", "idEncomenda", "id_encomenda");
        }
        return null;
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
                    // next field
                }
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

    private BigDecimal readBigDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException ignored) {
                // next field
            }
        }
        return null;
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Itens de encomenda nao encontrados.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para consultar itens de encomenda.";
        }
        return "Nao foi possivel carregar itens de encomenda.";
    }

    private String resolveBaseUrl() {
        String fromProperty = System.getProperty(BASE_URL_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty.trim();
        }

        String fromEnv = System.getenv(BASE_URL_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        return "http://localhost:8080/api/itens-encomenda";
    }
}
