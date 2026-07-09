package com.lesourire.commun.dto;

import com.lesourire.commun.Role;

/**
 * Représentation d'un utilisateur telle qu'échangée entre serveur et client.
 * Ne contient jamais le mot de passe.
 */
public record UtilisateurDTO(
        Long id,
        String nomUtilisateur,
        String nom,
        String prenom,
        Role role,
        String email) {

    public String nomComplet() {
        String n = nom == null ? "" : nom;
        String p = prenom == null ? "" : prenom;
        String complet = (p + " " + n).trim();
        return complet.isEmpty() ? nomUtilisateur : complet;
    }
}
