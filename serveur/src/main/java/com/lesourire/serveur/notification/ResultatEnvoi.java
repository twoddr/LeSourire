package com.lesourire.serveur.notification;

/** Résultat d'une tentative d'envoi de notification. */
public record ResultatEnvoi(boolean succes, String messageErreur) {

    public static ResultatEnvoi ok() {
        return new ResultatEnvoi(true, null);
    }

    public static ResultatEnvoi echec(String messageErreur) {
        return new ResultatEnvoi(false, messageErreur);
    }
}
