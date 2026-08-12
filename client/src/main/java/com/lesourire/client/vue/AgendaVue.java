package com.lesourire.client.vue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Module Agenda : grille horaire jour/semaine et salle d'attente. */
public class AgendaVue {

    private static final DateTimeFormatter FORMAT_JOUR =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_SEMAINE_DEBUT =
            DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_SEMAINE_FIN =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final BorderPane racine = new BorderPane();
    private final ServiceRdv serviceRdv;
    private final ServicePatients servicePatients;

    private LocalDate ancre = LocalDate.now();
    private boolean modeSemaine = false;

    private final Label labelPeriode = new Label();
    private final ComboBox<UtilisateurDTO> filtrePraticien = new ComboBox<>();
    private final AgendaGrille grille = new AgendaGrille();
    private final ListView<RdvDTO> salleAttente = new ListView<>();
    private final VBox panneauSalle = new VBox();
    private final Label labelStatut = new Label();
    private final Button btnPrec = new Button();
    private final Button btnSuiv = new Button();
    private List<UtilisateurDTO> praticiens = new ArrayList<>();
    private RdvDTO selectionSalle;

    public AgendaVue() {
        boolean demo = Session.estModeDemonstration();
        this.serviceRdv = demo ? new ServiceRdvDemo() : new ServiceRdvApi(Session.api());
        this.servicePatients = demo ? new ServicePatientsDemo() : new ServicePatientsApi(Session.api());
        construire();
        chargerPraticiensPuisAgenda();
    }

    public Node getRacine() {
        return racine;
    }

    private void construire() {
        racine.getStyleClass().add("page");
        racine.setPadding(new Insets(28));

        Label titre = new Label("Agenda");
        titre.getStyleClass().add("titre-page");

        ToggleGroup modes = new ToggleGroup();
        ToggleButton btnJour = new ToggleButton("Jour");
        ToggleButton btnSemaine = new ToggleButton("Semaine");
        btnJour.setToggleGroup(modes);
        btnSemaine.setToggleGroup(modes);
        btnJour.setSelected(true);
        btnJour.getStyleClass().add("agenda-toggle");
        btnSemaine.getStyleClass().add("agenda-toggle");
        modes.selectedToggleProperty().addListener((o, ancien, selected) -> {
            if (selected == null) {
                if (ancien != null) {
                    ancien.setSelected(true);
                }
                return;
            }
            modeSemaine = selected == btnSemaine;
            mettreAJourTooltipsNav();
            chargerAgenda();
        });
        HBox toggle = new HBox(0, btnJour, btnSemaine);
        toggle.getStyleClass().add("agenda-toggle-groupe");

        btnPrec.setGraphic(new FontIcon(Material2AL.CHEVRON_LEFT));
        btnPrec.setOnAction(e -> {
            ancre = modeSemaine ? ancre.minusWeeks(1) : ancre.minusDays(1);
            chargerAgenda();
        });

        Button btnAuj = new Button("Aujourd'hui");
        btnAuj.setOnAction(e -> {
            ancre = LocalDate.now();
            chargerAgenda();
        });

        btnSuiv.setGraphic(new FontIcon(Material2AL.CHEVRON_RIGHT));
        btnSuiv.setOnAction(e -> {
            ancre = modeSemaine ? ancre.plusWeeks(1) : ancre.plusDays(1);
            chargerAgenda();
        });
        mettreAJourTooltipsNav();

        labelPeriode.getStyleClass().add("sous-titre-page");

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
        filtrePraticien.setOnAction(e -> chargerAgenda());
        filtrePraticien.setPrefWidth(200);

        Button actualiser = new Button();
        actualiser.setGraphic(new FontIcon(Material2MZ.REFRESH));
        actualiser.setOnAction(e -> chargerAgenda());

        Button nouveau = new Button("Nouveau RDV");
        nouveau.setGraphic(new FontIcon(Material2AL.ADD));
        nouveau.getStyleClass().add("bouton-principal");
        nouveau.setOnAction(e -> ouvrirFiche(null));

        Region espace = new Region();
        HBox.setHgrow(espace, Priority.ALWAYS);

        HBox bandeau = new HBox(10, titre, toggle, espace, btnPrec, btnAuj, btnSuiv,
                filtrePraticien, actualiser, nouveau);
        bandeau.setAlignment(Pos.CENTER_LEFT);

        HBox dateLigne = new HBox(labelPeriode);
        dateLigne.setPadding(new Insets(8, 0, 12, 0));

        grille.setOnCreer(this::ouvrirFicheAuCreneau);
        grille.setOnOuvrir(this::ouvrirFiche);
        grille.setOnSelection(rdv -> selectionSalle = null);
        VBox.setVgrow(grille, Priority.ALWAYS);

        HBox actionsStatut = new HBox(8,
                boutonStatut("Confirmer", StatutRdv.CONFIRME),
                boutonStatut("Arrivé", StatutRdv.EN_SALLE_ATTENTE),
                boutonStatut("Honoré", StatutRdv.HONORE),
                boutonStatut("Absent", StatutRdv.ABSENT),
                boutonStatut("Annuler", StatutRdv.ANNULE));
        actionsStatut.setAlignment(Pos.CENTER_LEFT);

        labelStatut.getStyleClass().add("note-discrete");

        VBox centre = new VBox(10, grille, actionsStatut, labelStatut);
        HBox.setHgrow(centre, Priority.ALWAYS);
        VBox.setVgrow(grille, Priority.ALWAYS);

        Label titreSalle = new Label("Salle d'attente");
        titreSalle.getStyleClass().add("sous-titre-section");
        construireSalleAttente();
        VBox.setVgrow(salleAttente, Priority.ALWAYS);
        panneauSalle.getChildren().setAll(titreSalle, salleAttente);
        panneauSalle.setPrefWidth(260);
        panneauSalle.getStyleClass().add("panneau-salle-attente");
        panneauSalle.setPadding(new Insets(12));

        HBox corps = new HBox(16, centre, panneauSalle);
        HBox.setHgrow(centre, Priority.ALWAYS);

        VBox haut = new VBox(bandeau, dateLigne);
        racine.setTop(haut);
        racine.setCenter(corps);
    }

