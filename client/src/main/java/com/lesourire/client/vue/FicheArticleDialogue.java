package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.util.List;

import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.CategorieArticleDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Fiche article de stock. */
public class FicheArticleDialogue extends Dialog<ArticleDTO> {

    private final TextField champNom = new TextField();
    private final TextField champMarque = new TextField();
    private final ComboBox<CategorieArticleDTO> champCategorie = new ComboBox<>();
    private final TextField champUnite = new TextField("unité");
    private final TextField champSeuil = new TextField("0");
    private final TextArea champNotes = new TextArea();
    private final CheckBox champActif = new CheckBox("Article actif");
    private final Label labelErreur = new Label();

    public FicheArticleDialogue(ArticleDTO existant, List<CategorieArticleDTO> categories) {
        boolean creation = existant == null || existant.id == null;
        setTitle(creation ? "Nouvel article" : "Modifier " + existant.nom);
        setResizable(true);

        champCategorie.setItems(FXCollections.observableArrayList(categories));
        champCategorie.setMaxWidth(Double.MAX_VALUE);
        champCategorie.getItems().add(0, null);

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
        grille.add(new Label("Marque"), 0, l);
        grille.add(champMarque, 1, l++);
        grille.add(new Label("Catégorie"), 0, l);
        grille.add(champCategorie, 1, l++);
        grille.add(new Label("Unité"), 0, l);
        grille.add(champUnite, 1, l++);
        grille.add(new Label("Seuil d'alerte"), 0, l);
        grille.add(champSeuil, 1, l++);
        grille.add(new Label("Notes"), 0, l);
        champNotes.setPrefRowCount(3);
        grille.add(champNotes, 1, l++);
        grille.add(champActif, 1, l++);
        labelErreur.getStyleClass().add("label-erreur");
        grille.add(labelErreur, 0, l, 2, 1);

        if (existant != null) {
            champNom.setText(existant.nom);
            champMarque.setText(existant.marque);
            categories.stream().filter(c -> c != null && c.id().equals(existant.categorieId))
                    .findFirst().ifPresent(champCategorie::setValue);
            champUnite.setText(existant.unite);
            if (existant.seuilAlerte != null) {
                champSeuil.setText(existant.seuilAlerte.stripTrailingZeros().toPlainString());
            }
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
            String err = valider();
            if (err != null) {
                labelErreur.setText(err);
                e.consume();
            }
        });

        setResultConverter(b -> {
            if (b != ok) {
                return null;
            }
            ArticleDTO dto = new ArticleDTO();
            if (existant != null) {
                dto.id = existant.id;
                dto.quantiteStock = existant.quantiteStock;
                dto.prixAchatDernier = existant.prixAchatDernier;
            }
            dto.nom = champNom.getText().trim();
            dto.marque = blank(champMarque.getText());
            CategorieArticleDTO cat = champCategorie.getValue();
            dto.categorieId = cat == null ? null : cat.id();
            dto.unite = champUnite.getText().trim();
            dto.seuilAlerte = decimal(champSeuil.getText());
            dto.notes = blank(champNotes.getText());
            dto.actif = champActif.isSelected();
            return dto;
        });
    }

    private String valider() {
        if (champNom.getText() == null || champNom.getText().isBlank()) {
            return "Le nom est obligatoire.";
        }
        try {
            BigDecimal s = decimal(champSeuil.getText());
            if (s == null || s.compareTo(BigDecimal.ZERO) < 0) {
                return "Seuil d'alerte invalide.";
            }
        } catch (NumberFormatException ex) {
            return "Seuil d'alerte invalide.";
        }
        return null;
    }

    private static BigDecimal decimal(String t) {
        if (t == null || t.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(t.trim().replace(',', '.').replace(" ", ""));
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
