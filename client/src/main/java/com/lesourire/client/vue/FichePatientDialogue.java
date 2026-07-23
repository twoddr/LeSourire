package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.PatientDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Fiche patient (création ou modification) : identité et contacts,
 * couvertures par les tiers payants, informations médicales.
 *
 * Couvertures : en création, elles sont accumulées localement et envoyées
 * avec le patient ; en modification, chaque ajout/clôture est appliqué
 * immédiatement sur le serveur.
 */
public class FichePatientDialogue extends Dialog<PatientDTO> {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PatientDTO patient;
    private final ServicePatients service;
    private final boolean creation;

    // Identité & contacts
    private final TextField champNom = new TextField();
    private final TextField champPrenom = new TextField();
    private final DatePicker champNaissance = new DatePicker();
    private final ComboBox<String> champSexe = new ComboBox<>(
            FXCollections.observableArrayList("", "M", "F"));
    private final TextField champTelephone = new TextField();
    private final TextField champWhatsapp = new TextField();
    private final TextField champEmail = new TextField();
    private final TextField champProfession = new TextField();
    private final TextField champAdresse = new TextField();
    private final TextField champQuartier = new TextField();
    private final TextField champVille = new TextField("Douala");
    private final TextField champUrgenceNom = new TextField();
    private final TextField champUrgenceTel = new TextField();

    // Couvertures
    private final TableView<CouvertureDTO> tableCouvertures = new TableView<>();
    private final CheckBox champMauvaisPayeur = new CheckBox("Payeur à surveiller");

    // Médical
    private final TextArea champAntecedents = new TextArea();
    private final TextArea champAllergies = new TextArea();
    private final TextArea champNotes = new TextArea();
    private final CheckBox champActif = new CheckBox("Dossier actif");

