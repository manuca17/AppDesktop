package com.example.appdesktop.services;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProjetoPersonalizadoService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.projetos-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_PROJETOS_BASE_URL";
    private static final ProjetoPersonalizadoService INSTANCE = new ProjetoPersonalizadoService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private ProjetoPersonalizadoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static ProjetoPersonalizadoService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<ProjetoPersonalizado>> findAll() {
        return get(baseUrl);
    }

    public CompletableFuture<ProjetoPersonalizado> create(ProjetoPersonalizado projeto) {
        try {
            String json = objectMapper.writeValueAsString(projeto);

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                JsonNode node = objectMapper.readTree(response.body());
                                return CompletableFuture.completedFuture(mapProjeto(node));
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(e);
                            }
                        }
                        return CompletableFuture.failedFuture(
                                new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    })
                    .exceptionallyCompose(ex -> {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        return CompletableFuture.failedFuture(new IllegalStateException(
                                "Nao foi possivel criar o projeto personalizado.", cause));
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<ProjetoPersonalizado>> findByUtilizadorId(Integer utilizadorId) {
        if (utilizadorId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do utilizador e obrigatorio."));
        }
        return get(baseUrl + "/utilizador/" + utilizadorId);
    }

    public CompletableFuture<ProjetoPersonalizado> updateEstado(Integer projetoId, String estado) {
        if (projetoId == null || estado == null || estado.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID e estado do projeto sao obrigatorios."));
        }

        Map<String, Object> payload = Map.of("estado", estado);
        String url = baseUrl + "/" + projetoId + "/estado";

        return sendEstadoUpdate("PATCH", url, payload)
                .handle((result, error) -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(result);
                    }
                    return sendEstadoUpdate("PUT", url, payload);
                })
                .thenCompose(future -> future);
    }

    private CompletableFuture<List<ProjetoPersonalizado>> get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
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
                    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Nao foi possivel contactar a API de projetos personalizados em " + url + ".", cause));
                });
    }

    private CompletableFuture<ProjetoPersonalizado> sendEstadoUpdate(String method, String url, Map<String, Object> payload) {
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
                            if (response.body() == null || response.body().isBlank()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            try {
                                JsonNode node = objectMapper.readTree(response.body());
                                return CompletableFuture.completedFuture(mapProjeto(node));
                            } catch (IOException e) {
                                return CompletableFuture.failedFuture(e);
                            }
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private List<ProjetoPersonalizado> parseList(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!root.isArray()) {
                return List.of();
            }

            List<ProjetoPersonalizado> result = new ArrayList<>();
            for (JsonNode node : root) {
                result.add(mapProjeto(node));
            }
            return result;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private ProjetoPersonalizado mapProjeto(JsonNode node) {
        ProjetoPersonalizado projeto = new ProjetoPersonalizado();
        projeto.setId(readInteger(node, "id"));
        projeto.setTituloProjeto(readText(node, "tituloProjeto"));
        projeto.setBriefing(readText(node, "briefing"));
        projeto.setEstadoAtual(readText(node, "estadoAtual"));
        projeto.setDataCriacao(readInstant(node, "dataCriacao"));
        projeto.setIdArtesa(readNestedId(node, "idArtesa"));
        projeto.setQuantidade(readInteger(node, "quantidade"));

        JsonNode utilizadorNode = node.get("idUtilizador");
        if (utilizadorNode != null && !utilizadorNode.isNull()) {
            Utilizador utilizador = new Utilizador();
            if (utilizadorNode.isObject()) {
                utilizador.setId(readInteger(utilizadorNode, "id"));
                utilizador.setNomeEmpresa(readText(utilizadorNode, "nomeEmpresa"));
                utilizador.setEmail(readText(utilizadorNode, "email"));
            } else if (utilizadorNode.isNumber()) {
                utilizador.setId(utilizadorNode.asInt());
            }
            projeto.setIdUtilizador(utilizador);
        }

        return projeto;
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
            return readInteger(field, "id");
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isNumber()) {
            return null;
        }
        return field.asInt();
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }

    private Instant readInstant(JsonNode node, String fieldName) {
        String raw = readText(node, fieldName);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Endpoint de projetos personalizados nao encontrado.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para consultar projetos personalizados.";
        }
        return "Nao foi possivel carregar projetos personalizados.";
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

        return "http://localhost:8080/api/projetos-personalizados";
    }
}