package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class BriefingController implements ClientPage {

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private TextField quantityField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextArea referencesArea;

    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        categoryCombo.getItems().setAll("Restauracao", "Hotelaria", "Decoracao", "Retalho", "Outro");
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSubmitBriefing() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Briefing enviado");
        alert.setHeaderText("Recebemos o seu pedido com sucesso");
        alert.setContentText("A equipa TaCa Lab vai analisar o briefing e enviar resposta em breve.");
        alert.showAndWait();

        if (navigator != null) {
            navigator.navigateTo("projects");
        }
    }

    @FXML
    private void onClearForm() {
        titleField.clear();
        categoryCombo.setValue(null);
        quantityField.clear();
        descriptionArea.clear();
        referencesArea.clear();
    }
}
