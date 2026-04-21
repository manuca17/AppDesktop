package com.example.appdesktop.services;

import com.example.appdesktop.models.Artesa;
import com.example.appdesktop.models.MensagemChat;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MensagemChatService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.mensagens-chat-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_MENSAGENS_CHAT_BASE_URL";
    private static final String LEGACY_BASE_URL_PROPERTY = "appdesktop.api.projetos-base-url";
    private static final String LEGACY_BASE_URL_ENV = "APPDESKTOP_API_PROJETOS_BASE_URL";
    private static final MensagemChatService INSTANCE = new MensagemChatService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private MensagemChatService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static MensagemChatService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<MensagemChat>> findByProjetoId(Integer projetoId) {
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
                        return CompletableFuture.completedFuture(List.<MensagemChat>of());
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                })
                .exceptionallyCompose(ex -> {
                    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException(
                            "Nao foi possivel carregar mensagens do chat.", cause));
                });
    }

    public CompletableFuture<MensagemChat> createAsArtesa(Integer projetoId, Integer artesaId, String conteudo) {
        return createForRole(projetoId, "artesa", artesaId, conteudo, null);
    }

    public CompletableFuture<MensagemChat> createAsUtilizador(Integer projetoId, Integer utilizadorId, String conteudo) {
        return createForRole(projetoId, "utilizador", utilizadorId, conteudo, null);
    }

    private CompletableFuture<MensagemChat> createForRole(Integer projetoId, String role, Integer senderId, String conteudo, String urlFoto) {
        if (senderId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Remetente invalido."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("conteudo", conteudo);
        payload.put("urlFoto", urlFoto);
        return create(baseUrl + "/projeto/" + projetoId + "/" + role + "/" + senderId, payload);
    }

    private CompletableFuture<MensagemChat> create(String url, Map<String, Object> payload) {
        if (url == null || url.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("URL do projeto e obrigatorio."));
        }
        Object rawContent = payload.get("conteudo");
        if (!(rawContent instanceof String) || ((String) rawContent).isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Conteudo da mensagem e obrigatorio."));
        }

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
                            JsonNode body = parseNode(response.body());
                            if (body == null || body.isNull()) {
                                return CompletableFuture.completedFuture(null);
                            }
                            return CompletableFuture.completedFuture(mapMensagem(body));
                        }
                        return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                    });
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido da mensagem.", ex));
        }
    }

    private List<MensagemChat> parseList(String rawBody) {
        JsonNode root = parseNode(rawBody);
        if (root == null || !root.isArray()) {
            return List.of();
        }

        List<MensagemChat> result = new ArrayList<>();
        for (JsonNode node : root) {
            result.add(mapMensagem(node));
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

    private MensagemChat mapMensagem(JsonNode node) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setId(readInteger(node, "id", "idMensagem", "id_mensagem"));
        mensagem.setConteudo(readText(node, "conteudo"));
        mensagem.setUrlFoto(readText(node, "urlFoto", "url_foto"));
        mensagem.setDataEnvio(readInstant(node, "dataEnvio", "data_envio"));

        ProjetoPersonalizado projeto = new ProjetoPersonalizado();
        Integer projetoId = readNestedId(node, "idProjeto", "projetoId", "projeto");
        if (projetoId != null) {
            projeto.setId(projetoId);
            mensagem.setIdProjeto(projeto);
        }

        JsonNode utilizadorNode = node.get("idRemetenteUtilizador");
        if (utilizadorNode != null && !utilizadorNode.isNull()) {
            Utilizador utilizador = new Utilizador();
            if (utilizadorNode.isObject()) {
                utilizador.setId(readInteger(utilizadorNode, "id"));
                utilizador.setNomeEmpresa(readText(utilizadorNode, "nomeEmpresa", "nome"));
                utilizador.setEmail(readText(utilizadorNode, "email"));
            } else if (utilizadorNode.isNumber()) {
                utilizador.setId(utilizadorNode.asInt());
            }
            mensagem.setIdRemetenteUtilizador(utilizador);
        }

        JsonNode artesaNode = node.get("idRemetenteArtesa");
        if (artesaNode != null && !artesaNode.isNull()) {
            Artesa artesa = new Artesa();
            if (artesaNode.isObject()) {
                artesa.setId(readInteger(artesaNode, "id"));
                artesa.setNome(readText(artesaNode, "nome"));
                artesa.setEmail(readText(artesaNode, "email"));
            } else if (artesaNode.isNumber()) {
                artesa.setId(artesaNode.asInt());
            }
            mensagem.setIdRemetenteArtesa(artesa);
        }

        return mensagem;
    }

    private Integer readNestedId(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field == null || field.isNull()) {
                continue;
            }
            if (field.isNumber()) {
                return field.asInt();
            }
            if (field.isObject()) {
                Integer nested = readInteger(field, "id");
                if (nested != null) {
                    return nested;
                }
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
            return "Projeto nao encontrado para mensagens.";
        }
        if (statusCode == 400) {
            return "Dados invalidos para envio da mensagem.";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "Sem permissao para aceder ao chat do projeto.";
        }
        return "Nao foi possivel processar mensagens do projeto.";
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

        String legacyProperty = System.getProperty(LEGACY_BASE_URL_PROPERTY);
        if (legacyProperty != null && !legacyProperty.isBlank()) {
            return legacyProperty.trim();
        }

        String legacyEnv = System.getenv(LEGACY_BASE_URL_ENV);
        if (legacyEnv != null && !legacyEnv.isBlank()) {
            return legacyEnv.trim();
        }

        return "http://localhost:8080/api/mensagens-chat";
    }

    public CompletableFuture<Integer> findTotalByProjetoId(Integer projetoId) {
        if (projetoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do projeto e obrigatorio."));
        }

        String url = baseUrl + "/projeto/" + projetoId + "/total";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        JsonNode node = parseNode(response.body());
                        if (node == null || node.isNull()) {
                            return CompletableFuture.completedFuture(0);
                        }
                        Integer total = readInteger(node, "totalMensagens", "total_mensagens");
                        return CompletableFuture.completedFuture(total == null ? 0 : total);
                    }
                    if (response.statusCode() == 404) {
                        return CompletableFuture.completedFuture(0);
                    }
                    return CompletableFuture.failedFuture(new IllegalStateException(defaultErrorMessage(response.statusCode())));
                });
    }
}