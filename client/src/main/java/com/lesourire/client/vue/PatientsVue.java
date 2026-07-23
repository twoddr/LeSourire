package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.client.service.ServicePatientsApi;
import com.lesourire.client.service.ServicePatientsDemo;
import com.lesourire.commun.dto.PatientDTO;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Module Patients : recherche, liste et accès à la fiche. */
public class PatientsVue {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ServicePatients service;
    private final VBox racine = new VBox(12);
    private final TextField champRecherche = new TextField();
    private final TableView<PatientDTO> tableau = new TableView<>();
    private final Label labelStatut = new Label();

    public PatientsVue() {
        this.service = Session.estModeDemonstration()
                ? new ServicePatientsDemo()
                : new ServicePatientsApi(Session.api());
        construire();
        charger();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));

        // En-tête : titre + actions
        Label titre = new Label("Patients");
        titre.getStyleClass().add("titre-page");

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        Button boutonActualiser = new Button();
        boutonActualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        boutonActualiser.setTooltip(new Tooltip("Actualiser la liste"));
        boutonActualiser.setOnAction(e -> charger());

        Button boutonNouveau = new Button("Nouveau patient");
        boutonNouveau.setGraphic(new FontIcon(Material2MZ.PERSON_ADD));
        boutonNouveau.getStyleClass().add("bouton-principal");
        boutonNouveau.setOnAction(e -> ouvrirFiche(null));

        HBox entete = new HBox(10, titre, espace, boutonActualiser, boutonNouveau);
        entete.setAlignment(Pos.CENTER_LEFT);

        // Recherche instantanée (300 ms après la dernière frappe)
        champRecherche.setPromptText("Rechercher par nom, n° de dossier ou téléphone…");
        champRecherche.getStyleClass().add("champ-recherche");
        PauseTransition attente = new PauseTransition(Duration.millis(300));
        attente.setOnFinished(e -> charger());
        champRecherche.textProperty().addListener((obs, avant, apres) -> attente.playFromStart());

        construireTableau();
        VBox.setVgrow(tableau, Priority.ALWAYS);

        labelStatut.getStyleClass().add("note-discrete");

        racine.getChildren().addAll(entete, champRecherche, tableau, labelStatut);
    }

    private void construireTableau() {
        TableColumn<PatientDTO, String> colDossier = colonne("N° dossier", 110,
                p -> p.numeroDossier);
        TableColumn<PatientDTO, String> colNom = colonne("Nom", 160, p -> p.nom);
        TableColumn<PatientDTO, String> colPrenom = colonne("Prénom", 140, p -> p.prenom);
        TableColumn<PatientDTO, String> colNaissance = colonne("Naissance", 130, p ->
                p.dateNaissance == null ? ""
                        : p.dateNaissance.format(FORMAT_DATE) + "  (" + age(p.dateNaissance) + " ans)");
        TableColumn<PatientDTO, String> colTelephone = colonne("Téléphone", 130, p -> p.telephone);
        TableColumn<PatientDTO, String> colAssureur = colonne("Assureur", 150, p -> p.assureurActifNom);
        TableColumn<PatientDTO, String> colSociete = colonne("Société", 150, p -> p.societeActiveNom);

        tableau.getColumns().setAll(java.util.List.of(
                colDossier, colNom, colPrenom, colNaissance, colTelephone, colAssureur, colSociete));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucun patient trouvé."));

        // Ligne surlignée pour les payeurs à surveiller + double-clic pour ouvrir
        tableau.setRowFactory(t -> {
            TableRow<PatientDTO> ligne = new TableRow<>() {
                @Override
                protected void updateItem(PatientDTO item, boolean vide) {
                    super.updateItem(item, vide);
                    getStyleClass().remove("ligne-mauvais-payeur");
                    if (!vide && item != null && item.mauvaisPayeur) {
                        getStyleClass().add("ligne-mauvais-payeur");
                    }
                }
            };
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirDossier(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private TableColumn<PatientDTO, String> colonne(String titre, double largeur,
            java.util.function.Function<PatientDTO, String> extracteur) {
        TableColumn<PatientDTO, String> colonne = new TableColumn<>(titre);
        colonne.setPrefWidth(largeur);
        colonne.setCellValueFactory(donnees -> {
            String valeur = extracteur.apply(donnees.getValue());
            return new SimpleStringProperty(valeur == null ? "" : valeur);
        });
        return colonne;
    }

    private static int age(LocalDate naissance) {
        return Period.between(naissance, LocalDate.now()).getYears();
    }

    // ------------------------------------------------------------ actions

    private void charger() {
        String recherche = champRecherche.getText();
        labelStatut.setText("Chargement…");
        Async.executer(() -> service.rechercher(recherche),
                liste -> {
                    tableau.getItems().setAll(liste);
                    labelStatut.setText(liste.size() + " patient(s)"
                            + (Session.estModeDemonstration() ? " — mode démonstration, rien n'est enregistré" : ""));
                },
                e -> {
                    labelStatut.setText("");
                    afficherErreur("Impossible de charger les patients", e);
                });
    }

    /** Recharge le dossier complet (historique des couvertures) avant ouverture. */
    private void ouvrirDossier(PatientDTO ligne) {
        Async.executer(() -> service.obtenir(ligne.id),
                this::ouvrirFiche,
                e -> afficherErreur("Impossible d'ouvrir le dossier", e));
    }

    private void ouvrirFiche(PatientDTO existant) {
        FichePatientDialogue fiche = new FichePatientDialogue(existant, service);
        Dialogues.afficher(fiche, racine.getScene().getWindow()).ifPresent(saisi -> {
            boolean creation = saisi.id == null;
            Async.executer(
                    () -> creation ? service.creer(saisi) : service.modifier(saisi),
                    enregistre -> charger(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer le patient", e);
                        // Rouvre la fiche avec les données saisies pour ne rien perdre
                        ouvrirFiche(saisi);
                    });
        });
    }

    private void afficherErreur(String entete, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText(entete);
        Dialogues.afficherSansResultat(alerte, racine.getScene().getWindow());
    }
}
