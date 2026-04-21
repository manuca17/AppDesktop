package com.example.appdesktop;

import com.example.appdesktop.models.ArtigoCatalogo;
import com.example.appdesktop.models.EncomendaCatalogo;
import com.example.appdesktop.models.ItemEncomenda;
import com.example.appdesktop.models.Utilizador;
import com.example.appdesktop.services.ArtigoCatalogoService;
import com.example.appdesktop.services.EncomendaService;
import com.example.appdesktop.services.ItemEncomendaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CatalogController implements ClientPage {

    private static final int CART_SYNC_MAX_RETRIES = 3;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Button cartButton;
    @FXML private TilePane productsPane;
    @FXML private Label emptyLabel;

    private final ArtigoCatalogoService artigoService = ArtigoCatalogoService.getInstance();
    private final EncomendaService encomendaService = EncomendaService.getInstance();
    private final ItemEncomendaService itemEncomendaService = ItemEncomendaService.getInstance();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "PT"));

    private List<ArtigoCatalogo> allProducts = new ArrayList<>();
    private final Map<Integer, Integer> cartQtyByArtigo = new HashMap<>();
    private int cartCount;
    private ClientPageNavigator navigator;

    @FXML
    private void initialize() {
        categoryCombo.getItems().setAll("Todos");
        categoryCombo.setValue("Todos");
        categoryCombo.setDisable(true);
        loadProducts();
        refreshCartCount();
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
        this.navigator = navigator;
    }

    private void loadProducts() {
        artigoService.findAll().whenComplete((products, error) -> Platform.runLater(() -> {
            if (error != null || products == null) {
                allProducts = List.of();
            } else {
                allProducts = products.stream()
                        .filter(p -> p != null && (p.getVisivel() == null || p.getVisivel()))
                        .toList();
            }
            refreshProducts();
        }));
    }

    private void refreshProducts() {
        productsPane.getChildren().clear();

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT);
        List<ArtigoCatalogo> filtered = allProducts.stream()
                .filter(p -> search.isBlank() || (p.getNome() != null && p.getNome().toLowerCase(Locale.ROOT).contains(search)))
                .toList();

        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());

        for (ArtigoCatalogo product : filtered) {
            productsPane.getChildren().add(createProductCard(product));
        }
    }

    private VBox createProductCard(ArtigoCatalogo product) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label title = new Label(product.getNome() == null ? "Artigo" : product.getNome());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label category = new Label("Catalogo");
        category.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label description = new Label("Artigo disponivel para encomenda.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4b5563;");

        BigDecimal preco = product.getPrecoUnitario() == null ? BigDecimal.ZERO : product.getPrecoUnitario();
        Label price = new Label(currencyFormat.format(preco));
        price.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        int stock = product.getStock() == null ? 0 : product.getStock();
        int inCart = resolveInCartQuantity(product.getId());
        int availableToAdd = Math.max(0, stock - inCart);

        Label stockLabel = new Label("Stock: " + stock + " unidades"
                + (inCart > 0 ? " (" + inCart + " no carrinho)" : ""));
        stockLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("Detalhes");
        detailsButton.setOnAction(event -> showDetails(product));
        detailsButton.setStyle("-fx-background-color: transparent; -fx-border-color: #d1d5db;");

        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(90);

        if (availableToAdd > 0) {
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, availableToAdd, 1));
        } else {
            qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 0, 0));
            qtySpinner.setDisable(true);
        }

        Button addButton = new Button(availableToAdd == 0 ? "Esgotado" : "Adicionar ao Carrinho");
        addButton.setDisable(availableToAdd == 0);
        addButton.setOnAction(event -> addToCart(product, qtySpinner.getValue(), availableToAdd));
        addButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox.setHgrow(addButton, Priority.ALWAYS);
        addButton.setMaxWidth(Double.MAX_VALUE);

        actions.getChildren().addAll(detailsButton, qtySpinner, addButton);
        card.getChildren().addAll(title, category, description, price, stockLabel, actions);
        return card;
    }

    private void showDetails(ArtigoCatalogo product) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(product.getNome() == null ? "Artigo" : product.getNome());
        alert.setHeaderText("Detalhes do artigo");
        alert.setContentText("Preco: " + currencyFormat.format(product.getPrecoUnitario() == null ? BigDecimal.ZERO : product.getPrecoUnitario())
                + "\nStock: " + (product.getStock() == null ? 0 : product.getStock()) + " unidades");
        alert.showAndWait();
    }

    private void addToCart(ArtigoCatalogo product, Integer quantidade, int maxPermitido) {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Sessao", "Tem de iniciar sessao para adicionar ao carrinho.");
            return;
        }

        int qty = quantidade == null ? 1 : quantidade;
        if (qty <= 0) {
            showAlert(Alert.AlertType.WARNING, "Quantidade", "Escolha uma quantidade valida.");
            return;
        }
        if (qty > maxPermitido) {
            showAlert(Alert.AlertType.WARNING, "Stock", "Quantidade superior ao stock disponivel.");
            return;
        }

        encomendaService.addItemAoCarrinho(currentUser.getId(), product.getId(), qty)
                .whenComplete((carrinho, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showAlert(Alert.AlertType.ERROR, "Carrinho", "Nao foi possivel adicionar o artigo ao carrinho.");
                        return;
                    }
                    if (product.getId() != null) {
                        cartQtyByArtigo.merge(product.getId(), qty, Integer::sum);
                    }
                    cartCount += qty;
                    updateCartButton();
                    showAlert(Alert.AlertType.INFORMATION, "Carrinho",
                            (product.getNome() == null ? "Artigo" : product.getNome()) + " foi adicionado ao carrinho.");
                    // Depois do add, sincronizamos via API do carrinho (o POST pode devolver body vazio).
                    syncCartCountWithRetry(new AtomicInteger(0));
                }));
    }

    private void refreshCartCount() {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            cartCount = 0;
            updateCartButton();
            return;
        }

        encomendaService.getCarrinhoAtual(currentUser.getId())
                .whenComplete((carrinho, error) -> Platform.runLater(() -> refreshCartCount(error == null ? carrinho : null)));
    }

    private void syncCartCountWithRetry(AtomicInteger attempts) {
        Utilizador currentUser = Utilizador.getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return;
        }

        encomendaService.getCarrinhoAtual(currentUser.getId())
                .whenComplete((carrinho, error) -> Platform.runLater(() -> {
                    if (error == null && carrinho != null && carrinho.getId() != null) {
                        refreshCartCount(carrinho);
                        return;
                    }

                    int currentAttempt = attempts.incrementAndGet();
                    if (currentAttempt >= CART_SYNC_MAX_RETRIES) {
                        return;
                    }

                    javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(Duration.millis(350));
                    wait.setOnFinished(evt -> syncCartCountWithRetry(attempts));
                    wait.play();
                }));
    }

    private void refreshCartCount(EncomendaCatalogo carrinho) {
        cartQtyByArtigo.clear();
        if (carrinho == null || carrinho.getId() == null) {
            cartCount = 0;
            updateCartButton();
            refreshProducts();
            return;
        }

        itemEncomendaService.findByEncomendaId(carrinho.getId())
                .whenComplete((itens, error) -> Platform.runLater(() -> {
                    if (error != null || itens == null) {
                        cartCount = 0;
                    } else {
                        cartCount = itens.stream().mapToInt(item -> item.getQuantidade() == null ? 0 : item.getQuantidade()).sum();
                        for (ItemEncomenda item : itens) {
                            if (item.getIdArtigo() != null && item.getIdArtigo().getId() != null) {
                                cartQtyByArtigo.merge(item.getIdArtigo().getId(),
                                        item.getQuantidade() == null ? 0 : item.getQuantidade(), Integer::sum);
                            }
                        }
                    }
                    updateCartButton();
                    refreshProducts();
                }));
    }

    private int resolveInCartQuantity(Integer artigoId) {
        if (artigoId == null) {
            return 0;
        }
        return cartQtyByArtigo.getOrDefault(artigoId, 0);
    }

    private void updateCartButton() {
        cartButton.setText("Carrinho (" + cartCount + ")");
        cartButton.setOnAction(event -> {
            if (navigator != null) {
                navigator.navigateTo("cart");
            }
        });
        cartButton.setVisible(true);
        cartButton.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}