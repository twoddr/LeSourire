package com.lesourire.commun.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiche patient échangée entre serveur et client.
 * Classe mutable à champs publics : elle sert de support direct au formulaire
 * côté client, et Jackson la sérialise telle quelle.
 */
public class PatientDTO {

    public Long id;
    public String numeroDossier;        // généré par le serveur à la création

    // Identité
    public String nom;
    public String prenom;
    public LocalDate dateNaissance;
    public String sexe;                 // "M" ou "F"

    // Contacts
    public String telephone;
    public String telephoneWhatsapp;    // si différent du téléphone principal
    public String email;
    public String adresse;
    public String quartier;
    public String ville;
    public String profession;
    public String personneUrgenceNom;
    public String personneUrgenceTel;

    // Médical
    public String antecedents;
    public String allergies;
    public String notes;

    // Prise en charge : historique complet des couvertures (assureur/société).
    // À la création d'un patient, les couvertures fournies ici sont créées avec lui ;
    // ensuite, elles se gèrent par les endpoints dédiés (ajout / clôture).
    public List<CouvertureDTO> couvertures = new ArrayList<>();

    // Renseignés par le serveur pour l'affichage en liste (couvertures actives)
    public String assureurActifNom;
    public String societeActiveNom;

    public boolean mauvaisPayeur;
    public boolean actif = true;

    public String nomComplet() {
        String n = nom == null ? "" : nom;
        String p = prenom == null ? "" : prenom;
        return (n + " " + p).trim();
    }
}
