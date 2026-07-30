package com.lesourire.client.vue;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceParametres;
import com.lesourire.commun.dto.ParametreDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class ParametresPanneau {

    private final ServiceParametres service;
    private final java.util.function.BiConsumer<String, Exception> surErreur;
    private final VBox racine = new VBox(12);
    private final TableView<ParametreDTO> tableau = new TableView<>();
    private final Label labelStatut = new Label();

    ParametresPanneau(ServiceParametres service,
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
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setTooltip(new Tooltip("Actualiser"));
        actualiser.setOnAction(e -> charger());
        Label aide = new Label("Double-clic sur une ligne pour modifier la valeur.");
        aide.getStyleClass().add("note-discrete");
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, aide, espace, actualiser);
        actions.setAlignment(Pos.CENTER_LEFT);
        TableColumn<ParametreDTO, String> colCle = colonne("Clé", 220, ParametreDTO::cle);
        TableColumn<ParametreDTO, String> colValeur = colonne("Valeur", 280, ParametreDTO::valeur);
        TableColumn<ParametreDTO, String> colDesc = colonne("Description", 320,
                p -> p.description() == null ? "" : p.description());
        tableau.getColumns().setAll(colCle, colValeur, colDesc);
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucun paramètre."));
        tableau.setRowFactory(t -> {
            TableRow<ParametreDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    editer(ligne.getItem());
                }
            });
            return ligne;
        });
        VBox.setVgrow(tableau, Priority.ALWAYS);
        labelStatut.getStyleClass().add("note-discrete");
        racine.getChildren().addAll(actions, tableau, labelStatut);
    }

    private static TableColumn<ParametreDTO, String> colonne(String titre, double largeur,
            java.util.function.Function<ParametreDTO, String> extracteur) {
        TableColumn<ParametreDTO, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return col;
    }

    private void charger() {
        labelStatut.setText("Chargement…");
        Async.executer(service::lister,
                liste -> {
                    tableau.getItems().setAll(liste);
                    labelStatut.setText(liste.size() + " paramètre(s)"
                            + (Session.estModeDemonstration()
                                    ? " — mode démonstration, rien n'est enregistré"
                                    : ""));
                },
                e -> {
                    labelStatut.setText("");
                    surErreur.accept("Impossible de charger les paramètres", e);
                });
    }

    private void editer(ParametreDTO p) {
        Dialog<String> dialogue = new Dialog<>();
        dialogue.setTitle("Modifier " + p.cle());
        TextField champ = new TextField(p.valeur() == null ? "" : p.valeur());
        Label desc = new Label(p.description() == null ? "" : p.description());
        desc.setWrapText(true);
        desc.getStyleClass().add("note-discrete");
        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        grille.add(new Label("Clé"), 0, 0);
        grille.add(new Label(p.cle()), 1, 0);
        grille.add(new Label("Valeur"), 0, 1);
        grille.add(champ, 1, 1);
        grille.add(desc, 0, 2, 2, 1);
        dialogue.getDialogPane().setContent(grille);
        dialogue.getDialogPane().setPrefWidth(480);
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialogue.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialogue.setResultConverter(b -> b == ok ? champ.getText() : null);
        Dialogues.afficher(dialogue, racine.getScene().getWindow())
                .ifPresent(valeur -> Async.executer(() -> service.modifier(p.cle(), valeur),
                        maj -> charger(),
                        e -> surErreur.accept("Impossible d'enregistrer", e)));
    }
}
