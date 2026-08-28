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
    requires java.desktop;
    /* Thymeleaf référence java.sql.Date dans ExpressionUtils (JPMS) */
    requires java.sql;

    /* Facture PDF : HTML (Thymeleaf) → OpenHTMLtoPDF */
    requires thymeleaf;
    requires org.slf4j;
    requires org.slf4j.nop;
    requires ognl;
    requires javassist;
    requires attoparser;
    requires unbescape;
    requires openhtmltopdf.pdfbox;
    requires openhtmltopdf.core;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires org.apache.xmpbox;
    requires commons.logging;
    requires de.rototor.pdfbox.graphics2d;

    opens com.lesourire.client to javafx.graphics;
    opens com.lesourire.client.impression to thymeleaf;
}
