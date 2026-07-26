package com.lesourire.serveur.entite;

import java.time.LocalDateTime;

import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.TypeRdv;
import com.lesourire.commun.dto.RdvDTO;

import jakarta.persistence.Column;
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
@Table(name = "rdv")
public class Rdv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_patient", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_praticien", nullable = false)
    private Utilisateur praticien;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRdv type = TypeRdv.CONSULTATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutRdv statut = StatutRdv.PLANIFIE;

    @Column(length = 255)
    private String motif;

    @Column(name = "fk_acte_origine")
    private Long acteOrigineId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public RdvDTO versDTO() {
        RdvDTO dto = new RdvDTO();
        dto.id = id;
        dto.patientId = patient.getId();
        dto.patientNom = patient.nomComplet();
        dto.patientTelephone = patient.getTelephone();
        dto.praticienId = praticien.getId();
        dto.praticienNom = praticien.versDTO().nomComplet();
        dto.debut = debut;
        dto.fin = fin;
        dto.type = type;
        dto.statut = statut;
        dto.motif = motif;
        dto.notes = notes;
        dto.acteOrigineId = acteOrigineId;
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

    public Utilisateur getPraticien() {
        return praticien;
    }

    public void setPraticien(Utilisateur praticien) {
        this.praticien = praticien;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public void setDebut(LocalDateTime debut) {
        this.debut = debut;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public TypeRdv getType() {
        return type;
    }

    public void setType(TypeRdv type) {
        this.type = type;
    }

    public StatutRdv getStatut() {
        return statut;
    }

    public void setStatut(StatutRdv statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Long getActeOrigineId() {
        return acteOrigineId;
    }

    public void setActeOrigineId(Long acteOrigineId) {
        this.acteOrigineId = acteOrigineId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreePar(Utilisateur creePar) {
        this.creePar = creePar;
    }
}
