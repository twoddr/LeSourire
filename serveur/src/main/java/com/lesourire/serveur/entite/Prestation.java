package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lesourire.commun.dto.PrestationDTO;

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
@Table(name = "prestation")
public class Prestation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "fk_categorie", nullable = false)
    private CategoriePrestation categorie;

    @Column(name = "fk_lettre_cle", length = 5)
    private String lettreCle;

    @Column(precision = 8, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "tarif_forfait", precision = 12, scale = 2)
    private BigDecimal tarifForfait;

    @Column(length = 255)
    private String notes;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "cree_le", insertable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", insertable = false, updatable = false)
    private LocalDateTime modifieLe;

    public PrestationDTO versDTO() {
        PrestationDTO dto = new PrestationDTO();
        dto.id = id;
        dto.code = code;
        dto.libelle = libelle;
        dto.categorieId = categorie.getId();
        dto.categorieLibelle = categorie.getLibelle();
        dto.lettreCle = lettreCle;
        dto.coefficient = coefficient;
        dto.tarifForfait = tarifForfait;
        dto.notes = notes;
        dto.actif = actif;
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public CategoriePrestation getCategorie() {
        return categorie;
    }

    public void setCategorie(CategoriePrestation categorie) {
        this.categorie = categorie;
    }

    public String getLettreCle() {
        return lettreCle;
    }

    public void setLettreCle(String lettreCle) {
        this.lettreCle = lettreCle;
    }

    public BigDecimal getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(BigDecimal coefficient) {
        this.coefficient = coefficient;
    }

    public BigDecimal getTarifForfait() {
        return tarifForfait;
    }

    public void setTarifForfait(BigDecimal tarifForfait) {
        this.tarifForfait = tarifForfait;
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
