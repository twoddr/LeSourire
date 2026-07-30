package com.lesourire.client.vue;

import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Montants;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceFacturation;
import com.lesourire.client.service.ServiceFacturationApi;
import com.lesourire.client.service.ServiceFacturationDemo;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.client.service.ServicePatientsApi;
import com.lesourire.client.service.ServicePatientsDemo;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Module Facturation : liste des factures, création de brouillons,
 * émission, encaissement des paiements et annulation.
 */
public class FacturationVue {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VBox racine = new VBox();
    private final ServiceFacturation service;
    private final ServicePatients servicePatients;

    private final TextField champRecherche = new TextField();
    private final ComboBox<StatutFacture> champStatut = new ComboBox<>();
    private final TableView<FactureDTO> tableau = new TableView<>();
    private final Label labelStatut = new Label();

    public FacturationVue() {
        if (Session.estModeDemonstration()) {
            this.service = new ServiceFacturationDemo();
            this.servicePatients = new ServicePatientsDemo();
        } else {
            this.service = new ServiceFacturationApi(Session.api());
            this.servicePatients = new ServicePatientsApi(Session.api());
        }
        construire();
        charger();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));
        VBox.setVgrow(racine, Priority.ALWAYS);

        Label titre = new Label("Facturation");
        titre.getStyleClass().add("titre-page");

        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setTooltip(new Tooltip("Actualiser la liste"));
        actualiser.setOnAction(e -> charger());

        Button encaisser = new Button("Encaisser…");
        encaisser.setGraphic(new FontIcon(Material2AL.ATTACH_MONEY));
        encaisser.setOnAction(e -> encaisserSelection());

        Button emettre = new Button("Émettre");
        emettre.setGraphic(new FontIcon(Material2MZ.SEND));
        emettre.setTooltip(new Tooltip("Émettre le brouillon sélectionné : "
                + "il devient définitif et encaissable"));
        emettre.setOnAction(e -> emettreSelection());

        Button annuler = new Button("Annuler");
        annuler.setGraphic(new FontIcon(Material2AL.BLOCK));
        annuler.setTooltip(new Tooltip("Annuler la facture sélectionnée (si aucun paiement)"));
        annuler.setOnAction(e -> annulerSelection());

        Button nouvelle = new Button("Nouvelle facture");
        nouvelle.setGraphic(new FontIcon(Material2AL.ADD));
        nouvelle.getStyleClass().add("bouton-principal");
        nouvelle.setOnAction(e -> nouvelleFacture());

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, actualiser, annuler, emettre, encaisser, nouvelle);
        actions.setAlignment(Pos.CENTER_RIGHT);

        champRecherche.setPromptText("Rechercher par numéro, patient ou dossier…");
        champRecherche.getStyleClass().add("champ-recherche");
        PauseTransition attente = new PauseTransition(Duration.millis(250));
        champRecherche.textProperty().addListener((obs, avant, apres) -> {
            attente.setOnFinished(e -> charger());
            attente.playFromStart();
        });

        champStatut.setItems(FXCollections.observableArrayList(StatutFacture.values()));
        champStatut.getItems().add(0, null);
        champStatut.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatutFacture statut) {
                return statut == null ? "Tous les statuts"
                        : FactureDialogue.libelleStatut(statut);
            }

            @Override
            public StatutFacture fromString(String texte) {
                return null;
            }
        });
        champStatut.getSelectionModel().selectFirst();
        champStatut.setOnAction(e -> charger());

        HBox filtres = new HBox(10, champRecherche, champStatut);
        HBox.setHgrow(champRecherche, Priority.ALWAYS);

        construireTableau();
        labelStatut.getStyleClass().add("note-discrete");

        VBox carte = new VBox(12, actions, filtres, tableau, labelStatut);
        carte.getStyleClass().add("carte");
        carte.setPadding(new Insets(18));
        VBox.setVgrow(carte, Priority.ALWAYS);
        VBox.setVgrow(tableau, Priority.ALWAYS);

        racine.getChildren().setAll(titre, carte);
        VBox.setMargin(carte, new Insets(16, 0, 0, 0));
    }

    private void construireTableau() {
        tableau.getColumns().setAll(java.util.List.of(
                colonne("Numéro", 110, f -> f.numero),
                colonne("Date", 90, f -> f.dateFacture == null ? ""
                        : f.dateFacture.format(FORMAT_DATE)),
                colonne("Patient", 200, f -> f.patientNom),
                colonne("Net", 110, f -> Montants.formater(f.montantNet)),
                colonne("Payé", 110, f -> Montants.formater(f.totalPaye())),
                colonne("Reste dû", 110, f -> Montants.formater(f.soldeTotal())),
                colonne("Tiers payant", 160, f -> {
                    if (f.assureurNom != null && f.societeNom != null) {
                        return f.assureurNom + " + " + f.societeNom;
                    }
                    return f.assureurNom != null ? f.assureurNom
                            : f.societeNom != null ? f.societeNom : "";
                }),
                colonne("Statut", 130, f -> FactureDialogue.libelleStatut(f.statut))));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucune facture. Créez la première avec « Nouvelle facture »."));

        tableau.setRowFactory(t -> {
            TableRow<FactureDTO> ligne = new TableRow<>() {
                @Override
                protected void updateItem(FactureDTO facture, boolean vide) {
                    super.updateItem(facture, vide);
                    getStyleClass().removeAll("facture-brouillon", "facture-en-retard",
                            "facture-payee", "facture-annulee");
                    if (vide || facture == null) {
                        return;
                    }
                    switch (facture.statut) {
                        case BROUILLON -> getStyleClass().add("facture-brouillon");
                        case PAYEE -> getStyleClass().add("facture-payee");
                        case ANNULEE -> getStyleClass().add("facture-annulee");
                        default -> {
                        }
                    }
                }
            };
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFacture(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private TableColumn<FactureDTO, String> colonne(String titre, double largeur,
            Function<FactureDTO, String> extracteur) {
        TableColumn<FactureDTO, String> colonne = new TableColumn<>(titre);
        colonne.setPrefWidth(largeur);
        colonne.setCellValueFactory(d -> {
            String valeur = extracteur.apply(d.getValue());
            return new SimpleStringProperty(valeur == null ? "" : valeur);
        });
        return colonne;
    }

    // -------------------------------------------------------------- actions

    private void charger() {
        String q = champRecherche.getText();
        StatutFacture statut = champStatut.getSelectionModel().getSelectedItem();
        Async.executer(() -> service.rechercher(q, statut), factures -> {
            tableau.getItems().setAll(factures);
            labelStatut.setText(factures.size() + " facture(s)");
        }, e -> afficherErreur("Impossible de charger les factures", e));
    }

    private void nouvelleFacture() {
        ChoixPatientDialogue choix = new ChoixPatientDialogue(servicePatients);
        Dialogues.afficher(choix, racine.getScene().getWindow()).ifPresent(patient -> {
            FactureDialogue fiche = new FactureDialogue(null, patient, service, servicePatients);
            Dialogues.afficher(fiche, racine.getScene().getWindow()).ifPresent(saisie ->
                    Async.executer(() -> service.creer(saisie),
                            creee -> charger(),
                            e -> afficherErreur("Impossible de créer la facture", e)));
        });
    }

    private void ouvrirFacture(FactureDTO resume) {
        Async.executer(() -> service.obtenir(resume.id), complete -> {
            FactureDialogue fiche = new FactureDialogue(complete, null, service, servicePatients);
            Dialogues.afficher(fiche, racine.getScene().getWindow()).ifPresent(saisie -> {
                if (saisie.statut == StatutFacture.BROUILLON) {
                    Async.executer(() -> service.modifier(saisie.id, saisie),
                            modifiee -> charger(),
                            e -> afficherErreur("Impossible d'enregistrer la facture", e));
                }
            });
        }, e -> afficherErreur("Impossible d'ouvrir la facture", e));
    }

    private void emettreSelection() {
        FactureDTO selection = exigerSelection();
        if (selection == null) {
            return;
        }
        Async.executer(() -> service.emettre(selection.id),
                emise -> charger(),
                e -> afficherErreur("Impossible d'émettre la facture", e));
    }

    private void annulerSelection() {
        FactureDTO selection = exigerSelection();
        if (selection == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Annuler définitivement la facture " + selection.numero + " ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Annulation de facture");
        confirmation.initOwner(racine.getScene().getWindow());
        confirmation.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b ->
                Async.executer(() -> service.annuler(selection.id),
                        annulee -> charger(),
                        e -> afficherErreur("Impossible d'annuler la facture", e)));
    }

    private void encaisserSelection() {
        FactureDTO selection = exigerSelection();
        if (selection == null) {
            return;
        }
        Async.executer(() -> service.obtenir(selection.id), complete -> {
            if (complete.statut != StatutFacture.EMISE
                    && complete.statut != StatutFacture.PARTIELLEMENT_PAYEE) {
                afficherErreur("Encaissement impossible", new IllegalStateException(
                        "Cette facture est au statut « "
                                + FactureDialogue.libelleStatut(complete.statut)
                                + " » : émettez-la d'abord."));
                return;
            }
            PaiementDialogue dialogue = new PaiementDialogue(complete);
            Dialogues.afficher(dialogue, racine.getScene().getWindow()).ifPresent(paiement ->
                    Async.executer(() -> service.encaisser(complete.id, paiement),
                            encaissee -> charger(),
                            e -> afficherErreur("Impossible d'encaisser le paiement", e)));
        }, e -> afficherErreur("Impossible d'ouvrir la facture", e));
    }

    private FactureDTO exigerSelection() {
        FactureDTO selection = tableau.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherErreur("Aucune facture sélectionnée",
                    new IllegalStateException("Sélectionnez une facture dans la liste."));
        }
        return selection;
    }

    private void afficherErreur(String titre, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText(titre);
        alerte.initOwner(racine.getScene().getWindow());
        alerte.showAndWait();
    }
}
