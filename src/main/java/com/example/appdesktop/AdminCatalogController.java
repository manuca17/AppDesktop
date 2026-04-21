package com.example.appdesktop;

import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.models.FichaTecnica;
import com.example.appdesktop.services.ArtigoCatalogoService;
import com.example.appdesktop.services.FichaTecnicaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AdminCatalogController implements AdminPage {

    @FXML private TextField searchField;
    @FXML private TilePane productsPane;
    @FXML private Label emptyLabel;

    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final FichaTecnicaService fichaTecnicaService = FichaTecnicaService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ArtigoCatalogo> allProducts;
    private AdminPageNavigator navigator;

    @FXML
    private void initialize() {
        allProducts = new ArrayList<>();
        loadProducts();
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
    private void onAddProduct() {
        showProductDialog(null);
    }

    private void loadProducts() {
        artigoService.findAll()
                .whenComplete((products, error) -> Platform.runLater(() -> {
                    if (error != null || products == null) {
                        allProducts = List.of();
                    } else {
                        allProducts = products;
                    }
                    refreshProducts();
                }));
    }

    private void refreshProducts() {
        productsPane.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<ArtigoCatalogo> filtered = allProducts.stream()
                .filter(p -> search.isBlank()
                        || (p.getNome() != null && p.getNome().toLowerCase(Locale.ROOT).contains(search))
                        || (p.getId() != null && ("p-" + p.getId()).toLowerCase(Locale.ROOT).contains(search)))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ArtigoCatalogo product : filtered) {
            productsPane.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(ArtigoCatalogo product) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(product.getNome() == null ? "Artigo" : product.getNome());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label stockBadge = buildStockBadge(product.getStock() == null ? 0 : product.getStock());
        header.getChildren().addAll(name, spacer, stockBadge);

        Label description = new Label("Sem descricao.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(currencyFormat.format(product.getPrecoUnitario() == null ? BigDecimal.ZERO : product.getPrecoUnitario()));
        price.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Label stockLabel = new Label("Stock: " + (product.getStock() == null ? 0 : product.getStock()));
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        priceRow.getChildren().addAll(price, spacer, stockLabel);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.Button editBtn = new javafx.scene.control.Button("Editar");
        editBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db; -fx-border-radius: 6;");
        editBtn.setOnAction(e -> showProductDialog(product));

        javafx.scene.control.Button sheetBtn = new javafx.scene.control.Button("Ficha Tec.");
        sheetBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db; -fx-border-radius: 6;");
        sheetBtn.setOnAction(e -> openFichaTecnicaDialog(product));

        javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button("Remover");
        deleteBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-border-color: #fca5a5; -fx-border-radius: 6;");
        deleteBtn.setOnAction(e -> deleteProduct(product));

        actions.getChildren().addAll(editBtn, sheetBtn, deleteBtn);
        card.getChildren().addAll(header, description, priceRow, actions);
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

    private void showProductDialog(ArtigoCatalogo existing) {
        boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Editar Produto" : "Adicionar Produto");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(250);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField nameField = new TextField(isEdit ? nullToEmpty(existing.getNome()) : "");
        TextField priceField = new TextField(isEdit && existing.getPrecoUnitario() != null ? existing.getPrecoUnitario().toPlainString() : "");
        TextField stockField = new TextField(isEdit && existing.getStock() != null ? existing.getStock().toString() : "");
        CheckBox visibleBox = new CheckBox("Visivel no catalogo");
        visibleBox.setSelected(isEdit && Boolean.TRUE.equals(existing.getVisivel()));

        grid.addRow(0, new Label("Nome"), nameField);
        grid.addRow(1, new Label("Preco (EUR)"), priceField);
        grid.addRow(2, new Label("Stock"), stockField);
        grid.addRow(3, new Label(""), visibleBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType(isEdit ? "Guardar" : "Adicionar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String priceRaw = priceField.getText() == null ? "" : priceField.getText().trim().replace(",", ".");
        String stockRaw = stockField.getText() == null ? "" : stockField.getText().trim();

        if (name.isBlank()) {
            showError("Dados invalidos", "Indique o nome do artigo.");
            return;
        }

        BigDecimal price;
        Integer stock;
        try {
            price = new BigDecimal(priceRaw);
            stock = Integer.parseInt(stockRaw);
        } catch (NumberFormatException ex) {
            showError("Dados invalidos", "Verifique o preco e o stock.");
            return;
        }

        ArtigoCatalogo payload = isEdit ? existing : new ArtigoCatalogo();
        payload.setNome(name);
        payload.setPrecoUnitario(price);
        payload.setStock(stock);
        payload.setVisivel(visibleBox.isSelected());

        CompletableFuture<?> request = isEdit
                ? artigoService.update(existing.getId(), payload)
                : artigoService.create(payload);

        request.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) {
                showError("Erro", "Nao foi possivel guardar o artigo.");
                return;
            }
            loadProducts();
        }));
    }

    private void deleteProduct(ArtigoCatalogo product) {
        if (product == null || product.getId() == null) {
            showInfo("Artigo sem ID para ocultar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Ocultar Produto");
        confirm.setHeaderText("Ocultar \"" + (product.getNome() == null ? "Artigo" : product.getNome()) + "\"?");
        confirm.setContentText("O artigo deixara de estar visivel no catalogo.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ArtigoCatalogo payload = new ArtigoCatalogo();
            payload.setNome(product.getNome());
            payload.setPrecoUnitario(product.getPrecoUnitario());
            payload.setStock(product.getStock());
            payload.setVisivel(false);

            artigoService.update(product.getId(), payload)
                    .whenComplete((done, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showError("Erro", "Nao foi possivel ocultar o artigo. " + formatError(error, "Nao foi possivel ocultar o artigo."));
                            return;
                        }
                        loadProducts();
                    }));
        }
    }

    private void openFichaTecnicaDialog(ArtigoCatalogo product) {
        if (product == null || product.getId() == null) {
            showInfo("Artigo sem ID para carregar ficha tecnica.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Fichas Tecnicas");
        dialog.setHeaderText(product.getNome() == null ? "Artigo" : product.getNome());

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        VBox listBox = new VBox(8);
        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(280);
        scrollPane.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        Button addButton = new Button("Adicionar ficha");
        addButton.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white;");
        addButton.setOnAction(e -> openFichaForm(product.getId(), null, listBox));

        root.getChildren().addAll(scrollPane, addButton);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        loadFichas(product.getId(), listBox);
        dialog.showAndWait();
    }

    private void loadFichas(Integer artigoId, VBox listBox) {
        listBox.getChildren().clear();
        Label loading = new Label("A carregar fichas tecnicas...");
        loading.setStyle("-fx-text-fill: #6b7280;");
        listBox.getChildren().add(loading);

        fichaTecnicaService.findByArtigoId(artigoId)
                .whenComplete((fichas, error) -> Platform.runLater(() -> {
                    listBox.getChildren().clear();
                    if (error != null || fichas == null || fichas.isEmpty()) {
                        Label empty = new Label("Sem fichas tecnicas.");
                        empty.setStyle("-fx-text-fill: #6b7280;");
                        listBox.getChildren().add(empty);
                        return;
                    }
                    for (FichaTecnica ficha : fichas) {
                        listBox.getChildren().add(createFichaCard(artigoId, ficha, listBox));
                    }
                }));
    }

    private VBox createFichaCard(Integer artigoId, FichaTecnica ficha, VBox listBox) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        String title = ficha.getRefMolde() == null || ficha.getRefMolde().isBlank()
                ? "Ficha tecnica"
                : "Ficha " + ficha.getRefMolde();
        Label header = new Label(title);
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

        Label meta = new Label("Tipo barro: " + valueOrDash(ficha.getTipoBarro())
                + "  |  Cor vidrado: " + valueOrDash(ficha.getCorVidrado())
                + "  |  Cozedura: " + (ficha.getTemperaturaCozedura() == null ? "--" : ficha.getTemperaturaCozedura() + "°C"));
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox actions = new HBox(8);
        Button edit = new Button("Editar");
        edit.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        edit.setOnAction(e -> openFichaForm(artigoId, ficha, listBox));

        Button remove = new Button("Remover");
        remove.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");
        remove.setOnAction(e -> {
            fichaTecnicaService.deleteForArtigo(artigoId)
                    .whenComplete((done, error) -> Platform.runLater(() -> {
                        if (error != null) {
                            showInfo("Nao foi possivel remover a ficha tecnica. " + formatError(error, "Nao foi possivel remover a ficha tecnica."));
                            return;
                        }
                        loadFichas(artigoId, listBox);
                    }));
        });

        actions.getChildren().addAll(edit, remove);
        card.getChildren().addAll(header, meta, actions);
        return card;
    }

    private void openFichaForm(Integer artigoId, FichaTecnica ficha, VBox listBox) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(ficha == null ? "Adicionar Ficha Tecnica" : "Editar Ficha Tecnica");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField tipoBarroField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getTipoBarro()));
        TextField corVidradoField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getCorVidrado()));
        TextField temperaturaField = new TextField(ficha == null || ficha.getTemperaturaCozedura() == null ? "" : ficha.getTemperaturaCozedura().toString());
        TextField tempoSecagemField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getTempoSecagem()));
        TextField refMoldeField = new TextField(ficha == null ? "" : nullToEmpty(ficha.getRefMolde()));
        TextArea observacoesArea = new TextArea(ficha == null ? "" : nullToEmpty(ficha.getObservacoes()));
        observacoesArea.setPrefRowCount(3);

        grid.addRow(0, new Label("Tipo barro"), tipoBarroField);
        grid.addRow(1, new Label("Cor vidrado"), corVidradoField);
        grid.addRow(2, new Label("Temperatura cozedura"), temperaturaField);
        grid.addRow(3, new Label("Tempo secagem"), tempoSecagemField);
        grid.addRow(4, new Label("Ref. molde"), refMoldeField);
        grid.addRow(5, new Label("Observacoes"), observacoesArea);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        FichaTecnica payload = ficha == null ? new FichaTecnica() : ficha;
        payload.setTipoBarro(nullIfBlank(tipoBarroField.getText()));
        payload.setCorVidrado(nullIfBlank(corVidradoField.getText()));
        payload.setTempoSecagem(nullIfBlank(tempoSecagemField.getText()));
        payload.setRefMolde(nullIfBlank(refMoldeField.getText()));
        payload.setObservacoes(nullIfBlank(observacoesArea.getText()));
        payload.setTemperaturaCozedura(parseInteger(temperaturaField.getText()));

        CompletableFuture<?> request = ficha == null
                ? fichaTecnicaService.createForArtigo(artigoId, payload)
                : fichaTecnicaService.updateForArtigo(artigoId, payload);

        request.whenComplete((saved, error) -> Platform.runLater(() -> {
            if (error != null) {
                showInfo("Nao foi possivel guardar a ficha tecnica. " + formatError(error, "Dados de ficha tecnica invalidos."));
                return;
            }
            showInfo("Ficha tecnica guardada com sucesso.");
            loadFichas(artigoId, listBox);
        }));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullIfBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
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

    private String formatError(Throwable error, String fallback) {
        if (error == null) {
            return fallback == null ? "" : fallback;
        }
        Throwable cause = error.getCause() == null ? error : error.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return message;
    }
}