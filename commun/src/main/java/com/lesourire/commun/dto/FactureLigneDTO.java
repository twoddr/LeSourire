package com.lesourire.commun.dto;

import java.math.BigDecimal;

/**
 * Ligne de facture : une prestation du tarifaire (qui crée l'acte clinique
 * correspondant) ou une ligne libre (désignation + prix saisis à la main).
 */
public class FactureLigneDTO {

    public Long id;
    public Long prestationId;           // null = ligne libre
    public String prestationCode;       // renseigné par le serveur (affichage)
    public String designation;
    public String dents;                // numéros FDI, ex. "16" ou "11,21"
    public int quantite = 1;
    public BigDecimal prixUnitaire;     // calculé par le serveur si prestation
    public BigDecimal montant;          // calculé par le serveur
}
