package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceFacturation;
import com.lesourire.client.service.ServiceFacturationApi;
import com.lesourire.client.service.ServiceFacturationDemo;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.client.service.ServicePatientsApi;
import com.lesourire.client.service.ServicePatientsDemo;
import com.lesourire.client.service.ServiceRdv;
import com.lesourire.client.service.ServiceRdvApi;
import com.lesourire.client.service.ServiceRdvDemo;
import com.lesourire.client.service.ServiceStock;
import com.lesourire.client.service.ServiceStockApi;
import com.lesourire.client.service.ServiceStockDemo;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.Role;
import com.lesourire.commun.dto.ArticleDTO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Tableau de bord : indicateurs du jour et raccourcis vers les modules.
 */
public final class TableauBordVue {

    private TableauBordVue() {
    }

    public static Node creer(Consumer<Module> naviguer) {
        Role role = Session.utilisateur().role();
        String prenom = Session.utilisateur().prenom();
        String salutation = "Bonjour" + (prenom == null || prenom.isBlank() ? "" : ", " + prenom) + " !";

        Label titre = new Label(salutation);
        titre.getStyleClass().add("titre-page");

        DateTimeFormatter format = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        Label date = new Label(LocalDate.now().format(format));
        date.getStyleClass().add("sous-titre-page");

        FlowPane cartes = new FlowPane(16, 16);
        cartes.setPadding(new Insets(8, 0, 0, 0));

        Label valeurRdv = new Label("…");
        if (Module.AGENDA.estAccessiblePar(role)) {
            cartes.getChildren().add(carteStatistique(
                    "Rendez-vous aujourd'hui", valeurRdv, Material2AL.EVENT,
                    "Ouvrir l'agenda",
                    () -> naviguer.accept(Module.AGENDA)));
            chargerCountRdv(valeurRdv);
        }

        Label valeurPatients = new Label("…");
        if (Module.PATIENTS.estAccessiblePar(role)) {
            cartes.getChildren().add(carteStatistique(
                    "Patients enregistrés", valeurPatients, Material2AL.GROUP,
                    "Ouvrir les patients",
                    () -> naviguer.accept(Module.PATIENTS)));
            chargerCountPatients(valeurPatients);
        }

        Label valeurFactures = new Label("…");
        if (Module.FACTURATION.estAccessiblePar(role)) {
            cartes.getChildren().add(carteStatistique(
                    "Factures en attente", valeurFactures, Material2MZ.RECEIPT,
                    "Ouvrir la facturation",
                    () -> naviguer.accept(Module.FACTURATION)));
            chargerCountFacturesEnAttente(valeurFactures);
        }

        Label valeurStock = new Label("…");
        if (Module.STOCK.estAccessiblePar(role)) {
            cartes.getChildren().add(carteStatistique(
                    "Alertes de stock", valeurStock, Material2MZ.WARNING,
                    "Ouvrir le stock",
                    () -> naviguer.accept(Module.STOCK)));
            chargerCountAlertesStock(valeurStock);
        }

        Label note = new Label("Cliquez sur une tuile pour ouvrir le module correspondant.");
        note.getStyleClass().add("note-discrete");
        note.setWrapText(true);

        VBox page = new VBox(6, titre, date, cartes, note);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(32));
        VBox.setMargin(note, new Insets(16, 0, 0, 0));
        return page;
    }

    private static void chargerCountRdv(Label valeur) {
        ServiceRdv service = Session.estModeDemonstration()
                ? new ServiceRdvDemo()
                : new ServiceRdvApi(Session.api());
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().plusDays(1).atStartOfDay();
        Async.executer(() -> service.compter(debut, fin),
                n -> valeur.setText(String.valueOf(n)),
                e -> valeur.setText("—"));
    }

    private static void chargerCountPatients(Label valeur) {
        ServicePatients service = Session.estModeDemonstration()
                ? new ServicePatientsDemo()
                : new ServicePatientsApi(Session.api());
        Async.executer(() -> service.rechercher(""),
                liste -> valeur.setText(String.valueOf(liste.size())),
                e -> valeur.setText("—"));
    }

    private static void chargerCountFacturesEnAttente(Label valeur) {
        ServiceFacturation service = Session.estModeDemonstration()
                ? new ServiceFacturationDemo()
                : new ServiceFacturationApi(Session.api());
        Async.executer(() -> {
            int emises = service.rechercher("", StatutFacture.EMISE, null, null).size();
            int partielles = service.rechercher("", StatutFacture.PARTIELLEMENT_PAYEE, null, null)
                    .size();
            return emises + partielles;
        }, n -> valeur.setText(String.valueOf(n)), e -> valeur.setText("—"));
    }

    private static void chargerCountAlertesStock(Label valeur) {
        ServiceStock service = Session.estModeDemonstration()
                ? new ServiceStockDemo()
                : new ServiceStockApi(Session.api());
        Async.executer(() -> service.rechercherArticles("", false).stream()
                .filter(ArticleDTO::enAlerte)
                .count(),
                n -> valeur.setText(String.valueOf(n)),
                e -> valeur.setText("—"));
    }

    private static Node carteStatistique(String libelle, Label labelValeur, Ikon icone,
            String tooltip, Runnable action) {
        FontIcon fontIcon = new FontIcon(icone);
        fontIcon.getStyleClass().add("carte-stat-icone");

        labelValeur.getStyleClass().add("carte-stat-valeur");

        Label labelLibelle = new Label(libelle);
        labelLibelle.getStyleClass().add("carte-stat-libelle");
        labelLibelle.setWrapText(true);

        Region espace = new Region();
        VBox.setVgrow(espace, Priority.ALWAYS);

        VBox carte = new VBox(6, fontIcon, espace, labelValeur, labelLibelle);
        carte.getStyleClass().addAll("carte-stat", "carte-stat-cliquable");
        carte.setPadding(new Insets(18));
        carte.setPrefSize(220, 130);
        carte.setAlignment(Pos.TOP_LEFT);
        carte.setCursor(Cursor.HAND);
        Tooltip.install(carte, new Tooltip(tooltip));
        carte.setOnMouseClicked(e -> {
            if (action != null) {
                action.run();
            }
        });
        return carte;
    }
}
