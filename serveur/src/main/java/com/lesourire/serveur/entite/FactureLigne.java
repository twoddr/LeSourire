package com.lesourire.serveur.entite;

import java.math.BigDecimal;

import com.lesourire.commun.dto.FactureLigneDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Ligne d'une facture, éventuellement adossée à un acte clinique. */
@Entity
@Table(name = "facture_ligne")
public class FactureLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_facture")
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_acte")
    private Acte acte;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private int quantite = 1;

    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    public FactureLigneDTO versDTO() {
        FactureLigneDTO dto = new FactureLigneDTO();
        dto.id = id;
        if (acte != null) {
            dto.prestationId = acte.getPrestation().getId();
            dto.prestationCode = acte.getPrestation().getCode();
            dto.dents = acte.getDents();
        }
        dto.designation = designation;
        dto.quantite = quantite;
        dto.prixUnitaire = prixUnitaire;
        dto.montant = montant;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }

    public Acte getActe() {
        return acte;
    }

    public void setActe(Acte acte) {
        this.acte = acte;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}
