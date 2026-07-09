package com.lesourire.commun;

/** Nature d'un rendez-vous. */
public enum TypeRdv {
    CONSULTATION("Consultation"),
    SOIN("Soin"),
    CONTROLE("Contrôle"),
    REVISITE("Revisite post-intervention"),
    URGENCE("Urgence");

    private final String libelle;

    TypeRdv(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
