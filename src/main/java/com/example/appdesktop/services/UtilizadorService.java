package com.example.appdesktop.services;

import com.example.appdesktop.models.Utilizador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class UtilizadorService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_BASE_URL";
    private static final UtilizadorService INSTANCE = new UtilizadorService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String loginUrl;

    private UtilizadorService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.loginUrl = resolveBaseUrl() + "/login";
    }

    public static UtilizadorService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<Utilizador> login(String email, String password) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("email", email);
        payload.put("password", password);

        final String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pedido de login."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(loginUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    JsonNode body = parseResponse(response.body());
                    if (response.statusCode() >= 200 && response.statusCode() < 300 && body != null) {
                        Utilizador utilizador = new Utilizador();
                        JsonNode idNode = body.get("id");
                        utilizador.setId(idNode != null && !idNode.isNull() ? idNode.asInt() : null);
                        utilizador.setNomeEmpresa(readText(body, "nomeEmpresa"));
                        utilizador.setEmail(readText(body, "email"));
                        Utilizador.setCurrentUser(utilizador);
                        return CompletableFuture.completedFuture(utilizador);
                    }

                    String message = body != null && readText(body, "message") != null && !readText(body, "message").isBlank()
                            ? readText(body, "message")
                            : defaultErrorMessage(response.statusCode());
                    return CompletableFuture.failedFuture(new IllegalArgumentException(message));
                })
                .exceptionallyCompose(ex -> {
                    if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        return CompletableFuture.failedFuture(ex);
                    }
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    return CompletableFuture.failedFuture(new IllegalStateException("Nao foi possivel contactar a API de login em " + loginUrl + ".", cause));
                });
    }


    public void logout() {
        Utilizador.clearCurrentUser();
    }

    private JsonNode parseResponse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(rawBody);
        } catch (IOException ex) {
            return null;
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return null;
        }
        return field.asText();
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 401) {
            return "Credenciais invalidas.";
        }
        if (statusCode == 400) {
            return "Dados de login invalidos.";
        }
        return "Nao foi possivel autenticar com a API.";
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

        return "http://localhost:8080/api/utilizadores";
    }

}
