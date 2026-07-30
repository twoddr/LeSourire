package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Montants;
import com.lesourire.client.service.ServiceFacturation;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.FactureLigneDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Fiche facture : édition d'un brouillon (lignes, remise, praticien, dates)
 * ou consultation d'une facture émise (montants, répartition, paiements).
 *
 * Les montants affichés en édition sont des aperçus au tarif du jour ;
 * le serveur recalcule tout à l'enregistrement (source de vérité).
 */
public class FactureDialogue extends Dialog<FactureDTO> {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_DATE_HEURE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BigDecimal CENT = new BigDecimal("100");

    private final ServiceFacturation service;
    private final ServicePatients servicePatients;
    private final FactureDTO facture;
    private final boolean lectureSeule;

    private final ComboBox<UtilisateurDTO> champPraticien = new ComboBox<>();
    private final DatePicker champDate = new DatePicker(LocalDate.now());
    private final DatePicker champEcheance = new DatePicker();
    private final TableView<FactureLigneDTO> tableLignes = new TableView<>();
    private final TextField champRemise = new TextField("0");
    private final TextField champNotes = new TextField();
    private final Label labelCouverture = new Label();
    private final Label labelTotaux = new Label();
    private final TableView<PaiementDTO> tablePaiements = new TableView<>();

    private BigDecimal pctAssureur = BigDecimal.ZERO;
    private BigDecimal pctSociete = BigDecimal.ZERO;
    private String nomAssureur;
    private String nomSociete;

