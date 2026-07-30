package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.dto.PaiementDTO;

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

/**
 * Paiement encaissé sur une facture. Les triggers de la base recalculent
 * automatiquement les montants payés et le statut de la facture.
 */
@Entity
@Table(name = "paiement")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_facture")
    private Facture facture;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModePaiement mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Payeur payeur = Payeur.PATIENT;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recu_par")
    private Utilisateur recuPar;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public PaiementDTO versDTO() {
        PaiementDTO dto = new PaiementDTO();
        dto.id = id;
        dto.factureId = facture.getId();
        dto.datePaiement = datePaiement;
        dto.montant = montant;
        dto.mode = mode;
        dto.payeur = payeur;
        dto.reference = reference;
        dto.recuParNom = recuPar == null ? null
                : (recuPar.getNom() + " " + (recuPar.getPrenom() == null ? "" : recuPar.getPrenom())).trim();
        dto.notes = notes;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Facture getFacture() {
        return facture;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }

    public void setDatePaiement(LocalDateTime datePaiement) {
        this.datePaiement = datePaiement;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Payeur getPayeur() {
        return payeur;
    }

    public void setMode(ModePaiement mode) {
        this.mode = mode;
    }

    public void setPayeur(Payeur payeur) {
        this.payeur = payeur;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setRecuPar(Utilisateur recuPar) {
        this.recuPar = recuPar;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
