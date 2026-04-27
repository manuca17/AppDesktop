package com.example.appdesktop;

import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.UtilizadorService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    private final UtilizadorService utilizadorService = UtilizadorService.getInstance();

    @FXML
    private TextField nomeEmpresaField;

    @FXML
    private TextField nifField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField telefoneField;

    @FXML
    private TextArea moradaFaturacaoField;

    @FXML
    private Button registerButton;

    @FXML
    private Button backButton;

    @FXML
    private void onRegister() {
        if (!validateRequiredFields()) {
            showAlert(Alert.AlertType.WARNING, "Dados invalidos", "Preencha nome, NIF, email e password.");
            return;
        }

        Utilizador utilizador = new Utilizador();
        utilizador.setNomeEmpresa(nomeEmpresaField.getText().trim());
        utilizador.setNif(nifField.getText().trim());
        utilizador.setEmail(emailField.getText().trim());
        utilizador.setPassword(passwordField.getText());
        if (telefoneField.getText() != null) {
            utilizador.setTelefone(telefoneField.getText().trim());
        }
        if (moradaFaturacaoField.getText() != null) {
            utilizador.setMoradaFaturacao(moradaFaturacaoField.getText().trim());
        }

        setLoading(true);
        utilizadorService.registerClient(utilizador)
                .whenComplete((saved, error) -> Platform.runLater(() -> {
                    setLoading(false);
                    if (error != null) {
                        String message = error.getCause() != null && error.getCause().getMessage() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        showAlert(Alert.AlertType.WARNING, "Registo falhou", message == null ? "Nao foi possivel criar a conta." : message);
                        return;
                    }

                    showAlert(Alert.AlertType.INFORMATION, "Conta criada", "Registo efetuado com sucesso. Pode iniciar sessao.");
                    navigateToLogin();
                }));
    }

    @FXML
    private void onBackToLogin() {
        navigateToLogin();
    }

    private boolean validateRequiredFields() {
        return nomeEmpresaField != null && nifField != null && emailField != null && passwordField != null
                && !nomeEmpresaField.getText().isBlank()
                && !nifField.getText().isBlank()
                && !emailField.getText().isBlank()
                && !passwordField.getText().isBlank();
    }

    private void navigateToLogin() {
        try {
            Parent root = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml")).load();
            Stage stage = resolveStage();
            stage.setScene(new Scene(root, 900, 620));
            stage.setTitle("Taca Lab - Login");
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Nao foi possivel voltar ao login");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private Stage resolveStage() {
        if (registerButton != null && registerButton.getScene() != null) {
            return (Stage) registerButton.getScene().getWindow();
        }
        if (backButton != null && backButton.getScene() != null) {
            return (Stage) backButton.getScene().getWindow();
        }
        throw new IllegalStateException("Stage not available.");
    }

    private void setLoading(boolean loading) {
        if (registerButton != null) {
            registerButton.setDisable(loading);
        }
        if (backButton != null) {
            backButton.setDisable(loading);
        }
    }

    private void showAlert(Alert.AlertType type, String header, String details) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Aviso");
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }
}