    /** Création : {@code existante} null et patient choisi ; sinon édition/consultation. */
    public FactureDialogue(FactureDTO existante, PatientDTO patient,
            ServiceFacturation service, ServicePatients servicePatients) {
        this.service = service;
        this.servicePatients = servicePatients;
        this.lectureSeule = existante != null && existante.statut != StatutFacture.BROUILLON;

        if (existante != null) {
            this.facture = existante;
        } else {
            this.facture = new FactureDTO();
            this.facture.patientId = patient.id;
            this.facture.patientNom = patient.nomComplet();
            this.facture.patientNumeroDossier = patient.numeroDossier;
        }

        setTitle(existante == null ? "Nouvelle facture"
                : "Facture " + facture.numero + " (" + libelleStatut(facture.statut) + ")");
        setHeaderText(facture.patientNom + " — dossier " + facture.patientNumeroDossier);
        setResizable(true);

        getDialogPane().setContent(construireContenu());
        getDialogPane().setPrefSize(860, 620);
        if (lectureSeule) {
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        } else {
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
            boutonOk.setText("Enregistrer le brouillon");
            boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
                if (!valider()) {
                    e.consume();
                }
            });
        }
        setResultConverter(bouton -> bouton == ButtonType.OK ? construireResultat() : null);

        remplir();
        chargerReferentiels();
    }

    // ---------------------------------------------------------- construction

    private VBox construireContenu() {
        champPraticien.setConverter(new StringConverter<>() {
            @Override
            public String toString(UtilisateurDTO u) {
                return u == null ? "" : u.nomComplet();
            }

            @Override
            public UtilisateurDTO fromString(String texte) {
                return null;
            }
        });
        champPraticien.setMaxWidth(Double.MAX_VALUE);

        GridPane entete = new GridPane();
        entete.setHgap(12);
        entete.setVgap(8);
        entete.add(new Label("Praticien"), 0, 0);
        entete.add(champPraticien, 1, 0);
        entete.add(new Label("Date"), 2, 0);
        entete.add(champDate, 3, 0);
        entete.add(new Label("Échéance"), 4, 0);
        entete.add(champEcheance, 5, 0);
        entete.add(labelCouverture, 0, 1, 6, 1);
        labelCouverture.getStyleClass().add("note-discrete");
        labelCouverture.setWrapText(true);
        GridPane.setHgrow(champPraticien, Priority.ALWAYS);

        construireTableLignes();

        Button ajouter = new Button("Ajouter une ligne");
        ajouter.setOnAction(e -> ajouterLigne());
        Button retirer = new Button("Retirer");
        retirer.setOnAction(e -> {
            FactureLigneDTO sel = tableLignes.getSelectionModel().getSelectedItem();
            if (sel != null) {
                tableLignes.getItems().remove(sel);
                majTotaux();
            }
        });
        retirer.disableProperty().bind(
                tableLignes.getSelectionModel().selectedItemProperty().isNull());
        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        champRemise.setPrefWidth(110);
        champRemise.textProperty().addListener((obs, avant, apres) -> majTotaux());
        HBox actionsLignes = new HBox(8, ajouter, retirer, espace,
                new Label("Remise (XAF)"), champRemise);
        actionsLignes.setSpacing(8);
        actionsLignes.setPadding(new Insets(4, 0, 0, 0));

        labelTotaux.getStyleClass().add("total-facture");
        labelTotaux.setWrapText(true);

        champNotes.setPromptText("Notes internes (facultatif)");

        VBox boite = new VBox(12, entete, tableLignes, actionsLignes, labelTotaux, champNotes);
        boite.setPadding(new Insets(14));
        VBox.setVgrow(tableLignes, Priority.ALWAYS);

        if (lectureSeule) {
            champPraticien.setDisable(true);
            champDate.setDisable(true);
            champEcheance.setDisable(true);
            champRemise.setDisable(true);
            champNotes.setDisable(true);
            ajouter.setDisable(true);
            construireTablePaiements();
            Label titrePaiements = new Label("Paiements encaissés");
            titrePaiements.getStyleClass().add("sous-titre");
            boite.getChildren().addAll(titrePaiements, tablePaiements);
        }
        return boite;
    }

    private void construireTableLignes() {
        tableLignes.getColumns().setAll(java.util.List.of(
                colonneLigne("Code", 80, l -> l.prestationCode),
                colonneLigne("Désignation", 320, l -> l.designation),
                colonneLigne("Qté", 50, l -> String.valueOf(l.quantite)),
                colonneLigne("P.U.", 110, l -> Montants.formater(l.prixUnitaire)),
                colonneLigne("Montant", 120, l -> Montants.formater(l.montant))));
        tableLignes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableLignes.setPlaceholder(new Label("Aucune ligne : ajoutez les prestations réalisées."));
    }

    private TableColumn<FactureLigneDTO, String> colonneLigne(String titre, double largeur,
            Function<FactureLigneDTO, String> extracteur) {
        TableColumn<FactureLigneDTO, String> colonne = new TableColumn<>(titre);
        colonne.setPrefWidth(largeur);
        colonne.setCellValueFactory(d -> {
            String valeur = extracteur.apply(d.getValue());
            return new SimpleStringProperty(valeur == null ? "" : valeur);
        });
        return colonne;
    }

    private void construireTablePaiements() {
        tablePaiements.getColumns().setAll(java.util.List.of(
                colonnePaiement("Date", 130, p -> p.datePaiement == null ? ""
                        : p.datePaiement.format(FORMAT_DATE_HEURE)),
                colonnePaiement("Payeur", 100, p -> p.payeur == null ? "" : p.payeur.name()),
                colonnePaiement("Montant", 110, p -> Montants.formater(p.montant)),
                colonnePaiement("Mode", 110, p -> p.mode == null ? "" : p.mode.getLibelle()),
                colonnePaiement("Référence", 130, p -> p.reference),
                colonnePaiement("Reçu par", 130, p -> p.recuParNom)));
        tablePaiements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablePaiements.setPlaceholder(new Label("Aucun paiement encaissé."));
        tablePaiements.setPrefHeight(140);
        tablePaiements.getItems().setAll(facture.paiements);
    }

    private TableColumn<PaiementDTO, String> colonnePaiement(String titre, double largeur,
            Function<PaiementDTO, String> extracteur) {
        TableColumn<PaiementDTO, String> colonne = new TableColumn<>(titre);
        colonne.setPrefWidth(largeur);
        colonne.setCellValueFactory(d -> {
            String valeur = extracteur.apply(d.getValue());
            return new SimpleStringProperty(valeur == null ? "" : valeur);
        });
        return colonne;
    }

    // ------------------------------------------------------------- données

    private void remplir() {
        if (facture.id != null) {
            champDate.setValue(facture.dateFacture);
            champEcheance.setValue(facture.dateEcheance);
            champRemise.setText(facture.remise.stripTrailingZeros().toPlainString());
            champNotes.setText(facture.notes == null ? "" : facture.notes);
            tableLignes.getItems().setAll(facture.lignes);
            pctAssureur = facture.pourcentageAssureur;
            pctSociete = facture.pourcentageSociete;
            nomAssureur = facture.assureurNom;
            nomSociete = facture.societeNom;
            majEtiquetteCouverture();
        }
        majTotaux();
    }

    private void chargerReferentiels() {
        Async.executer(service::praticiens, praticiens -> {
            champPraticien.getItems().setAll(praticiens);
            if (facture.praticienId != null) {
                praticiens.stream().filter(p -> p.id().equals(facture.praticienId))
                        .findFirst().ifPresent(p ->
                                champPraticien.getSelectionModel().select(p));
            } else if (praticiens.size() == 1) {
                champPraticien.getSelectionModel().selectFirst();
            }
        }, e -> {
        });

        // En création/édition : la prise en charge du patient sert d'aperçu
        if (!lectureSeule) {
            Async.executer(() -> servicePatients.obtenir(facture.patientId), patient -> {
                pctAssureur = BigDecimal.ZERO;
                pctSociete = BigDecimal.ZERO;
                nomAssureur = null;
                nomSociete = null;
                for (CouvertureDTO c : patient.couvertures) {
                    if (!c.estEnCours()) {
                        continue;
                    }
                    BigDecimal pct = c.pourcentageEffectif == null
                            ? BigDecimal.ZERO : c.pourcentageEffectif;
                    if (CouvertureDTO.TYPE_ASSUREUR.equals(c.type)) {
                        pctAssureur = pct;
                        nomAssureur = c.payeurNom;
                    } else {
                        pctSociete = pct;
                        nomSociete = c.payeurNom;
                    }
                }
                majEtiquetteCouverture();
                majTotaux();
            }, e -> {
            });
        }
    }

    private void majEtiquetteCouverture() {
        StringBuilder texte = new StringBuilder("Prise en charge : ");
        if (nomAssureur == null && nomSociete == null) {
            texte.append("aucune — le patient règle la totalité.");
        } else {
            if (nomAssureur != null) {
                texte.append(nomAssureur).append(" ")
                        .append(pctAssureur.stripTrailingZeros().toPlainString()).append(" %");
            }
            if (nomSociete != null) {
                if (nomAssureur != null) {
                    texte.append("  +  ");
                }
                texte.append(nomSociete).append(" ")
                        .append(pctSociete.stripTrailingZeros().toPlainString()).append(" %");
            }
        }
        labelCouverture.setText(texte.toString());
    }

    private BigDecimal lireRemise() {
        try {
            return new BigDecimal(champRemise.getText().trim().replace(" ", "")
                    .replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void majTotaux() {
        BigDecimal brut = tableLignes.getItems().stream()
                .map(l -> l.montant == null ? BigDecimal.ZERO : l.montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remise = lireRemise();
        BigDecimal net = remise == null ? brut : brut.subtract(remise);
        BigDecimal quoteAssureur = net.multiply(pctAssureur).divide(CENT, 2, RoundingMode.HALF_UP);
        BigDecimal quoteSociete = net.multiply(pctSociete).divide(CENT, 2, RoundingMode.HALF_UP);
        BigDecimal quotePatient = net.subtract(quoteAssureur).subtract(quoteSociete);

        StringBuilder texte = new StringBuilder();
        texte.append("Brut : ").append(Montants.formaterAvecDevise(brut));
        if (remise != null && remise.compareTo(BigDecimal.ZERO) > 0) {
            texte.append("   −  Remise : ").append(Montants.formaterAvecDevise(remise));
        }
        texte.append("   =  Net : ").append(Montants.formaterAvecDevise(net));
        texte.append("\nPatient : ").append(Montants.formaterAvecDevise(quotePatient));
        if (quoteAssureur.compareTo(BigDecimal.ZERO) > 0) {
            texte.append("   ·   Assureur : ").append(Montants.formaterAvecDevise(quoteAssureur));
        }
        if (quoteSociete.compareTo(BigDecimal.ZERO) > 0) {
            texte.append("   ·   Société : ").append(Montants.formaterAvecDevise(quoteSociete));
        }
        if (lectureSeule) {
            texte.append("\nPayé : ").append(Montants.formaterAvecDevise(facture.totalPaye()))
                    .append("   ·   Reste dû : ")
                    .append(Montants.formaterAvecDevise(facture.soldeTotal()));
        }
        labelTotaux.setText(texte.toString());
    }

    // -------------------------------------------------------------- actions

    private void ajouterLigne() {
        LigneFactureDialogue dialogue = new LigneFactureDialogue(service);
        dialogue.initOwner(getDialogPane().getScene().getWindow());
        dialogue.showAndWait().ifPresent(ligne -> {
            tableLignes.getItems().add(ligne);
            majTotaux();
        });
    }

    private boolean valider() {
        if (champPraticien.getSelectionModel().getSelectedItem() == null) {
            return avertir("Choisissez le praticien à qui imputer les actes.");
        }
        if (tableLignes.getItems().isEmpty()) {
            return avertir("Ajoutez au moins une ligne à la facture.");
        }
        BigDecimal remise = lireRemise();
        if (remise == null || remise.compareTo(BigDecimal.ZERO) < 0) {
            return avertir("La remise doit être un nombre positif ou zéro.");
        }
        return true;
    }

    private FactureDTO construireResultat() {
        facture.praticienId = champPraticien.getSelectionModel().getSelectedItem().id();
        facture.dateFacture = champDate.getValue();
        facture.dateEcheance = champEcheance.getValue();
        facture.lignes = new java.util.ArrayList<>(tableLignes.getItems());
        facture.remise = lireRemise();
        String notes = champNotes.getText().trim();
        facture.notes = notes.isEmpty() ? null : notes;
        return facture;
    }

    private boolean avertir(String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alerte.setHeaderText("Facture incomplète");
        alerte.initOwner(getDialogPane().getScene().getWindow());
        alerte.showAndWait();
        return false;
    }

    static String libelleStatut(StatutFacture statut) {
        return switch (statut) {
            case BROUILLON -> "Brouillon";
            case EMISE -> "Émise";
            case PARTIELLEMENT_PAYEE -> "Partiellement payée";
            case PAYEE -> "Payée";
            case ANNULEE -> "Annulée";
        };
    }
}
