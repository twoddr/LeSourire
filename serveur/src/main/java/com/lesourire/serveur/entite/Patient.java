package com.lesourire.serveur.entite;

import java.math.BigDecimal;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_assureur")
    private Assureur assureur;

    @Column(name = "numero_assure", length = 50)
    private String numeroAssure;

    @Column(name = "pourcentage_assureur", precision = 5, scale = 2)
    private BigDecimal pourcentageAssureur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_societe")
    private Societe societe;

    @Column(name = "pourcentage_societe", precision = 5, scale = 2)
    private BigDecimal pourcentageSociete;

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
        if (assureur != null) {
            dto.assureurId = assureur.getId();
            dto.assureurNom = assureur.getNom();
        }
        dto.numeroAssure = numeroAssure;
        dto.pourcentageAssureur = pourcentageAssureur;
        if (societe != null) {
            dto.societeId = societe.getId();
            dto.societeNom = societe.getNom();
        }
        dto.pourcentageSociete = pourcentageSociete;
        dto.mauvaisPayeur = mauvaisPayeur;
        dto.actif = actif;
        return dto;
    }

    /** Recopie les champs modifiables depuis la fiche reçue du client. */
    public void appliquer(PatientDTO dto, Assureur assureur, Societe societe) {
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
        this.assureur = assureur;
        this.numeroAssure = dto.numeroAssure;
        this.pourcentageAssureur = dto.pourcentageAssureur;
        this.societe = societe;
        this.pourcentageSociete = dto.pourcentageSociete;
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
