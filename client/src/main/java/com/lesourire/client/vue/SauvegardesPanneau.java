package com.lesourire.client.vue;

import java.time.format.DateTimeFormatter;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceSauvegardes;
import com.lesourire.commun.dto.SauvegardeDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Onglet Sauvegardes : liste des dumps et lancement manuel. */
public class SauvegardesPanneau {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ServiceSauvegardes service;
    private final java.util.function.BiConsumer<String, Exception> surErreur;
    private final VBox racine = new VBox(12);
    private final TableView<SauvegardeDTO> tableau = new TableView<>();
    private final Label labelStatut = new Label();
    private final Button boutonLancer = new Button("Sauvegarder maintenant");

    SauvegardesPanneau(ServiceSauvegardes service,
            java.util.function.BiConsumer<String, Exception> surErreur) {
        this.service = service;
        this.surErreur = surErreur;
        construire();
        charger();
    }

    Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.setPadding(new Insets(16, 0, 0, 0));
        Label aide = new Label(
                "Les fichiers sont écrits dans le dossier défini par le paramètre sauvegarde.dossier.");
        aide.setWrapText(true);
        aide.getStyleClass().add("note-discrete");
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setTooltip(new Tooltip("Actualiser la liste"));
        actualiser.setOnAction(e -> charger());
        boutonLancer.setGraphic(new FontIcon(Material2AL.CLOUD_UPLOAD));
        boutonLancer.getStyleClass().add("bouton-principal");
        boutonLancer.setOnAction(e -> lancer());
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, aide, espace, actualiser, boutonLancer);
        actions.setAlignment(Pos.CENTER_LEFT);
        tableau.getColumns().setAll(
                colonne("Fichier", 320, SauvegardeDTO::nom),
                colonne("Date", 160, s -> s.dateModification().format(FORMAT_DATE)),
                colonne("Taille", 100, s -> formaterTaille(s.tailleOctets())));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucune sauvegarde pour l'instant."));
        VBox.setVgrow(tableau, Priority.ALWAYS);
        labelStatut.getStyleClass().add("note-discrete");
        racine.getChildren().addAll(actions, tableau, labelStatut);
    }

    private static TableColumn<SauvegardeDTO, String> colonne(String titre, double largeur,
            java.util.function.Function<SauvegardeDTO, String> extracteur) {
        TableColumn<SauvegardeDTO, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return col;
    }

    private static String formaterTaille(long octets) {
        if (octets < 1024) {
            return octets + " o";
        }
        if (octets < 1024 * 1024) {
            return String.format("%.1f Ko", octets / 1024.0);
        }
        return String.format("%.1f Mo", octets / (1024.0 * 1024.0));
    }

    private void charger() {
        labelStatut.setText("Chargement…");
        Async.executer(service::lister,
                liste -> {
                    tableau.getItems().setAll(liste);
                    labelStatut.setText(liste.size() + " sauvegarde(s)"
                            + (Session.estModeDemonstration()
                                    ? " — mode démonstration, rien n'est enregistré"
                                    : ""));
                },
                e -> {
                    labelStatut.setText("");
                    surErreur.accept("Impossible de lister les sauvegardes", e);
                });
    }

    private void lancer() {
        boutonLancer.setDisable(true);
        labelStatut.setText("Sauvegarde en cours…");
        Async.executer(service::lancer,
                ok -> {
                    boutonLancer.setDisable(false);
                    charger();
                },
                e -> {
                    boutonLancer.setDisable(false);
                    labelStatut.setText("");
                    surErreur.accept("Échec de la sauvegarde", e);
                });
    }
}
