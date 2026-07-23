package com.lesourire.client.vue;

import java.math.BigDecimal;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServiceTarifaire;
import com.lesourire.client.service.ServiceTarifaireApi;
import com.lesourire.client.service.ServiceTarifaireDemo;
import com.lesourire.client.service.ServiceTiersPayants;
import com.lesourire.client.service.ServiceTiersPayantsApi;
import com.lesourire.client.service.ServiceTiersPayantsDemo;
import com.lesourire.client.service.ServiceUtilisateurs;
import com.lesourire.client.service.ServiceUtilisateursApi;
import com.lesourire.client.service.ServiceUtilisateursDemo;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Module Administration : personnel, tiers payants, tarifaire…
 * Organisé en onglets ; chaque onglet gère un référentiel.
 */
public class AdministrationVue {

    private final VBox racine = new VBox();
    private final ServiceUtilisateurs serviceUtilisateurs;
    private final ServiceTiersPayants serviceTiersPayants;
    private final ServiceTarifaire serviceTarifaire;

    // Personnel
    private final TextField champRecherchePersonnel = new TextField();
    private final CheckBox caseInactifsPersonnel = new CheckBox("Afficher les comptes inactifs");
    private final TableView<UtilisateurDTO> tableauPersonnel = new TableView<>();
    private final Label labelStatutPersonnel = new Label();

    // Tiers payants
    private final CheckBox caseInactifsTiers = new CheckBox("Afficher les inactifs");
    private final TableView<AssureurDTO> tableauAssureurs = new TableView<>();
    private final TableView<SocieteDTO> tableauSocietes = new TableView<>();
    private final Label labelStatutAssureurs = new Label();
    private final Label labelStatutSocietes = new Label();

