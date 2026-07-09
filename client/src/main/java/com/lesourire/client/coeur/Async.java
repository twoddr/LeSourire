package com.lesourire.client.coeur;

import java.util.function.Consumer;

import javafx.application.Platform;

/** Exécute un appel (réseau) hors du fil JavaFX et rapatrie le résultat dessus. */
public final class Async {

    private Async() {
    }

    @FunctionalInterface
    public interface Appel<T> {
        T executer() throws Exception;
    }

    public static <T> void executer(Appel<T> appel, Consumer<T> surSucces, Consumer<Exception> surEchec) {
        Thread thread = new Thread(() -> {
            try {
                T resultat = appel.executer();
                Platform.runLater(() -> surSucces.accept(resultat));
            } catch (Exception e) {
                Platform.runLater(() -> surEchec.accept(e));
            }
        }, "appel-serveur");
        thread.setDaemon(true);
        thread.start();
    }
}
