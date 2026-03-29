package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
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

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label shippingLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private HBox cartContent;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ClientPortalDataService.CartItem> cartItems = new ArrayList<>();
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
        cartItems.clear();
        refreshUI();
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
        cartItems = new ArrayList<>();
        cartItems.add(new ClientPortalDataService.CartItem("P-100", "Caneca Ria", new BigDecimal("19.90"), 2, "https://picsum.photos/seed/caneca1/120/120"));
        cartItems.add(new ClientPortalDataService.CartItem("P-104", "Travessa Sol", new BigDecimal("29.90"), 1, "https://picsum.photos/seed/travessa1/120/120"));
        refreshUI();
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
        for (ClientPortalDataService.CartItem item : cartItems) {
            cartItemsContainer.getChildren().add(createCartItemCard(item));
        }

        BigDecimal subtotal = cartItems.stream()
                .map(ClientPortalDataService.CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = subtotal.compareTo(new BigDecimal("100")) > 0 ? BigDecimal.ZERO : new BigDecimal("5.90");
        BigDecimal total = subtotal.add(shipping);

        subtotalLabel.setText(currencyFormat.format(subtotal));
        shippingLabel.setText(shipping.compareTo(BigDecimal.ZERO) == 0 ? "Gratis" : currencyFormat.format(shipping));
        totalLabel.setText(currencyFormat.format(total));
    }

    private VBox createCartItemCard(ClientPortalDataService.CartItem item) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox top = new HBox(10);
        Label name = new Label(item.productName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label price = new Label(currencyFormat.format(item.price()));
        price.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");

        top.getChildren().addAll(name, spacer, price);

        HBox controls = new HBox(8);
        Label qty = new Label("Qtd: " + item.quantity());
        qty.setStyle("-fx-text-fill: #6b7280;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label total = new Label(currencyFormat.format(item.lineTotal()));
        total.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        controls.getChildren().addAll(qty, spacer2, total);

        card.getChildren().addAll(top, controls);
        return card;
    }
}
