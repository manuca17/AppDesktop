module com.example.appdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;

    opens com.example.appdesktop to javafx.fxml;
    exports com.example.appdesktop;
}