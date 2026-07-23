package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceTarifaire;
import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
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
import javafx.util.Duration;

/** Onglet Tarifaire de l'Administration : lettres-clés D/Z et prestations. */
final class TarifairePanneau {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ServiceTarifaire service;
    private final java.util.function.BiConsumer<String, Exception> surErreur;
    private final VBox racine = new VBox(16);

    private final Label labelValeurD = new Label("D = …");
    private final Label labelValeurZ = new Label("Z = …");
    private final TextField champRecherche = new TextField();
    private final CheckBox caseInactifs = new CheckBox("Afficher les prestations inactives");
    private final TableView<PrestationDTO> tableau = new TableView<>();
    private final Label labelStatut = new Label();
    private List<CategoriePrestationDTO> categories = new ArrayList<>();

    TarifairePanneau(ServiceTarifaire service,
            java.util.function.BiConsumer<String, Exception> surErreur) {
        this.service = service;
        this.surErreur = surErreur;
        construire();
        chargerValeurs();
        chargerCategoriesPuisListe();
    }

    Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.setPadding(new Insets(16, 0, 0, 0));

        // Lettres-clés
        Label titreLettres = new Label("Lettres-clés en vigueur");
        titreLettres.getStyleClass().add("sous-titre-section");
        labelValeurD.getStyleClass().add("badge-lettre");
        labelValeurZ.getStyleClass().add("badge-lettre");

        Button btnChangerD = new Button("Modifier D…");
        btnChangerD.setOnAction(e -> changerValeur("D"));
        Button btnChangerZ = new Button("Modifier Z…");
        btnChangerZ.setOnAction(e -> changerValeur("Z"));

        HBox bandeauLettres = new HBox(16, labelValeurD, btnChangerD, labelValeurZ, btnChangerZ);
        bandeauLettres.setAlignment(Pos.CENTER_LEFT);

        // Prestations
        Label titrePrestations = new Label("Prestations");
        titrePrestations.getStyleClass().add("sous-titre-section");

        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setTooltip(new Tooltip("Actualiser"));
        actualiser.setOnAction(e -> chargerListe());

        Button nouveau = new Button("Nouvelle prestation");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFiche(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, titrePrestations, espace, actualiser, nouveau);
        actions.setAlignment(Pos.CENTER_LEFT);

        champRecherche.setPromptText("Rechercher par code ou libellé…");
        champRecherche.getStyleClass().add("champ-recherche");
        PauseTransition attente = new PauseTransition(Duration.millis(300));
        attente.setOnFinished(e -> chargerListe());
        champRecherche.textProperty().addListener((o, a, n) -> attente.playFromStart());
        caseInactifs.setSelected(true);
        caseInactifs.setOnAction(e -> chargerListe());

        construireTableau();
        VBox.setVgrow(tableau, Priority.ALWAYS);
        labelStatut.getStyleClass().add("note-discrete");

