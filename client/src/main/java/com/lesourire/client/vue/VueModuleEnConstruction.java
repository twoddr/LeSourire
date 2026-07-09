package com.lesourire.client.vue;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Vue provisoire affichée pour les modules pas encore développés :
 * elle présente au client ce que le module contiendra.
 */
public final class VueModuleEnConstruction {

    private VueModuleEnConstruction() {
    }

    public static Node creer(Module module) {
        FontIcon icone = new FontIcon(module.getIcone());
        icone.getStyleClass().add("placeholder-icone");

        Label titre = new Label(module.getLibelle());
        titre.getStyleClass().add("titre-page");

        Label description = new Label(module.getDescription());
        description.getStyleClass().add("placeholder-description");
        description.setWrapText(true);
        description.setMaxWidth(520);
        description.setAlignment(Pos.CENTER);

        Label etiquette = new Label("Module en préparation");
        etiquette.getStyleClass().add("etiquette-en-preparation");

        VBox page = new VBox(14, icone, titre, description, etiquette);
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(32));
        page.getStyleClass().add("page");
        return page;
    }
}
