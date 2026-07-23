package com.lesourire.client.vue;

import java.math.BigDecimal;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Fiche commune assureur / société conventionnée. */
public class FicheTiersPayantDialogue extends Dialog<FicheTiersPayantDialogue.Saisie> {

    public record Saisie(String nom, String telephone, String email, BigDecimal pourcentage,
            boolean actif) {
    }

    private final TextField champNom = new TextField();
    private final TextField champTelephone = new TextField();
    private final TextField champEmail = new TextField();
    private final TextField champPourcentage = new TextField("0");
    private final CheckBox champActif = new CheckBox("Actif");
    private final Label labelErreur = new Label();

    public static FicheTiersPayantDialogue assureur(AssureurDTO existant) {
        return new FicheTiersPayantDialogue(
                existant == null ? "Nouvel assureur" : "Modifier " + existant.nom(),
                existant == null ? null : existant.nom(),
                existant == null ? null : existant.telephone(),
                existant == null ? null : existant.email(),
                existant == null ? BigDecimal.ZERO : existant.pourcentageDefaut(),
                existant == null || existant.actif());
    }

    public static FicheTiersPayantDialogue societe(SocieteDTO existante) {
        return new FicheTiersPayantDialogue(
                existante == null ? "Nouvelle société" : "Modifier " + existante.nom(),
                existante == null ? null : existante.nom(),
                existante == null ? null : existante.telephone(),
                existante == null ? null : existante.email(),
                existante == null ? BigDecimal.ZERO : existante.pourcentageDefaut(),
                existante == null || existante.actif());
    }

    private FicheTiersPayantDialogue(String titre, String nom, String telephone, String email,
            BigDecimal pourcentage, boolean actif) {
        setTitle(titre);
        setResizable(true);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(c1, c2);

        int l = 0;
        grille.add(new Label("Nom"), 0, l);
        grille.add(champNom, 1, l++);
        grille.add(new Label("Téléphone"), 0, l);
        grille.add(champTelephone, 1, l++);
        grille.add(new Label("E-mail"), 0, l);
        grille.add(champEmail, 1, l++);
        grille.add(new Label("% prise en charge"), 0, l);
        grille.add(champPourcentage, 1, l++);
        grille.add(champActif, 1, l++);

        labelErreur.getStyleClass().add("label-erreur");
        grille.add(labelErreur, 0, l, 2, 1);

        if (nom != null) {
            champNom.setText(nom);
        }
        if (telephone != null) {
            champTelephone.setText(telephone);
        }
        if (email != null) {
            champEmail.setText(email);
        }
        if (pourcentage != null) {
            champPourcentage.setText(pourcentage.stripTrailingZeros().toPlainString());
        }
        champActif.setSelected(actif);

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(440, 280);
        ButtonType enregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(enregistrer, ButtonType.CANCEL);

        getDialogPane().lookupButton(enregistrer).addEventFilter(
                javafx.event.ActionEvent.ACTION, e -> {
                    String erreur = valider();
                    if (erreur != null) {
                        labelErreur.setText(erreur);
                        e.consume();
                    }
                });

        setResultConverter(bouton -> {
            if (bouton != enregistrer) {
                return null;
            }
            return new Saisie(
                    champNom.getText().trim(),
                    blank(champTelephone.getText()),
                    blank(champEmail.getText()),
                    new BigDecimal(champPourcentage.getText().trim().replace(',', '.')),
                    champActif.isSelected());
        });
    }

    private String valider() {
        if (champNom.getText() == null || champNom.getText().isBlank()) {
            return "Le nom est obligatoire.";
        }
        try {
            BigDecimal p = new BigDecimal(champPourcentage.getText().trim().replace(',', '.'));
            if (p.compareTo(BigDecimal.ZERO) < 0 || p.compareTo(new BigDecimal("100")) > 0) {
                return "Le pourcentage doit être entre 0 et 100.";
            }
        } catch (NumberFormatException ex) {
            return "Pourcentage invalide.";
        }
        return null;
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
