package com.lesourire.commun;

/** Énumérations liées aux rappels automatiques envoyés aux patients. */
public final class Rappels {

    private Rappels() {
    }

    /** Raison du rappel. */
    public enum Type {
        CONFIRMATION_RDV("Confirmation"),
        RAPPEL_RDV("Rappel"),
        REVISITE("Revisite");

        private final String libelle;

        Type(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Canal d'envoi du rappel. */
    public enum Canal {
        EMAIL("E-mail"),
        WHATSAPP("WhatsApp"),
        SMS("SMS");

        private final String libelle;

        Canal(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** État d'un rappel programmé. */
    public enum Statut {
        EN_ATTENTE("En attente"),
        ENVOYE("Envoyé"),
        ECHEC("Échec"),
        ANNULE("Annulé");

        private final String libelle;

        Statut(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }
}