    private void mettreAJourTooltipsNav() {
        btnPrec.setTooltip(new Tooltip(modeSemaine ? "Semaine précédente" : "Jour précédent"));
        btnSuiv.setTooltip(new Tooltip(modeSemaine ? "Semaine suivante" : "Jour suivant"));
    }

    private Button boutonStatut(String libelle, StatutRdv statut) {
        Button b = new Button(libelle);
        b.setOnAction(e -> appliquerStatut(statut));
        return b;
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
        salleAttente.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            selectionSalle = n;
        });
        salleAttente.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && salleAttente.getSelectionModel().getSelectedItem() != null) {
                ouvrirFiche(salleAttente.getSelectionModel().getSelectedItem());
            }
        });
        salleAttente.setPlaceholder(new Label("Personne en attente"));
    }

    private void chargerPraticiensPuisAgenda() {
        Async.executer(serviceRdv::praticiens,
                liste -> {
                    praticiens = liste;
                    List<UtilisateurDTO> options = new ArrayList<>();
                    options.add(null);
                    options.addAll(liste);
                    filtrePraticien.setItems(FXCollections.observableArrayList(options));
                    filtrePraticien.setValue(null);
                    chargerAgenda();
                },
                e -> {
                    afficherErreur("Impossible de charger les praticiens", e);
                    chargerAgenda();
                });
    }

    private LocalDate debutPeriode() {
        if (modeSemaine) {
            return ancre.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return ancre;
    }

    private int nbJours() {
        return modeSemaine ? 7 : 1;
    }

    private void chargerAgenda() {
        LocalDate debut = debutPeriode();
        int jours = nbJours();
        LocalDate finExclue = debut.plusDays(jours);

        if (modeSemaine) {
            labelPeriode.setText("Semaine du " + debut.format(FORMAT_SEMAINE_DEBUT)
                    + " au " + finExclue.minusDays(1).format(FORMAT_SEMAINE_FIN));
        } else {
            labelPeriode.setText(capitalize(debut.format(FORMAT_JOUR)));
        }

        panneauSalle.setVisible(!modeSemaine);
        panneauSalle.setManaged(!modeSemaine);

        UtilisateurDTO filtre = filtrePraticien.getValue();
        Long praticienId = filtre == null ? null : filtre.id();
        labelStatut.setText("Chargement…");

        Async.executer(() -> serviceRdv.lister(debut.atStartOfDay(), finExclue.atStartOfDay(), praticienId),
                liste -> {
                    grille.afficher(debut, jours, liste);
                    LocalDate jourSalle = modeSemaine ? LocalDate.now() : debut;
                    salleAttente.getItems().setAll(liste.stream()
                            .filter(r -> r.statut == StatutRdv.EN_SALLE_ATTENTE)
                            .filter(r -> r.debut != null && r.debut.toLocalDate().equals(jourSalle))
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

    private void ouvrirFicheAuCreneau(LocalDateTime debut) {
        RdvDTO prefill = new RdvDTO();
        prefill.debut = debut;
        prefill.fin = debut.plusMinutes(30);
        ouvrirFiche(prefill);
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
            LocalDate jour = modeSemaine ? LocalDate.now() : debutPeriode();
            prefill.debut = LocalDateTime.of(jour, LocalTime.of(9, 0));
            prefill.fin = prefill.debut.plusMinutes(30);
        }

        Dialogues.afficher(new FicheRdvDialogue(prefill, servicePatients, listePraticiens),
                racine.getScene().getWindow()).ifPresent(saisie -> {
            boolean creation = saisie.id == null;
            Async.executer(
                    () -> creation ? serviceRdv.creer(saisie) : serviceRdv.modifier(saisie.id, saisie),
                    ok -> {
                        if (saisie.debut != null) {
                            ancre = saisie.debut.toLocalDate();
                        }
                        chargerAgenda();
                    },
                    e -> {
                        afficherErreur("Impossible d'enregistrer le rendez-vous", e);
                        ouvrirFiche(saisie);
                    });
        });
    }

    private void appliquerStatut(StatutRdv statut) {
        RdvDTO sel = grille.getSelection();
        if (sel == null) {
            sel = selectionSalle;
        }
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
                ok -> chargerAgenda(),
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
