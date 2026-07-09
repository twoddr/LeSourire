package com.lesourire.commun.dto;

import java.math.BigDecimal;

/** Société conventionnée (tiers payant). */
public record SocieteDTO(
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
