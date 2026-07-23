package com.lesourire.client.vue;

import java.util.Arrays;

import com.lesourire.commun.Role;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/** Fiche de création / modification d'un membre du personnel. */
public class FicheUtilisateurDialogue extends Dialog<UtilisateurEcritureDTO> {

    private final TextField champLogin = new TextField();
    private final TextField champNom = new TextField();
    private final TextField champPrenom = new TextField();
    private final ComboBox<Role> champRole = new ComboBox<>(
            FXCollections.observableArrayList(Role.values()));
    private final TextField champEmail = new TextField();
    private final TextField champTelephone = new TextField();
    private final PasswordField champMotDePasse = new PasswordField();
    private final PasswordField champConfirmation = new PasswordField();
    private final CheckBox champActif = new CheckBox("Compte actif");
    private final Label labelErreur = new Label();
    private final boolean creation;

    public FicheUtilisateurDialogue(UtilisateurDTO existant) {
        this.creation = existant == null;
        setTitle(creation ? "Nouveau membre du personnel" : "Modifier " + existant.nomComplet());
        setResizable(true);

        champRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : role.getLibelle();
            }

            @Override
            public Role fromString(String s) {
                return Arrays.stream(Role.values())
                        .filter(r -> r.getLibelle().equals(s))
                        .findFirst()
                        .orElse(null);
            }
        });

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(c1, c2);

        int ligne = 0;
        grille.add(new Label("Identifiant"), 0, ligne);
        grille.add(champLogin, 1, ligne++);
        grille.add(new Label("Nom"), 0, ligne);
        grille.add(champNom, 1, ligne++);
        grille.add(new Label("Prénom"), 0, ligne);
        grille.add(champPrenom, 1, ligne++);
        grille.add(new Label("Rôle"), 0, ligne);
        grille.add(champRole, 1, ligne++);
        grille.add(new Label("Téléphone"), 0, ligne);
        grille.add(champTelephone, 1, ligne++);
        grille.add(new Label("E-mail"), 0, ligne);
        grille.add(champEmail, 1, ligne++);
        grille.add(new Label(creation ? "Mot de passe" : "Nouveau mot de passe"), 0, ligne);
        grille.add(champMotDePasse, 1, ligne++);
        grille.add(new Label("Confirmation"), 0, ligne);
        grille.add(champConfirmation, 1, ligne++);
        grille.add(champActif, 1, ligne++);

        if (!creation) {
            champMotDePasse.setPromptText("Laisser vide pour ne pas changer");
            champConfirmation.setPromptText("Laisser vide pour ne pas changer");
        }

        labelErreur.getStyleClass().add("label-erreur");
        labelErreur.setWrapText(true);
        grille.add(labelErreur, 0, ligne, 2, 1);

        if (existant != null) {
            champLogin.setText(existant.nomUtilisateur());
            champNom.setText(existant.nom());
            champPrenom.setText(existant.prenom());
            champRole.setValue(existant.role());
            champEmail.setText(existant.email());
            champTelephone.setText(existant.telephone());
            champActif.setSelected(existant.actif());
        } else {
            champRole.setValue(Role.SECRETAIRE);
            champActif.setSelected(true);
        }

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(480, 420);
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
            UtilisateurEcritureDTO dto = new UtilisateurEcritureDTO();
            if (existant != null) {
                dto.id = existant.id();
            }
            dto.nomUtilisateur = champLogin.getText();
            dto.nom = champNom.getText();
            dto.prenom = champPrenom.getText();
            dto.role = champRole.getValue();
            dto.email = champEmail.getText();
            dto.telephone = champTelephone.getText();
            dto.actif = champActif.isSelected();
            String mdp = champMotDePasse.getText();
            dto.motDePasse = mdp == null || mdp.isBlank() ? null : mdp;
            return dto;
        });
    }

    private String valider() {
        if (champLogin.getText() == null || champLogin.getText().isBlank()) {
            return "L'identifiant est obligatoire.";
        }
        if (champNom.getText() == null || champNom.getText().isBlank()) {
            return "Le nom est obligatoire.";
        }
        if (champRole.getValue() == null) {
            return "Le rôle est obligatoire.";
        }
        String mdp = champMotDePasse.getText();
        String conf = champConfirmation.getText();
        boolean mdpSaisi = mdp != null && !mdp.isBlank();
        boolean confSaisie = conf != null && !conf.isBlank();
        if (creation && !mdpSaisi) {
            return "Le mot de passe est obligatoire.";
        }
        if (mdpSaisi || confSaisie) {
            if (!mdpSaisi || !mdp.equals(conf)) {
                return "Le mot de passe et sa confirmation ne correspondent pas.";
            }
            if (mdp.length() < 4) {
                return "Le mot de passe doit contenir au moins 4 caractères.";
            }
        }
        return null;
    }
}
