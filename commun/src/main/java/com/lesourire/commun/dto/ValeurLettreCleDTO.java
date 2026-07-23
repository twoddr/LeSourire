package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valeur monétaire d'une lettre-clé sur une période. */
public record ValeurLettreCleDTO(
        Long id,
        String lettreCle,
        BigDecimal valeur,
        LocalDate dateDebut,
        LocalDate dateFin) {
}
