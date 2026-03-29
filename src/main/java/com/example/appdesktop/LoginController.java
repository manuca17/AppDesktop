package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField clientEmailField;

    @FXML
    private PasswordField clientPasswordField;

    @FXML
    private TextField adminEmailField;

    @FXML
    private PasswordField adminPasswordField;

    @FXML
    private void onClientLogin() {
        String email = clientEmailField.getText();

        navigateToClientDashboard(email);
    }

    @FXML
    private void onAdminLogin() {
        String email = adminEmailField.getText();

        navigateToAdminDashboard(email);
    }

    private void navigateToAdminDashboard(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-layout-view.fxml"));
            Parent root = loader.load();

            AdminLayoutController controller = loader.getController();
            controller.setAdminIdentity(resolveDisplayName(email), "Administrador");

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

    private void navigateToClientDashboard(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("client-layout-view.fxml"));
            Parent root = loader.load();

            ClientLayoutController controller = loader.getController();
            controller.setClientIdentity(resolveDisplayName(email), "Conta Cliente");

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
}
