package com.lesourire.client.vue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Montants;
import com.lesourire.client.service.ServiceFacturation;
import com.lesourire.commun.dto.FactureLigneDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Ajout d'une ligne de facture : une prestation du tarifaire (avec dents et
 * quantité) ou une ligne libre. Les montants affichés sont un aperçu au tarif
 * du jour ; le serveur reste seul juge du montant définitif.
 */
public class LigneFactureDialogue extends Dialog<FactureLigneDTO> {

    private final ServiceFacturation service;

    private final TextField champRecherche = new TextField();
    private final TableView<PrestationDTO> tablePrestations = new TableView<>();
    private final TextField champDents = new TextField();
    private final Spinner<Integer> champQuantite = new Spinner<>(1, 32, 1);
    private final Label labelApercu = new Label();

    private final TextField champDesignationLibre = new TextField();
    private final TextField champPrixLibre = new TextField();
    private final Spinner<Integer> champQuantiteLibre = new Spinner<>(1, 99, 1);

    private final TabPane onglets = new TabPane();
    private final Map<String, BigDecimal> valeursLettres = new HashMap<>();

    public LigneFactureDialogue(ServiceFacturation service) {
        this.service = service;
        setTitle("Ajouter une ligne");
        setHeaderText("Prestation du tarifaire ou ligne libre");
        setResizable(true);

        onglets.getTabs().addAll(
                new Tab("Tarifaire", construireOngletTarifaire()),
                new Tab("Ligne libre", construireOngletLibre()));
        onglets.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        getDialogPane().setContent(onglets);
        getDialogPane().setPrefSize(640, 460);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button boutonOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        boutonOk.setText("Ajouter");
        boutonOk.addEventFilter(ActionEvent.ACTION, e -> {
            if (!valider()) {
                e.consume();
            }
        });
        setResultConverter(bouton -> bouton == ButtonType.OK ? construire() : null);

        Async.executer(service::valeursLettres, valeurs -> {
            for (ValeurLettreCleDTO v : valeurs) {
                valeursLettres.put(v.lettreCle(), v.valeur());
            }
            majApercu();
        }, e -> {
        });
        chargerPrestations();
    }

