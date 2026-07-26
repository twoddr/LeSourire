package com.lesourire.client.vue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import com.lesourire.client.coeur.Async;
import com.lesourire.client.service.ServicePatients;
import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.TypeRdv;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/** Fiche de création / modification d'un rendez-vous. */
public class FicheRdvDialogue extends Dialog<RdvDTO> {

    private final ComboBox<PatientDTO> champPatient = new ComboBox<>();
    private final TextField champRecherchePatient = new TextField();
    private final ComboBox<UtilisateurDTO> champPraticien = new ComboBox<>();
    private final DatePicker champDate = new DatePicker(LocalDate.now());
    private final Spinner<Integer> champHeure = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(7, 20, 9));
    private final Spinner<Integer> champMinute = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 55, 0, 5));
    private final Spinner<Integer> champDuree = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 240, 30, 15));
    private final ComboBox<TypeRdv> champType = new ComboBox<>(
            FXCollections.observableArrayList(TypeRdv.values()));
    private final ComboBox<StatutRdv> champStatut = new ComboBox<>(
            FXCollections.observableArrayList(StatutRdv.values()));
    private final TextField champMotif = new TextField();
    private final TextArea champNotes = new TextArea();
    private final Label labelErreur = new Label();

    public FicheRdvDialogue(RdvDTO existant, ServicePatients servicePatients,
            List<UtilisateurDTO> praticiens) {
        boolean creation = existant == null || existant.id == null;
        setTitle(creation ? "Nouveau rendez-vous" : "Modifier le rendez-vous");
        setResizable(true);

        champPraticien.setItems(FXCollections.observableArrayList(praticiens));
        champPraticien.setConverter(converterPraticien());
        champPraticien.setMaxWidth(Double.MAX_VALUE);

        champPatient.setEditable(false);
        champPatient.setMaxWidth(Double.MAX_VALUE);
        champPatient.setConverter(converterPatient());
        champRecherchePatient.setPromptText("Rechercher un patient…");
        champRecherchePatient.textProperty().addListener((o, a, n) ->
                Async.executer(() -> servicePatients.rechercher(n),
                        liste -> {
                            champPatient.setItems(FXCollections.observableArrayList(liste));
                            if (!liste.isEmpty()) {
                                champPatient.show();
                            }
                        },
                        e -> { /* silencieux pendant la frappe */ }));

        champType.setConverter(new StringConverter<>() {
            @Override
            public String toString(TypeRdv t) {
                return t == null ? "" : t.getLibelle();
            }

            @Override
            public TypeRdv fromString(String s) {
                return Arrays.stream(TypeRdv.values())
                        .filter(t -> t.getLibelle().equals(s)).findFirst().orElse(null);
            }
        });
        champStatut.setConverter(new StringConverter<>() {
            @Override
            public String toString(StatutRdv s) {
                return s == null ? "" : s.getLibelle();
            }

            @Override
            public StatutRdv fromString(String s) {
                return Arrays.stream(StatutRdv.values())
                        .filter(t -> t.getLibelle().equals(s)).findFirst().orElse(null);
            }
        });

        champHeure.setEditable(true);
        champMinute.setEditable(true);
        champDuree.setEditable(true);
        champNotes.setPrefRowCount(3);

        GridPane grille = new GridPane();
        grille.setHgap(12);
        grille.setVgap(10);
        grille.setPadding(new Insets(8));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grille.getColumnConstraints().addAll(c1, c2);

        int l = 0;
        grille.add(new Label("Patient"), 0, l);
        grille.add(champRecherchePatient, 1, l++);
        grille.add(new Label("Sélection"), 0, l);
        grille.add(champPatient, 1, l++);
        grille.add(new Label("Praticien"), 0, l);
        grille.add(champPraticien, 1, l++);
        grille.add(new Label("Date"), 0, l);
        grille.add(champDate, 1, l++);
        grille.add(new Label("Heure"), 0, l);
        grille.add(new javafx.scene.layout.HBox(8, champHeure, new Label(":"), champMinute), 1, l++);
        grille.add(new Label("Durée (min)"), 0, l);
        grille.add(champDuree, 1, l++);
        grille.add(new Label("Type"), 0, l);
        grille.add(champType, 1, l++);
        grille.add(new Label("Statut"), 0, l);
        grille.add(champStatut, 1, l++);
        grille.add(new Label("Motif"), 0, l);
        grille.add(champMotif, 1, l++);
        grille.add(new Label("Notes"), 0, l);
        grille.add(champNotes, 1, l++);
        labelErreur.getStyleClass().add("label-erreur");
        labelErreur.setWrapText(true);
        grille.add(labelErreur, 0, l, 2, 1);

        champType.setValue(existant != null && existant.type != null
                ? existant.type : TypeRdv.CONSULTATION);
        champStatut.setValue(existant != null && existant.statut != null
                ? existant.statut : StatutRdv.PLANIFIE);
        if (!praticiens.isEmpty()) {
            champPraticien.setValue(praticiens.get(0));
        }

        if (existant != null) {
            if (existant.patientId != null) {
                PatientDTO fantome = new PatientDTO();
                fantome.id = existant.patientId;
                fantome.nom = existant.patientNom;
                champPatient.setItems(FXCollections.observableArrayList(fantome));
                champPatient.setValue(fantome);
            }
            if (existant.praticienId != null) {
                praticiens.stream().filter(p -> p.id().equals(existant.praticienId))
                        .findFirst().ifPresent(champPraticien::setValue);
            }
            if (existant.debut != null) {
                champDate.setValue(existant.debut.toLocalDate());
                champHeure.getValueFactory().setValue(existant.debut.getHour());
                champMinute.getValueFactory().setValue(
                        (existant.debut.getMinute() / 5) * 5);
                if (existant.fin != null) {
                    long minutes = java.time.Duration.between(existant.debut, existant.fin)
                            .toMinutes();
                    champDuree.getValueFactory().setValue((int) Math.max(15, minutes));
                }
            }
            champMotif.setText(existant.motif);
            champNotes.setText(existant.notes);
        }

        if (existant == null || existant.patientId == null) {
            Async.executer(() -> servicePatients.rechercher(""),
                    liste -> champPatient.setItems(FXCollections.observableArrayList(liste)),
                    e -> { });
        }

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(520, 520);
        ButtonType ok = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = valider();
            if (err != null) {
                labelErreur.setText(err);
                e.consume();
            }
        });

        setResultConverter(b -> {
            if (b != ok) {
                return null;
            }
            RdvDTO dto = new RdvDTO();
            if (existant != null) {
                dto.id = existant.id;
                dto.acteOrigineId = existant.acteOrigineId;
            }
            PatientDTO patient = champPatient.getValue();
            dto.patientId = patient.id;
            dto.patientNom = patient.nomComplet();
            dto.patientTelephone = patient.telephone;
            UtilisateurDTO praticien = champPraticien.getValue();
            dto.praticienId = praticien.id();
            dto.praticienNom = praticien.nomComplet();
            LocalDateTime debut = LocalDateTime.of(champDate.getValue(),
                    LocalTime.of(champHeure.getValue(), champMinute.getValue()));
            dto.debut = debut;
            dto.fin = debut.plusMinutes(champDuree.getValue());
            dto.type = champType.getValue();
            dto.statut = champStatut.getValue();
            dto.motif = champMotif.getText();
            dto.notes = champNotes.getText();
            return dto;
        });
    }

    private String valider() {
        if (champPatient.getValue() == null || champPatient.getValue().id == null) {
            return "Sélectionnez un patient.";
        }
        if (champPraticien.getValue() == null) {
            return "Sélectionnez un praticien.";
        }
        if (champDate.getValue() == null) {
            return "La date est obligatoire.";
        }
        if (champType.getValue() == null || champStatut.getValue() == null) {
            return "Type et statut sont obligatoires.";
        }
        return null;
    }

    private static StringConverter<PatientDTO> converterPatient() {
        return new StringConverter<>() {
            @Override
            public String toString(PatientDTO p) {
                if (p == null) {
                    return "";
                }
                String dossier = p.numeroDossier == null ? "" : " (" + p.numeroDossier + ")";
                return p.nomComplet() + dossier;
            }

            @Override
            public PatientDTO fromString(String s) {
                return null;
            }
        };
    }

    private static StringConverter<UtilisateurDTO> converterPraticien() {
        return new StringConverter<>() {
            @Override
            public String toString(UtilisateurDTO u) {
                return u == null ? "" : u.nomComplet();
            }

            @Override
            public UtilisateurDTO fromString(String s) {
                return null;
            }
        };
    }
}
