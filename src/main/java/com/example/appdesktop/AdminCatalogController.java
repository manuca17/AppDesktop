package com.example.appdesktop;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class AdminCatalogController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TilePane productsPane;
    @FXML private Label emptyLabel;

    private final ClientPortalDataService dataService = new ClientPortalDataService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ClientPortalDataService.ProductItem> allProducts;
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        allProducts = new ArrayList<>(dataService.mockProducts());

        Set<String> categories = new LinkedHashSet<>();
        categories.add("all");
        for (ClientPortalDataService.ProductItem p : allProducts) {
            categories.add(p.category());
        }
        categoryCombo.getItems().setAll(categories);
        categoryCombo.setValue("all");
        refreshProducts();
    }

    @Override
    public void setNavigator(AdminPageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void onSearchChanged() {
        refreshProducts();
    }

    @FXML
    private void onCategoryChanged() {
        refreshProducts();
    }

    @FXML
    private void onAddProduct() {
        showProductDialog(null);
    }

    private void refreshProducts() {
        productsPane.getChildren().clear();

        List<ClientPortalDataService.ProductItem> filtered = dataService.filterProducts(
                allProducts, searchField.getText(), categoryCombo.getValue());

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ClientPortalDataService.ProductItem product : filtered) {
            productsPane.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(ClientPortalDataService.ProductItem product) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(product.name());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stockBadge = buildStockBadge(product.stock());
        header.getChildren().addAll(name, spacer, stockBadge);

        Label category = new Label(product.category());
        category.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label description = new Label(product.description());
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(currencyFormat.format(product.price()));
        price.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label stockLabel = new Label("Stock: " + product.stock());
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        priceRow.getChildren().addAll(price, spacer, stockLabel);

        // Action buttons row
        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.Button editBtn = new javafx.scene.control.Button("Editar");
        editBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db; -fx-border-radius: 6;");
        editBtn.setOnAction(e -> showProductDialog(product));

        javafx.scene.control.Button sheetBtn = new javafx.scene.control.Button("Ficha Tec.");
        sheetBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db; -fx-border-radius: 6;");
        sheetBtn.setOnAction(e -> showTechnicalSheet(product));

        javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button("Remover");
        deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-border-color: #fca5a5; -fx-border-radius: 6;");
        deleteBtn.setOnAction(e -> deleteProduct(product));

        actions.getChildren().addAll(editBtn, sheetBtn, deleteBtn);
        card.getChildren().addAll(header, category, description, priceRow, actions);
        return card;
    }

    private Label buildStockBadge(int stock) {
        Label badge;
        if (stock == 0) {
            badge = new Label("Esgotado");
            badge.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-padding: 3 8; -fx-background-radius: 999;");
        } else if (stock < 5) {
            badge = new Label("Stock baixo");
            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 3 8; -fx-background-radius: 999;");
        } else {
            badge = new Label("Em stock");
            badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 3 8; -fx-background-radius: 999;");
        }
        return badge;
    }

    private void showProductDialog(ClientPortalDataService.ProductItem existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Editar Produto" : "Adicionar Produto");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(100);
        ColumnConstraints col2 = new ColumnConstraints(250);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField nameField     = new TextField(isEdit ? existing.name() : "");
        TextField categoryField = new TextField(isEdit ? existing.category() : "");
        TextField descField     = new TextField(isEdit ? existing.description() : "");
        TextField priceField    = new TextField(isEdit ? existing.price().toPlainString() : "");
        TextField stockField    = new TextField(isEdit ? String.valueOf(existing.stock()) : "");

        grid.addRow(0, new Label("Nome:"),      nameField);
        grid.addRow(1, new Label("Categoria:"), categoryField);
        grid.addRow(2, new Label("Descricao:"), descField);
        grid.addRow(3, new Label("Preco (EUR):"), priceField);
        grid.addRow(4, new Label("Stock:"),     stockField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType(isEdit ? "Guardar" : "Adicionar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            try {
                String id = isEdit ? existing.id() : "P-" + System.currentTimeMillis();
                BigDecimal price = new BigDecimal(priceField.getText().replace(",", ".").trim());
                int stock = Integer.parseInt(stockField.getText().trim());
                ClientPortalDataService.TechnicalSheet sheet = isEdit ? existing.technicalSheet() : null;

                ClientPortalDataService.ProductItem updated = new ClientPortalDataService.ProductItem(
                        id, nameField.getText().trim(), categoryField.getText().trim(),
                        descField.getText().trim(), price, stock, "", sheet);

                if (isEdit) {
                    allProducts.replaceAll(p -> p.id().equals(id) ? updated : p);
                } else {
                    allProducts.add(updated);
                }

                refreshProducts();
                showInfo(isEdit ? "Produto atualizado com sucesso." : "Produto adicionado com sucesso.");
            } catch (NumberFormatException ex) {
                showError("Dados invalidos", "Verifique o preco e o stock.");
            }
        }
    }

    private void showTechnicalSheet(ClientPortalDataService.ProductItem product) {
        ClientPortalDataService.TechnicalSheet sheet = product.technicalSheet();
        if (sheet == null) {
            showInfo("Este produto nao tem ficha tecnica registada.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ficha Tecnica - " + product.name());
        alert.setHeaderText(product.name());
        alert.setContentText(
                "Argila: "       + sheet.clay()        + "\n" +
                "Vidrado: "      + sheet.glaze()       + "\n" +
                "Temperatura: "  + sheet.firingTemp()  + " \u00b0C\n" +
                "Dimensoes: "    + sheet.dimensions()  + "\n" +
                "Peso: "         + sheet.weight()      + "\n" +
                "Notas: "        + sheet.notes());
        alert.showAndWait();
    }

    private void deleteProduct(ClientPortalDataService.ProductItem product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remover Produto");
        confirm.setHeaderText("Remover \"" + product.name() + "\"?");
        confirm.setContentText("Esta acao nao pode ser desfeita.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            allProducts.removeIf(p -> p.id().equals(product.id()));
            refreshProducts();
            showInfo("Produto removido com sucesso.");
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
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
