package com.lesourire.client.vue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServiceRappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelStatsDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Historique des notifications patients déjà parties.
 *
 * <p>Le bandeau rappelle le nombre d'envois réussis (dont ceux du jour) et les
 * échecs restés sans suite ; la table liste les derniers envois, les plus
 * récents d'abord, avec le rendez-vous concerné quand il est connu.</p>
 */
public class JournalNotificationsDialogue extends Dialog<Void> {

    /** Nombre de lignes demandées au serveur : au-delà, la table devient illisible. */
    private static final int LIMITE = 200;

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ServiceRappels service;
    private final BiConsumer<String, Exception> surErreur;

    private final Label labelCompteurs = new Label("Chargement…");
    private final TableView<RappelDTO> tableau = new TableView<>();

    public JournalNotificationsDialogue(ServiceRappels service,
            BiConsumer<String, Exception> surErreur) {
        this.service = service;
        this.surErreur = surErreur;

        setTitle("Historique des notifications");
        setResizable(true);

        labelCompteurs.getStyleClass().add("sous-titre-section");
        construireTableau();

        VBox contenu = new VBox(10, labelCompteurs, tableau);
        contenu.setPadding(new Insets(4));
        VBox.setVgrow(tableau, Priority.ALWAYS);
        getDialogPane().setContent(contenu);
        getDialogPane().setPrefSize(860, 520);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setOnShown(e -> charger());
    }

    private void construireTableau() {
        tableau.getColumns().setAll(
                colonne("Envoyée le", 130, r -> formater(r.dateEnvoi)),
                colonne("Patient", 180, r -> texte(r.patientNom)),
                colonne("Motif", 110, r -> r.type == null ? "" : r.type.getLibelle()),
                colonne("Canal", 100, r -> r.canal == null ? "" : r.canal.getLibelle()),
                colonne("Destinataire", 160, r -> texte(r.destinataire)),
                colonne("Rendez-vous", 130, r -> formater(r.rdvDebut)));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucune notification envoyée pour le moment."));
        tableau.setRowFactory(tv -> {
            TableRow<RappelDTO> ligne = new TableRow<>();
            ligne.itemProperty().addListener((o, avant, item) -> {
                if (item == null || item.contenu == null || item.contenu.isBlank()) {
                    ligne.setTooltip(null);
                } else {
                    ligne.setTooltip(new Tooltip(item.contenu));
                }
            });
            return ligne;
        });
}

    private void charger() {
        labelCompteurs.setText("Chargement…");
        Async.executer(() -> new Chargement(service.journal(LIMITE), service.statistiques()),
                charge -> {
                    tableau.getItems().setAll(charge.rappels());
                    labelCompteurs.setText(compteurs(charge.stats(), charge.rappels().size()));
                },
                e -> {
                    labelCompteurs.setText("Historique indisponible");
                    surErreur.accept("Impossible de charger l'historique des notifications", e);
                });
    }

    private static String compteurs(RappelStatsDTO stats, int affichees) {
        if (stats == null) {
            return affichees + " notification(s) envoyée(s)";
        }
        StringBuilder texte = new StringBuilder();
        texte.append(stats.envoyees).append(" notification(s) envoyée(s)");
        if (stats.envoyeesAujourdhui > 0) {
            texte.append(" · ").append(stats.envoyeesAujourdhui).append(" aujourd'hui");
        }
        if (stats.echecs > 0) {
            texte.append(" · ").append(stats.echecs).append(" en échec");
        }
        if (stats.enAttente > 0) {
            texte.append(" · ").append(stats.enAttente).append(" en attente");
        }
        if (stats.derniereEnvoyeeLe != null) {
            texte.append(" · dernier envoi le ").append(stats.derniereEnvoyeeLe.format(FORMAT_DATE));
        }
        if (affichees < stats.envoyees) {
            texte.append(" · ").append(affichees).append(" plus récentes affichées");
        }
        return texte.toString();
    }

    private static TableColumn<RappelDTO, String> colonne(String titre, double largeur,
            Function<RappelDTO, String> getter) {
        TableColumn<RappelDTO, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() == null ? "" : getter.apply(c.getValue())));
        return col;
    }

    private static String formater(LocalDateTime date) {
        return date == null ? "" : date.format(FORMAT_DATE);
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    /** Résultat d'un chargement : les lignes et les compteurs, en un aller-retour. */
    private record Chargement(List<RappelDTO> rappels, RappelStatsDTO stats) {
    }
}