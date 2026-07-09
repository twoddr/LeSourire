package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // Prise en charge
    public Long assureurId;
    public String assureurNom;          // renseigné par le serveur (affichage)
    public String numeroAssure;
    public BigDecimal pourcentageAssureur;  // null = défaut de l'assureur
    public Long societeId;
    public String societeNom;           // renseigné par le serveur (affichage)
    public BigDecimal pourcentageSociete;   // null = défaut de la société

    public boolean mauvaisPayeur;
    public boolean actif = true;

    public String nomComplet() {
        String n = nom == null ? "" : nom;
        String p = prenom == null ? "" : prenom;
        return (n + " " + p).trim();
    }
}
