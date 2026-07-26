package com.lesourire.serveur.entite;

import java.time.LocalDateTime;

import com.lesourire.commun.Rappels;

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

    @Column(length = 255)
    private String destinataire;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "message_erreur", columnDefinition = "TEXT")
    private String messageErreur;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public Long getId() {
        return id;
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
}
