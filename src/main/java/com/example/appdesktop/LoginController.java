package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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

        navigateToArea("Administrador", email);
    }

    private void navigateToArea(String role, String email) {
        Text title = new Text("Bem-vindo ao portal " + role);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Text subtitle = new Text("Utilizador: " + email);
        subtitle.setStyle("-fx-font-size: 14px; -fx-fill: #4b5563;");

        VBox root = new VBox(12, title, subtitle);
        root.setStyle("-fx-alignment: center; -fx-padding: 40; -fx-background-color: #fff7ed;");

        Stage stage = resolveStage();
        stage.setScene(new Scene(root, 900, 620));
        stage.setTitle("Taca Lab - " + role);
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
