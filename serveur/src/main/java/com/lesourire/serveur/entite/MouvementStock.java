package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lesourire.commun.TypeMouvementStock;
import com.lesourire.commun.dto.MouvementStockDTO;

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
@Table(name = "mouvement_stock")
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_article", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeMouvementStock type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire", precision = 12, scale = 2)
    private BigDecimal prixUnitaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_fournisseur")
    private Fournisseur fournisseur;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_utilisateur")
    private Utilisateur utilisateur;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    public MouvementStockDTO versDTO() {
        MouvementStockDTO dto = new MouvementStockDTO();
        dto.id = id;
        dto.articleId = article.getId();
        dto.articleNom = article.getNom();
        dto.type = type;
        dto.quantite = quantite;
        dto.prixUnitaire = prixUnitaire;
        if (fournisseur != null) {
            dto.fournisseurId = fournisseur.getId();
            dto.fournisseurNom = fournisseur.getNom();
        }
        dto.dateMouvement = dateMouvement;
        dto.datePeremption = datePeremption;
        dto.reference = reference;
        dto.notes = notes;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public TypeMouvementStock getType() {
        return type;
    }

    public void setType(TypeMouvementStock type) {
        this.type = type;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public void setDateMouvement(LocalDateTime dateMouvement) {
        this.dateMouvement = dateMouvement;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
