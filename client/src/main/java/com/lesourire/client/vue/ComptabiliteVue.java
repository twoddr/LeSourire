package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceComptabilite;
import com.lesourire.client.service.ServiceComptabiliteApi;
import com.lesourire.client.service.ServiceComptabiliteDemo;
import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Module Comptabilité : encaissements du jour, impayés et journal des paiements.
 * Lecture seule — les encaissements se saisissent dans Facturation.
 */
public class ComptabiliteVue {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox racine = new VBox();
    private final ServiceComptabilite service;

    private final DatePicker dateEncaissements = new DatePicker(LocalDate.now());
    private final TableView<EncaissementDTO> tableauEncaissements = new TableView<>();
    private final Label labelStatutEncaissements = new Label();

    private final TableView<ImpayeDTO> tableauImpayes = new TableView<>();
    private final Label labelStatutImpayes = new Label();

    private final DatePicker dateJournalDebut = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker dateJournalFin = new DatePicker(LocalDate.now());
    private final TableView<EncaissementDTO> tableauJournal = new TableView<>();
    private final Label labelStatutJournal = new Label();

    public ComptabiliteVue() {
        this.service = Session.estModeDemonstration()
                ? new ServiceComptabiliteDemo()
                : new ServiceComptabiliteApi(Session.api());
        construire();
        chargerEncaissements();
        chargerImpayes();
        chargerJournal();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));
        VBox.setVgrow(racine, Priority.ALWAYS);

        Label titre = new Label("Comptabilité");
        titre.getStyleClass().add("titre-page");

        TabPane onglets = new TabPane(
                new Tab("Encaissements du jour", construireOngletEncaissements()),
                new Tab("Impayés", construireOngletImpayes()),
                new Tab("Journal", construireOngletJournal()));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(onglets, Priority.ALWAYS);

        racine.getChildren().setAll(titre, onglets);
        VBox.setMargin(onglets, new Insets(16, 0, 0, 0));
    }

    private Node construireOngletEncaissements() {
        construireTableauEncaissements(tableauEncaissements);

        Button actualiser = boutonActualiser(e -> chargerEncaissements());
        dateEncaissements.valueProperty().addListener((obs, a, b) -> chargerEncaissements());

        HBox barre = new HBox(12, new Label("Date"), dateEncaissements, espace(), actualiser);
        barre.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(12, barre, tableauEncaissements, labelStatutEncaissements);
        page.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(tableauEncaissements, Priority.ALWAYS);
        return page;
    }

    private Node construireOngletImpayes() {
        construireTableauImpayes();

        Button actualiser = boutonActualiser(e -> chargerImpayes());
        HBox barre = new HBox(12, espace(), actualiser);
        barre.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(12, barre, tableauImpayes, labelStatutImpayes);
        page.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(tableauImpayes, Priority.ALWAYS);
        return page;
    }

    private Node construireOngletJournal() {
        construireTableauEncaissements(tableauJournal);

        Button actualiser = boutonActualiser(e -> chargerJournal());
        dateJournalDebut.valueProperty().addListener((obs, a, b) -> chargerJournal());
        dateJournalFin.valueProperty().addListener((obs, a, b) -> chargerJournal());

        HBox barre = new HBox(12,
                new Label("Du"), dateJournalDebut,
                new Label("au"), dateJournalFin,
                espace(), actualiser);
        barre.setAlignment(Pos.CENTER_LEFT);

        VBox page = new VBox(12, barre, tableauJournal, labelStatutJournal);
        page.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(tableauJournal, Priority.ALWAYS);
        return page;
    }

    private void construireTableauEncaissements(TableView<EncaissementDTO> tableau) {
        tableau.getColumns().setAll(
                colE("Heure", 120, e -> e.datePaiement == null ? "" : DATE_HEURE.format(e.datePaiement)),
                colE("Facture", 110, e -> e.factureNumero == null ? "" : e.factureNumero),
                colE("Patient", 180, e -> e.patientNom == null ? "" : e.patientNom),
                colE("Montant", 100, e -> montant(e.montant)),
                colE("Mode", 120, e -> e.mode == null ? "" : e.mode.getLibelle()),
                colE("Payeur", 100, e -> e.payeur == null ? "" : e.payeur.getLibelle()),
                colE("Référence", 120, e -> e.reference == null ? "" : e.reference),
                colE("Reçu par", 140, e -> e.recuParNom == null ? "" : e.recuParNom));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucun encaissement."));
    }

    private void construireTableauImpayes() {
        tableauImpayes.getColumns().setAll(
                colI("Facture", 110, i -> i.factureNumero == null ? "" : i.factureNumero),
                colI("Date", 100, i -> i.dateFacture == null ? "" : DATE.format(i.dateFacture)),
                colI("Échéance", 100, i -> i.dateEcheance == null ? "" : DATE.format(i.dateEcheance)),
                colI("Patient", 180, i -> i.patientNom == null ? "" : i.patientNom),
                colI("Payeur", 100, i -> i.payeur == null ? "" : i.payeur.getLibelle()),
                colI("Tiers", 160, i -> i.payeurNom == null ? "" : i.payeurNom),
                colI("Solde", 110, i -> montant(i.solde)));
        tableauImpayes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauImpayes.setPlaceholder(new Label("Aucun impayé."));
    }

    private void chargerEncaissements() {
        LocalDate date = dateEncaissements.getValue();
        if (date == null) {
            return;
        }
        labelStatutEncaissements.setText("Chargement…");
        Async.executer(() -> service.encaissements(date),
                liste -> {
                    tableauEncaissements.getItems().setAll(liste);
                    labelStatutEncaissements.setText(resumeEncaissements(liste) + suffixeDemo());
                },
                e -> {
                    labelStatutEncaissements.setText("");
                    afficherErreur("Impossible de charger les encaissements", e);
                });
    }

    private void chargerImpayes() {
        labelStatutImpayes.setText("Chargement…");
        Async.executer(service::impayes,
                liste -> {
                    tableauImpayes.getItems().setAll(liste);
                    BigDecimal total = liste.stream()
                            .map(i -> i.solde == null ? BigDecimal.ZERO : i.solde)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    labelStatutImpayes.setText(liste.size() + " relance(s) — total dû "
                            + montant(total) + " XAF" + suffixeDemo());
                },
                e -> {
                    labelStatutImpayes.setText("");
                    afficherErreur("Impossible de charger les impayés", e);
                });
    }

    private void chargerJournal() {
        LocalDate debut = dateJournalDebut.getValue();
        LocalDate fin = dateJournalFin.getValue();
        if (debut == null || fin == null) {
            return;
        }
        if (fin.isBefore(debut)) {
            labelStatutJournal.setText("La date de fin doit être ≥ date de début.");
            return;
        }
        labelStatutJournal.setText("Chargement…");
        Async.executer(() -> service.journal(debut, fin),
                liste -> {
                    tableauJournal.getItems().setAll(liste);
                    labelStatutJournal.setText(resumeEncaissements(liste) + suffixeDemo());
                },
                e -> {
                    labelStatutJournal.setText("");
                    afficherErreur("Impossible de charger le journal", e);
                });
    }

    private static String resumeEncaissements(List<EncaissementDTO> liste) {
        BigDecimal total = liste.stream()
                .map(e -> e.montant == null ? BigDecimal.ZERO : e.montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<ModePaiement, BigDecimal> parMode = liste.stream()
                .filter(e -> e.mode != null && e.montant != null)
                .collect(Collectors.groupingBy(e -> e.mode, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, e -> e.montant, BigDecimal::add)));
        String detail = parMode.entrySet().stream()
                .map(e -> e.getKey().getLibelle() + " " + montant(e.getValue()))
                .collect(Collectors.joining(" · "));
        String base = liste.size() + " paiement(s) — total " + montant(total) + " XAF";
        return detail.isBlank() ? base : base + " (" + detail + ")";
    }

    private static String montant(BigDecimal m) {
        if (m == null) {
            return "0";
        }
        return m.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static Button boutonActualiser(javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button b = new Button();
        b.setGraphic(new FontIcon(Material2MZ.REFRESH));
        b.setOnAction(action);
        return b;
    }

    private static Region espace() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static TableColumn<EncaissementDTO, String> colE(String titre, double largeur,
            Function<EncaissementDTO, String> getter) {
        TableColumn<EncaissementDTO, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() == null ? "" : getter.apply(c.getValue())));
        return col;
    }

    private static TableColumn<ImpayeDTO, String> colI(String titre, double largeur,
            Function<ImpayeDTO, String> getter) {
        TableColumn<ImpayeDTO, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() == null ? "" : getter.apply(c.getValue())));
        return col;
    }

    private String suffixeDemo() {
        return Session.estModeDemonstration() ? " (démo)" : "";
    }

    private void afficherErreur(String titre, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setTitle(titre);
        alerte.setHeaderText(titre);
        alerte.showAndWait();
    }
}
