package com.example.appdesktop.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public class UploadService {

    private static final String UPLOAD_URL = "http://localhost:8080/api/upload/imagem";
    private static final UploadService INSTANCE = new UploadService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private UploadService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public static UploadService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<String> uploadImage(File file) {
        if (file == null || !file.exists()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Ficheiro invalido."));
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            String boundary = "JavaFXUpload" + System.currentTimeMillis();
            byte[] header = ("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"ficheiro\"; filename=\"" + file.getName() + "\"\r\n"
                    + "Content-Type: " + mimeType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8);
            byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

            byte[] body = new byte[header.length + fileBytes.length + footer.length];
            System.arraycopy(header, 0, body, 0, header.length);
            System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
            System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

            HttpRequest request = HttpRequest.newBuilder(URI.create(UPLOAD_URL))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            try {
                                JsonNode json = objectMapper.readTree(response.body());
                                JsonNode urlNode = json.get("url");
                                String url = urlNode != null && !urlNode.isNull() ? urlNode.asText() : null;
                                if (url == null || url.isBlank()) {
                                    return CompletableFuture.failedFuture(
                                            new IllegalStateException("Resposta do upload sem URL."));
                                }
                                return CompletableFuture.completedFuture(url);
                            } catch (IOException ex) {
                                return CompletableFuture.failedFuture(ex);
                            }
                        }
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Erro no upload: HTTP " + response.statusCode()));
                    })
                    .exceptionallyCompose(ex -> {
                        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("Nao foi possivel fazer upload da imagem.", cause));
                    });

        } catch (IOException ex) {
            return CompletableFuture.failedFuture(new IllegalStateException("Erro ao ler ficheiro.", ex));
        }
    }
}
