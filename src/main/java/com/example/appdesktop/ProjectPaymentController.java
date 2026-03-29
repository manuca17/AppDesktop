package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProjectPaymentController implements ClientPage {

    @FXML
    private Label projectNameLabel;

    @FXML
    private Label projectIdLabel;

    @FXML
    private Label phaseInfoLabel;

    @FXML
    private Label phaseInfoDescriptionLabel;

    @FXML
    private Label phaseSummaryLabel;

    @FXML
    private Label phaseSummaryDescriptionLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label dueDateLabel;

    @FXML
    private RadioButton mbwayRadio;

    @FXML
    private RadioButton cardRadio;

    @FXML
    private RadioButton multibancoRadio;

    @FXML
    private RadioButton transferRadio;

    @FXML
    private VBox mbwayDetails;

    @FXML
    private VBox cardDetails;

    @FXML
    private VBox multibancoDetails;

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
    private TextField nifField;

    @FXML
    private Button submitButton;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("pt", "PT"));

    private ClientPageNavigator navigator;
    private String projectId;
    private String paymentId;
    private ClientPortalDataService.Payment payment;
    private boolean isProcessing;
    private ToggleGroup paymentMethodGroup;

    @FXML
    private void initialize() {
        setupPaymentMethodToggle();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    public void setPaymentIds(String projectId, String paymentId) {
        this.projectId = projectId == null ? null : projectId.trim();
        this.paymentId = paymentId == null ? null : paymentId.trim();
        refresh();
    }

    @FXML
    private void onBack() {
        if (navigator != null) {
            navigator.navigateTo("project-detail:" + projectId);
        }
    }

    @FXML
    private void onConfirmPayment() {
        if (!validateForm()) {
            return;
        }

        isProcessing = true;
        submitButton.setDisable(true);
        submitButton.setText("A processar...");

        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            showSuccess("Pagamento processado com sucesso!");
            isProcessing = false;
            submitButton.setDisable(false);
            submitButton.setText("Processar Pagamento");

            if (navigator != null) {
                navigator.navigateTo("project-detail:" + projectId);
            }
        });
    }

    private void refresh() {
        if (projectId == null || projectId.isBlank() || paymentId == null || paymentId.isBlank()) {
            return;
        }

        ClientPortalDataService.ProjectItem project = dataService.findProjectById(projectId).orElse(null);
        List<ClientPortalDataService.Payment> payments = dataService.projectPayments(projectId);
        payment = payments.stream().filter(p -> p.id().equalsIgnoreCase(paymentId)).findFirst().orElse(null);

        if (project == null || payment == null) {
            showError("Pagamento", "Projeto ou pagamento não encontrado");
            if (navigator != null) {
                navigator.navigateTo("projects");
            }
            return;
        }

        projectNameLabel.setText(project.title());
        projectIdLabel.setText(project.id());
        String phaseName = phaseLabel(payment.phase());
        String phaseDescription = phaseDescription(payment.phase());

        phaseInfoLabel.setText(phaseName);
        phaseInfoDescriptionLabel.setText(phaseDescription);
        phaseSummaryLabel.setText(phaseName);
        phaseSummaryDescriptionLabel.setText(phaseDescription);
        amountLabel.setText(currencyFormat.format(payment.amount()));

        if (payment.dueDate() != null) {
            dueDateLabel.setText("Vencimento: " + dateFormatter.format(payment.dueDate()));
        }
    }

    private void setupPaymentMethodToggle() {
        paymentMethodGroup = new ToggleGroup();
        mbwayRadio.setToggleGroup(paymentMethodGroup);
        cardRadio.setToggleGroup(paymentMethodGroup);
        multibancoRadio.setToggleGroup(paymentMethodGroup);
        transferRadio.setToggleGroup(paymentMethodGroup);

        mbwayRadio.setUserData("mbway");
        cardRadio.setUserData("card");
        multibancoRadio.setUserData("multibanco");
        transferRadio.setUserData("transfer");

        mbwayRadio.setSelected(true);
        showPaymentMethod("mbway");

        paymentMethodGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            showPaymentMethod(resolvePaymentMethod(newToggle));
        });
    }

    private String resolvePaymentMethod(Toggle toggle) {
        if (toggle == null || toggle.getUserData() == null) {
            return "mbway";
        }
        return toggle.getUserData().toString();
    }

    private void showPaymentMethod(String method) {
        mbwayDetails.setVisible(false);
        mbwayDetails.setManaged(false);
        cardDetails.setVisible(false);
        cardDetails.setManaged(false);
        multibancoDetails.setVisible(false);
        multibancoDetails.setManaged(false);
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
            case "multibanco" -> {
                multibancoDetails.setVisible(true);
                multibancoDetails.setManaged(true);
            }
            case "transfer" -> {
                transferDetails.setVisible(true);
                transferDetails.setManaged(true);
            }
        }
    }

    private boolean validateForm() {
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

    private String phaseLabel(String phase) {
        return switch (phase) {
            case "design" -> "Fase 1: Design";
            case "mold" -> "Fase 2: Molde";
            case "production" -> "Fase 3: Produção";
            default -> phase;
        };
    }

    private String phaseDescription(String phase) {
        return switch (phase) {
            case "design" -> "Desenvolvimento do conceito e aprovação de protótipos";
            case "mold" -> "Criação dos moldes necessários para produção";
            case "production" -> "Produção das peças e controlo de qualidade";
            default -> "";
        };
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
