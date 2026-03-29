package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutController implements ClientPage {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField cityField;

    @FXML
    private TextField postalCodeField;

    @FXML
    private RadioButton mbwayRadio;

    @FXML
    private RadioButton cardRadio;

    @FXML
    private RadioButton multibcoRadio;

    @FXML
    private RadioButton transferRadio;

    @FXML
    private VBox mbwayDetails;

    @FXML
    private VBox cardDetails;

    @FXML
    private VBox multibcoDetails;

    @FXML
    private VBox transferDetails;

    @FXML
    private TextField mbwayPhoneField;

    @FXML
    private TextField cardNumberField;

    @FXML
    private TextField cardNameField;

    @FXML
    private TextField cardExpiryField;

    @FXML
    private TextField cardCvvField;

    @FXML
    private TextArea notesArea;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label shippingLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Button confirmButton;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ClientPortalDataService.CartItem> cartItems = new ArrayList<>();
    private ClientPageNavigator navigator;
    private boolean isProcessing;

    @FXML
    private void initialize() {
        loadCart();
        setupPaymentMethodToggle();
        updateOrderSummary();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onConfirmOrder() {
        if (!validateForm()) {
            return;
        }

        isProcessing = true;
        confirmButton.setDisable(true);
        confirmButton.setText("A processar...");

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            cartItems.clear();
            showSuccess("Encomenda realizada com sucesso!");
            isProcessing = false;
            confirmButton.setDisable(false);
            confirmButton.setText("Confirmar Encomenda");

            if (navigator != null) {
                navigator.navigateTo("orders");
            }
        });
    }

    private void loadCart() {
        cartItems.add(new ClientPortalDataService.CartItem("P-100", "Caneca Ria", new BigDecimal("19.90"), 2, "https://picsum.photos/seed/caneca1/120/120"));
        cartItems.add(new ClientPortalDataService.CartItem("P-104", "Travessa Sol", new BigDecimal("29.90"), 1, "https://picsum.photos/seed/travessa1/120/120"));
    }

    private void setupPaymentMethodToggle() {
        ToggleGroup group = new ToggleGroup();
        mbwayRadio.setToggleGroup(group);
        cardRadio.setToggleGroup(group);
        multibcoRadio.setToggleGroup(group);
        transferRadio.setToggleGroup(group);
        mbwayRadio.setSelected(true);
        showPaymentMethod("mbway");

        mbwayRadio.selectedProperty().addListener((obs, old, val) -> {
            if (val) showPaymentMethod("mbway");
        });
        cardRadio.selectedProperty().addListener((obs, old, val) -> {
            if (val) showPaymentMethod("card");
        });
        multibcoRadio.selectedProperty().addListener((obs, old, val) -> {
            if (val) showPaymentMethod("multibco");
        });
        transferRadio.selectedProperty().addListener((obs, old, val) -> {
            if (val) showPaymentMethod("transfer");
        });
    }

    private void showPaymentMethod(String method) {
        mbwayDetails.setVisible(false);
        mbwayDetails.setManaged(false);
        cardDetails.setVisible(false);
        cardDetails.setManaged(false);
        multibcoDetails.setVisible(false);
        multibcoDetails.setManaged(false);
        transferDetails.setVisible(false);
        transferDetails.setManaged(false);

        switch (method) {
            case "mbway" -> {
                mbwayDetails.setVisible(true);
                mbwayDetails.setManaged(true);
            }
            case "card" -> {
                cardDetails.setVisible(true);
                cardDetails.setManaged(true);
            }
            case "multibco" -> {
                multibcoDetails.setVisible(true);
                multibcoDetails.setManaged(true);
            }
            case "transfer" -> {
                transferDetails.setVisible(true);
                transferDetails.setManaged(true);
            }
        }
    }

    private void updateOrderSummary() {
        BigDecimal subtotal = cartItems.stream()
                .map(ClientPortalDataService.CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = subtotal.compareTo(new BigDecimal("100")) > 0 ? BigDecimal.ZERO : new BigDecimal("5.90");
        BigDecimal total = subtotal.add(shipping);

        subtotalLabel.setText(currencyFormat.format(subtotal));
        shippingLabel.setText(shipping.compareTo(BigDecimal.ZERO) == 0 ? "Gratis" : currencyFormat.format(shipping));
        totalLabel.setText(currencyFormat.format(total));
    }

    private boolean validateForm() {
        if (fullNameField.getText().isBlank() || emailField.getText().isBlank() ||
            phoneField.getText().isBlank() || addressField.getText().isBlank() ||
            cityField.getText().isBlank() || postalCodeField.getText().isBlank()) {
            showError("Campos obrigatórios", "Por favor preencha todos os campos obrigatórios");
            return false;
        }

        if (mbwayRadio.isSelected() && mbwayPhoneField.getText().isBlank()) {
            showError("MBWay", "Por favor introduza o número de telemóvel");
            return false;
        }

        if (cardRadio.isSelected() && (cardNumberField.getText().isBlank() || cardNameField.getText().isBlank() ||
            cardExpiryField.getText().isBlank() || cardCvvField.getText().isBlank())) {
            showError("Cartão", "Por favor preencha todos os dados do cartão");
            return false;
        }

        return true;
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sucesso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
