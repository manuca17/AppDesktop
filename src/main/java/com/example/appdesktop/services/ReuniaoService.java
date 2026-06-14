package com.example.appdesktop.services;

import com.example.appdesktop.models.Reuniao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReuniaoService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.reuniao-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_REUNIAO_BASE_URL";
    private static final ReuniaoService INSTANCE = new ReuniaoService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private ReuniaoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static ReuniaoService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<Reuniao>> findAll() {
        String url = baseUrl;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseList(response.body()));
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<List<Reuniao>> findByProjetoId(Integer projetoId) {
        if (projetoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do projeto e obrigatorio."));
        }

        String url = baseUrl + "/projeto/" + projetoId;
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
                        return CompletableFuture.completedFuture(List.<Reuniao>of());
                    }
                    return CompletableFuture.failedFuture(
                            new IllegalStateException(defaultErrorMessage(response.statusCode())));
                })
                .exceptionallyCompose(ex -> {
                    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Nao foi possivel contactar a API de reunioes.", cause));
                });
    }

    public CompletableFuture<Reuniao> createForProjetoArtesa(Integer projetoId,
                                                             Integer artesaId,
                                                             LocalDate data,
                                                             String hora,
                                                             String tipo,
                                                             String local) {
        if (projetoId == null || artesaId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Projeto e artesa sao obrigatorios."));
        }
        if (data == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Data da reuniao e obrigatoria."));
        }

        LocalTime parsedTime;
        try {
            parsedTime = hora == null || hora.isBlank()
                    ? LocalTime.of(10, 0)
                    : LocalTime.parse(hora.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Hora invalida. Use HH:mm."));
        }

        String dataHora = OffsetDateTime.of(data.atTime(parsedTime), ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String endpoint = baseUrl + "/projeto/" + projetoId;
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("data_hora", dataHora);
            payload.put("tipo", (tipo == null || tipo.isBlank()) ? null : tipo.trim());
            payload.put("local", (local == null || local.isBlank()) ? null : local.trim());

            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            List<Reuniao> created = parseList("[" + response.body() + "]");
                            return CompletableFuture.completedFuture(created.isEmpty() ? null : created.get(0));
                        }
                        String body = response.body();
                        String message = (body == null || body.isBlank())
                                ? defaultErrorMessage(response.statusCode())
                                : body;
                        return CompletableFuture.failedFuture(new IllegalStateException(message));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido da reuniao.", ex));
        }
    }

    public CompletableFuture<Reuniao> confirmPresence(Integer reuniaoId) {
        if (reuniaoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da reuniao e obrigatorio."));
        }

        String url = baseUrl + "/" + reuniaoId + "/confirmar-presenca";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseSingle(response.body()));
                    }
                    String message = response.body() == null || response.body().isBlank()
                            ? defaultErrorMessage(response.statusCode())
                            : response.body();
                    return CompletableFuture.failedFuture(new IllegalStateException(message));
                });
    }

    public CompletableFuture<Reuniao> reschedule(Integer reuniaoId,
                                                 LocalDate data,
                                                 String hora,
                                                 String tipo,
                                                 String local) {
        if (reuniaoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da reuniao e obrigatorio."));
        }
        if (data == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Data da reuniao e obrigatoria."));
        }

        LocalTime parsedTime;
        try {
            parsedTime = hora == null || hora.isBlank()
                    ? LocalTime.of(10, 0)
                    : LocalTime.parse(hora.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Hora invalida. Use HH:mm."));
        }

        String dataHora = OffsetDateTime.of(data.atTime(parsedTime), ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String endpoint = baseUrl + "/" + reuniaoId + "/remarcar";
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("dataHora", dataHora);
            if (tipo != null && !tipo.isBlank()) {
                payload.put("tipo", tipo.trim());
            }
            if (local != null && !local.isBlank()) {
                payload.put("local", local.trim());
            }

            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            return CompletableFuture.completedFuture(parseSingle(response.body()));
                        }
                        String message = response.body() == null || response.body().isBlank()
                                ? defaultErrorMessage(response.statusCode())
                                : response.body();
                        return CompletableFuture.failedFuture(new IllegalStateException(message));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar remarcacao.", ex));
        }
    }

    public CompletableFuture<Reuniao> cancel(Integer reuniaoId) {
        if (reuniaoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da reuniao e obrigatorio."));
        }

        String endpoint = baseUrl + "/" + reuniaoId + "/cancelar";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return CompletableFuture.completedFuture(parseSingle(response.body()));
                    }
                    String message = response.body() == null || response.body().isBlank()
                            ? defaultErrorMessage(response.statusCode())
                            : response.body();
                    return CompletableFuture.failedFuture(new IllegalStateException(message));
                });
    }

    private List<Reuniao> parseList(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!root.isArray()) {
                return List.of();
            }
            List<Reuniao> result = new ArrayList<>();
            for (JsonNode node : root) {
                result.add(mapReuniao(node));
            }
            return result;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private Reuniao mapReuniao(JsonNode node) {
        Reuniao reuniao = new Reuniao();
        reuniao.setId(readInteger(node, "id"));
        reuniao.setIdProjeto(readProjeto(node, "idProjeto"));
        reuniao.setData(readLocalDate(node, "data"));
        reuniao.setHora(readText(node, "hora"));
        reuniao.setNotas(readText(node, "notas"));
        reuniao.setEstado(readText(node, "estado", "status"));
        reuniao.setTipo(readText(node, "tipo"));
        reuniao.setLocal(readText(node, "local"));

        if (reuniao.getData() == null && (reuniao.getHora() == null || reuniao.getHora().isBlank())) {
            applyDataHoraFallback(reuniao, readText(node, "dataHora", "data_hora"));
        }

        return reuniao;
    }

    private void applyDataHoraFallback(Reuniao reuniao, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(raw);
            reuniao.setData(parsed.toLocalDate());
            reuniao.setHora(parsed.toLocalTime().withSecond(0).withNano(0).toString());
        } catch (Exception ignored) {
            // keep original values
        }
    }

    private Reuniao parseSingle(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            if (node == null || node.isNull()) {
                return null;
            }
            return mapReuniao(node);
        } catch (IOException ex) {
            return null;
        }
    }

    private com.example.appdesktop.models.ProjetoPersonalizado readProjeto(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }

        com.example.appdesktop.models.ProjetoPersonalizado projeto = new com.example.appdesktop.models.ProjetoPersonalizado();
        if (field.isNumber()) {
            projeto.setId(field.asInt());
            return projeto;
        }
        if (field.isObject()) {
            Integer id = readInteger(field, "id");
            projeto.setId(id);
            projeto.setTituloProjeto(readText(field, "tituloProjeto"));
            return projeto;
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

    private LocalDate readLocalDate(JsonNode node, String fieldName) {
        String raw = readText(node, fieldName);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Reuniao nao encontrada.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para consultar reunioes.";
        }
        return "Nao foi possivel carregar as reunioes.";
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
        return "http://localhost:8080/api/reunioes";
    }
}
