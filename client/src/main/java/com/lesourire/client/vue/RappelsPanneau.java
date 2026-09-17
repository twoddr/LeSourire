package com.lesourire.client.vue;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.lesourire.client.LeSourireClient;
import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceRappels;
import com.lesourire.client.service.ServiceRappelsApi;
import com.lesourire.client.service.ServiceRappelsDemo;
import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * File d'attente des notifications patients.
 *
 * <p>Le SMS part d'un clic (le serveur l'envoie via l'API de l'opérateur) ;
 * WhatsApp et l'e-mail sont des envois <em>assistés</em> : le lien
 * {@code wa.me} / {@code mailto:} est ouvert dans l'application du poste, puis
 * le secrétariat confirme l'envoi pour sortir la ligne de la file.</p>
 */
public final class RappelsPanneau {

    private static final DateTimeFormatter FORMAT_ECHEANCE =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ServiceRappels service;
    private final BiConsumer<String, Exception> surErreur;

    private final VBox racine = new VBox(8);
    private final ListView<RappelDTO> liste = new ListView<>();
    private final Button btnEnvoyer = new Button("Envoyer");
    private final Button btnOuvrir = new Button("Ouvrir le lien");
    private final Button btnMarquer = new Button("Marquer envoyé");
    private final Button btnAnnuler = new Button("Annuler");

    public RappelsPanneau(BiConsumer<String, Exception> surErreur) {
        boolean demo = Session.estModeDemonstration();
        this.service = demo ? new ServiceRappelsDemo() : new ServiceRappelsApi(Session.api());
        this.surErreur = surErreur;
        construire();
        charger();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        liste.setPlaceholder(new Label("Aucune notification en attente."));
        liste.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RappelDTO item, boolean vide) {
                super.updateItem(item, vide);
                if (vide || item == null) {
                    setText(null);
                    return;
                }
                String echeance = item.datePrevue == null
                        ? "" : item.datePrevue.format(FORMAT_ECHEANCE) + "  ";
                setText(echeance + item.patientNom + "\n"
                        + item.type.getLibelle() + " · " + item.canal.getLibelle()
                        + (item.tentatives > 0 ? " · " + item.tentatives + " essai(s)" : ""));
            }
        });
        liste.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> majBoutons());

        btnEnvoyer.setOnAction(e -> envoyerAutomatiquement());
        btnOuvrir.setOnAction(e -> ouvrirLien());
        btnMarquer.setOnAction(e -> marquerEnvoye());
        btnAnnuler.setOnAction(e -> annuler());
        HBox actions = new HBox(8, btnEnvoyer, btnOuvrir, btnMarquer, btnAnnuler);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox.setVgrow(liste, Priority.ALWAYS);
        racine.getChildren().setAll(liste, actions);
        majBoutons();
    }

    /** Recharge la file d'attente depuis le serveur. */
    public void charger() {
        Async.executer(service::aEnvoyer,
                rappels -> {
                    liste.getItems().setAll(rappels);
                    majBoutons();
                },
                e -> surErreur.accept("Impossible de charger les notifications", e));
    }

    private void majBoutons() {
        RappelDTO r = liste.getSelectionModel().getSelectedItem();
        boolean sms = r != null && r.canal == Rappels.Canal.SMS;
        boolean assiste = r != null && (r.canal == Rappels.Canal.WHATSAPP
                || r.canal == Rappels.Canal.EMAIL);

        btnEnvoyer.setDisable(!sms);
        btnOuvrir.setDisable(!assiste || r.lienEnvoi == null);
        btnMarquer.setDisable(!assiste);
        btnAnnuler.setDisable(r == null);

        if (r != null && r.canal == Rappels.Canal.WHATSAPP) {
            btnOuvrir.setText("Ouvrir WhatsApp");
        } else if (r != null && r.canal == Rappels.Canal.EMAIL) {
            btnOuvrir.setText("Ouvrir l'e-mail");
        } else {
            btnOuvrir.setText("Ouvrir le lien");
        }
    }

    // ------------------------------------------------------------- traitement

    private void envoyerAutomatiquement() {
        RappelDTO r = liste.getSelectionModel().getSelectedItem();
        if (r == null) {
            return;
        }
        Async.executer(() -> service.envoyer(r.id),
                envoye -> charger(),
                e -> surErreur.accept("Envoi impossible", e));
    }

    private void ouvrirLien() {
        RappelDTO r = liste.getSelectionModel().getSelectedItem();
        if (r == null) {
            return;
        }
        if (!LeSourireClient.ouvrirLien(r.lienEnvoi)) {
            surErreur.accept("Lien non ouvert", new IllegalStateException(
                    "Le poste n'a pas pu ouvrir le lien.\nAdresse à recopier :\n" + r.lienEnvoi));
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Le message a-t-il bien été envoyé à " + r.patientNom + " ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Envoi " + r.canal.getLibelle());
        Optional<ButtonType> reponse = Dialogues.afficher(confirmation,
                racine.getScene() == null ? null : racine.getScene().getWindow());
        if (reponse.isPresent() && reponse.get() == ButtonType.YES) {
            marquerEnvoye();
        }
    }

    private void marquerEnvoye() {
        RappelDTO r = liste.getSelectionModel().getSelectedItem();
        if (r == null) {
            return;
        }
        Async.executer(() -> service.marquerEnvoye(r.id),
                maj -> charger(),
                e -> surErreur.accept("Enregistrement impossible", e));
    }

    private void annuler() {
        RappelDTO r = liste.getSelectionModel().getSelectedItem();
        if (r == null) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Ne plus notifier " + r.patientNom + " ?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Annuler la notification");
        Optional<ButtonType> reponse = Dialogues.afficher(confirmation,
                racine.getScene() == null ? null : racine.getScene().getWindow());
        if (reponse.isEmpty() || reponse.get() != ButtonType.YES) {
            return;
        }
        Async.executer(() -> service.annuler(r.id),
                maj -> charger(),
                e -> surErreur.accept("Annulation impossible", e));
    }
}
