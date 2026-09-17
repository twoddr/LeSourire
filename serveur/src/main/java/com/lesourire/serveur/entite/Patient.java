package com.lesourire.serveur.entite;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lesourire.commun.CanalNotification;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.serveur.crypto.ChiffreurTexte;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    // -- Données identifiantes et médicales : chiffrées en base (AES-256-GCM) --
    @Convert(converter = ChiffreurTexte.class)
    @Column(nullable = false, length = 512)
    private String nom;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(columnDefinition = "CHAR(1)")
    private String sexe;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 255)
    private String telephone;

    @Convert(converter = ChiffreurTexte.class)
    @Column(name = "telephone_whatsapp", length = 255)
    private String telephoneWhatsapp;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String email;

    // -- Notifications : canal préféré + accord du patient pour être rappelé --
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_notification", nullable = false, length = 20)
    private CanalNotification canalNotification = CanalNotification.AUTO;

    @Column(name = "consentement_rappel", nullable = false)
    private boolean consentementRappel = true;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String adresse;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String quartier;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String ville;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String profession;

    @Convert(converter = ChiffreurTexte.class)
    @Column(name = "personne_urgence_nom", length = 512)
    private String personneUrgenceNom;

    @Convert(converter = ChiffreurTexte.class)
    @Column(name = "personne_urgence_tel", length = 255)
    private String personneUrgenceTel;

    @Convert(converter = ChiffreurTexte.class)
    @Column(columnDefinition = "TEXT")
    private String antecedents;

    @Convert(converter = ChiffreurTexte.class)
    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Convert(converter = ChiffreurTexte.class)
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
        dto.canalNotification = canalNotification == null ? CanalNotification.AUTO : canalNotification;
        dto.consentementRappel = consentementRappel;
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
        this.canalNotification = dto.canalNotification == null
                ? CanalNotification.AUTO : dto.canalNotification;
        this.consentementRappel = dto.consentementRappel;
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

    public CanalNotification getCanalNotification() {
        return canalNotification == null ? CanalNotification.AUTO : canalNotification;
    }

    public boolean isConsentementRappel() {
        return consentementRappel;
    }

    /** Numéro à utiliser pour joindre le patient sur WhatsApp. */
    public String numeroWhatsapp() {
        if (telephoneWhatsapp != null && !telephoneWhatsapp.isBlank()) {
            return telephoneWhatsapp;
        }
        return telephone;
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