        racine.getChildren().addAll(
                titreLettres, bandeauLettres,
                actions, champRecherche, caseInactifs, tableau, labelStatut);
    }

    private void construireTableau() {
        tableau.getColumns().setAll(
                col("Code", 110, p -> p.code),
                col("Libellé", 260, p -> p.libelle),
                col("Catégorie", 160, p -> p.categorieLibelle),
                col("Tarif", 120, PrestationDTO::tarifLibelle),
                col("Statut", 80, p -> p.actif ? "Active" : "Inactive"));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucune prestation."));
        tableau.setRowFactory(t -> {
            TableRow<PrestationDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFiche(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private static TableColumn<PrestationDTO, String> col(String titre, double largeur,
            java.util.function.Function<PrestationDTO, String> extracteur) {
        TableColumn<PrestationDTO, String> c = new TableColumn<>(titre);
        c.setPrefWidth(largeur);
        c.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return c;
    }

    private void chargerValeurs() {
        Async.executer(service::valeursEnVigueur,
                liste -> {
                    labelValeurD.setText("D = ?");
                    labelValeurZ.setText("Z = ?");
                    for (ValeurLettreCleDTO v : liste) {
                        String texte = v.lettreCle() + " = "
                                + v.valeur().stripTrailingZeros().toPlainString() + " XAF"
                                + " (depuis " + v.dateDebut().format(FORMAT_DATE) + ")";
                        if ("D".equals(v.lettreCle())) {
                            labelValeurD.setText(texte);
                        } else if ("Z".equals(v.lettreCle())) {
                            labelValeurZ.setText(texte);
                        }
                    }
                },
                e -> surErreur.accept("Impossible de charger les lettres-clés", e));
    }

    private void chargerCategoriesPuisListe() {
        Async.executer(service::categories,
                cats -> {
                    categories = cats;
                    chargerListe();
                },
                e -> surErreur.accept("Impossible de charger les catégories", e));
    }

    private void chargerListe() {
        String q = champRecherche.getText();
        boolean inactifs = caseInactifs.isSelected();
        labelStatut.setText("Chargement…");
        Async.executer(() -> service.rechercher(q, inactifs),
                liste -> {
                    tableau.getItems().setAll(liste);
                    labelStatut.setText(liste.size() + " prestation(s)"
                            + (Session.estModeDemonstration()
                                    ? " — mode démonstration, rien n'est enregistré" : ""));
                },
                e -> {
                    labelStatut.setText("");
                    surErreur.accept("Impossible de charger les prestations", e);
                });
    }

    private void ouvrirFiche(PrestationDTO existante) {
        if (categories.isEmpty()) {
            surErreur.accept("Catégories manquantes",
                    new IllegalStateException("Aucune catégorie de prestation disponible."));
            return;
        }
        FichePrestationDialogue fiche = new FichePrestationDialogue(existante, categories);
        Dialogues.afficher(fiche, racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = existante == null || existante.id == null;
            Async.executer(
                    () -> creation ? service.creer(saisie) : service.modifier(existante.id, saisie),
                    ok -> chargerListe(),
                    e -> {
                        surErreur.accept("Impossible d'enregistrer la prestation", e);
                        ouvrirFiche(saisie);
                    });
        });
    }

    private void changerValeur(String lettre) {
        Dialog<NouvelleValeurLettreDTO> dialogue = new Dialog<>();
        dialogue.setTitle("Nouvelle valeur de " + lettre);
        TextField champValeur = new TextField();
        DatePicker champDate = new DatePicker(LocalDate.now());
        Label erreur = new Label();
        erreur.getStyleClass().add("label-erreur");

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        grille.add(new Label("Nouvelle valeur (XAF)"), 0, 0);
        grille.add(champValeur, 1, 0);
        grille.add(new Label("À partir du"), 0, 1);
        grille.add(champDate, 1, 1);
        grille.add(erreur, 0, 2, 2, 1);

        dialogue.getDialogPane().setContent(grille);
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialogue.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialogue.getDialogPane().lookupButton(ok).addEventFilter(
                javafx.event.ActionEvent.ACTION, e -> {
                    try {
                        BigDecimal v = new BigDecimal(
                                champValeur.getText().trim().replace(',', '.').replace(" ", ""));
                        if (v.compareTo(BigDecimal.ZERO) <= 0) {
                            erreur.setText("La valeur doit être strictement positive.");
                            e.consume();
                        }
                    } catch (Exception ex) {
                        erreur.setText("Valeur invalide.");
                        e.consume();
                    }
                    if (champDate.getValue() == null) {
                        erreur.setText("La date est obligatoire.");
                        e.consume();
                    }
                });
        dialogue.setResultConverter(b -> {
            if (b != ok) {
                return null;
            }
            return new NouvelleValeurLettreDTO(
                    new BigDecimal(champValeur.getText().trim().replace(',', '.').replace(" ", "")),
                    champDate.getValue());
        });

        Dialogues.afficher(dialogue, racine.getScene().getWindow()).ifPresent(dto ->
                Async.executer(() -> service.changerValeur(lettre, dto),
                        okRes -> chargerValeurs(),
                        e -> surErreur.accept("Impossible de modifier " + lettre, e)));
    }
}
