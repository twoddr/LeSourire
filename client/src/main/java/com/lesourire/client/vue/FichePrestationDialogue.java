package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.util.List;

import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.PrestationDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Fiche de création / modification d'une prestation du tarifaire. */
public class FichePrestationDialogue extends Dialog<PrestationDTO> {

    private final TextField champCode = new TextField();
    private final TextField champLibelle = new TextField();
    private final ComboBox<CategoriePrestationDTO> champCategorie = new ComboBox<>();
    private final RadioButton radioLettre = new RadioButton("Lettre-clé × coefficient");
    private final RadioButton radioForfait = new RadioButton("Forfait");
    private final ComboBox<String> champLettre = new ComboBox<>(
            FXCollections.observableArrayList("D", "Z"));
    private final TextField champCoefficient = new TextField();
    private final TextField champForfait = new TextField();
    private final TextArea champNotes = new TextArea();
    private final CheckBox champActif = new CheckBox("Prestation active");
    private final Label labelErreur = new Label();

    public FichePrestationDialogue(PrestationDTO existante, List<CategoriePrestationDTO> categories) {
        boolean creation = existante == null || existante.id == null;
        setTitle(creation ? "Nouvelle prestation" : "Modifier " + existante.code);
        setResizable(true);

        champCategorie.setItems(FXCollections.observableArrayList(categories));
        champCategorie.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup groupe = new ToggleGroup();
        radioLettre.setToggleGroup(groupe);
        radioForfait.setToggleGroup(groupe);
        radioLettre.setSelected(true);
        radioLettre.selectedProperty().addListener((o, a, sel) -> basculerMode(sel));
        radioForfait.selectedProperty().addListener((o, a, sel) -> basculerMode(!sel));

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
        grille.add(new Label("Code"), 0, l);
        grille.add(champCode, 1, l++);
        grille.add(new Label("Libellé"), 0, l);
        grille.add(champLibelle, 1, l++);
        grille.add(new Label("Catégorie"), 0, l);
        grille.add(champCategorie, 1, l++);
        grille.add(new Label("Tarification"), 0, l);
        grille.add(new HBox(16, radioLettre, radioForfait), 1, l++);
        grille.add(new Label("Lettre-clé"), 0, l);
        grille.add(champLettre, 1, l++);
        grille.add(new Label("Coefficient"), 0, l);
        grille.add(champCoefficient, 1, l++);
        grille.add(new Label("Forfait (XAF)"), 0, l);
        grille.add(champForfait, 1, l++);
        grille.add(new Label("Notes"), 0, l);
        champNotes.setPrefRowCount(3);
        grille.add(champNotes, 1, l++);
        grille.add(champActif, 1, l++);

        labelErreur.getStyleClass().add("label-erreur");
        labelErreur.setWrapText(true);
        grille.add(labelErreur, 0, l, 2, 1);

        if (existante != null) {
            champCode.setText(existante.code);
            champLibelle.setText(existante.libelle);
            categories.stream()
                    .filter(c -> c.id().equals(existante.categorieId))
                    .findFirst()
                    .ifPresent(champCategorie::setValue);
            champNotes.setText(existante.notes);
            champActif.setSelected(existante.actif);
            if (existante.lettreCle != null) {
                radioLettre.setSelected(true);
                champLettre.setValue(existante.lettreCle);
                if (existante.coefficient != null) {
                    champCoefficient.setText(existante.coefficient.stripTrailingZeros().toPlainString());
                }
            } else {
                radioForfait.setSelected(true);
                if (existante.tarifForfait != null) {
                    champForfait.setText(existante.tarifForfait.stripTrailingZeros().toPlainString());
                }
            }
        } else {
            champLettre.setValue("D");
            champActif.setSelected(true);
            if (!categories.isEmpty()) {
                champCategorie.setValue(categories.get(0));
            }
        }
        basculerMode(radioLettre.isSelected());

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(520, 440);
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
            PrestationDTO dto = new PrestationDTO();
            if (existante != null) {
                dto.id = existante.id;
            }
            dto.code = champCode.getText().trim();
            dto.libelle = champLibelle.getText().trim();
            dto.categorieId = champCategorie.getValue().id();
            dto.notes = champNotes.getText();
            dto.actif = champActif.isSelected();
            if (radioLettre.isSelected()) {
                dto.lettreCle = champLettre.getValue();
                dto.coefficient = decimal(champCoefficient.getText());
                dto.tarifForfait = null;
            } else {
                dto.lettreCle = null;
                dto.coefficient = null;
                dto.tarifForfait = decimal(champForfait.getText());
            }
            return dto;
        });
    }

    private void basculerMode(boolean modeLettre) {
        champLettre.setDisable(!modeLettre);
        champCoefficient.setDisable(!modeLettre);
        champForfait.setDisable(modeLettre);
    }

    private String valider() {
        if (champCode.getText() == null || champCode.getText().isBlank()) {
            return "Le code est obligatoire.";
        }
        if (champLibelle.getText() == null || champLibelle.getText().isBlank()) {
            return "Le libellé est obligatoire.";
        }
        if (champCategorie.getValue() == null) {
            return "La catégorie est obligatoire.";
        }
        try {
            if (radioLettre.isSelected()) {
                if (champLettre.getValue() == null) {
                    return "Choisissez une lettre-clé.";
                }
                BigDecimal coef = decimal(champCoefficient.getText());
                if (coef == null || coef.compareTo(BigDecimal.ZERO) <= 0) {
                    return "Le coefficient doit être strictement positif.";
                }
            } else {
                BigDecimal forfait = decimal(champForfait.getText());
                if (forfait == null || forfait.compareTo(BigDecimal.ZERO) <= 0) {
                    return "Le forfait doit être strictement positif.";
                }
            }
        } catch (NumberFormatException ex) {
            return "Nombre invalide.";
        }
        return null;
    }

    private static BigDecimal decimal(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        return new BigDecimal(texte.trim().replace(',', '.').replace(" ", ""));
    }
}
