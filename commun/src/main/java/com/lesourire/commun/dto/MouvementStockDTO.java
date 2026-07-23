package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lesourire.commun.TypeMouvementStock;

/** Mouvement de stock (entrée, sortie…). */
public class MouvementStockDTO {

    public Long id;
    public Long articleId;
    public String articleNom;
    public TypeMouvementStock type;
    public BigDecimal quantite;
    public BigDecimal prixUnitaire;
    public Long fournisseurId;
    public String fournisseurNom;
    public LocalDateTime dateMouvement;
    public LocalDate datePeremption;
    public String reference;
    public String notes;
}
