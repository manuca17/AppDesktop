package com.example.appdesktop;

import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ItemEncomenda;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.models.ProjetoPersonalizado;
import com.example.appdesktop.services.ArtigoCatalogoService;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.ItemEncomendaService;
import com.example.appdesktop.services.ProjetoPersonalizadoService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CheckoutController implements ClientPage {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private RadioButton mbwayRadio;
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton multibcoRadio;
    @FXML private RadioButton transferRadio;
    @FXML private VBox mbwayDetails;
    @FXML private VBox cardDetails;
    @FXML private VBox multibcoDetails;
    @FXML private VBox transferDetails;
    @FXML private TextField mbwayPhoneField;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardNameField;
    @FXML private TextField cardExpiryField;
    @FXML private TextField cardCvvField;
    @FXML private TextArea notesArea;
    @FXML private Label subtotalLabel;
    @FXML private Label shippingLabel;
    @FXML private Label totalLabel;
    @FXML private Button confirmButton;

    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final ItemEncomendaService itemEncomendaService = ItemEncomendaService.getInstance();
    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final ProjetoPersonalizadoService projetoService = ProjetoPersonalizadoService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ItemEncomenda> cartItems = new ArrayList<>();
    private EncomendaCatalogo carrinhoAtual;
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        setupPaymentMethodToggle();
        loadCart();
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

        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showError("Sessao", "Tem de iniciar sessao para concluir a encomenda.");
            return;
        }

        if (carrinhoAtual == null || carrinhoAtual.getId() == null || cartItems.isEmpty()) {
            showError("Carrinho", "Nao existe carrinho com itens para checkout.");
            return;
        }

        confirmButton.setDisable(true);
        confirmButton.setText("A processar...");

        ensureUnlimitedStockForProjectItems(currentUser.getId())
                .exceptionally(error -> null)
                .thenCompose(ignore -> encomendaService.checkout(currentUser.getId()))
                .whenComplete((encomenda, error) -> Platform.runLater(() -> {
                    confirmButton.setDisable(false);
                    confirmButton.setText("Confirmar Encomenda");

                    if (error != null) {
                        showError("Checkout", "Nao foi possivel confirmar a encomenda. " + formatError(error));
                        return;
                    }

                    if (!isPaid(encomenda)) {
                        showError("Pagamento", "Encomenda criada, mas pagamento nao confirmado.");
                        if (navigator != null) {
                            navigator.navigateTo("orders");
                        }
                        return;
                    }

                    showSuccess("Encomenda realizada com sucesso!");
                    cartItems.clear();
                    updateOrderSummary();
                    if (navigator != null) {
                        navigator.navigateTo("orders");
                    }
                }));
    }

    private CompletableFuture<Void> ensureUnlimitedStockForProjectItems(Integer utilizadorId) {
        if (utilizadorId == null || cartItems.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return projetoService.findByUtilizadorId(utilizadorId)
                .exceptionally(error -> List.of())
                .thenCompose(projects -> {
                    Set<String> projectNames = new HashSet<>();
                    for (ProjetoPersonalizado project : projects) {
                        if (project == null) {
                            continue;
                        }
                        if (!isCompletedProject(project.getEstadoAtual())) {
                            continue;
                        }
                        projectNames.add(resolveProjectArticleName(project).toLowerCase(Locale.ROOT));
                    }

                    if (projectNames.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    return artigoService.findAll()
                            .exceptionally(error -> List.of())
                            .thenCompose(artigos -> updateUnlimitedStockForCartItems(projectNames, artigos));
                });
    }

    private CompletableFuture<Void> updateUnlimitedStockForCartItems(Set<String> projectNames, List<ArtigoCatalogo> artigos) {
        if (projectNames == null || projectNames.isEmpty() || cartItems.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Map<Integer, ArtigoCatalogo> artigosById = new HashMap<>();
        if (artigos != null) {
            for (ArtigoCatalogo artigo : artigos) {
                if (artigo != null && artigo.getId() != null) {
                    artigosById.put(artigo.getId(), artigo);
                }
            }
        }

        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (ItemEncomenda item : cartItems) {
            if (item == null || item.getIdArtigo() == null || item.getIdArtigo().getId() == null) {
                continue;
            }
            String name = item.resolvedNomeProduto();
            if (name == null || !projectNames.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }

            ArtigoCatalogo artigo = artigosById.get(item.getIdArtigo().getId());
            if (artigo == null || artigo.getId() == null || artigo.getStock() == null) {
                continue;
            }

            ArtigoCatalogo payload = new ArtigoCatalogo();
            payload.setNome(artigo.getNome());
            payload.setPrecoUnitario(artigo.getPrecoUnitario());
            payload.setStock(null);
            payload.setVisivel(artigo.getVisivel());
            tasks.add(artigoService.update(artigo.getId(), payload));
        }

        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
    }

    private boolean isCompletedProject(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "concluido", "concluído", "completed", "completo" -> true;
            default -> false;
        };
    }

    private String resolveProjectArticleName(ProjetoPersonalizado project) {
        if (project == null) {
            return "Projeto personalizado";
        }
        if (project.getTituloProjeto() != null && !project.getTituloProjeto().isBlank()) {
            return project.getTituloProjeto();
        }
        return project.getId() == null ? "Projeto personalizado" : "Projeto " + project.getId();
    }

    private void loadCart() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            cartItems = List.of();
            updateOrderSummary();
            return;
        }

        encomendaService.getCarrinhoAtual(currentUser.getId())
                .thenCompose(carrinho -> {
                    carrinhoAtual = carrinho;
                    if (carrinho == null || carrinho.getId() == null) {
                        return java.util.concurrent.CompletableFuture.completedFuture(List.<ItemEncomenda>of());
                    }
                    return itemEncomendaService.findByEncomendaId(carrinho.getId())
                            .exceptionally(error -> List.<ItemEncomenda>of());
                })
                .whenComplete((items, error) -> Platform.runLater(() -> {
                    if (error != null || items == null) {
                        cartItems = List.of();
                    } else {
                        cartItems = new ArrayList<>(items);
                    }
                    updateOrderSummary();
                }));
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
                .map(item -> item.resolvedPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade() == null ? 0 : item.getQuantidade())))
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
            showError("Campos obrigatorios", "Por favor preencha todos os campos obrigatorios");
            return false;
        }

        if (mbwayRadio.isSelected() && mbwayPhoneField.getText().isBlank()) {
            showError("MBWay", "Por favor introduza o numero de telemovel");
            return false;
        }

        if (cardRadio.isSelected() && (cardNumberField.getText().isBlank() || cardNameField.getText().isBlank() ||
                cardExpiryField.getText().isBlank() || cardCvvField.getText().isBlank())) {
            showError("Cartao", "Por favor preencha todos os dados do cartao");
            return false;
        }

        return true;
    }

    private boolean isPaid(EncomendaCatalogo encomenda) {
        if (encomenda == null) {
            return false;
        }
        String paymentStatus = encomenda.getEstadoPagamento();
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            String normalized = paymentStatus.trim().toLowerCase(Locale.ROOT);
            if ("pago".equals(normalized) || "paid".equals(normalized)) {
                return true;
            }
        }
        String estado = encomenda.getEstado();
        if (estado != null && !estado.isBlank()) {
            String normalized = estado.trim().toLowerCase(Locale.ROOT);
            return "pago".equals(normalized) || "paid".equals(normalized);
        }
        return false;
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

    private String formatError(Throwable error) {
        if (error == null) {
            return "";
        }
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "";
        }
        return message;
    }
}