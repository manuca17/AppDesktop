package com.example.appdesktop;

import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class BriefingController implements ClientPage {

    @FXML
    private TextField titleField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextArea descriptionArea;

    private ClientPageNavigator navigator;
    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();

    @FXML
    private void initialize() {
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSubmitBriefing() {
        String titulo = titleField.getText() == null ? "" : titleField.getText().trim();
        String briefingTexto = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();

        if (titulo.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Campo obrigatorio", "Por favor preencha o Nome do Projeto.");
            return;
        }

        Integer quantidade = null;
        String quantidadeRaw = quantityField.getText() == null ? "" : quantityField.getText().trim();
        if (!quantidadeRaw.isBlank()) {
            try {
                quantidade = Integer.parseInt(quantidadeRaw);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Quantidade invalida", "Por favor insira um numero valido no campo Quantidade.");
                return;
            }
        }

        ProjetoPersonalizado projeto = new ProjetoPersonalizado();
        projeto.setTituloProjeto(titulo);
        projeto.setBriefing(briefingTexto);
        projeto.setQuantidade(quantidade);

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser != null) {
            Utilizador utilizadorRef = new Utilizador();
            utilizadorRef.setId(currentUser.getId());
            projeto.setIdUtilizador(utilizadorRef);
        }

        projetoService.create(projeto)
                .thenAccept(saved -> Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Briefing enviado",
                            "Recebemos o seu pedido com sucesso.\nA equipa TaCa Lab vai analisar o briefing e enviar resposta em breve.");
                    clearForm();
                    if (navigator != null) navigator.navigateTo("projects");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Erro ao enviar", ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void onClearForm() {
        clearForm();
    }

    private void clearForm() {
        titleField.clear();
        quantityField.clear();
        descriptionArea.clear();
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.INFORMATION ? "Briefing enviado" : "Aviso");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}