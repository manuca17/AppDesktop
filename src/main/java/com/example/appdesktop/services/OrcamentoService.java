package com.example.appdesktop.services;

import com.example.appdesktop.models.Artesa;
import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OrcamentoService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.orcamentos-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_ORCAMENTOS_BASE_URL";
    private static final OrcamentoService INSTANCE = new OrcamentoService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private OrcamentoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static OrcamentoService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<Orcamento>> findAll() {
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
                });
    }

    public CompletableFuture<Orcamento> createForProjeto(Integer projetoId, Orcamento orcamento) {
        if (projetoId == null || orcamento == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Projeto e orcamento sao obrigatorios."));
        }

        try {
            String json = objectMapper.writeValueAsString(orcamento);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/projeto/" + projetoId))
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
                            return CompletableFuture.completedFuture(mapOrcamento(node));
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido de orcamento.", ex));
        }
    }

    public CompletableFuture<List<Orcamento>> findByProjetoId(Integer projetoId) {
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
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }

    public CompletableFuture<Orcamento> findById(Integer orcamentoId) {
        if (orcamentoId == null) {
            return CompletableFuture.completedFuture(null);
        }

        return findAll().thenApply(list -> list.stream()
                .filter(orc -> orc != null && orcamentoId.equals(orc.getId()))
                .findFirst()
                .orElse(null));
    }

    private List<Orcamento> parseList(String rawBody) {
        JsonNode root = parseNode(rawBody);
        if (root == null || !root.isArray()) {
            return List.of();
        }

        List<Orcamento> result = new ArrayList<>();
        for (JsonNode node : root) {
            result.add(mapOrcamento(node));
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

    private Orcamento mapOrcamento(JsonNode node) {
        Orcamento orcamento = new Orcamento();
        orcamento.setId(readInteger(node, "id", "idOrcamento", "id_orcamento"));
        orcamento.setTipo(readText(node, "tipo", "tipoOrcamento", "fase"));
        orcamento.setEstado(readText(node, "estado"));
        orcamento.setObservacoes(readText(node, "observacoes", "observacao", "notas"));
        orcamento.setValorTotalEstimado(readBigDecimal(node, "valorTotalEstimado", "valor_total_estimado", "valor"));
        orcamento.setDataEnvio(readInstant(node, "dataEnvio", "data_envio"));

        JsonNode projetoNode = readNode(node, "idProjeto", "projetoId", "projeto");
        if (projetoNode != null && !projetoNode.isNull()) {
            ProjetoPersonalizado projeto = new ProjetoPersonalizado();
            if (projetoNode.isObject()) {
                projeto.setId(readInteger(projetoNode, "id", "projetoId"));
                projeto.setTituloProjeto(readText(projetoNode, "tituloProjeto", "titulo"));
            } else if (projetoNode.isNumber()) {
                projeto.setId(projetoNode.asInt());
            }
            orcamento.setIdProjeto(projeto);
        }

        JsonNode artesaNode = readNode(node, "idArtesa", "artesaId", "artesa");
        if (artesaNode != null && !artesaNode.isNull()) {
            Artesa artesa = new Artesa();
            if (artesaNode.isObject()) {
                artesa.setId(readInteger(artesaNode, "id", "artesaId"));
                artesa.setNome(readText(artesaNode, "nome"));
                artesa.setEmail(readText(artesaNode, "email"));
            } else if (artesaNode.isNumber()) {
                artesa.setId(artesaNode.asInt());
            }
            orcamento.setIdArtesa(artesa);
        }

        return orcamento;
    }

    private JsonNode readNode(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                return field;
            }
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

    private BigDecimal readBigDecimal(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException ignored) {
                // next
            }
        }
        return null;
    }

    private Instant readInstant(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String raw = readText(node, fieldName);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                return Instant.parse(raw);
            } catch (DateTimeParseException ignored) {
                // next
            }
        }
        return null;
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Projeto/orcamento nao encontrado.";
        }
        if (statusCode == 400) {
            return "Dados de orcamento invalidos.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para aceder a orcamentos.";
        }
        return "Nao foi possivel processar orcamentos.";
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

        return "http://localhost:8080/api/orcamentos";
    }
}