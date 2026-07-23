package com.lesourire.commun.dto;

import com.lesourire.commun.Role;

/**
 * Données de création / modification d'un utilisateur.
 * {@code motDePasse} : obligatoire à la création ; si vide en modification,
 * le mot de passe actuel est conservé.
 */
public class UtilisateurEcritureDTO {

    public Long id;
    public String nomUtilisateur;
    public String nom;
    public String prenom;
    public Role role;
    public String email;
    public String telephone;
    public boolean actif = true;
    public String motDePasse;
}
