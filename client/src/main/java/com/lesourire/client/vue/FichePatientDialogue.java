package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.util.List;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

import javafx.application.Platform;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Fiche patient (création ou modification) : identité et contacts,
 * prise en charge par les tiers payants, informations médicales.
 */
public class FichePatientDialogue extends Dialog<PatientDTO> {

    private final PatientDTO patient;
    private final ServicePatients service;

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

    // Prise en charge
    private final ComboBox<AssureurDTO> champAssureur = new ComboBox<>();
    private final TextField champNumeroAssure = new TextField();
    private final TextField champPctAssureur = new TextField();
    private final ComboBox<SocieteDTO> champSociete = new ComboBox<>();
    private final TextField champPctSociete = new TextField();
    private final CheckBox champMauvaisPayeur = new CheckBox("Payeur à surveiller");

    // Médical
    private final TextArea champAntecedents = new TextArea();
    private final TextArea champAllergies = new TextArea();
    private final TextArea champNotes = new TextArea();
    private final CheckBox champActif = new CheckBox("Dossier actif");

    public FichePatientDialogue(PatientDTO existant, ServicePatients service) {
        this.service = service;
        this.patient = existant != null ? existant : new PatientDTO();
        boolean creation = existant == null;

        setTitle(creation ? "Nouveau patient"
                : "Dossier " + patient.numeroDossier + " — " + patient.nomComplet());
        setResizable(true);

        TabPane onglets = new TabPane(
                new Tab("Identité & contacts", construireOngletIdentite()),
                new Tab("Prise en charge", construireOngletPriseEnCharge()),
                new Tab("Médical & notes", construireOngletMedical(creation)));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        onglets.setPrefSize(680, 460);

        getDialogPane().setContent(onglets);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Enregistrer");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (!validerSaisie()) {
                e.consume();
            }
        });

        remplirDepuis(patient);
        chargerReferentiels();

        setResultConverter(bouton ->
                bouton == ButtonType.OK ? construireResultat() : null);

        Platform.runLater(champNom::requestFocus);
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

    private GridPane construireOngletPriseEnCharge() {
        GridPane grille = nouvelleGrille();

        champAssureur.setMaxWidth(Double.MAX_VALUE);
        champSociete.setMaxWidth(Double.MAX_VALUE);
        champPctAssureur.setPromptText("vide = % par défaut de l'assureur");
        champPctSociete.setPromptText("vide = % par défaut de la société");

        Button ajouterAssureur = boutonAjout("Créer un assureur",
                () -> creationRapideAssureur());
        Button ajouterSociete = boutonAjout("Créer une société",
                () -> creationRapideSociete());

        HBox ligneAssureur = new HBox(6, champAssureur, ajouterAssureur);
        HBox.setHgrow(champAssureur, Priority.ALWAYS);
        HBox ligneSociete = new HBox(6, champSociete, ajouterSociete);
        HBox.setHgrow(champSociete, Priority.ALWAYS);

        int ligne = 0;
        champ(grille, ligne, 0, "Assureur", ligneAssureur);
        champ(grille, ligne++, 2, "N° assuré", champNumeroAssure);
        champ(grille, ligne++, 0, "% pris en charge (assureur)", champPctAssureur);
        champ(grille, ligne++, 0, "Société conventionnée", ligneSociete);
        champ(grille, ligne++, 0, "% pris en charge (société)", champPctSociete);
        grille.add(champMauvaisPayeur, 0, ligne, 4, 1);

        Label aide = new Label("Les pourcentages saisis ici priment sur les pourcentages "
                + "par défaut du tiers payant. Ils seront recopiés sur chaque facture.");
        aide.getStyleClass().add("note-discrete");
        aide.setWrapText(true);
        grille.add(aide, 0, ligne + 1, 4, 1);
        return grille;
    }

    private GridPane construireOngletMedical(boolean creation) {
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

    private Button boutonAjout(String infobulle, Runnable action) {
        Button bouton = new Button("+");
        bouton.setTooltip(new Tooltip(infobulle));
        bouton.setOnAction(e -> action.run());
        return bouton;
    }

    // ------------------------------------------------------------ données

    private void chargerReferentiels() {
        Async.executer(service::listerAssureurs, liste -> {
            champAssureur.getItems().setAll(liste);
            champAssureur.getItems().add(0, null);
            selectionnerAssureur(patient.assureurId);
        }, e -> {
        });
        Async.executer(service::listerSocietes, liste -> {
            champSociete.getItems().setAll(liste);
            champSociete.getItems().add(0, null);
            selectionnerSociete(patient.societeId);
        }, e -> {
        });

        champAssureur.setButtonCell(celluleAvecVide("— Aucun —"));
        champAssureur.setCellFactory(l -> celluleAvecVide("— Aucun —"));
        champSociete.setButtonCell(celluleSocieteAvecVide("— Aucune —"));
        champSociete.setCellFactory(l -> celluleSocieteAvecVide("— Aucune —"));
    }

    private javafx.scene.control.ListCell<AssureurDTO> celluleAvecVide(String texteVide) {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AssureurDTO item, boolean vide) {
                super.updateItem(item, vide);
                setText(vide ? null : item == null ? texteVide : item.nom());
            }
        };
    }

    private javafx.scene.control.ListCell<SocieteDTO> celluleSocieteAvecVide(String texteVide) {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SocieteDTO item, boolean vide) {
                super.updateItem(item, vide);
                setText(vide ? null : item == null ? texteVide : item.nom());
            }
        };
    }

    private void selectionnerAssureur(Long id) {
        if (id == null) {
            return;
        }
        champAssureur.getItems().stream()
                .filter(a -> a != null && id.equals(a.id()))
                .findFirst()
                .ifPresent(champAssureur.getSelectionModel()::select);
    }

    private void selectionnerSociete(Long id) {
        if (id == null) {
            return;
        }
        champSociete.getItems().stream()
                .filter(s -> s != null && id.equals(s.id()))
                .findFirst()
                .ifPresent(champSociete.getSelectionModel()::select);
    }

    private void creationRapideAssureur() {
        TextInputDialog saisie = new TextInputDialog();
        saisie.setTitle("Nouvel assureur");
        saisie.setHeaderText("Création rapide d'un assureur");
        saisie.setContentText("Nom de l'assureur :");
        saisie.showAndWait().filter(nom -> !nom.isBlank()).ifPresent(nom ->
                Async.executer(() -> service.creerAssureur(nom.trim(), BigDecimal.ZERO),
                        cree -> {
                            champAssureur.getItems().add(cree);
                            champAssureur.getSelectionModel().select(cree);
                        },
                        this::afficherErreur));
    }

    private void creationRapideSociete() {
        TextInputDialog saisie = new TextInputDialog();
        saisie.setTitle("Nouvelle société");
        saisie.setHeaderText("Création rapide d'une société conventionnée");
        saisie.setContentText("Nom de la société :");
        saisie.showAndWait().filter(nom -> !nom.isBlank()).ifPresent(nom ->
                Async.executer(() -> service.creerSociete(nom.trim(), BigDecimal.ZERO),
                        cree -> {
                            champSociete.getItems().add(cree);
                            champSociete.getSelectionModel().select(cree);
                        },
                        this::afficherErreur));
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
        champNumeroAssure.setText(texte(p.numeroAssure));
        champPctAssureur.setText(p.pourcentageAssureur == null ? "" : p.pourcentageAssureur.toPlainString());
        champPctSociete.setText(p.pourcentageSociete == null ? "" : p.pourcentageSociete.toPlainString());
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
        if (pourcentageInvalide(champPctAssureur) || pourcentageInvalide(champPctSociete)) {
            afficherErreur(new IllegalArgumentException(
                    "Les pourcentages doivent être des nombres entre 0 et 100."));
            return false;
        }
        return true;
    }

    private boolean pourcentageInvalide(TextField champ) {
        BigDecimal valeur = lirePourcentage(champ);
        if (champ.getText().trim().isEmpty()) {
            return false;
        }
        return valeur == null
                || valeur.compareTo(BigDecimal.ZERO) < 0
                || valeur.compareTo(new BigDecimal("100")) > 0;
    }

    private BigDecimal lirePourcentage(TextField champ) {
        String brut = champ.getText().trim().replace(',', '.');
        if (brut.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(brut);
        } catch (NumberFormatException e) {
            return null;
        }
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
        AssureurDTO assureur = champAssureur.getSelectionModel().getSelectedItem();
        p.assureurId = assureur == null ? null : assureur.id();
        p.numeroAssure = vide(champNumeroAssure.getText());
        p.pourcentageAssureur = lirePourcentage(champPctAssureur);
        SocieteDTO societe = champSociete.getSelectionModel().getSelectedItem();
        p.societeId = societe == null ? null : societe.id();
        p.pourcentageSociete = lirePourcentage(champPctSociete);
        p.mauvaisPayeur = champMauvaisPayeur.isSelected();
        p.antecedents = vide(champAntecedents.getText());
        p.allergies = vide(champAllergies.getText());
        p.notes = vide(champNotes.getText());
        p.actif = champActif.isSelected();
        return p;
    }

    private void afficherErreur(Exception e) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText("Impossible d'enregistrer");
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
