package com.lesourire.commun;

/** Énumérations liées aux rappels automatiques envoyés aux patients. */
public final class Rappels {

    private Rappels() {
    }

    /** Raison du rappel. */
    public enum Type {
        RAPPEL_RDV,     // rappel J-2 avant un rendez-vous
        REVISITE        // invitation à une revisite après intervention
    }

    /** Canal d'envoi du rappel. */
    public enum Canal {
        EMAIL,
        WHATSAPP,
        SMS
    }

    /** État d'un rappel programmé. */
    public enum Statut {
        EN_ATTENTE,
        ENVOYE,
        ECHEC,
        ANNULE
    }
}
