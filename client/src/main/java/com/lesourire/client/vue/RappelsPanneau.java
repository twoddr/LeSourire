package com.lesourire.client.vue;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.LeSourireClient;
import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.service.ServiceRappels;
import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelStatsDTO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * File d'attente des notifications patients.
 *
 * <p>
 * Le SMS part d'un clic (le serveur l'envoie via l'API de l'opérateur) ;
 * WhatsApp et l'e-mail sont des envois <em>assistés</em> : le lien
 * {@code wa.me} / {@code mailto:} est ouvert dans l'application du poste, puis
 * le secrétariat confirme l'envoi pour sortir la ligne de la file.
 * </p>
 *
 * <p>
 * Le bandeau du haut rappelle le nombre de notifications en attente et le
 * total déjà parti ; le bouton « Historique » ouvre le journal des envois.
 * </p>
 */
public final class RappelsPanneau {

    private static final DateTimeFormatter FORMAT_ECHEANCE = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ServiceRappels service;
    private final BiConsumer<String, Exception> surErreur;

    private final VBox racine = new VBox(8);
    private final Label labelCompteurs = new Label();
    private final ListView<RappelDTO> liste = new ListView<>();
    private final Button btnEnvoyer = new Button();
    private final Button btnOuvrir = new Button();
    private final Button btnMarquer = new Button();
    private final Button btnAnnuler = new Button();

    public RappelsPanneau(ServiceRappels service, BiConsumer<String, Exception> surErreur) {
        this.service = service;
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
                        ? ""
                        : item.datePrevue.format(FORMAT_ECHEANCE) + "  ";
                String rdv = item.rdvDebut == null
                        ? ""
                        : "\nRDV du " + item.rdvDebut.format(FORMAT_ECHEANCE);
                setText(echeance + item.patientNom + "\n"
                        + item.type.getLibelle() + " · " + item.canal.getLibelle()
                        + (item.tentatives > 0 ? " · " + item.tentatives + " essai(s)" : "")
                        + rdv);
            }
        });
        liste.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> majBoutons());

        btnEnvoyer.setGraphic(new FontIcon(Material2MZ.SEND));
        btnEnvoyer.setTooltip(new Tooltip("Envoyer une notification"));
        btnEnvoyer.setOnAction(e -> envoyerAutomatiquement());
        btnOuvrir.setGraphic(new FontIcon(Material2MZ.OPEN_IN_NEW));
        btnOuvrir.setTooltip(new Tooltip("Ouvrir le lien d'envoi"));
        btnOuvrir.setOnAction(e -> ouvrirLien());
        btnMarquer.setGraphic(new FontIcon(Material2AL.CHECK));
        btnMarquer.setTooltip(new Tooltip("Marquer comme envoyé"));
        btnMarquer.setOnAction(e -> marquerEnvoye());
        btnAnnuler.setGraphic(new FontIcon(Material2AL.BLOCK));
        btnAnnuler.setTooltip(new Tooltip("Annuler la notification"));
        btnAnnuler.setOnAction(e -> annuler());
        HBox actions = new HBox(8, btnEnvoyer, btnOuvrir, btnMarquer, btnAnnuler);
        actions.setPadding(new Insets(4, 0, 0, 0));

        labelCompteurs.getStyleClass().add("note-discrete");
        labelCompteurs.setWrapText(true);
        labelCompteurs.setMinWidth(0);
        Button btnHistorique = new Button("Historique");
        btnHistorique.setGraphic(new FontIcon(Material2MZ.SCHEDULE));
        btnHistorique.setTooltip(new Tooltip("Voir les notifications déjà envoyées"));
        btnHistorique.setOnAction(e -> ouvrirHistorique());
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox entete = new HBox(8, labelCompteurs, espace, btnHistorique);
        entete.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(liste, Priority.ALWAYS);
        racine.getChildren().setAll(entete, liste, actions);
        majBoutons();
    }

    /** Recharge la file d'attente et les compteurs depuis le serveur. */
    public void charger() {
        Async.executer(() -> new Chargement(service.aEnvoyer(), service.statistiques()),
                charge -> {
                    liste.getItems().setAll(charge.rappels());
                    majCompteurs(charge.stats());
                    majBoutons();
                },
                e -> surErreur.accept("Impossible de charger les notifications", e));
    }

    /**
     * Met en avant la notification rattachée au rendez-vous sélectionné dans la
     * grille, pour faire le lien entre l'agenda et la file d'attente.
     */
    public void mettreEnEvidence(Long rdvId) {
        if (rdvId == null) {
            liste.getSelectionModel().clearSelection();
            return;
        }
        for (RappelDTO r : liste.getItems()) {
            if (rdvId.equals(r.rdvId)) {
                liste.getSelectionModel().select(r);
                liste.scrollTo(r);
                return;
            }
        }
        liste.getSelectionModel().clearSelection();
    }

    /** Journal des envois déjà partis (compteurs + dernières lignes). */
    private void ouvrirHistorique() {
        Dialogues.afficherSansResultat(new JournalNotificationsDialogue(service, surErreur),
                racine.getScene() == null ? null : racine.getScene().getWindow());
    }

    private void majCompteurs(RappelStatsDTO stats) {
        if (stats == null) {
            labelCompteurs.setText("");
            return;
        }
        labelCompteurs.setText(stats.enAttente + " en attente  ·  "
                + stats.envoyees + " envoyées");
    }

    /** Résultat d'un chargement : la file et les compteurs, en un aller-retour. */
    private record Chargement(List<RappelDTO> rappels, RappelStatsDTO stats) {
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
            btnOuvrir.setText("Ouvrir e-mail");
        } else {
            btnOuvrir.setText("Ouvrir lien");
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
