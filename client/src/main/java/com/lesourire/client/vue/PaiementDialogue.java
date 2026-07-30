package com.lesourire.client.vue;

import java.math.BigDecimal;

import com.lesourire.client.coeur.Montants;
import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.PaiementDTO;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/**
 * Encaissement d'un paiement sur une facture émise : choix du payeur
 * (patient, assureur ou société selon les soldes restants), montant, mode.
 */
public class PaiementDialogue extends Dialog<PaiementDTO> {

    private final FactureDTO facture;
    private final ComboBox<Payeur> champPayeur = new ComboBox<>();
    private final TextField champMontant = new TextField();
    private final ComboBox<ModePaiement> champMode = new ComboBox<>(
            FXCollections.observableArrayList(ModePaiement.values()));
    private final TextField champReference = new TextField();
    private final Label labelSolde = new Label();

    public PaiementDialogue(FactureDTO facture) {
        this.facture = facture;
        setTitle("Encaisser un paiement");
        setHeaderText("Facture " + facture.numero + " — " + facture.patientNom
                + "\nReste dû total : " + Montants.formaterAvecDevise(facture.soldeTotal()));

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(18));
        ColumnConstraints libelle = new ColumnConstraints(140);
        ColumnConstraints saisie = new ColumnConstraints();
        saisie.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(libelle, saisie);

        for (Payeur payeur : Payeur.values()) {
            if (solde(payeur).compareTo(BigDecimal.ZERO) > 0) {
                champPayeur.getItems().add(payeur);
            }
        }
        champPayeur.setConverter(new StringConverter<>() {
            @Override
            public String toString(Payeur payeur) {
                if (payeur == null) {
                    return "";
                }
                return switch (payeur) {
                    case PATIENT -> "Patient";
                    case ASSUREUR -> "Assureur"
                            + (facture.assureurNom == null ? "" : " (" + facture.assureurNom + ")");
                    case SOCIETE -> "Société"
                            + (facture.societeNom == null ? "" : " (" + facture.societeNom + ")");
                };
            }

            @Override
            public Payeur fromString(String texte) {
                return null;
            }
        });
        champPayeur.setMaxWidth(Double.MAX_VALUE);
        champPayeur.setOnAction(e -> payeurChoisi());
        champPayeur.getSelectionModel().selectFirst();
        payeurChoisi();

        champMode.getSelectionModel().select(ModePaiement.ESPECES);
        champMode.setMaxWidth(Double.MAX_VALUE);
        champReference.setPromptText("n° de chèque, référence de virement…");
        labelSolde.getStyleClass().add("note-discrete");

        int ligne = 0;
        grille.add(new Label("Payeur"), 0, ligne);
        grille.add(champPayeur, 1, ligne++);
        grille.add(new Label("Montant (XAF)"), 0, ligne);
        grille.add(champMontant, 1, ligne++);
        grille.add(labelSolde, 1, ligne++);
        grille.add(new Label("Mode"), 0, ligne);
        grille.add(champMode, 1, ligne++);
        grille.add(new Label("Référence"), 0, ligne);
        grille.add(champReference, 1, ligne);

        getDialogPane().setContent(grille);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Encaisser");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (!valider()) {
                e.consume();
            }
        });

        setResultConverter(bouton -> bouton == ButtonType.OK ? construire() : null);
    }

    private BigDecimal solde(Payeur payeur) {
        return switch (payeur) {
            case PATIENT -> facture.soldePatient;
            case ASSUREUR -> facture.soldeAssureur;
            case SOCIETE -> facture.soldeSociete;
        };
    }

    private void payeurChoisi() {
        Payeur payeur = champPayeur.getSelectionModel().getSelectedItem();
        if (payeur == null) {
            return;
        }
        BigDecimal solde = solde(payeur);
        champMontant.setText(solde.stripTrailingZeros().toPlainString());
        labelSolde.setText("Reste dû par ce payeur : " + Montants.formaterAvecDevise(solde));
    }

    private BigDecimal lireMontant() {
        try {
            return new BigDecimal(champMontant.getText().trim().replace(" ", "")
                    .replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean valider() {
        Payeur payeur = champPayeur.getSelectionModel().getSelectedItem();
        if (payeur == null) {
            return avertir("Choisissez le payeur.");
        }
        BigDecimal montant = lireMontant();
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            return avertir("Indiquez un montant strictement positif.");
        }
        if (montant.compareTo(solde(payeur)) > 0) {
            return avertir("Le montant dépasse le reste dû par ce payeur ("
                    + Montants.formaterAvecDevise(solde(payeur)) + ").");
        }
        return true;
    }

    private boolean avertir(String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alerte.setHeaderText("Paiement non valide");
        alerte.initOwner(getDialogPane().getScene().getWindow());
        alerte.showAndWait();
        return false;
    }

    private PaiementDTO construire() {
        PaiementDTO paiement = new PaiementDTO();
        paiement.factureId = facture.id;
        paiement.payeur = champPayeur.getSelectionModel().getSelectedItem();
        paiement.montant = lireMontant();
        paiement.mode = champMode.getSelectionModel().getSelectedItem();
        String reference = champReference.getText().trim();
        paiement.reference = reference.isEmpty() ? null : reference;
        return paiement;
    }
}