    private VBox construireOngletTarifaire() {
        champRecherche.setPromptText("Rechercher par code ou libellé…");
        PauseTransition attente = new PauseTransition(Duration.millis(250));
        champRecherche.textProperty().addListener((obs, avant, apres) -> {
            attente.setOnFinished(e -> chargerPrestations());
            attente.playFromStart();
        });

        TableColumn<PrestationDTO, String> colCode = new TableColumn<>("Code");
        colCode.setPrefWidth(90);
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().code));
        TableColumn<PrestationDTO, String> colLibelle = new TableColumn<>("Libellé");
        colLibelle.setPrefWidth(280);
        colLibelle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().libelle));
        TableColumn<PrestationDTO, String> colTarif = new TableColumn<>("Tarif");
        colTarif.setPrefWidth(120);
        colTarif.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tarifLibelle()));
        tablePrestations.getColumns().setAll(java.util.List.of(colCode, colLibelle, colTarif));
        tablePrestations.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablePrestations.setPlaceholder(new Label("Aucune prestation trouvée."));
        tablePrestations.getSelectionModel().selectedItemProperty()
                .addListener((obs, avant, apres) -> majApercu());

        champDents.setPromptText("ex. 16 ou 11,21 (facultatif)");
        champQuantite.valueProperty().addListener((obs, avant, apres) -> majApercu());
        champQuantite.setEditable(true);
        labelApercu.getStyleClass().add("note-discrete");

        GridPane bas = new GridPane();
        bas.setHgap(12);
        bas.setVgap(8);
        bas.add(new Label("Dent(s)"), 0, 0);
        bas.add(champDents, 1, 0);
        bas.add(new Label("Quantité"), 2, 0);
        bas.add(champQuantite, 3, 0);
        bas.add(labelApercu, 0, 1, 4, 1);
        GridPane.setHgrow(champDents, Priority.ALWAYS);

        VBox boite = new VBox(10, champRecherche, tablePrestations, bas);
        boite.setPadding(new Insets(14));
        VBox.setVgrow(tablePrestations, Priority.ALWAYS);
        return boite;
    }

    private VBox construireOngletLibre() {
        champDesignationLibre.setPromptText("ex. Prothèse laboratoire externe");
        champPrixLibre.setPromptText("prix unitaire en XAF");
        champQuantiteLibre.setEditable(true);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.add(new Label("Désignation"), 0, 0);
        grille.add(champDesignationLibre, 1, 0);
        grille.add(new Label("Prix unitaire"), 0, 1);
        grille.add(champPrixLibre, 1, 1);
        grille.add(new Label("Quantité"), 0, 2);
        grille.add(champQuantiteLibre, 1, 2);
        GridPane.setHgrow(champDesignationLibre, Priority.ALWAYS);

        Label aide = new Label("Pour un montant hors tarifaire "
                + "(ex. frais de laboratoire refacturés).");
        aide.getStyleClass().add("note-discrete");
        aide.setWrapText(true);

        VBox boite = new VBox(12, grille, aide);
        boite.setPadding(new Insets(14));
        return boite;
    }

    private void chargerPrestations() {
        String q = champRecherche.getText();
        Async.executer(() -> service.prestations(q),
                liste -> tablePrestations.getItems().setAll(liste),
                e -> {
                });
    }

    private boolean ongletTarifaire() {
        return onglets.getSelectionModel().getSelectedIndex() == 0;
    }

    /** Prix unitaire estimé au tarif du jour (aperçu seulement). */
    private BigDecimal prixUnitaireEstime(PrestationDTO prestation) {
        if (prestation.tarifForfait != null) {
            return prestation.tarifForfait;
        }
        BigDecimal valeur = valeursLettres.get(prestation.lettreCle);
        if (valeur == null || prestation.coefficient == null) {
            return null;
        }
        return prestation.coefficient.multiply(valeur).setScale(2, RoundingMode.HALF_UP);
    }

    private void majApercu() {
        PrestationDTO sel = tablePrestations.getSelectionModel().getSelectedItem();
        if (sel == null) {
            labelApercu.setText("");
            return;
        }
        BigDecimal pu = prixUnitaireEstime(sel);
        if (pu == null) {
            labelApercu.setText("Tarif du jour indisponible — il sera calculé par le serveur.");
            return;
        }
        BigDecimal total = pu.multiply(BigDecimal.valueOf(champQuantite.getValue()));
        labelApercu.setText(sel.tarifLibelle() + "  →  " + Montants.formaterAvecDevise(pu)
                + " × " + champQuantite.getValue() + " = " + Montants.formaterAvecDevise(total));
    }

    private boolean valider() {
        if (ongletTarifaire()) {
            if (tablePrestations.getSelectionModel().getSelectedItem() == null) {
                return avertir("Sélectionnez une prestation dans le tarifaire.");
            }
            return true;
        }
        if (champDesignationLibre.getText().isBlank()) {
            return avertir("La désignation de la ligne libre est obligatoire.");
        }
        if (lirePrixLibre() == null || lirePrixLibre().compareTo(BigDecimal.ZERO) < 0) {
            return avertir("Indiquez un prix unitaire valide (nombre positif).");
        }
        return true;
    }

    private BigDecimal lirePrixLibre() {
        try {
            return new BigDecimal(champPrixLibre.getText().trim().replace(" ", "")
                    .replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private FactureLigneDTO construire() {
        FactureLigneDTO ligne = new FactureLigneDTO();
        if (ongletTarifaire()) {
            PrestationDTO prestation = tablePrestations.getSelectionModel().getSelectedItem();
            ligne.prestationId = prestation.id;
            ligne.prestationCode = prestation.code;
            String dents = champDents.getText().trim();
            ligne.dents = dents.isEmpty() ? null : dents;
            ligne.designation = prestation.libelle
                    + (dents.isEmpty() ? "" : " — dent(s) " + dents);
            ligne.quantite = champQuantite.getValue();
            BigDecimal pu = prixUnitaireEstime(prestation);
            ligne.prixUnitaire = pu;
            ligne.montant = pu == null ? null
                    : pu.multiply(BigDecimal.valueOf(ligne.quantite));
        } else {
            ligne.designation = champDesignationLibre.getText().trim();
            ligne.prixUnitaire = lirePrixLibre();
            ligne.quantite = champQuantiteLibre.getValue();
            ligne.montant = ligne.prixUnitaire.multiply(BigDecimal.valueOf(ligne.quantite));
        }
        return ligne;
    }

    private boolean avertir(String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alerte.setHeaderText("Ligne non valide");
        alerte.initOwner(getDialogPane().getScene().getWindow());
        alerte.showAndWait();
        return false;
    }
}
