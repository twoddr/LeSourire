package com.lesourire.client.vue;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.dto.PatientDTO;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Recherche et sélection d'un patient (pour démarrer une facture). */
public class ChoixPatientDialogue extends Dialog<PatientDTO> {

    private final ServicePatients service;
    private final TextField champRecherche = new TextField();
    private final ListView<PatientDTO> liste = new ListView<>();

    public ChoixPatientDialogue(ServicePatients service) {
        this.service = service;
        setTitle("Choisir un patient");
        setHeaderText("Pour qui cette facture est-elle établie ?");
        setResizable(true);

        champRecherche.setPromptText("Nom, prénom, n° de dossier ou téléphone…");
        PauseTransition attente = new PauseTransition(Duration.millis(250));
        champRecherche.textProperty().addListener((obs, avant, apres) -> {
            attente.setOnFinished(e -> charger());
            attente.playFromStart();
        });

        liste.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(PatientDTO patient, boolean vide) {
                super.updateItem(patient, vide);
                setText(vide || patient == null ? null
                        : patient.numeroDossier + "  —  " + patient.nomComplet()
                                + (patient.telephone == null ? "" : "  (" + patient.telephone + ")"));
            }
        });
        liste.setPlaceholder(new Label("Aucun patient trouvé."));
        liste.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && liste.getSelectionModel().getSelectedItem() != null) {
                setResult(liste.getSelectionModel().getSelectedItem());
                close();
            }
        });

        VBox boite = new VBox(10, champRecherche, liste);
        boite.setPadding(new Insets(14));
        VBox.setVgrow(liste, Priority.ALWAYS);

        getDialogPane().setContent(boite);
        getDialogPane().setPrefSize(520, 420);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Choisir");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (liste.getSelectionModel().getSelectedItem() == null) {
                e.consume();
            }
        });
        setResultConverter(bouton -> bouton == ButtonType.OK
                ? liste.getSelectionModel().getSelectedItem()
                : null);

        charger();
    }

    private void charger() {
        String q = champRecherche.getText();
        Async.executer(() -> service.rechercher(q),
                resultats -> liste.getItems().setAll(resultats),
                e -> {
                });
    }
}
