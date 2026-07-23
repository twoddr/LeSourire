package com.lesourire.commun.dto;

import java.math.BigDecimal;

/**
 * Prestation du tarifaire.
 * Tarifée soit en lettre-clé × coefficient (ex. D12), soit au forfait.
 */
public class PrestationDTO {

    public Long id;
    public String code;
    public String libelle;
    public Long categorieId;
    public String categorieLibelle;     // lecture seule (liste)
    public String lettreCle;            // "D", "Z" ou null si forfait
    public BigDecimal coefficient;
    public BigDecimal tarifForfait;
    public String notes;
    public boolean actif = true;

    /** Affichage court du tarif (ex. "D12" ou "15 000 XAF"). */
    public String tarifLibelle() {
        if (lettreCle != null && coefficient != null) {
            String coef = coefficient.stripTrailingZeros().toPlainString();
            return lettreCle + coef;
        }
        if (tarifForfait != null) {
            return tarifForfait.stripTrailingZeros().toPlainString() + " XAF";
        }
        return "";
    }
}
