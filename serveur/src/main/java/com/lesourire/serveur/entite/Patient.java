package com.lesourire.serveur.entite;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lesourire.commun.dto.PatientDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_dossier", nullable = false, unique = true, length = 20)
    private String numeroDossier;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 150)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(columnDefinition = "CHAR(1)")
    private String sexe;

    @Column(length = 30)
    private String telephone;

    @Column(name = "telephone_whatsapp", length = 30)
    private String telephoneWhatsapp;

    private String email;

    private String adresse;

    @Column(length = 150)
    private String quartier;

    @Column(length = 150)
    private String ville;

    @Column(length = 150)
    private String profession;

    @Column(name = "personne_urgence_nom", length = 150)
    private String personneUrgenceNom;

    @Column(name = "personne_urgence_tel", length = 30)
    private String personneUrgenceTel;

    @Column(columnDefinition = "TEXT")
    private String antecedents;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "mauvais_payeur", nullable = false)
    private boolean mauvaisPayeur;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public PatientDTO versDTO() {
        PatientDTO dto = new PatientDTO();
        dto.id = id;
        dto.numeroDossier = numeroDossier;
        dto.nom = nom;
        dto.prenom = prenom;
        dto.dateNaissance = dateNaissance;
        dto.sexe = sexe;
        dto.telephone = telephone;
        dto.telephoneWhatsapp = telephoneWhatsapp;
        dto.email = email;
        dto.adresse = adresse;
        dto.quartier = quartier;
        dto.ville = ville;
        dto.profession = profession;
        dto.personneUrgenceNom = personneUrgenceNom;
        dto.personneUrgenceTel = personneUrgenceTel;
        dto.antecedents = antecedents;
        dto.allergies = allergies;
        dto.notes = notes;
        dto.mauvaisPayeur = mauvaisPayeur;
        dto.actif = actif;
        return dto;
    }

    /** Recopie les champs modifiables depuis la fiche reçue du client. */
    public void appliquer(PatientDTO dto) {
        this.nom = dto.nom;
        this.prenom = dto.prenom;
        this.dateNaissance = dto.dateNaissance;
        this.sexe = dto.sexe;
        this.telephone = dto.telephone;
        this.telephoneWhatsapp = dto.telephoneWhatsapp;
        this.email = dto.email;
        this.adresse = dto.adresse;
        this.quartier = dto.quartier;
        this.ville = dto.ville;
        this.profession = dto.profession;
        this.personneUrgenceNom = dto.personneUrgenceNom;
        this.personneUrgenceTel = dto.personneUrgenceTel;
        this.antecedents = dto.antecedents;
        this.allergies = dto.allergies;
        this.notes = dto.notes;
        this.mauvaisPayeur = dto.mauvaisPayeur;
        this.actif = dto.actif;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroDossier() {
        return numeroDossier;
    }

    public void setNumeroDossier(String numeroDossier) {
        this.numeroDossier = numeroDossier;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getTelephoneWhatsapp() {
        return telephoneWhatsapp;
    }

    public String getEmail() {
        return email;
    }

    public String nomComplet() {
        String n = nom == null ? "" : nom;
        String p = prenom == null ? "" : prenom;
        return (n + " " + p).trim();
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public void setCreePar(Utilisateur creePar) {
        this.creePar = creePar;
    }
}
