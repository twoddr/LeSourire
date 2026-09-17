package com.lesourire.serveur.entite;

import java.time.LocalDateTime;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
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
@Table(name = "rappel")
public class Rappel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_patient", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_rdv")
    private Rdv rdv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rappels.Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rappels.Canal canal;

    @Column(name = "date_prevue", nullable = false)
    private LocalDateTime datePrevue;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rappels.Statut statut = Rappels.Statut.EN_ATTENTE;

    @Column(nullable = false)
    private int tentatives;

    @Convert(converter = ChiffreurTexte.class)
    @Column(length = 512)
    private String destinataire;

    @Convert(converter = ChiffreurTexte.class)
    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Convert(converter = ChiffreurTexte.class)
    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public Long getId() {
        return id;
    }

    /** Vue échangée avec le client ; le lien d'envoi assisté est ajouté par le service. */
    public RappelDTO versDTO() {
        RappelDTO dto = new RappelDTO();
        dto.id = id;
        if (patient != null) {
            dto.patientId = patient.getId();
            dto.patientNom = patient.nomComplet();
            dto.patientTelephone = patient.getTelephone();
        }
        if (rdv != null) {
            dto.rdvId = rdv.getId();
            dto.rdvDebut = rdv.getDebut();
        }
        dto.type = type;
        dto.canal = canal;
        dto.statut = statut;
        dto.tentatives = tentatives;
        dto.datePrevue = datePrevue;
        dto.dateEnvoi = dateEnvoi;
        dto.destinataire = destinataire;
        dto.contenu = contenu;
        dto.messageErreur = messageErreur;
        return dto;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Rdv getRdv() {
        return rdv;
    }

    public void setRdv(Rdv rdv) {
        this.rdv = rdv;
    }

    public Rappels.Type getType() {
        return type;
    }

    public void setType(Rappels.Type type) {
        this.type = type;
    }

    public Rappels.Canal getCanal() {
        return canal;
    }

    public void setCanal(Rappels.Canal canal) {
        this.canal = canal;
    }

    public LocalDateTime getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDateTime datePrevue) {
        this.datePrevue = datePrevue;
    }

    public Rappels.Statut getStatut() {
        return statut;
    }

    public void setStatut(Rappels.Statut statut) {
        this.statut = statut;
    }

    public String getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public String getMessageErreur() {
        return messageErreur;
    }

    public void setMessageErreur(String messageErreur) {
        this.messageErreur = messageErreur;
    }

    public int getTentatives() {
        return tentatives;
    }

    public void setTentatives(int tentatives) {
        this.tentatives = tentatives;
    }
}
