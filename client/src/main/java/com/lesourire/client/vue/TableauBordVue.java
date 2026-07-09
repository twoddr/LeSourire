package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Session;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tableau de bord : vue globale de la journée du cabinet.
 * Les indicateurs seront alimentés par le serveur au fil des phases ;
 * la mise en page est déjà en place pour les démonstrations.
 */
public final class TableauBordVue {

    private TableauBordVue() {
    }

    public static Node creer() {
        String prenom = Session.utilisateur().prenom();
        String salutation = "Bonjour" + (prenom == null || prenom.isBlank() ? "" : ", " + prenom) + " !";

        Label titre = new Label(salutation);
        titre.getStyleClass().add("titre-page");

        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        Label date = new Label(LocalDate.now().format(format));
        date.getStyleClass().add("sous-titre-page");

        FlowPane cartes = new FlowPane(16, 16,
                carteStatistique("Rendez-vous aujourd'hui", "—", Material2AL.EVENT),
                carteStatistique("Patients enregistrés", "—", Material2AL.GROUP),
                carteStatistique("Factures en attente", "—", Material2MZ.RECEIPT),
                carteStatistique("Alertes de stock", "—", Material2MZ.WARNING));
        cartes.setPadding(new Insets(8, 0, 0, 0));

        Label note = new Label("Les indicateurs s'activeront au fur et à mesure "
                + "de la mise en service des modules.");
        note.getStyleClass().add("note-discrete");
        note.setWrapText(true);

        VBox page = new VBox(6, titre, date, cartes, note);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));
        VBox.setMargin(note, new Insets(16, 0, 0, 0));
        return page;
    }

    private static Node carteStatistique(String libelle, String valeur, Ikon icone) {
        FontIcon fontIcon = new FontIcon(icone);
        fontIcon.getStyleClass().add("carte-stat-icone");

        Label labelValeur = new Label(valeur);
        labelValeur.getStyleClass().add("carte-stat-valeur");

        Label labelLibelle = new Label(libelle);
        labelLibelle.getStyleClass().add("carte-stat-libelle");
        labelLibelle.setWrapText(true);

        Region espace = new Region();
        VBox.setVgrow(espace, Priority.ALWAYS);

        VBox carte = new VBox(6, fontIcon, espace, labelValeur, labelLibelle);
        carte.getStyleClass().add("carte-stat");
        carte.setPadding(new Insets(18));
        carte.setPrefSize(220, 130);
        carte.setAlignment(Pos.TOP_LEFT);
        return carte;
    }
}
