package com.example.appdesktop.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PagamentoService {

    private static final String BASE_URL_PROPERTY = "appdesktop.api.pagamentos-base-url";
    private static final String BASE_URL_ENV = "APPDESKTOP_API_PAGAMENTOS_BASE_URL";
    private static final PagamentoService INSTANCE = new PagamentoService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    private PagamentoService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl();
    }

    public static PagamentoService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<Void> payOrcamento(Integer orcamentoId, String tipoPagamento) {
        if (orcamentoId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID do orcamento e obrigatorio."));
        }

        String endpoint = baseUrl + "/orcamento/" + orcamentoId + "/pagar";
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (tipoPagamento != null && !tipoPagamento.isBlank()) {
                payload.put("tipoPagamento", tipoPagamento);
            }

            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
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
        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Falha ao preparar pagamento.", ex));
        }
    }

    private String defaultErrorMessage(int statusCode) {
        if (statusCode == 404) {
            return "Orcamento nao encontrado.";
        }
        if (statusCode == 409) {
            return "Orcamento ja pago.";
        }
        if (statusCode == 400) {
            return "Dados de pagamento invalidos.";
        }
        return "Nao foi possivel processar o pagamento.";
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
        return "http://localhost:8080/api/pagamentos";
    }
}