    public AdministrationVue() {
        boolean demo = Session.estModeDemonstration();
        this.serviceUtilisateurs = demo
                ? new ServiceUtilisateursDemo()
                : new ServiceUtilisateursApi(Session.api());
        this.serviceTiersPayants = demo
                ? new ServiceTiersPayantsDemo()
                : new ServiceTiersPayantsApi(Session.api());
        this.serviceTarifaire = demo
                ? new ServiceTarifaireDemo()
                : new ServiceTarifaireApi(Session.api());
        construire();
        chargerPersonnel();
        chargerAssureurs();
        chargerSocietes();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));
        VBox.setVgrow(racine, Priority.ALWAYS);

        Label titre = new Label("Administration");
        titre.getStyleClass().add("titre-page");

        TabPane onglets = new TabPane(
                new Tab("Personnel", construireOngletPersonnel()),
                new Tab("Tiers payants", construireOngletTiersPayants()),
                new Tab("Tarifaire",
                        new TarifairePanneau(serviceTarifaire, this::afficherErreur).getRacine()));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(onglets, Priority.ALWAYS);

        racine.getChildren().setAll(titre, onglets);
        VBox.setMargin(onglets, new Insets(16, 0, 0, 0));
    }

    // -------------------------------------------------------------- Personnel

    private Node construireOngletPersonnel() {
        Button boutonActualiser = new Button();
        boutonActualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        boutonActualiser.setTooltip(new Tooltip("Actualiser la liste"));
        boutonActualiser.setOnAction(e -> chargerPersonnel());

        Button boutonNouveau = new Button("Nouveau membre");
        boutonNouveau.setGraphic(new FontIcon(Material2MZ.PERSON_ADD));
        boutonNouveau.getStyleClass().add("bouton-principal");
        boutonNouveau.setOnAction(e -> ouvrirFichePersonnel(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, boutonActualiser, boutonNouveau);
        actions.setAlignment(Pos.CENTER_RIGHT);

        champRecherchePersonnel.setPromptText("Rechercher par identifiant, nom ou prénom…");
        champRecherchePersonnel.getStyleClass().add("champ-recherche");
        PauseTransition attente = new PauseTransition(Duration.millis(300));
        attente.setOnFinished(e -> chargerPersonnel());
        champRecherchePersonnel.textProperty().addListener((o, a, n) -> attente.playFromStart());
        caseInactifsPersonnel.setSelected(true);
        caseInactifsPersonnel.setOnAction(e -> chargerPersonnel());

        construireTableauPersonnel();
        VBox.setVgrow(tableauPersonnel, Priority.ALWAYS);
        labelStatutPersonnel.getStyleClass().add("note-discrete");

        VBox page = new VBox(12, actions, champRecherchePersonnel, caseInactifsPersonnel,
                tableauPersonnel, labelStatutPersonnel);
        page.setPadding(new Insets(16, 0, 0, 0));
        return page;
    }

    private void construireTableauPersonnel() {
        tableauPersonnel.getColumns().setAll(
                colonne("Identifiant", 120, UtilisateurDTO::nomUtilisateur),
                colonne("Nom", 140, UtilisateurDTO::nom),
                colonne("Prénom", 120, u -> u.prenom() == null ? "" : u.prenom()),
                colonne("Rôle", 130, u -> u.role().getLibelle()),
                colonne("Téléphone", 120, u -> u.telephone() == null ? "" : u.telephone()),
                colonne("Statut", 90, u -> u.actif() ? "Actif" : "Inactif"));
        tableauPersonnel.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauPersonnel.setPlaceholder(new Label("Aucun membre du personnel."));
        tableauPersonnel.setRowFactory(t -> {
            TableRow<UtilisateurDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFichePersonnel(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void chargerPersonnel() {
        String recherche = champRecherchePersonnel.getText();
        boolean inactifs = caseInactifsPersonnel.isSelected();
        labelStatutPersonnel.setText("Chargement…");
        Async.executer(() -> serviceUtilisateurs.rechercher(recherche, inactifs),
                liste -> {
                    tableauPersonnel.getItems().setAll(liste);
                    labelStatutPersonnel.setText(liste.size() + " membre(s)" + suffixeDemo());
                },
                e -> {
                    labelStatutPersonnel.setText("");
                    afficherErreur("Impossible de charger le personnel", e);
                });
    }

    private void ouvrirFichePersonnel(UtilisateurDTO existant) {
        FicheUtilisateurDialogue fiche = new FicheUtilisateurDialogue(existant);
        Dialogues.afficher(fiche, racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = existant == null;
            Async.executer(
                    () -> creation
                            ? serviceUtilisateurs.creer(saisie)
                            : serviceUtilisateurs.modifier(existant.id(), saisie),
                    ok -> chargerPersonnel(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer", e);
                        UtilisateurEcritureDTO s = saisie;
                        UtilisateurDTO fantome = new UtilisateurDTO(
                                existant == null ? null : existant.id(),
                                s.nomUtilisateur, s.nom, s.prenom, s.role, s.email,
                                s.telephone, s.actif);
                        ouvrirFichePersonnel(fantome);
                    });
        });
    }

    // ---------------------------------------------------------- Tiers payants

    private Node construireOngletTiersPayants() {
        caseInactifsTiers.setSelected(true);
        caseInactifsTiers.setOnAction(e -> {
            chargerAssureurs();
            chargerSocietes();
        });

        TabPane sousOnglets = new TabPane(
                new Tab("Assureurs", construirePanneauAssureurs()),
                new Tab("Sociétés", construirePanneauSocietes()));
        sousOnglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(sousOnglets, Priority.ALWAYS);

        VBox page = new VBox(12, caseInactifsTiers, sousOnglets);
        page.setPadding(new Insets(16, 0, 0, 0));
        return page;
    }

    private Node construirePanneauAssureurs() {
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerAssureurs());

        Button nouveau = new Button("Nouvel assureur");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFicheAssureur(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, actualiser, nouveau);
        actions.setAlignment(Pos.CENTER_RIGHT);

        construireTableauAssureurs();
        VBox.setVgrow(tableauAssureurs, Priority.ALWAYS);
        labelStatutAssureurs.getStyleClass().add("note-discrete");

        return new VBox(12, actions, tableauAssureurs, labelStatutAssureurs);
    }

    private Node construirePanneauSocietes() {
        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerSocietes());

        Button nouveau = new Button("Nouvelle société");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFicheSociete(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);
        HBox actions = new HBox(10, espace, actualiser, nouveau);
        actions.setAlignment(Pos.CENTER_RIGHT);

        construireTableauSocietes();
        VBox.setVgrow(tableauSocietes, Priority.ALWAYS);
        labelStatutSocietes.getStyleClass().add("note-discrete");

        return new VBox(12, actions, tableauSocietes, labelStatutSocietes);
    }

    private void construireTableauAssureurs() {
        tableauAssureurs.getColumns().setAll(
                colonneA("Nom", 220, AssureurDTO::nom),
                colonneA("Téléphone", 120, a -> a.telephone() == null ? "" : a.telephone()),
                colonneA("E-mail", 180, a -> a.email() == null ? "" : a.email()),
                colonneA("% défaut", 90, a -> formatPourcent(a.pourcentageDefaut())),
                colonneA("Statut", 90, a -> a.actif() ? "Actif" : "Inactif"));
        tableauAssureurs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauAssureurs.setPlaceholder(new Label("Aucun assureur."));
        tableauAssureurs.setRowFactory(t -> {
            TableRow<AssureurDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFicheAssureur(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void construireTableauSocietes() {
        tableauSocietes.getColumns().setAll(
                colonneS("Nom", 220, SocieteDTO::nom),
                colonneS("Téléphone", 120, s -> s.telephone() == null ? "" : s.telephone()),
                colonneS("E-mail", 180, s -> s.email() == null ? "" : s.email()),
                colonneS("% défaut", 90, s -> formatPourcent(s.pourcentageDefaut())),
                colonneS("Statut", 90, s -> s.actif() ? "Actif" : "Inactif"));
        tableauSocietes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableauSocietes.setPlaceholder(new Label("Aucune société."));
        tableauSocietes.setRowFactory(t -> {
            TableRow<SocieteDTO> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFicheSociete(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void chargerAssureurs() {
        boolean inactifs = caseInactifsTiers.isSelected();
        labelStatutAssureurs.setText("Chargement…");
        Async.executer(() -> serviceTiersPayants.listerAssureurs(inactifs),
                liste -> {
                    tableauAssureurs.getItems().setAll(liste);
                    labelStatutAssureurs.setText(liste.size() + " assureur(s)" + suffixeDemo());
                },
                e -> {
                    labelStatutAssureurs.setText("");
                    afficherErreur("Impossible de charger les assureurs", e);
                });
    }

    private void chargerSocietes() {
        boolean inactifs = caseInactifsTiers.isSelected();
        labelStatutSocietes.setText("Chargement…");
        Async.executer(() -> serviceTiersPayants.listerSocietes(inactifs),
                liste -> {
                    tableauSocietes.getItems().setAll(liste);
                    labelStatutSocietes.setText(liste.size() + " société(s)" + suffixeDemo());
                },
                e -> {
                    labelStatutSocietes.setText("");
                    afficherErreur("Impossible de charger les sociétés", e);
                });
    }

    private void ouvrirFicheAssureur(AssureurDTO existant) {
        Dialogues.afficher(FicheTiersPayantDialogue.assureur(existant),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            AssureurDTO dto = new AssureurDTO(
                    existant == null ? null : existant.id(),
                    saisie.nom(), saisie.telephone(), saisie.email(),
                    saisie.pourcentage(), saisie.actif());
            Async.executer(
                    () -> existant == null
                            ? serviceTiersPayants.creerAssureur(dto)
                            : serviceTiersPayants.modifierAssureur(existant.id(), dto),
                    ok -> chargerAssureurs(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer l'assureur", e);
                        ouvrirFicheAssureur(dto);
                    });
        });
    }

    private void ouvrirFicheSociete(SocieteDTO existante) {
        Dialogues.afficher(FicheTiersPayantDialogue.societe(existante),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            SocieteDTO dto = new SocieteDTO(
                    existante == null ? null : existante.id(),
                    saisie.nom(), saisie.telephone(), saisie.email(),
                    saisie.pourcentage(), saisie.actif());
            Async.executer(
                    () -> existante == null
                            ? serviceTiersPayants.creerSociete(dto)
                            : serviceTiersPayants.modifierSociete(existante.id(), dto),
                    ok -> chargerSocietes(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer la société", e);
                        ouvrirFicheSociete(dto);
                    });
        });
    }

    // ----------------------------------------------------------------- utils

    private static String formatPourcent(BigDecimal p) {
        if (p == null) {
            return "";
        }
        return p.stripTrailingZeros().toPlainString() + " %";
    }

    private static String suffixeDemo() {
        return Session.estModeDemonstration()
                ? " — mode démonstration, rien n'est enregistré" : "";
    }

    private static <T> TableColumn<T, String> colonne(String titre, double largeur,
            java.util.function.Function<T, String> extracteur) {
        TableColumn<T, String> col = new TableColumn<>(titre);
        col.setPrefWidth(largeur);
        col.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return col;
    }

    private TableColumn<AssureurDTO, String> colonneA(String titre, double largeur,
            java.util.function.Function<AssureurDTO, String> extracteur) {
        return colonne(titre, largeur, extracteur);
    }

    private TableColumn<SocieteDTO, String> colonneS(String titre, double largeur,
            java.util.function.Function<SocieteDTO, String> extracteur) {
        return colonne(titre, largeur, extracteur);
    }

    void afficherErreur(String entete, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText(entete);
        Dialogues.afficherSansResultat(alerte, racine.getScene().getWindow());
    }
}
