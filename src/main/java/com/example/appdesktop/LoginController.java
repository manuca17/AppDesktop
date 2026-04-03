package com.example.appdesktop;

import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.UtilizadorService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    private final UtilizadorService utilizadorService = UtilizadorService.getInstance();

    @FXML
    private TextField clientEmailField;

    @FXML
    private PasswordField clientPasswordField;

    @FXML
    private Button clientLoginButton;

    @FXML
    private TextField adminEmailField;

    @FXML
    private PasswordField adminPasswordField;

    @FXML
    private Button adminLoginButton;

    @FXML
    private void onClientLogin() {
        handleLogin(clientEmailField.getText(), clientPasswordField.getText(), false);
    }

    @FXML
    private void onAdminLogin() {
        handleLogin(adminEmailField.getText(), adminPasswordField.getText(), true);
    }

    private void handleLogin(String email, String password, boolean adminAccess) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Dados invalidos", "Preencha email e password.");
            return;
        }

        setLoading(true);
        utilizadorService.login(email, password).whenComplete((utilizador, error) -> javafx.application.Platform.runLater(() -> {
            setLoading(false);

            if (error != null) {
                String message = error.getCause() != null && error.getCause().getMessage() != null
                        ? error.getCause().getMessage()
                        : error.getMessage();
                showAlert(Alert.AlertType.WARNING, "Login falhou", message == null ? "Credenciais invalidas." : message);
                return;
            }

            if (utilizador == null) {
                showAlert(Alert.AlertType.WARNING, "Login falhou", "Credenciais invalidas.");
                return;
            }

            if (adminAccess) {
                navigateToAdminDashboard(utilizador);
            } else {
                navigateToClientDashboard(utilizador);
            }
        }));
    }

    private void navigateToAdminDashboard(Utilizador utilizador) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-layout-view.fxml"));
            Parent root = loader.load();

            AdminLayoutController controller = loader.getController();
            controller.setAdminIdentity(resolveDisplayName(utilizador.getEmail()), defaultCompany(utilizador.getNomeEmpresa(), "Administrador"));

            Stage stage = resolveStage();
            stage.setScene(new Scene(root, 1000, 720));
            stage.setTitle("Taca Lab - Administracao");
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Nao foi possivel abrir o painel de administracao");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void navigateToClientDashboard(Utilizador utilizador) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("client-layout-view.fxml"));
            Parent root = loader.load();

            ClientLayoutController controller = loader.getController();
            controller.setClientIdentity(resolveDisplayName(utilizador.getEmail()), defaultCompany(utilizador.getNomeEmpresa(), "Conta Cliente"));

            Stage stage = resolveStage();
            stage.setScene(new Scene(root, 1000, 720));
            stage.setTitle("Taca Lab - Area do Cliente");
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Nao foi possivel abrir o dashboard");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private String resolveDisplayName(String email) {
        if (email == null || email.isBlank()) {
            return "Cliente";
        }

        String localPart = email.split("@")[0].trim();
        if (localPart.isBlank()) {
            return "Cliente";
        }

        return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
    }

    private Stage resolveStage() {
        if (clientEmailField != null && clientEmailField.getScene() != null) {
            return (Stage) clientEmailField.getScene().getWindow();
        }
        if (adminEmailField != null && adminEmailField.getScene() != null) {
            return (Stage) adminEmailField.getScene().getWindow();
        }
        throw new IllegalStateException("Stage not available.");
    }

    private String defaultCompany(String nomeEmpresa, String fallback) {
        if (nomeEmpresa == null || nomeEmpresa.isBlank()) {
            return fallback;
        }
        return nomeEmpresa;
    }

    private void setLoading(boolean loading) {
        if (clientLoginButton != null) {
            clientLoginButton.setDisable(loading);
        }
        if (adminLoginButton != null) {
            adminLoginButton.setDisable(loading);
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
