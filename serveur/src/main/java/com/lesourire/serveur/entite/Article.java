package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lesourire.commun.dto.ArticleDTO;

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
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 150)
    private String marque;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fk_categorie")
    private CategorieArticle categorie;

    @Column(nullable = false, length = 30)
    private String unite = "unité";

    @Column(name = "quantite_stock", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantiteStock = BigDecimal.ZERO;

    @Column(name = "seuil_alerte", nullable = false, precision = 12, scale = 2)
    private BigDecimal seuilAlerte = BigDecimal.ZERO;

    @Column(name = "prix_achat_dernier", precision = 12, scale = 2)
    private BigDecimal prixAchatDernier;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public ArticleDTO versDTO() {
        ArticleDTO dto = new ArticleDTO();
        dto.id = id;
        dto.nom = nom;
        dto.marque = marque;
        if (categorie != null) {
            dto.categorieId = categorie.getId();
            dto.categorieLibelle = categorie.getLibelle();
        }
        dto.unite = unite;
        dto.quantiteStock = quantiteStock;
        dto.seuilAlerte = seuilAlerte;
        dto.prixAchatDernier = prixAchatDernier;
        dto.notes = notes;
        dto.actif = actif;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public CategorieArticle getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieArticle categorie) {
        this.categorie = categorie;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public BigDecimal getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(BigDecimal quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public BigDecimal getSeuilAlerte() {
        return seuilAlerte;
    }

    public void setSeuilAlerte(BigDecimal seuilAlerte) {
        this.seuilAlerte = seuilAlerte;
    }

    public BigDecimal getPrixAchatDernier() {
        return prixAchatDernier;
    }

    public void setPrixAchatDernier(BigDecimal prixAchatDernier) {
        this.prixAchatDernier = prixAchatDernier;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
