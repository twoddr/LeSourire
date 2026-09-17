package com.lesourire.commun;

/**
 * Canal utilisé pour notifier un patient (confirmation de rendez-vous, rappel J-2…).
 *
 * <p>Le cabinet est au Cameroun : le SMS est le canal de masse, WhatsApp est
 * utilisé en repli (envoi assisté « click-to-chat ») et l'e-mail reste le
 * dernier recours. Le choix {@link #AUTO} laisse le serveur décider en
 * fonction des coordonnées renseignées sur la fiche patient.</p>
 */
public enum CanalNotification {
    AUTO("Automatique"),
    SMS("SMS"),
    WHATSAPP("WhatsApp"),
    EMAIL("E-mail");

    private final String libelle;

    CanalNotification(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
