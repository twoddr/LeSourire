module com.lesourire.client {
    requires javafx.controls;
    requires javafx.graphics;

    requires lesourire.commun;
    requires atlantafx.base;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    requires java.prefs;

    opens com.lesourire.client to javafx.graphics;
}
