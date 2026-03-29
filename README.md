# AppDesktop

JavaFX desktop app with a login screen inspired by the React design (tabs for Cliente and Administrador).

## Requirements

- JDK 17+
- Windows cmd.exe (examples below)

## Run

```bat
mvnw.cmd clean javafx:run
```

## Tests

```bat
mvnw.cmd test
```

## What is implemented

- Login screen in `src/main/resources/com/example/appdesktop/login-view.fxml`
- Controller logic in `src/main/java/com/example/appdesktop/LoginController.java`
- Startup wired in `src/main/java/com/example/appdesktop/HelloApplication.java`
- Client dashboard in `src/main/resources/com/example/appdesktop/client-dashboard-view.fxml`
- Dashboard logic in `src/main/java/com/example/appdesktop/ClientDashboardController.java`
- Mock data and calculations in `src/main/java/com/example/appdesktop/ClientDashboardService.java`
- Client shell layout with sidebar in `src/main/resources/com/example/appdesktop/client-layout-view.fxml`
- Sidebar navigation logic in `src/main/java/com/example/appdesktop/ClientLayoutController.java`
- Catalog page in `src/main/resources/com/example/appdesktop/catalog-view.fxml`
- Briefing page in `src/main/resources/com/example/appdesktop/briefing-view.fxml`
- Projects page in `src/main/resources/com/example/appdesktop/projects-view.fxml`
- Orders page in `src/main/resources/com/example/appdesktop/orders-view.fxml`
- Project detail page in `src/main/resources/com/example/appdesktop/project-detail-view.fxml`
- Order detail page in `src/main/resources/com/example/appdesktop/order-detail-view.fxml`
- Cart page in `src/main/resources/com/example/appdesktop/cart-view.fxml`
- Checkout page in `src/main/resources/com/example/appdesktop/checkout-view.fxml`
- Project payment page in `src/main/resources/com/example/appdesktop/project-payment-view.fxml`
- Project payment logic in `src/main/java/com/example/appdesktop/ProjectPaymentController.java`
- Shared portal mock data in `src/main/java/com/example/appdesktop/ClientPortalDataService.java`

## Current behavior

- Login currently does not validate credentials (prototype flow)
- Cliente login opens a full client layout with sidebar navigation
- Dashboard, Catalogo, Briefing, Meus Projetos and Encomendas are available in the sidebar
- Project and order lists now open dedicated detail pages with tracking and summary blocks
- Cart page shows items with quantity and pricing, links to checkout
- Checkout page includes shipping form, payment method selection and order summary
- Project detail includes Pagamentos tab showing phased payments with status (paid/pending)
- Pending payments show "Efetuar Pagamento" button that opens dedicated payment page
- Admin login still opens a placeholder scene