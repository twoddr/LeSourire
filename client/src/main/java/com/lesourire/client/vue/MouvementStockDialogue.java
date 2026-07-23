package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.lesourire.commun.TypeMouvementStock;
import com.lesourire.commun.dto.ArticleDTO;
import com.lesourire.commun.dto.FournisseurDTO;
import com.lesourire.commun.dto.MouvementStockDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/** Saisie d'un mouvement de stock (entrée / sortie / ajustement / péremption). */
public class MouvementStockDialogue extends Dialog<MouvementStockDTO> {

    private final ComboBox<TypeMouvementStock> champType = new ComboBox<>(
            FXCollections.observableArrayList(TypeMouvementStock.values()));
    private final TextField champQuantite = new TextField();
    private final TextField champPrix = new TextField();
    private final ComboBox<FournisseurDTO> champFournisseur = new ComboBox<>();
    private final DatePicker champPeremption = new DatePicker();
    private final TextField champReference = new TextField();
    private final Label labelErreur = new Label();

    public MouvementStockDialogue(ArticleDTO article, List<FournisseurDTO> fournisseurs) {
        setTitle("Mouvement — " + article.nom);
        setResizable(true);

        champType.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeMouvementStock t) {
                return t == null ? "" : t.getLibelle();
            }

            @Override
            public TypeMouvementStock fromString(String s) {
                return null;
            }
        });
        champType.setValue(TypeMouvementStock.ENTREE);

        champFournisseur.setItems(FXCollections.observableArrayList(fournisseurs));
        champFournisseur.getItems().add(0, null);
        champFournisseur.setMaxWidth(Double.MAX_VALUE);

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
        grille.add(new Label("Type"), 0, l);
        grille.add(champType, 1, l++);
        grille.add(new Label("Quantité"), 0, l);
        grille.add(champQuantite, 1, l++);
        grille.add(new Label("Prix unitaire (XAF)"), 0, l);
        grille.add(champPrix, 1, l++);
        grille.add(new Label("Fournisseur"), 0, l);
        grille.add(champFournisseur, 1, l++);
        grille.add(new Label("Péremption"), 0, l);
        grille.add(champPeremption, 1, l++);
        grille.add(new Label("Référence"), 0, l);
        grille.add(champReference, 1, l++);
        Label aide = new Label("Stock actuel : "
                + article.quantiteStock.stripTrailingZeros().toPlainString()
                + " " + article.unite
                + " — pour un ajustement, une quantité négative diminue le stock.");
        aide.setWrapText(true);
        aide.getStyleClass().add("note-discrete");
        grille.add(aide, 0, l++, 2, 1);
        labelErreur.getStyleClass().add("label-erreur");
        grille.add(labelErreur, 0, l, 2, 1);

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(480, 360);
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
            MouvementStockDTO dto = new MouvementStockDTO();
            dto.articleId = article.id;
            dto.type = champType.getValue();
            dto.quantite = decimal(champQuantite.getText());
            dto.prixUnitaire = blankDecimal(champPrix.getText());
            FournisseurDTO f = champFournisseur.getValue();
            dto.fournisseurId = f == null ? null : f.id;
            LocalDate peremption = champPeremption.getValue();
            dto.datePeremption = peremption;
            dto.reference = blank(champReference.getText());
            return dto;
        });
    }

    private String valider() {
        if (champType.getValue() == null) {
            return "Le type est obligatoire.";
        }
        try {
            BigDecimal q = decimal(champQuantite.getText());
            if (q == null || q.compareTo(BigDecimal.ZERO) == 0) {
                return "La quantité ne peut pas être nulle.";
            }
            if (champType.getValue() != TypeMouvementStock.AJUSTEMENT
                    && q.compareTo(BigDecimal.ZERO) < 0) {
                return "La quantité doit être positive (sauf ajustement).";
            }
        } catch (NumberFormatException ex) {
            return "Quantité invalide.";
        }
        try {
            blankDecimal(champPrix.getText());
        } catch (NumberFormatException ex) {
            return "Prix invalide.";
        }
        return null;
    }

    private static BigDecimal decimal(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        return new BigDecimal(t.trim().replace(',', '.').replace(" ", ""));
    }

    private static BigDecimal blankDecimal(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        return decimal(t);
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
