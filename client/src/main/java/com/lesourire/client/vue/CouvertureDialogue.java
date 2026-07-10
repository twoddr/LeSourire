package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.SocieteDTO;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Ajout d'une couverture (assureur ou société) à un patient.
 * Le résultat est un CouvertureDTO sans id : c'est l'appelant qui décide
 * de l'envoyer au serveur (patient existant) ou de le garder pour la création.
 */
public class CouvertureDialogue extends Dialog<CouvertureDTO> {

    private static final String CHOIX_ASSUREUR = "Assureur";
    private static final String CHOIX_SOCIETE = "Société conventionnée";

    private final ServicePatients service;

    private final ComboBox<String> champType = new ComboBox<>(
            FXCollections.observableArrayList(CHOIX_ASSUREUR, CHOIX_SOCIETE));
    private final ComboBox<Object> champPayeur = new ComboBox<>();
    private final TextField champNumeroAssure = new TextField();
    private final TextField champPourcentage = new TextField();
    private final DatePicker champDebut = new DatePicker(LocalDate.now());

    private List<AssureurDTO> assureurs = List.of();
    private List<SocieteDTO> societes = List.of();

    public CouvertureDialogue(ServicePatients service) {
        this.service = service;
        setTitle("Nouvelle couverture");
        setHeaderText("Prise en charge par un tiers payant");

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(18));
        ColumnConstraints libelle = new ColumnConstraints(170);
        ColumnConstraints saisie = new ColumnConstraints();
        saisie.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(libelle, saisie);

        champType.getSelectionModel().selectFirst();
        champType.setMaxWidth(Double.MAX_VALUE);
        champType.setOnAction(e -> remplirPayeurs());

        champPayeur.setMaxWidth(Double.MAX_VALUE);
        Button boutonCreer = new Button("+");
        boutonCreer.setTooltip(new Tooltip("Créer un nouveau tiers payant"));
        boutonCreer.setOnAction(e -> creationRapide());
        HBox lignePayeur = new HBox(6, champPayeur, boutonCreer);
        HBox.setHgrow(champPayeur, Priority.ALWAYS);

        champNumeroAssure.setPromptText("n° de police / matricule");
        champPourcentage.setPromptText("vide = % par défaut du tiers payant");

        int ligne = 0;
        grille.add(new Label("Type"), 0, ligne);
        grille.add(champType, 1, ligne++);
        grille.add(new Label("Tiers payant"), 0, ligne);
        grille.add(lignePayeur, 1, ligne++);
        grille.add(new Label("N° assuré"), 0, ligne);
        grille.add(champNumeroAssure, 1, ligne++);
        grille.add(new Label("% pris en charge"), 0, ligne);
        grille.add(champPourcentage, 1, ligne++);
        grille.add(new Label("Début de couverture"), 0, ligne);
        grille.add(champDebut, 1, ligne);

        getDialogPane().setContent(grille);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Ajouter");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (!valider()) {
                e.consume();
            }
        });

        setResultConverter(bouton -> bouton == ButtonType.OK ? construire() : null);

        chargerReferentiels();
    }

    private void chargerReferentiels() {
        Async.executer(service::listerAssureurs, liste -> {
            assureurs = liste;
            remplirPayeurs();
        }, e -> {
        });
        Async.executer(service::listerSocietes, liste -> {
            societes = liste;
            remplirPayeurs();
        }, e -> {
        });
    }

    private boolean typeAssureur() {
        return CHOIX_ASSUREUR.equals(champType.getSelectionModel().getSelectedItem());
    }

    private void remplirPayeurs() {
        champPayeur.getItems().setAll(typeAssureur() ? assureurs : societes);
        champNumeroAssure.setDisable(!typeAssureur());
        if (!typeAssureur()) {
            champNumeroAssure.clear();
        }
        if (champPayeur.getItems().size() == 1) {
            champPayeur.getSelectionModel().selectFirst();
        }
    }

    private void creationRapide() {
        TextInputDialog saisie = new TextInputDialog();
        saisie.setTitle(typeAssureur() ? "Nouvel assureur" : "Nouvelle société");
        saisie.setHeaderText("Création rapide");
        saisie.setContentText("Nom :");
        boolean creationAssureur = typeAssureur();
        saisie.showAndWait().filter(nom -> !nom.isBlank()).ifPresent(nom ->
                Async.executer(() -> creationAssureur
                                ? service.creerAssureur(nom.trim(), BigDecimal.ZERO)
                                : (Object) service.creerSociete(nom.trim(), BigDecimal.ZERO),
                        cree -> {
                            chargerEtSelectionner(cree, creationAssureur);
                        },
                        this::afficherErreur));
    }

    private void chargerEtSelectionner(Object cree, boolean estAssureur) {
        if (estAssureur && cree instanceof AssureurDTO assureur) {
            assureurs = ajouter(assureurs, assureur);
        } else if (cree instanceof SocieteDTO societe) {
            societes = ajouter(societes, societe);
        }
        remplirPayeurs();
        champPayeur.getSelectionModel().select(cree);
    }

    private <T> List<T> ajouter(List<T> liste, T element) {
        var copie = new java.util.ArrayList<>(liste);
        copie.add(element);
        return List.copyOf(copie);
    }

    private boolean valider() {
        if (champPayeur.getSelectionModel().getSelectedItem() == null) {
            afficherErreur(new IllegalArgumentException("Choisissez le tiers payant."));
            return false;
        }
        if (champDebut.getValue() == null) {
            afficherErreur(new IllegalArgumentException("La date de début est obligatoire."));
            return false;
        }
        BigDecimal pct = lirePourcentage();
        if (!champPourcentage.getText().trim().isEmpty()
                && (pct == null || pct.compareTo(BigDecimal.ZERO) < 0
                        || pct.compareTo(new BigDecimal("100")) > 0)) {
            afficherErreur(new IllegalArgumentException(
                    "Le pourcentage doit être un nombre entre 0 et 100."));
            return false;
        }
        return true;
    }

    private BigDecimal lirePourcentage() {
        String brut = champPourcentage.getText().trim().replace(',', '.');
        if (brut.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(brut);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CouvertureDTO construire() {
        CouvertureDTO c = new CouvertureDTO();
        Object payeur = champPayeur.getSelectionModel().getSelectedItem();
        if (payeur instanceof AssureurDTO assureur) {
            c.type = CouvertureDTO.TYPE_ASSUREUR;
            c.payeurId = assureur.id();
            c.payeurNom = assureur.nom();
            c.numeroAssure = champNumeroAssure.getText().isBlank()
                    ? null : champNumeroAssure.getText().trim();
            c.pourcentageEffectif = lirePourcentage() != null
                    ? lirePourcentage() : assureur.pourcentageDefaut();
        } else if (payeur instanceof SocieteDTO societe) {
            c.type = CouvertureDTO.TYPE_SOCIETE;
            c.payeurId = societe.id();
            c.payeurNom = societe.nom();
            c.pourcentageEffectif = lirePourcentage() != null
                    ? lirePourcentage() : societe.pourcentageDefaut();
        }
        c.pourcentage = lirePourcentage();
        c.dateDebut = champDebut.getValue();
        return c;
    }

    private void afficherErreur(Exception e) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText("Couverture non valide");
        alerte.initOwner(getDialogPane().getScene().getWindow());
        alerte.showAndWait();
    }
}