    public FichePatientDialogue(PatientDTO existant, ServicePatients service) {
        this.service = service;
        this.patient = existant != null ? existant : new PatientDTO();
        this.creation = existant == null;

        setTitle(creation ? "Nouveau patient"
                : "Dossier " + patient.numeroDossier + " — " + patient.nomComplet());
        setResizable(true);

        TabPane onglets = new TabPane(
                new Tab("Identité & contacts", construireOngletIdentite()),
                new Tab("Prise en charge", construireOngletCouvertures()),
                new Tab("Médical & notes", construireOngletMedical()));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        onglets.setPrefSize(720, 480);

        getDialogPane().setContent(onglets);
        // Taille sur le DialogPane (pas seulement le contenu) : sinon le Stage
        // s'ouvre souvent trop petit, avant que le TabPane soit correctement mesuré.
        getDialogPane().setPrefSize(740, 520);
        getDialogPane().setMinWidth(640);
        getDialogPane().setMinHeight(420);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Enregistrer");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (!validerSaisie()) {
                e.consume();
            }
        });

        remplirDepuis(patient);
        setOnShown(e -> {
            if (getDialogPane().getScene() != null
                    && getDialogPane().getScene().getWindow() != null) {
                getDialogPane().getScene().getWindow().sizeToScene();
            }
            champNom.requestFocus();
        });

        setResultConverter(bouton ->
                bouton == ButtonType.OK ? construireResultat() : null);
    }

    // ------------------------------------------------------------ onglets

    private GridPane construireOngletIdentite() {
        GridPane grille = nouvelleGrille();
        int ligne = 0;
        champ(grille, ligne, 0, "Nom *", champNom);
        champ(grille, ligne++, 2, "Prénom", champPrenom);
        champ(grille, ligne, 0, "Date de naissance", champNaissance);
        champ(grille, ligne++, 2, "Sexe", champSexe);
        champ(grille, ligne, 0, "Téléphone", champTelephone);
        champ(grille, ligne++, 2, "WhatsApp (si différent)", champWhatsapp);
        champ(grille, ligne, 0, "E-mail", champEmail);
        champ(grille, ligne++, 2, "Profession", champProfession);
        champ(grille, ligne, 0, "Adresse", champAdresse);
        champ(grille, ligne++, 2, "Quartier", champQuartier);
        champ(grille, ligne++, 0, "Ville", champVille);
        champ(grille, ligne, 0, "Contact d'urgence", champUrgenceNom);
        champ(grille, ligne, 2, "Tél. urgence", champUrgenceTel);

        champNaissance.setPromptText("jj/mm/aaaa");
        champSexe.getSelectionModel().selectFirst();
        return grille;
    }

    private VBox construireOngletCouvertures() {
        construireTableCouvertures();

        Button boutonAjouter = new Button("Ajouter une couverture");
        boutonAjouter.setOnAction(e -> ajouterCouverture());

        Button boutonCloturer = new Button(creation ? "Retirer" : "Clôturer");
        boutonCloturer.setOnAction(e -> cloturerOuRetirer());
        boutonCloturer.disableProperty().bind(
                tableCouvertures.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(8, boutonAjouter, boutonCloturer);

        Label aide = new Label(creation
                ? "Les couvertures seront créées avec le dossier."
                : "L'historique est conservé : une couverture ne se modifie pas, "
                        + "elle se clôture (avec motif) et une nouvelle est créée. "
                        + "Les ajouts et clôtures sont appliqués immédiatement.");
        aide.getStyleClass().add("note-discrete");
        aide.setWrapText(true);

        VBox boite = new VBox(10, tableCouvertures, actions, champMauvaisPayeur, aide);
        boite.setPadding(new Insets(18));
        VBox.setVgrow(tableCouvertures, Priority.ALWAYS);
        return boite;
    }

    private void construireTableCouvertures() {
        tableCouvertures.getColumns().setAll(java.util.List.of(
                colonne("Type", 90, c -> CouvertureDTO.TYPE_ASSUREUR.equals(c.type)
                        ? "Assureur" : "Société"),
                colonne("Tiers payant", 150, c -> c.payeurNom),
                colonne("N° assuré", 100, c -> c.numeroAssure),
                colonne("%", 60, c -> c.pourcentageEffectif == null
                        ? "" : c.pourcentageEffectif.stripTrailingZeros().toPlainString() + " %"),
                colonne("Début", 90, c -> formater(c.dateDebut)),
                colonne("Fin", 90, c -> formater(c.dateFin)),
                colonne("Motif de fin", 140, c -> c.motifFin)));
        tableCouvertures.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableCouvertures.setPlaceholder(new Label("Aucune couverture : le patient paie tout lui-même."));
        tableCouvertures.setPrefHeight(200);

        // Les couvertures clôturées apparaissent grisées
        tableCouvertures.setRowFactory(t -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(CouvertureDTO item, boolean vide) {
                super.updateItem(item, vide);
                getStyleClass().remove("ligne-couverture-close");
                if (!vide && item != null && item.dateFin != null
                        && item.dateFin.isBefore(LocalDate.now())) {
                    getStyleClass().add("ligne-couverture-close");
                }
            }
        });
    }

    private TableColumn<CouvertureDTO, String> colonne(String titre, double largeur,
            Function<CouvertureDTO, String> extracteur) {
        TableColumn<CouvertureDTO, String> colonne = new TableColumn<>(titre);
        colonne.setPrefWidth(largeur);
        colonne.setCellValueFactory(donnees -> {
            String valeur = extracteur.apply(donnees.getValue());
            return new SimpleStringProperty(valeur == null ? "" : valeur);
        });
        return colonne;
    }

    private String formater(LocalDate date) {
        return date == null ? "" : date.format(FORMAT_DATE);
    }

    private GridPane construireOngletMedical() {
        GridPane grille = nouvelleGrille();
        champAntecedents.setPrefRowCount(3);
        champAllergies.setPrefRowCount(2);
        champNotes.setPrefRowCount(3);

        int ligne = 0;
        champ(grille, ligne++, 0, "Antécédents médicaux", champAntecedents, 3);
        champ(grille, ligne++, 0, "Allergies", champAllergies, 3);
        champ(grille, ligne++, 0, "Notes", champNotes, 3);
        if (!creation) {
            champActif.setTooltip(new Tooltip(
                    "Décocher pour archiver le dossier : il n'apparaîtra plus dans les recherches."));
            grille.add(champActif, 0, ligne, 4, 1);
        }
        return grille;
    }

    // ------------------------------------------------------ actions couvertures

    private void ajouterCouverture() {
        CouvertureDialogue dialogue = new CouvertureDialogue(service);
        dialogue.initOwner(getDialogPane().getScene().getWindow());
        dialogue.showAndWait().ifPresent(couverture -> {
            if (creation) {
                tableCouvertures.getItems().add(0, couverture);
                return;
            }
            Async.executer(() -> service.ajouterCouverture(patient.id, couverture),
                    creee -> tableCouvertures.getItems().add(0, creee),
                    this::afficherErreur);
        });
    }

    private void cloturerOuRetirer() {
        CouvertureDTO selection = tableCouvertures.getSelectionModel().getSelectedItem();
        if (selection == null) {
            return;
        }
        if (creation || selection.id == null) {
            tableCouvertures.getItems().remove(selection);
            return;
        }
        if (selection.dateFin != null) {
            afficherErreur(new IllegalArgumentException("Cette couverture est déjà clôturée."));
            return;
        }

        TextInputDialog saisieMotif = new TextInputDialog();
        saisieMotif.setTitle("Clôturer la couverture");
        saisieMotif.setHeaderText("Clôture de la couverture " + selection.payeurNom
                + " à la date d'aujourd'hui");
        saisieMotif.setContentText("Motif (facultatif) :");
        saisieMotif.initOwner(getDialogPane().getScene().getWindow());
        saisieMotif.showAndWait().ifPresent(motif ->
                Async.executer(
                        () -> service.cloturerCouverture(patient.id, selection.id,
                                LocalDate.now(), motif.isBlank() ? null : motif.trim()),
                        cloturee -> {
                            int index = tableCouvertures.getItems().indexOf(selection);
                            tableCouvertures.getItems().set(index, cloturee);
                        },
                        this::afficherErreur));
    }

    // ------------------------------------------------------ aides de mise en page

    private GridPane nouvelleGrille() {
        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(18));
        ColumnConstraints libelle = new ColumnConstraints();
        libelle.setMinWidth(150);
        ColumnConstraints saisie = new ColumnConstraints();
        saisie.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(libelle, saisie, new ColumnConstraints(150), saisie);
        return grille;
    }

    private void champ(GridPane grille, int ligne, int colonne, String libelle,
            javafx.scene.Node noeud) {
        champ(grille, ligne, colonne, libelle, noeud, 1);
    }

    private void champ(GridPane grille, int ligne, int colonne, String libelle,
            javafx.scene.Node noeud, int largeur) {
        grille.add(new Label(libelle), colonne, ligne);
        grille.add(noeud, colonne + 1, ligne, largeur, 1);
    }

    // ------------------------------------------------------------ résultat

    private void remplirDepuis(PatientDTO p) {
        champNom.setText(texte(p.nom));
        champPrenom.setText(texte(p.prenom));
        champNaissance.setValue(p.dateNaissance);
        if (p.sexe != null) {
            champSexe.getSelectionModel().select(p.sexe);
        }
        champTelephone.setText(texte(p.telephone));
        champWhatsapp.setText(texte(p.telephoneWhatsapp));
        champEmail.setText(texte(p.email));
        champProfession.setText(texte(p.profession));
        champAdresse.setText(texte(p.adresse));
        champQuartier.setText(texte(p.quartier));
        if (p.ville != null) {
            champVille.setText(p.ville);
        }
        champUrgenceNom.setText(texte(p.personneUrgenceNom));
        champUrgenceTel.setText(texte(p.personneUrgenceTel));
        tableCouvertures.getItems().setAll(p.couvertures);
        champMauvaisPayeur.setSelected(p.mauvaisPayeur);
        champAntecedents.setText(texte(p.antecedents));
        champAllergies.setText(texte(p.allergies));
        champNotes.setText(texte(p.notes));
        champActif.setSelected(p.actif);
    }

    private boolean validerSaisie() {
        if (champNom.getText().trim().isEmpty()) {
            afficherErreur(new IllegalArgumentException("Le nom du patient est obligatoire."));
            return false;
        }
        return true;
    }

    private PatientDTO construireResultat() {
        PatientDTO p = patient;
        p.nom = champNom.getText().trim().toUpperCase();
        p.prenom = vide(champPrenom.getText());
        p.dateNaissance = champNaissance.getValue();
        String sexe = champSexe.getSelectionModel().getSelectedItem();
        p.sexe = sexe == null || sexe.isBlank() ? null : sexe;
        p.telephone = vide(champTelephone.getText());
        p.telephoneWhatsapp = vide(champWhatsapp.getText());
        p.email = vide(champEmail.getText());
        p.profession = vide(champProfession.getText());
        p.adresse = vide(champAdresse.getText());
        p.quartier = vide(champQuartier.getText());
        p.ville = vide(champVille.getText());
        p.personneUrgenceNom = vide(champUrgenceNom.getText());
        p.personneUrgenceTel = vide(champUrgenceTel.getText());
        // En création : les couvertures accumulées partent avec le patient.
        // En modification : elles ont déjà été appliquées au fil de l'eau.
        p.couvertures = creation
                ? new java.util.ArrayList<>(tableCouvertures.getItems())
                : new java.util.ArrayList<>();
        p.mauvaisPayeur = champMauvaisPayeur.isSelected();
        p.antecedents = vide(champAntecedents.getText());
        p.allergies = vide(champAllergies.getText());
        p.notes = vide(champNotes.getText());
        p.actif = champActif.isSelected();
        return p;
    }

    private void afficherErreur(Exception e) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText("Opération impossible");
        alerte.initOwner(getDialogPane().getScene().getWindow());
        alerte.showAndWait();
    }

    private String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private String vide(String valeur) {
        String v = valeur == null ? "" : valeur.trim();
        return v.isEmpty() ? null : v;
    }
}
