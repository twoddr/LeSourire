package com.lesourire.client.coeur;

import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Affichage de dialogues en préservant l'état maximisé de la fenêtre
 * propriétaire.
 * Sous Linux, JavaFX retire souvent le maximisé à l'ouverture d'un Dialog modal
 * avec {@code initOwner} — on le rétablit à la fermeture.
 */
public final class Dialogues {

    private Dialogues() {
    }

    public static <T> Optional<T> afficher(Dialog<T> dialogue, Window proprietaire) {
        boolean maximise = proprietaire instanceof Stage stage && stage.isMaximized();
        if (proprietaire != null) {
            dialogue.initOwner(proprietaire);
        }
        Optional<T> resultat = dialogue.showAndWait();
        restaurerMaximise(proprietaire, maximise);
        return resultat;
    }

    public static void afficherSansResultat(Dialog<?> dialogue, Window proprietaire) {
        afficher(dialogue, proprietaire);
    }

    private static void restaurerMaximise(Window proprietaire, boolean maximise) {
        if (maximise && proprietaire instanceof Stage stage && !stage.isMaximized()) {
            Platform.runLater(() -> stage.setMaximized(true));
        }
    }

}
