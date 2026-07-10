package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.lesourire.commun.dto.CouvertureDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Couverture d'un patient par un tiers payant sur une période.
 * Règle métier : jamais d'UPDATE des dates hormis la clôture (date_fin),
 * le trigger anti-chevauchement ne contrôlant que les INSERT.
 */
@Entity
@Table(name = "patient_couverture")
public class PatientCouverture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_patient")
    private Patient patient;

    @Column(nullable = false, length = 20)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_assureur")
    private Assureur assureur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_societe")
    private Societe societe;

    @Column(name = "numero_assure", length = 50)
    private String numeroAssure;

    @Column(precision = 5, scale = 2)
    private BigDecimal pourcentage;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "motif_fin")
    private String motifFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    public CouvertureDTO versDTO() {
        CouvertureDTO dto = new CouvertureDTO();
        dto.id = id;
        dto.patientId = patient.getId();
        dto.type = type;
        if (assureur != null) {
            dto.payeurId = assureur.getId();
            dto.payeurNom = assureur.getNom();
            dto.pourcentageEffectif = pourcentage != null ? pourcentage : assureur.getPourcentageDefaut();
        }
        if (societe != null) {
            dto.payeurId = societe.getId();
            dto.payeurNom = societe.getNom();
            dto.pourcentageEffectif = pourcentage != null ? pourcentage : societe.getPourcentageDefaut();
        }
        dto.numeroAssure = numeroAssure;
        dto.pourcentage = pourcentage;
        dto.dateDebut = dateDebut;
        dto.dateFin = dateFin;
        dto.motifFin = motifFin;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Assureur getAssureur() {
        return assureur;
    }

    public void setAssureur(Assureur assureur) {
        this.assureur = assureur;
    }

    public Societe getSociete() {
        return societe;
    }

    public void setSociete(Societe societe) {
        this.societe = societe;
    }

    public void setNumeroAssure(String numeroAssure) {
        this.numeroAssure = numeroAssure;
    }

    public void setPourcentage(BigDecimal pourcentage) {
        this.pourcentage = pourcentage;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public void setMotifFin(String motifFin) {
        this.motifFin = motifFin;
    }

    public void setCreePar(Utilisateur creePar) {
        this.creePar = creePar;
    }
}
