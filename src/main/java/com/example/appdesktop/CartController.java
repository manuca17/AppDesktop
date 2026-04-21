package com.example.appdesktop;

import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ItemEncomenda;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.ItemEncomendaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartController implements ClientPage {

    @FXML private VBox cartItemsContainer;
    @FXML private Label subtotalLabel;
    @FXML private Label shippingLabel;
    @FXML private Label totalLabel;
    @FXML private Label emptyStateLabel;
    @FXML private HBox cartContent;

    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final ItemEncomendaService itemEncomendaService = ItemEncomendaService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ItemEncomenda> cartItems = new ArrayList<>();
    private EncomendaCatalogo carrinhoAtual;
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        loadCart();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onClearCart() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showInfo("Carrinho", "Tem de iniciar sessao para limpar o carrinho.");
            return;
        }

        encomendaService.clearCarrinho(currentUser.getId())
                .whenComplete((ok, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showInfo("Carrinho", "Nao foi possivel limpar o carrinho com os endpoints atuais.");
                        return;
                    }
                    carrinhoAtual = null;
                    cartItems = List.of();
                    refreshUI();
                    showInfo("Carrinho", "Carrinho limpo com sucesso.");
                }));
    }

    @FXML
    private void onContinueShopping() {
        if (navigator != null) {
            navigator.navigateTo("catalog");
        }
    }

    @FXML
    private void onCheckout() {
        if (cartItems.isEmpty()) {
            return;
        }
        if (navigator != null) {
            navigator.navigateTo("checkout");
        }
    }

    private void loadCart() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            cartItems = List.of();
            refreshUI();
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
                    refreshUI();
                }));
    }

    private void refreshUI() {
        if (cartItems.isEmpty()) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            cartContent.setVisible(false);
            cartContent.setManaged(false);
            return;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);
        cartContent.setVisible(true);
        cartContent.setManaged(true);

        cartItemsContainer.getChildren().clear();
        for (ItemEncomenda item : cartItems) {
            cartItemsContainer.getChildren().add(createCartItemCard(item));
        }

        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.resolvedPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade() == null ? 0 : item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = subtotal.compareTo(new BigDecimal("100")) > 0 ? BigDecimal.ZERO : new BigDecimal("5.90");
        BigDecimal total = subtotal.add(shipping);

        subtotalLabel.setText(currencyFormat.format(subtotal));
        shippingLabel.setText(shipping.compareTo(BigDecimal.ZERO) == 0 ? "Gratis" : currencyFormat.format(shipping));
        totalLabel.setText(currencyFormat.format(total));
    }

    private VBox createCartItemCard(ItemEncomenda item) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox top = new HBox(10);
        Label name = new Label(item.resolvedNomeProduto());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label price = new Label(currencyFormat.format(item.resolvedPrecoUnitario()));
        price.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");

        top.getChildren().addAll(name, spacer, price);

        HBox controls = new HBox(8);
        int qtyValue = item.getQuantidade() == null ? 0 : item.getQuantidade();
        Label qty = new Label("Qtd: " + qtyValue);
        qty.setStyle("-fx-text-fill: #6b7280;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        BigDecimal lineTotal = item.resolvedPrecoUnitario().multiply(BigDecimal.valueOf(qtyValue));
        Label total = new Label(currencyFormat.format(lineTotal));
        total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        controls.getChildren().addAll(qty, spacer2, total);

        card.getChildren().addAll(top, controls);
        return card;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}