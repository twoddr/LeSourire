package com.lesourire.client.vue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.lesourire.commun.dto.RdvDTO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

/**
 * Grille horaire (jour ou semaine) : blocs proportionnels à la durée,
 * clic dans une zone vide pour créer, double-clic sur un bloc pour ouvrir.
 */
public class AgendaGrille extends VBox {

    private static final LocalTime HEURE_DEBUT = LocalTime.of(7, 0);
    private static final LocalTime HEURE_FIN = LocalTime.of(20, 0);
    private static final double PX_PAR_MIN = 1.2;
    private static final double LARGEUR_HEURES = 56;
    private static final double HAUTEUR_ENTETE = 36;
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMAT_JOUR_COURT =
            DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH);

    private final HBox bandeauJours = new HBox();
    private final Pane fond = new Pane();
    private final Pane couchesRdv = new Pane();
    private final ScrollPane scroll = new ScrollPane();
    private final Pane echelleHeures = new Pane();

    private LocalDate debutPeriode = LocalDate.now();
    private int nbJours = 1;
    private List<RdvDTO> rdvsCourants = List.of();
    private RdvDTO selection;
    private Consumer<LocalDateTime> onCreer;
    private Consumer<RdvDTO> onOuvrir;
    private Consumer<RdvDTO> onSelection;

    public AgendaGrille() {
        getStyleClass().add("agenda-grille");
        setSpacing(0);

        bandeauJours.getStyleClass().add("agenda-entetes-jours");

        echelleHeures.getStyleClass().add("agenda-echelle-heures");
        echelleHeures.setPrefWidth(LARGEUR_HEURES);
        echelleHeures.setMinWidth(LARGEUR_HEURES);
        construireEchelleHeures();

        StackPane zone = new StackPane(fond, couchesRdv);
        zone.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(zone, Priority.ALWAYS);

        HBox ligneTemps = new HBox(echelleHeures, zone);
        HBox.setHgrow(zone, Priority.ALWAYS);

        scroll.setContent(ligneTemps);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("agenda-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(bandeauJours, scroll);

        fond.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 1) {
                return;
            }
            LocalDateTime creneau = coordonneesVersDateHeure(e.getX(), e.getY());
            if (creneau != null && onCreer != null) {
                selectionner(null);
                onCreer.accept(creneau);
            }
        });

        zone.widthProperty().addListener((o, a, b) -> {
            peindreFond();
            peindreRdvs(rdvsCourants);
        });
    }

    public void setOnCreer(Consumer<LocalDateTime> onCreer) {
        this.onCreer = onCreer;
    }

    public void setOnOuvrir(Consumer<RdvDTO> onOuvrir) {
        this.onOuvrir = onOuvrir;
    }

    public void setOnSelection(Consumer<RdvDTO> onSelection) {
        this.onSelection = onSelection;
    }

    public RdvDTO getSelection() {
        return selection;
    }

    public void afficher(LocalDate debutPeriode, int nbJours, List<RdvDTO> rdvs) {
        this.debutPeriode = debutPeriode;
        this.nbJours = Math.max(1, nbJours);
        this.rdvsCourants = rdvs == null ? List.of() : List.copyOf(rdvs);
        peindreEntetes();
        peindreFond();
        peindreRdvs(rdvsCourants);
    }

    private void construireEchelleHeures() {
        echelleHeures.getChildren().clear();
        double hauteur = hauteurGrille();
        echelleHeures.setPrefHeight(hauteur);
        echelleHeures.setMinHeight(hauteur);

        int minutesTotal = minutesEntre(HEURE_DEBUT, HEURE_FIN);
        for (int m = 0; m <= minutesTotal; m += 60) {
            LocalTime t = HEURE_DEBUT.plusMinutes(m);
            Label lab = new Label(t.format(FORMAT_HEURE));
            lab.getStyleClass().add("agenda-label-heure");
            lab.setLayoutX(4);
            lab.setLayoutY(m * PX_PAR_MIN - 7);
            echelleHeures.getChildren().add(lab);
        }
    }

    private void peindreEntetes() {
        bandeauJours.getChildren().clear();
        Region spacerHeures = new Region();
        spacerHeures.setPrefWidth(LARGEUR_HEURES);
        spacerHeures.setMinWidth(LARGEUR_HEURES);
        spacerHeures.setPrefHeight(HAUTEUR_ENTETE);
        bandeauJours.getChildren().add(spacerHeures);

        LocalDate aujourdhui = LocalDate.now();
        for (int i = 0; i < nbJours; i++) {
            LocalDate d = debutPeriode.plusDays(i);
            Label lab = new Label(capitalize(d.format(FORMAT_JOUR_COURT)));
            lab.setAlignment(Pos.CENTER);
            lab.setMaxWidth(Double.MAX_VALUE);
            lab.setPrefHeight(HAUTEUR_ENTETE);
            lab.getStyleClass().add("agenda-entete-jour");
            if (d.equals(aujourdhui)) {
                lab.getStyleClass().add("agenda-entete-aujourdhui");
            }
            HBox.setHgrow(lab, Priority.ALWAYS);
            bandeauJours.getChildren().add(lab);
        }
    }

    private void peindreFond() {
        fond.getChildren().clear();
        double largeur = Math.max(200, Math.max(fond.getWidth(), couchesRdv.getWidth()));
        double hauteur = hauteurGrille();
        fond.setPrefSize(largeur, hauteur);
        fond.setMinSize(Region.USE_COMPUTED_SIZE, hauteur);
        couchesRdv.setPrefSize(largeur, hauteur);
        couchesRdv.setMinSize(Region.USE_COMPUTED_SIZE, hauteur);

        int minutesTotal = minutesEntre(HEURE_DEBUT, HEURE_FIN);
        for (int m = 0; m <= minutesTotal; m += 30) {
            double y = m * PX_PAR_MIN;
            Line ligne = new Line(0, y, largeur, y);
            ligne.endXProperty().bind(fond.widthProperty());
            ligne.getStyleClass().add(m % 60 == 0 ? "agenda-ligne-heure" : "agenda-ligne-demi");
            ligne.setMouseTransparent(true);
            fond.getChildren().add(ligne);
        }

        if (nbJours > 1) {
            for (int i = 1; i < nbJours; i++) {
                final int idx = i;
                Line vert = new Line();
                vert.startYProperty().set(0);
                vert.endYProperty().bind(fond.heightProperty());
                vert.startXProperty().bind(fond.widthProperty().divide(nbJours).multiply(idx));
                vert.endXProperty().bind(vert.startXProperty());
                vert.getStyleClass().add("agenda-ligne-jour");
                vert.setMouseTransparent(true);
                fond.getChildren().add(vert);
            }
        }
    }

    private void peindreRdvs(List<RdvDTO> rdvs) {
        couchesRdv.getChildren().clear();
        Map<LocalDate, List<RdvDTO>> parJour = new HashMap<>();
        for (RdvDTO r : rdvs) {
            if (r.debut == null || r.fin == null) {
                continue;
            }
            LocalDate d = r.debut.toLocalDate();
            if (d.isBefore(debutPeriode) || !d.isBefore(debutPeriode.plusDays(nbJours))) {
                continue;
            }
            parJour.computeIfAbsent(d, k -> new ArrayList<>()).add(r);
        }

        double largeur = Math.max(fond.getWidth(), 200);

        for (int i = 0; i < nbJours; i++) {
            LocalDate d = debutPeriode.plusDays(i);
            List<Placement> placements = calculerPlacements(parJour.getOrDefault(d, List.of()));
            for (Placement p : placements) {
                couchesRdv.getChildren().add(creerBloc(p, i, largeur));
            }
        }

        if (selection != null) {
            Long id = selection.id;
            selection = rdvs.stream().filter(r -> id != null && id.equals(r.id)).findFirst()
                    .orElse(null);
        }
        appliquerStyleSelection();
    }

    private Region creerBloc(Placement p, int indexJour, double largeur) {
        RdvDTO rdv = p.rdv;
        VBox bloc = new VBox(2);
        bloc.getStyleClass().addAll("agenda-bloc-rdv", classeStatut(rdv));
        bloc.setPadding(new Insets(4, 6, 4, 6));

        Label titre = new Label(rdv.debut.toLocalTime().format(FORMAT_HEURE)
                + "  " + (rdv.patientNom == null ? "" : rdv.patientNom));
        titre.getStyleClass().add("agenda-bloc-titre");
        titre.setWrapText(true);
        Label detail = new Label(
                (rdv.type == null ? "" : rdv.type.getLibelle())
                        + (rdv.motif == null || rdv.motif.isBlank() ? "" : " — " + rdv.motif));
        detail.getStyleClass().add("agenda-bloc-detail");
        detail.setWrapText(true);
        bloc.getChildren().add(titre);
        if (blocHauteur(rdv) > 28) {
            bloc.getChildren().add(detail);
        }

        String tip = (rdv.patientNom == null ? "" : rdv.patientNom) + "\n"
                + rdv.debut.format(FORMAT_HEURE) + " – " + rdv.fin.format(FORMAT_HEURE)
                + (rdv.praticienNom == null ? "" : "\n" + rdv.praticienNom)
                + (rdv.statut == null ? "" : "\n" + rdv.statut.getLibelle());
        Tooltip.install(bloc, new Tooltip(tip));

        double colW = largeur / nbJours;
        double x = indexJour * colW + 2 + p.colonne * ((colW - 4) / p.nbColonnes);
        double w = Math.max(24, (colW - 4) / p.nbColonnes - 2);
        double y = minutesDepuisOuverture(rdv.debut) * PX_PAR_MIN;
        double h = Math.max(18, blocHauteur(rdv));

        bloc.setLayoutX(x);
        bloc.setLayoutY(Math.max(0, y));
        bloc.setPrefSize(w, h);
        bloc.setMinSize(w, h);
        bloc.setMaxSize(w, h);
        bloc.setUserData(rdv);

        bloc.setOnMouseClicked(e -> {
            e.consume();
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            selectionner(rdv);
            if (e.getClickCount() >= 2 && onOuvrir != null) {
                onOuvrir.accept(rdv);
            }
        });
        return bloc;
    }

    private void selectionner(RdvDTO rdv) {
        selection = rdv;
        appliquerStyleSelection();
        if (onSelection != null) {
            onSelection.accept(rdv);
        }
    }

    private void appliquerStyleSelection() {
        for (var n : couchesRdv.getChildren()) {
            n.getStyleClass().remove("agenda-bloc-selectionne");
            Object ud = n.getUserData();
            if (selection != null && ud instanceof RdvDTO r
                    && selection.id != null && selection.id.equals(r.id)) {
                n.getStyleClass().add("agenda-bloc-selectionne");
            }
        }
    }

    private static List<Placement> calculerPlacements(List<RdvDTO> jour) {
        List<RdvDTO> tries = new ArrayList<>(jour);
        tries.sort(Comparator.comparing((RdvDTO r) -> r.debut).thenComparing(r -> r.fin));

        List<List<RdvDTO>> groupes = new ArrayList<>();
        for (RdvDTO r : tries) {
            List<List<RdvDTO>> touches = new ArrayList<>();
            for (List<RdvDTO> g : groupes) {
                if (g.stream().anyMatch(x -> chevauche(x, r))) {
                    touches.add(g);
                }
            }
            if (touches.isEmpty()) {
                List<RdvDTO> g = new ArrayList<>();
                g.add(r);
                groupes.add(g);
            } else {
                List<RdvDTO> base = touches.get(0);
                base.add(r);
                for (int i = 1; i < touches.size(); i++) {
                    List<RdvDTO> autre = touches.get(i);
                    base.addAll(autre);
                    groupes.remove(autre);
                }
            }
        }

        List<Placement> resultat = new ArrayList<>();
        for (List<RdvDTO> groupe : groupes) {
            List<LocalDateTime> finColonne = new ArrayList<>();
            Map<RdvDTO, Integer> colonnes = new HashMap<>();
            for (RdvDTO r : groupe.stream()
                    .sorted(Comparator.comparing((RdvDTO x) -> x.debut).thenComparing(x -> x.fin))
                    .toList()) {
                int c = 0;
                while (c < finColonne.size() && r.debut.isBefore(finColonne.get(c))) {
                    c++;
                }
                if (c == finColonne.size()) {
                    finColonne.add(r.fin);
                } else {
                    finColonne.set(c, r.fin);
                }
                colonnes.put(r, c);
            }
            int nb = Math.max(1, finColonne.size());
            for (RdvDTO r : groupe) {
                resultat.add(new Placement(r, colonnes.getOrDefault(r, 0), nb));
            }
        }
        return resultat;
    }

    private static boolean chevauche(RdvDTO a, RdvDTO b) {
        return a.debut.isBefore(b.fin) && b.debut.isBefore(a.fin);
    }

    private LocalDateTime coordonneesVersDateHeure(double x, double y) {
        double largeur = Math.max(fond.getWidth(), 1);
        int indexJour = (int) Math.min(nbJours - 1, Math.max(0, x / (largeur / nbJours)));
        int minutes = (int) Math.max(0, y / PX_PAR_MIN);
        minutes = (minutes / 15) * 15;
        int maxMin = minutesEntre(HEURE_DEBUT, HEURE_FIN) - 15;
        minutes = Math.min(minutes, maxMin);
        LocalDate jour = debutPeriode.plusDays(indexJour);
        return LocalDateTime.of(jour, HEURE_DEBUT.plusMinutes(minutes));
    }

    private static double blocHauteur(RdvDTO rdv) {
        long min = Math.max(15, Duration.between(rdv.debut, rdv.fin).toMinutes());
        return min * PX_PAR_MIN;
    }

    private static double minutesDepuisOuverture(LocalDateTime debut) {
        LocalTime t = debut.toLocalTime();
        if (t.isBefore(HEURE_DEBUT)) {
            return 0;
        }
        return minutesEntre(HEURE_DEBUT, t);
    }

    private static int minutesEntre(LocalTime a, LocalTime b) {
        return (int) Duration.between(a, b).toMinutes();
    }

    private static double hauteurGrille() {
        return minutesEntre(HEURE_DEBUT, HEURE_FIN) * PX_PAR_MIN;
    }

    private static String classeStatut(RdvDTO rdv) {
        if (rdv.statut == null) {
            return "rdv-bloc-planifie";
        }
        return "rdv-bloc-" + rdv.statut.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private record Placement(RdvDTO rdv, int colonne, int nbColonnes) {
    }
}
