package com.lesourire.commun.dto;

import java.math.BigDecimal;

/** Catégorie du tarifaire (Consultation, Soins conservateurs…). */
public record CategoriePrestationDTO(
        Long id,
        String libelle,
        int ordreAffichage) {

    @Override
    public String toString() {
        return libelle;
    }
}
