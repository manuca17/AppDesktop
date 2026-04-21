package com.example.appdesktop;

import com.example.appdesktop.models.Orcamento;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.OrcamentoService;
import com.example.appdesktop.services.PagamentoService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
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

    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final OrcamentoService orcamentoService = OrcamentoService.getInstance();
    private final PagamentoService pagamentoService = PagamentoService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("pt", "PT"));

    private ClientPageNavigator navigator;
    private String projectId;
    private String paymentId;
    private Orcamento payment;
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

        if (isProcessing) {
            return;
        }
        if (payment == null || payment.getId() == null) {
            showError("Pagamento", "Orcamento invalido.");
            return;
        }

        isProcessing = true;
        submitButton.setDisable(true);
        submitButton.setText("A processar...");

        String tipoPagamento = resolvePaymentMethodCode(paymentMethodGroup == null ? null : paymentMethodGroup.getSelectedToggle());
        pagamentoService.payOrcamento(payment.getId(), tipoPagamento)
                .whenComplete((done, error) -> javafx.application.Platform.runLater(() -> {
                    isProcessing = false;
                    submitButton.setDisable(false);
                    submitButton.setText("Processar Pagamento");

                    if (error != null) {
                        showError("Erro ao processar pagamento", error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
                        return;
                    }

                    showSuccess("Pagamento processado com sucesso!");
                    if (navigator != null) {
                        navigator.navigateTo("project-detail:" + projectId);
                    }
                }));
    }

    private void refresh() {
        if (projectId == null || projectId.isBlank() || paymentId == null || paymentId.isBlank()) {
            return;
        }

        Integer projectNumericId = extractNumericId(projectId);
        Integer paymentNumericId = extractNumericId(paymentId);
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (projectNumericId == null || paymentNumericId == null || currentUser == null || currentUser.getId() == null) {
            showError("Pagamento", "Projeto ou orcamento invalido.");
            return;
        }

        projetoService.findByUtilizadorId(currentUser.getId())
                .thenCombine(orcamentoService.findById(paymentNumericId), (projects, quote) -> {
                    ProjetoPersonalizado selectedProject = projects.stream()
                            .filter(p -> p != null && projectNumericId.equals(p.getId()))
                            .findFirst()
                            .orElse(null);
                    return new Object[]{selectedProject, quote};
                })
                .whenComplete((result, error) -> javafx.application.Platform.runLater(() -> {
                    if (error != null || result == null) {
                        showError("Pagamento", "Projeto ou orcamento nao encontrado.");
                        return;
                    }

                    ProjetoPersonalizado project = (ProjetoPersonalizado) result[0];
                    payment = (Orcamento) result[1];
                    if (project == null || payment == null) {
                        showError("Pagamento", "Projeto ou orcamento nao encontrado.");
                        if (navigator != null) {
                            navigator.navigateTo("projects");
                        }
                        return;
                    }

                    projectNameLabel.setText(project.getTituloProjeto() == null ? "Projeto personalizado" : project.getTituloProjeto());
                    projectIdLabel.setText("PRJ-" + project.getId());

                    String phase = normalizePhase(payment.getTipo());
                    String phaseName = phaseLabel(phase);
                    String phaseDescription = phaseDescription(phase);
                    phaseInfoLabel.setText(phaseName);
                    phaseInfoDescriptionLabel.setText(phaseDescription);
                    phaseSummaryLabel.setText(phaseName);
                    phaseSummaryDescriptionLabel.setText(phaseDescription);

                    amountLabel.setText(currencyFormat.format(payment.getValorTotalEstimado() == null ? java.math.BigDecimal.ZERO : payment.getValorTotalEstimado()));

                    if (payment.getDataEnvio() != null) {
                        dueDateLabel.setText("Emitido em: " + dateFormatter.format(payment.getDataEnvio().atZone(java.time.ZoneId.systemDefault()).toLocalDate()));
                    } else {
                        dueDateLabel.setText("Emitido em: --");
                    }
                }));
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

    private String resolvePaymentMethodCode(Toggle toggle) {
        String method = resolvePaymentMethod(toggle);
        return switch (method) {
            case "card" -> "cartao";
            case "transfer" -> "transferencia";
            default -> method;
        };
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
            case "mold", "molde" -> "Fase 2: Molde";
            case "production", "producao" -> "Fase 3: Producao";
            default -> phase;
        };
    }

    private String phaseDescription(String phase) {
        return switch (phase) {
            case "design" -> "Desenvolvimento do conceito e aprovação de protótipos";
            case "mold", "molde" -> "Criacao dos moldes necessarios para producao";
            case "production", "producao" -> "Producao das pecas e controlo de qualidade";
            default -> "";
        };
    }

    private Integer extractNumericId(String rawId) {
        if (rawId == null) {
            return null;
        }
        String cleaned = rawId.trim().toUpperCase(Locale.ROOT);
        if (cleaned.matches("PRJ-\\d+")) {
            return Integer.parseInt(cleaned.substring(4));
        }
        if (cleaned.matches("ENC-\\d+")) {
            return Integer.parseInt(cleaned.substring(4));
        }
        if (cleaned.matches("\\d+")) {
            return Integer.parseInt(cleaned);
        }
        return null;
    }

    private String normalizePhase(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "design";
        }
        return switch (tipo.trim().toLowerCase(Locale.ROOT)) {
            case "molde", "mold" -> "molde";
            case "producao", "production" -> "producao";
            default -> "design";
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
