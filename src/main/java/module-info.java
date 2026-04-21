module com.example.appdesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;

    opens com.example.appdesktop to javafx.fxml;
    opens com.example.appdesktop.models to com.fasterxml.jackson.databind;
    opens com.example.appdesktop.services to com.fasterxml.jackson.databind;
    exports com.example.appdesktop;
    exports com.example.appdesktop.models;
    exports com.example.appdesktop.services;
}