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
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    private final UtilizadorService utilizadorService = UtilizadorService.getInstance();

    @FXML private TextField adminEmailField;
    @FXML private PasswordField adminPasswordField;
    @FXML private Button adminLoginButton;

    @FXML
    private void onAdminLogin() {
        String email = adminEmailField.getText();
        String password = adminPasswordField.getText();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Dados invalidos", "Preencha email e password.");
            return;
        }

        adminLoginButton.setDisable(true);
        utilizadorService.loginAdmin(email, password)
                .whenComplete((utilizador, error) -> Platform.runLater(() -> {
                    adminLoginButton.setDisable(false);

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

                    navigateToAdminDashboard(utilizador);
                }));
    }

    private void navigateToAdminDashboard(Utilizador utilizador) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-layout-view.fxml"));
            Parent root = loader.load();

            AdminLayoutController controller = loader.getController();
            controller.setAdminIdentity(resolveDisplayName(utilizador), "Administrador");

            Stage stage = (Stage) adminEmailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 720));
            stage.setTitle("Taca Lab - Administracao");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Erro", "Nao foi possivel abrir o painel: " + ex.getMessage());
        }
    }

    private String resolveDisplayName(Utilizador utilizador) {
        if (utilizador == null) return "Administrador";

        String nome = utilizador.getNomeEmpresa();
        if (nome != null && !nome.isBlank()) {
            String raw = nome.trim();
            return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        }

        String email = utilizador.getEmail();
        if (email != null && !email.isBlank()) {
            String localPart = email.split("@")[0].trim();
            if (!localPart.isBlank()) {
                return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
            }
        }

        return "Administrador";
    }

    private void showAlert(Alert.AlertType type, String header, String details) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Erro" : "Aviso");
        alert.setHeaderText(header);
        alert.setContentText(details);
        alert.showAndWait();
    }
}
