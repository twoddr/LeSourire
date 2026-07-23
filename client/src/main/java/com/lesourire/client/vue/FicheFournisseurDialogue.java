package com.lesourire.client.vue;

import com.lesourire.commun.dto.FournisseurDTO;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Fiche fournisseur. */
public class FicheFournisseurDialogue extends Dialog<FournisseurDTO> {

    private final TextField champNom = new TextField();
    private final TextField champContact = new TextField();
    private final TextField champTelephone = new TextField();
    private final TextField champEmail = new TextField();
    private final TextField champAdresse = new TextField();
    private final TextArea champNotes = new TextArea();
    private final CheckBox champActif = new CheckBox("Fournisseur actif");
    private final Label labelErreur = new Label();

    public FicheFournisseurDialogue(FournisseurDTO existant) {
        boolean creation = existant == null || existant.id == null;
        setTitle(creation ? "Nouveau fournisseur" : "Modifier " + existant.nom);
        setResizable(true);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(120);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(c1, c2);

        int l = 0;
        grille.add(new Label("Nom"), 0, l);
        grille.add(champNom, 1, l++);
        grille.add(new Label("Contact"), 0, l);
        grille.add(champContact, 1, l++);
        grille.add(new Label("Téléphone"), 0, l);
        grille.add(champTelephone, 1, l++);
        grille.add(new Label("E-mail"), 0, l);
        grille.add(champEmail, 1, l++);
        grille.add(new Label("Adresse"), 0, l);
        grille.add(champAdresse, 1, l++);
        grille.add(new Label("Notes"), 0, l);
        champNotes.setPrefRowCount(3);
        grille.add(champNotes, 1, l++);
        grille.add(champActif, 1, l++);
        labelErreur.getStyleClass().add("label-erreur");
        grille.add(labelErreur, 0, l, 2, 1);

        if (existant != null) {
            champNom.setText(existant.nom);
            champContact.setText(existant.contact);
            champTelephone.setText(existant.telephone);
            champEmail.setText(existant.email);
            champAdresse.setText(existant.adresse);
            champNotes.setText(existant.notes);
            champActif.setSelected(existant.actif);
        } else {
            champActif.setSelected(true);
        }

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(460, 380);
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (champNom.getText() == null || champNom.getText().isBlank()) {
                labelErreur.setText("Le nom est obligatoire.");
                e.consume();
            }
        });

        setResultConverter(b -> {
            if (b != ok) {
                return null;
            }
            FournisseurDTO dto = new FournisseurDTO();
            if (existant != null) {
                dto.id = existant.id;
            }
            dto.nom = champNom.getText().trim();
            dto.contact = blank(champContact.getText());
            dto.telephone = blank(champTelephone.getText());
            dto.email = blank(champEmail.getText());
            dto.adresse = blank(champAdresse.getText());
            dto.notes = blank(champNotes.getText());
            dto.actif = champActif.isSelected();
            return dto;
        });
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
