package com.lesourire.commun.dto;

import java.math.BigDecimal;

/** Assureur (tiers payant). */
public record AssureurDTO(
        Long id,
        String nom,
        String telephone,
        String email,
        BigDecimal pourcentageDefaut,
        boolean actif) {

    @Override
    public String toString() {
        return nom;
    }
}
