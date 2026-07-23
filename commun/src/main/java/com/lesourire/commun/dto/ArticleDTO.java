package com.lesourire.commun.dto;

import java.math.BigDecimal;

/** Article de stock. */
public class ArticleDTO {

    public Long id;
    public String nom;
    public String marque;
    public Long categorieId;
    public String categorieLibelle;
    public String unite = "unité";
    public BigDecimal quantiteStock = BigDecimal.ZERO;
    public BigDecimal seuilAlerte = BigDecimal.ZERO;
    public BigDecimal prixAchatDernier;
    public String notes;
    public boolean actif = true;

    public boolean enAlerte() {
        return quantiteStock != null && seuilAlerte != null
                && quantiteStock.compareTo(seuilAlerte) <= 0;
    }
}
