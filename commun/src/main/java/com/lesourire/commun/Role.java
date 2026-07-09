package com.lesourire.commun;

/**
 * Rôles des utilisateurs de l'application.
 * Chaque rôle détermine les modules accessibles côté client
 * et les autorisations côté serveur.
 */
public enum Role {
    DENTISTE("Dentiste"),
    ASSISTANT("Assistant(e)"),
    SECRETAIRE("Secrétaire"),
    COMPTABLE("Comptable"),
    ADMINISTRATEUR("Administrateur");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
