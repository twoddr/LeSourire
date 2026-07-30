package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
 * Acte clinique réalisé sur un patient. Le coefficient et la valeur de la
 * lettre-clé sont copiés au moment de l'acte : les changements de tarif
 * ultérieurs ne modifient jamais un acte passé.
 */
@Entity
@Table(name = "acte")
public class Acte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_patient")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_praticien")
    private Utilisateur praticien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_rdv")
    private Rdv rdv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_prestation")
    private Prestation prestation;

    @Column(name = "date_acte", nullable = false)
    private LocalDateTime dateActe;

    @Column(length = 100)
    private String dents;

    @Column(nullable = false)
    private int quantite = 1;

    @Column(name = "coefficient_applique", precision = 8, scale = 2)
    private BigDecimal coefficientApplique;

    @Column(name = "valeur_lettre_appliquee", precision = 12, scale = 2)
    private BigDecimal valeurLettreAppliquee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Utilisateur getPraticien() {
        return praticien;
    }

    public void setPraticien(Utilisateur praticien) {
        this.praticien = praticien;
    }

    public void setRdv(Rdv rdv) {
        this.rdv = rdv;
    }

    public Prestation getPrestation() {
        return prestation;
    }

    public void setPrestation(Prestation prestation) {
        this.prestation = prestation;
    }

    public void setDateActe(LocalDateTime dateActe) {
        this.dateActe = dateActe;
    }

    public String getDents() {
        return dents;
    }

    public void setDents(String dents) {
        this.dents = dents;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setCoefficientApplique(BigDecimal coefficientApplique) {
        this.coefficientApplique = coefficientApplique;
    }

    public void setValeurLettreAppliquee(BigDecimal valeurLettreAppliquee) {
        this.valeurLettreAppliquee = valeurLettreAppliquee;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public void setCreePar(Utilisateur creePar) {
        this.creePar = creePar;
    }
}
