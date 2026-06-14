package com.example.appdesktop.services;

import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.models.ArtigoFoto;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ArtigoCatalogoService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.artigos-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_ARTIGOS_BASE_URL";
    private static final ArtigoCatalogoService INSTANCE = new ArtigoCatalogoService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private ArtigoCatalogoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static ArtigoCatalogoService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<ArtigoCatalogo>> findAll() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseList(response.body()));
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                })
                .exceptionallyCompose(ex -> {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Nao foi possivel contactar a API de artigos em " + baseUrl + ".", cause));
                });
    }

    public CompletableFuture<ArtigoCatalogo> create(ArtigoCatalogo artigo) {
        if (artigo == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Artigo invalido."));
        }
        try {
            String json = objectMapper.writeValueAsString(artigo);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(parseSingle(response.body()));
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar artigo.", ex));
        }
    }

    public CompletableFuture<ArtigoCatalogo> update(Integer artigoId, ArtigoCatalogo artigo) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }
        if (artigo == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Artigo invalido."));
        }
        try {
            String json = objectMapper.writeValueAsString(artigo);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + artigoId))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(parseSingle(response.body()));
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao atualizar artigo.", ex));
        }
    }

    public CompletableFuture<Void> delete(Integer artigoId) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + artigoId))
                .header("Accept", "application/json")
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(null);
                    }
                    String body = response.body();
                    String message = (body == null || body.isBlank())
                            ? defaultErrorMessage(response.statusCode())
                            : body;
                    return CompletableFuture.failedFuture(new IllegalStateException(message));
                });
    }

    private List<ArtigoCatalogo> parseList(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!root.isArray()) {
                return List.of();
            }

            List<ArtigoCatalogo> result = new ArrayList<>();
            for (JsonNode node : root) {
                result.add(mapArtigo(node));
            }
            return result;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private ArtigoCatalogo mapArtigo(JsonNode node) {
        ArtigoCatalogo artigo = new ArtigoCatalogo();
        artigo.setId(readInteger(node, "id", "id_artigo"));
        artigo.setNome(readText(node, "nome", "nomeArtigo", "titulo"));
        artigo.setPrecoUnitario(readBigDecimal(node, "precoUnitario", "preco_unitario", "preco"));
        artigo.setStock(readInteger(node, "stock"));
        artigo.setFotoUrl(readText(node, "fotoUrl", "foto_url", "imagem"));

        JsonNode visivelNode = node.get("visivel");
        if (visivelNode != null && !visivelNode.isNull()) {
            artigo.setVisivel(visivelNode.asBoolean());
        }

        JsonNode fotosNode = node.get("fotos");
        if (fotosNode != null && fotosNode.isArray()) {
            List<ArtigoFoto> fotos = new ArrayList<>();
            for (JsonNode f : fotosNode) {
                ArtigoFoto foto = new ArtigoFoto();
                foto.setId(readInteger(f, "id"));
                foto.setUrl(readText(f, "url"));
                foto.setOrdem(readInteger(f, "ordem"));
                fotos.add(foto);
            }
            artigo.setFotos(fotos);
        }
        return artigo;
    }

    public CompletableFuture<Void> addFoto(Integer artigoId, String url) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of("url", url == null ? "" : url));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + artigoId + "/fotos"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException("Erro ao adicionar foto: " + response.statusCode()));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public CompletableFuture<Void> deleteFoto(Integer artigoId, Integer fotoId) {
        if (artigoId == null || fotoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("IDs obrigatorios."));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + artigoId + "/fotos/" + fotoId))
                .DELETE()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException("Erro ao remover foto: " + response.statusCode()));
                });
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
                // try next
            }
        }
        return null;
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Endpoint de artigos nao encontrado.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para consultar artigos.";
        }
        return "Nao foi possivel carregar o catalogo.";
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

        return "http://localhost:8080/api/artigos-catalogo";
    }

    private ArtigoCatalogo parseSingle(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            if (node == null || node.isNull()) {
                return null;
            }
            return mapArtigo(node);
        } catch (IOException ex) {
            return null;
        }
    }
}