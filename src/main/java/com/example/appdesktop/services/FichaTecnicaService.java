package com.example.appdesktop.services;

import com.example.appdesktop.models.FichaTecnica;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FichaTecnicaService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.fichas-tecnicas-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_FICHAS_TECNICAS_BASE_URL";
    private static final FichaTecnicaService INSTANCE = new FichaTecnicaService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private FichaTecnicaService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static FichaTecnicaService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<FichaTecnica>> findByProjetoId(Integer projetoId) {
        if (projetoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do projeto e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/projeto/" + projetoId))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseList(response.body()));
                    }
                    if (response.statusCode() == 404) {
                        return CompletableFuture.completedFuture(List.of());
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<FichaTecnica> create(Integer projetoId, FichaTecnica ficha) {
        if (projetoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do projeto e obrigatorio."));
        }
        if (ficha == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Ficha tecnica invalida."));
        }

        try {
            String json = objectMapper.writeValueAsString(ficha);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/projeto/" + projetoId))
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
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar ficha tecnica.", ex));
        }
    }

    public CompletableFuture<FichaTecnica> update(Integer fichaId, FichaTecnica ficha) {
        if (fichaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da ficha tecnica e obrigatorio."));
        }
        if (ficha == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Ficha tecnica invalida."));
        }

        try {
            String json = objectMapper.writeValueAsString(ficha);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + fichaId))
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
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao atualizar ficha tecnica.", ex));
        }
    }

    public CompletableFuture<Void> delete(Integer fichaId) {
        if (fichaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da ficha tecnica e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/" + fichaId))
                .header("Accept", "application/json")
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<List<FichaTecnica>> findByArtigoId(Integer artigoId) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/artigo/" + artigoId))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseList(response.body()));
                    }
                    if (response.statusCode() == 404) {
                        return CompletableFuture.completedFuture(List.of());
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<FichaTecnica> createForArtigo(Integer artigoId, FichaTecnica ficha) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }
        if (ficha == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Ficha tecnica invalida."));
        }

        try {
            String json = objectMapper.writeValueAsString(ficha);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/artigo/" + artigoId))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(parseSingle(response.body()));
                        }
                        String body = response.body();
                        String message = (body == null || body.isBlank())
                                ? defaultErrorMessage(response.statusCode())
                                : body;
                        return CompletableFuture.failedFuture(new IllegalStateException(message));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar ficha tecnica.", ex));
        }
    }

    public CompletableFuture<FichaTecnica> updateForArtigo(Integer artigoId, Integer fichaId) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }
        if (fichaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da ficha tecnica e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/artigo/" + artigoId + "/" + fichaId))
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseSingle(response.body()));
                    }
                    String body = response.body();
                    String message = (body == null || body.isBlank())
                            ? defaultErrorMessage(response.statusCode())
                            : body;
                    return CompletableFuture.failedFuture(new IllegalStateException(message));
                });
    }

    public CompletableFuture<Void> deleteForArtigo(Integer artigoId, Integer fichaId) {
        if (artigoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do artigo e obrigatorio."));
        }
        if (fichaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da ficha tecnica e obrigatorio."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/artigo/" + artigoId + "/" + fichaId))
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

    private List<FichaTecnica> parseList(String rawBody) {
        JsonNode root = parseNode(rawBody);
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isObject()) {
            return List.of(mapFicha(root));
        }
        if (!root.isArray()) {
            return List.of();
        }

        List<FichaTecnica> result = new ArrayList<>();
        for (JsonNode node : root) {
            result.add(mapFicha(node));
        }
        return result;
    }

    private FichaTecnica parseSingle(String rawBody) {
        JsonNode node = parseNode(rawBody);
        if (node == null || node.isNull()) {
            return null;
        }
        return mapFicha(node);
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

    private FichaTecnica mapFicha(JsonNode node) {
        FichaTecnica ficha = new FichaTecnica();
        ficha.setId(readInteger(node, "id"));
        ficha.setTipoBarro(readText(node, "tipoBarro", "tipo_barro"));
        ficha.setCorVidrado(readText(node, "corVidrado", "cor_vidrado"));
        ficha.setTemperaturaCozedura(readInteger(node, "temperaturaCozedura", "temperatura_cozedura"));
        ficha.setTempoSecagem(readText(node, "tempoSecagem", "tempo_secagem"));
        ficha.setObservacoes(readText(node, "observacoes"));
        ficha.setFotoDesign(readText(node, "fotoDesign", "foto_design"));
        ficha.setFotoPrototipo(readText(node, "fotoPrototipo", "foto_prototipo"));
        ficha.setRefMolde(readText(node, "refMolde", "ref_molde"));
        return ficha;
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
                    // next
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

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Ficha tecnica nao encontrada.";
        }
        if (statusCode == 400) {
            return "Dados de ficha tecnica invalidos.";
        }
        return "Nao foi possivel processar fichas tecnicas.";
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
        return "http://localhost:8080/api/fichas-tecnicas";
    }
}