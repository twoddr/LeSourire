package com.lesourire.commun;

/** Cycle de vie d'un rendez-vous. */
public enum StatutRdv {
    PLANIFIE("Planifié"),
    CONFIRME("Confirmé"),
    EN_SALLE_ATTENTE("En salle d'attente"),
    HONORE("Honoré"),
    ANNULE("Annulé"),
    ABSENT("Absent");

    private final String libelle;

    StatutRdv(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
