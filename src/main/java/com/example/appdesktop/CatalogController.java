package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CatalogController implements ClientPage {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private Button cartButton;

    @FXML
    private TilePane productsPane;

    @FXML
    private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ClientPortalDataService.ProductItem> allProducts;
    private int cartCount;

    @FXML
    private void initialize() {
        allProducts = dataService.mockProducts();
        Set<String> categories = new LinkedHashSet<>();
        categories.add("all");
        for (ClientPortalDataService.ProductItem product : allProducts) {
            categories.add(product.category());
        }

        categoryCombo.getItems().setAll(categories);
        categoryCombo.setValue("all");
        refreshProducts();
        updateCartButton();
    }

    @FXML
    private void onSearchChanged() {
        refreshProducts();
    }

    @FXML
    private void onCategoryChanged() {
        refreshProducts();
    }

    @Override
    public void setNavigator(ClientPageNavigator navigator) {
        // This page does not require cross-page action buttons for now.
    }

    private void refreshProducts() {
        productsPane.getChildren().clear();

        List<ClientPortalDataService.ProductItem> filtered = dataService.filterProducts(
                allProducts,
                searchField.getText(),
                categoryCombo.getValue()
        );

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ClientPortalDataService.ProductItem product : filtered) {
            productsPane.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(ClientPortalDataService.ProductItem product) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label(product.name());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label category = new Label(product.category());
        category.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label description = new Label(product.description());
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        Label price = new Label(currencyFormat.format(product.price()));
        price.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label stock = new Label("Stock: " + product.stock() + " unidades");
        stock.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("Detalhes");
        detailsButton.setOnAction(event -> showDetails(product));
        detailsButton.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");

        Button addButton = new Button(product.stock() == 0 ? "Esgotado" : "Adicionar ao Carrinho");
        addButton.setDisable(product.stock() == 0);
        addButton.setOnAction(event -> addToCart(product));
        addButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox.setHgrow(addButton, Priority.ALWAYS);
        addButton.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(detailsButton, addButton);
        card.getChildren().addAll(title, category, description, price, stock, actions);
        return card;
    }

    private void showDetails(ClientPortalDataService.ProductItem product) {
        ClientPortalDataService.TechnicalSheet sheet = product.technicalSheet();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(product.name());
        alert.setHeaderText(product.category());
        alert.setContentText(product.description()
                + "\n\nPreco: " + currencyFormat.format(product.price())
                + "\nStock: " + product.stock() + " unidades"
                + "\n\nFicha Tecnica"
                + "\nArgila: " + sheet.clay()
                + "\nVidrado: " + sheet.glaze()
                + "\nTemperatura: " + sheet.firingTemp() + " C"
                + "\nDimensoes: " + sheet.dimensions()
                + "\nPeso: " + sheet.weight()
                + "\nNotas: " + sheet.notes());
        alert.showAndWait();
    }

    private void addToCart(ClientPortalDataService.ProductItem product) {
        cartCount += 1;
        updateCartButton();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Carrinho");
        alert.setHeaderText("Produto adicionado");
        alert.setContentText(product.name() + " foi adicionado ao carrinho.");
        alert.showAndWait();
    }

    private void updateCartButton() {
        cartButton.setText("Carrinho (" + cartCount + ")");
        boolean visible = cartCount > 0;
        cartButton.setVisible(visible);
        cartButton.setManaged(visible);
    }
}
