package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.coeur.Dialogues;
import com.lesourire.client.coeur.Session;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.client.service.ServicePatientsApi;
import com.lesourire.client.service.ServicePatientsDemo;
import com.lesourire.client.service.ServiceRdv;
import com.lesourire.client.service.ServiceRdvApi;
import com.lesourire.client.service.ServiceRdvDemo;
import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Module Agenda : vue journée des rendez-vous et salle d'attente. */
public class AgendaVue {

    private static final DateTimeFormatter FORMAT_JOUR =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final BorderPane racine = new BorderPane();
    private final ServiceRdv serviceRdv;
    private final ServicePatients servicePatients;

    private LocalDate jour = LocalDate.now();
    private final Label labelJour = new Label();
    private final ComboBox<UtilisateurDTO> filtrePraticien = new ComboBox<>();
    private final TableView<RdvDTO> tableau = new TableView<>();
    private final ListView<RdvDTO> salleAttente = new ListView<>();
    private final Label labelStatut = new Label();
    private List<UtilisateurDTO> praticiens = new ArrayList<>();

    public AgendaVue() {
        boolean demo = Session.estModeDemonstration();
        this.serviceRdv = demo ? new ServiceRdvDemo() : new ServiceRdvApi(Session.api());
        this.servicePatients = demo ? new ServicePatientsDemo() : new ServicePatientsApi(Session.api());
        construire();
        chargerPraticiensPuisJournee();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));

        Label titre = new Label("Agenda");
        titre.getStyleClass().add("titre-page");

        Button btnPrec = new Button();
        btnPrec.setGraphic(new FontIcon(Material2AL.CHEVRON_LEFT));
        btnPrec.setTooltip(new Tooltip("Jour précédent"));
        btnPrec.setOnAction(e -> {
            jour = jour.minusDays(1);
            chargerJournee();
        });

        Button btnAuj = new Button("Aujourd'hui");
        btnAuj.setOnAction(e -> {
            jour = LocalDate.now();
            chargerJournee();
        });

        Button btnSuiv = new Button();
        btnSuiv.setGraphic(new FontIcon(Material2AL.CHEVRON_RIGHT));
        btnSuiv.setTooltip(new Tooltip("Jour suivant"));
        btnSuiv.setOnAction(e -> {
            jour = jour.plusDays(1);
            chargerJournee();
        });

        labelJour.getStyleClass().add("sous-titre-page");

        filtrePraticien.setPromptText("Tous les praticiens");
        filtrePraticien.setConverter(new StringConverter<>() {
            @Override
            public String toString(UtilisateurDTO u) {
                return u == null ? "Tous les praticiens" : u.nomComplet();
            }

            @Override
            public UtilisateurDTO fromString(String s) {
                return null;
            }
        });
        filtrePraticien.setOnAction(e -> chargerJournee());
        filtrePraticien.setPrefWidth(200);

        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerJournee());

        Button nouveau = new Button("Nouveau RDV");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFiche(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        HBox bandeau = new HBox(10, titre, espace, btnPrec, btnAuj, btnSuiv, filtrePraticien,
                actualiser, nouveau);
        bandeau.setAlignment(Pos.CENTER_LEFT);

        HBox dateLigne = new HBox(labelJour);
        dateLigne.setPadding(new Insets(8, 0, 12, 0));

        construireTableau();
        VBox.setVgrow(tableau, Priority.ALWAYS);

        HBox actionsStatut = new HBox(8,
                boutonStatut("Confirmer", StatutRdv.CONFIRME),
                boutonStatut("Arrivé", StatutRdv.EN_SALLE_ATTENTE),
                boutonStatut("Honoré", StatutRdv.HONORE),
                boutonStatut("Absent", StatutRdv.ABSENT),
                boutonStatut("Annuler", StatutRdv.ANNULE));
        actionsStatut.setAlignment(Pos.CENTER_LEFT);

        labelStatut.getStyleClass().add("note-discrete");

        VBox centre = new VBox(10, tableau, actionsStatut, labelStatut);
        HBox.setHgrow(centre, Priority.ALWAYS);

        Label titreSalle = new Label("Salle d'attente");
        titreSalle.getStyleClass().add("sous-titre-section");
        construireSalleAttente();
        VBox.setVgrow(salleAttente, Priority.ALWAYS);
        VBox panneauSalle = new VBox(10, titreSalle, salleAttente);
        panneauSalle.setPrefWidth(260);
        panneauSalle.getStyleClass().add("panneau-salle-attente");
        panneauSalle.setPadding(new Insets(12));

        HBox corps = new HBox(16, centre, panneauSalle);
        HBox.setHgrow(centre, Priority.ALWAYS);

        VBox haut = new VBox(bandeau, dateLigne);
        racine.setTop(haut);
        racine.setCenter(corps);
    }

    private Button boutonStatut(String libelle, StatutRdv statut) {
        Button b = new Button(libelle);
        b.setOnAction(e -> appliquerStatut(statut));
        return b;
    }

    private void construireTableau() {
        tableau.getColumns().setAll(
                col("Heure", 90, r -> r.debut.format(FORMAT_HEURE)
                        + " – " + r.fin.format(FORMAT_HEURE)),
                col("Patient", 180, r -> r.patientNom),
                col("Tél.", 110, r -> r.patientTelephone == null ? "" : r.patientTelephone),
                col("Type", 120, r -> r.type.getLibelle()),
                col("Statut", 130, r -> r.statut.getLibelle()),
                col("Motif", 180, r -> r.motif == null ? "" : r.motif),
                col("Praticien", 140, r -> r.praticienNom));
        tableau.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableau.setPlaceholder(new Label("Aucun rendez-vous ce jour."));
        tableau.setRowFactory(t -> {
            TableRow<RdvDTO> ligne = new TableRow<>() {
                @Override
                protected void updateItem(RdvDTO item, boolean vide) {
                    super.updateItem(item, vide);
                    getStyleClass().removeIf(c -> c.startsWith("rdv-statut-"));
                    if (!vide && item != null && item.statut != null) {
                        getStyleClass().add("rdv-statut-"
                                + item.statut.name().toLowerCase(Locale.ROOT).replace('_', '-'));
                    }
                }
            };
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    ouvrirFiche(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    private void construireSalleAttente() {
        salleAttente.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RdvDTO item, boolean vide) {
                super.updateItem(item, vide);
                if (vide || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.debut.format(FORMAT_HEURE) + "  " + item.patientNom
                            + (item.motif == null || item.motif.isBlank() ? "" : "\n" + item.motif));
                }
            }
        });
        salleAttente.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && salleAttente.getSelectionModel().getSelectedItem() != null) {
                ouvrirFiche(salleAttente.getSelectionModel().getSelectedItem());
            }
        });
        salleAttente.setPlaceholder(new Label("Personne en attente"));
    }

    private static TableColumn<RdvDTO, String> col(String titre, double largeur,
            java.util.function.Function<RdvDTO, String> extracteur) {
        TableColumn<RdvDTO, String> c = new TableColumn<>(titre);
        c.setPrefWidth(largeur);
        c.setCellValueFactory(d -> {
            String v = extracteur.apply(d.getValue());
            return new SimpleStringProperty(v == null ? "" : v);
        });
        return c;
    }

    private void chargerPraticiensPuisJournee() {
        Async.executer(serviceRdv::praticiens,
                liste -> {
                    praticiens = liste;
                    List<UtilisateurDTO> options = new ArrayList<>();
                    options.add(null);
                    options.addAll(liste);
                    filtrePraticien.setItems(FXCollections.observableArrayList(options));
                    filtrePraticien.setValue(null);
                    chargerJournee();
                },
                e -> {
                    afficherErreur("Impossible de charger les praticiens", e);
                    chargerJournee();
                });
    }

    private void chargerJournee() {
        labelJour.setText(capitalize(jour.format(FORMAT_JOUR)));
        LocalDateTime debut = jour.atStartOfDay();
        LocalDateTime fin = jour.plusDays(1).atStartOfDay();
        UtilisateurDTO filtre = filtrePraticien.getValue();
        Long praticienId = filtre == null ? null : filtre.id();
        labelStatut.setText("Chargement…");

        Async.executer(() -> serviceRdv.lister(debut, fin, praticienId),
                liste -> {
                    tableau.getItems().setAll(liste);
                    salleAttente.getItems().setAll(liste.stream()
                            .filter(r -> r.statut == StatutRdv.EN_SALLE_ATTENTE)
                            .toList());
                    labelStatut.setText(liste.size() + " rendez-vous"
                            + (Session.estModeDemonstration()
                                    ? " — mode démonstration, rien n'est enregistré" : ""));
                },
                e -> {
                    labelStatut.setText("");
                    afficherErreur("Impossible de charger l'agenda", e);
                });
    }

    private void ouvrirFiche(RdvDTO existant) {
        if (praticiens.isEmpty() && !Session.estModeDemonstration()) {
            afficherErreur("Aucun praticien",
                    new IllegalStateException(
                            "Créez d'abord un compte dentiste dans Administration."));
            return;
        }
        List<UtilisateurDTO> listePraticiens = praticiens.isEmpty()
                ? List.of(new UtilisateurDTO(1L, "demo", "Towe", "Nadine",
                        com.lesourire.commun.Role.DENTISTE, null, null, true))
                : praticiens;

        RdvDTO prefill = existant;
        if (prefill == null) {
            prefill = new RdvDTO();
            prefill.debut = LocalDateTime.of(jour, LocalTime.of(9, 0));
            prefill.fin = prefill.debut.plusMinutes(30);
        }

        Dialogues.afficher(new FicheRdvDialogue(prefill, servicePatients, listePraticiens),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = saisie.id == null;
            Async.executer(
                    () -> creation ? serviceRdv.creer(saisie) : serviceRdv.modifier(saisie.id, saisie),
                    ok -> chargerJournee(),
                    e -> {
                        afficherErreur("Impossible d'enregistrer le rendez-vous", e);
                        ouvrirFiche(saisie);
                    });
        });
    }

    private void appliquerStatut(StatutRdv statut) {
        RdvDTO sel = tableau.getSelectionModel().getSelectedItem();
        if (sel == null) {
            sel = salleAttente.getSelectionModel().getSelectedItem();
        }
        if (sel == null) {
            afficherErreur("Aucune sélection",
                    new IllegalStateException("Sélectionnez un rendez-vous."));
            return;
        }
        Long id = sel.id;
        Async.executer(() -> serviceRdv.changerStatut(id, statut),
                ok -> chargerJournee(),
                e -> afficherErreur("Impossible de changer le statut", e));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void afficherErreur(String entete, Exception e) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        alerte.setHeaderText(entete);
        Dialogues.afficherSansResultat(alerte, racine.getScene().getWindow());
    }
}
