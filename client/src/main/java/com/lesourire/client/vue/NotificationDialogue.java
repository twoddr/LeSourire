package com.lesourire.client.vue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RdvDTO;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

/**
 * Notification manuelle d'un patient, ouverte au clic droit sur un rendez-vous.
 *
 * <p>Le rendez-vous concerné est rappelé en tête du dialogue : c'est lui qui
 * porte le message (« votre RDV le … ») et la fiche patient d'où le serveur
 * tire l'adresse d'envoi. Le message peut rester vide : le modèle standard du
 * cabinet est alors appliqué par le serveur.</p>
 */
public class NotificationDialogue extends Dialog<NotificationDialogue.Saisie> {

    private static final DateTimeFormatter FORMAT_RDV =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    /** Ce que le dialogue rend à l'appelant : le rendez-vous est déjà connu. */
    public record Saisie(Rappels.Type type, Rappels.Canal canal, String contenu) {
    }

    private final ComboBox<Rappels.Type> champType = new ComboBox<>(
            FXCollections.observableArrayList(Rappels.Type.values()));
    private final ComboBox<Rappels.Canal> champCanal = new ComboBox<>(
            FXCollections.observableArrayList(Rappels.Canal.values()));
    private final TextArea champMessage = new TextArea();
    private final Label labelErreur = new Label();

    public NotificationDialogue(RdvDTO rdv) {
        setTitle("Notifier le patient");
        setResizable(true);

        champType.setConverter(converter(t -> t == null ? "" : t.getLibelle()));
        champType.setMaxWidth(Double.MAX_VALUE);
        champType.setValue(defautType(rdv));

        champCanal.setConverter(converter(c -> c == null ? "" : c.getLibelle()));
        champCanal.setMaxWidth(Double.MAX_VALUE);
        champCanal.setValue(Rappels.Canal.SMS);

        champMessage.setPromptText("Laissez vide pour utiliser le message standard du cabinet.");
        champMessage.setPrefRowCount(4);
        champMessage.setWrapText(true);

        Label aide = new Label("Le SMS part automatiquement ; WhatsApp et l'e-mail s'ouvrent "
                + "depuis le panneau « Notifications à envoyer », puis se confirment d'un clic.");
        aide.setWrapText(true);
        aide.getStyleClass().add("note-discrete");

        GridPane grille = new GridPane();
        grille.setHgap(10);
        grille.setVgap(8);
        grille.setPadding(new Insets(4));
        ColumnConstraints c0 = new ColumnConstraints();
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(260);
        grille.getColumnConstraints().addAll(c0, c1);

        int l = 0;
        grille.add(new Label("Patient"), 0, l);
        grille.add(new Label(rdv.patientNom == null ? "—" : rdv.patientNom), 1, l++);
        grille.add(new Label("Rendez-vous"), 0, l);
        grille.add(new Label((rdv.debut == null ? "—" : rdv.debut.format(FORMAT_RDV))
                + (rdv.type == null ? "" : "  ·  " + rdv.type.getLibelle())), 1, l++);
        grille.add(new Label("Téléphone"), 0, l);
        grille.add(new Label(rdv.patientTelephone == null || rdv.patientTelephone.isBlank()
                ? "non renseigné" : rdv.patientTelephone), 1, l++);
        grille.add(new Label("Motif"), 0, l);
        grille.add(champType, 1, l++);
        grille.add(new Label("Canal"), 0, l);
        grille.add(champCanal, 1, l++);
        grille.add(new Label("Message"), 0, l);
        grille.add(champMessage, 1, l++);
        grille.add(aide, 0, l, 2, 1);
        labelErreur.getStyleClass().add("label-erreur");
        grille.add(labelErreur, 0, ++l, 2, 1);

        getDialogPane().setContent(grille);
        getDialogPane().setPrefSize(520, 440);
        ButtonType ok = new ButtonType("Programmer l'envoi", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String erreur = valider();
            if (erreur != null) {
                labelErreur.setText(erreur);
                e.consume();
            }
        });

        setResultConverter(b -> {
            if (b != ok) {
                return null;
            }
            String message = champMessage.getText();
            return new Saisie(champType.getValue(), champCanal.getValue(),
                    message == null || message.isBlank() ? null : message.trim());
        });
    }

    /** Une confirmation pour un rendez-vous à venir, un rappel sinon. */
    private static Rappels.Type defautType(RdvDTO rdv) {
        if (rdv.debut != null && rdv.debut.isAfter(LocalDateTime.now())) {
            return Rappels.Type.CONFIRMATION_RDV;
        }
        return Rappels.Type.RAPPEL_RDV;
    }

    private String valider() {
        if (champType.getValue() == null) {
            return "Choisissez le motif de la notification.";
        }
        if (champCanal.getValue() == null) {
            return "Choisissez le canal d'envoi.";
        }
        return null;
    }

    private static <T> StringConverter<T> converter(java.util.function.Function<T, String> libelle) {
        return new StringConverter<>() {
            @Override
            public String toString(T valeur) {
                return libelle.apply(valeur);
            }

            @Override
            public T fromString(String texte) {
                return null;
            }
        };
    }
}